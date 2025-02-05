title=Lit with Kotlin
date=2025-02-05
type=post
tags=code,web
status=published
headline=Lit with Kotlin
summary=I use a custom string literal compiler plugin to connect Kotlin JS to Lit.
image=images/Kotlin_Icon.png

~~~~~~
## Lit - with Kotlin

I recently did [some experiments](https://hannomalie.github.io/posts/ktx.html) using a compiler plugin for 
custom string literals to have something like JSX, but for Kotlin JS. I came to realize that [Lit](https://lit.dev/docs/)
is much more suitable for that purpose, because in theory it is sufficient to map Javascript's custom
string literals to Kotlin's and boom, everything should work, because Lit already does all other stuff for us.

And indeed, it works.

I was able to pull this off:

```kotlin
class SimpleNumber : LitElement() {
    var number: Int = 0
        set(value) {
            field = value
            requestUpdate()
        }

    override fun render(): dynamic {
        return _html("<button @click=${ { number++; } }>$number</button>")
    }
}

customElements.define(HtmlTagName("simple-number"), SimpleNumber::class.js)

val result0 = _html("<div>${SimpleNumber(number)}</div>")
val result1 = _html("<div><simple-number></simple-number></div>")
```

### The glue between Lit and Kotlin: Terpal

Just like my JSX/KTX experiments, this one builds on top of the excellent 
[Terpal compiler plugin](https://github.com/exoquery/terpal) for Kotlin,
which gives you custom string literals.

So when using string literals like "foo${bar.baz}", a defined function will be called with an array
of string parts and values instead of the default interpolated string.
This is exactly what Javascript's string literals do, which is in turn exactly what the [_html_ function](https://lit.dev/docs/components/rendering/)
from Lit accepts.

And this is where even simple things start to get hairy.

First, you need to find out what _exactly_ is the shape of the things the html function needs. For that,
you go into the Lit sources and read the TypeScript definitions. It's a _TemplateStringsArray_. And it has a
property _raw_. Easy. In Kotlin you declare:

```kotlin
external interface TemplateStringsArray {
    var raw: Array<String>
}
```
And the html function as
```kotlin
external fun html(a: TemplateStringsArray, b: Array<Any?>): dynamic
```

But.

This will fail at runtime. Because in Lit there is a check that the TemplateStringsArray is of type array.
Well, in Kotlin, Array is not an open class, which means you cannot inherit from it. So there is no way to extend
the Array class, which is how the Javascript array class is represented.

We need to help ourselves by defining it in JavaScript:

```javascript
class TemplateStringsArray extends Array {
}
window.TemplateStringsArray = TemplateStringsArray // so that it can be used everywhere
```

In order to construct our _TemplateStringArray_, we resort to a helper function that bypasses Kotlin's typesystem again:

```kotlin
fun TemplateStringsArray(array: Array<String>): TemplateStringsArray {
    return js("new TemplateStringsArray()").unsafeCast<TemplateStringsArray>().apply {
        this.asDynamic()["raw"] = array
        val x = this
        array.forEach {
            x.asDynamic().push(it)
        }
    }
}
```

Test-driving it with a simple div containing a string from a property works.

But.

It's not correct, it renders an array value. And if I use more than one parameter it throws an exception. That's because
the html function doesn't actually take two arrays as parameters.
`external fun html(a: TemplateStringsArray, vararg b: Any?): dynamic` does the job - the values are passed as
multiple single parameters.

The Terpal interpolator can then look like this, utilizing the splat operator:

```kotlin
class LitInterpolator : Interpolator<Any, dynamic> {
    override fun interpolate(parts: () -> List<String>, params: () -> List<Any>): dynamic {
        val params: Array<Any?> = params().toTypedArray()
        return html(TemplateStringsArray(parts().toTypedArray()), *params)
    }
}

val _html = LitInterpolator()
```

### Kotlin -> ES classes

Kotlin compilation to Javascript normally results in a lot of functions, like Javascript worked back then.
However, Lit needs proper classes to work in a [component-oriented way](https://lit.dev/docs/components/defining/).
Otherwise, you would need to use the [standalone-templates approach](https://lit.dev/docs/libraries/standalone-templates/)
which is also very cool but makes organizing a complex user interface less good. Maybe I will explore that one
in the future. Kotlin support compilation to ecma script classes, but it's experimental and as I experienced
has a lot of bugs. For example a simple class definition like

```kotlin

class SimpleGreeting: LitElement() {
    val number: Int = 13
    var foo: String = "everyone"

    override fun render(): dynamic {
        return _html("<div><div>hello $foo</div>${ SimpleNumber(number) }<button @click=${ { foo = "bar" } }>Click Me</button></div>")
    }
}
```

doesn't work - the _foo_ property is completely empty, it's not initialized. When I add an _init {}_ block, I came to
conclusion that it's never executed, not even throwing an exception shows up. The number property is not 
properly passed to the SimpleNumber instance, which is also only a wrapper that in turn does not reflect the increment
of the number without me explicitly calling `requestUpdate()` in the property setter, as shown in the very first
code snippet. However, this doesn't help with the initial rendering - you remember, the init block didn't work,
and adding an `apply {}` to the property also doesn't work for some reason.

Furthermore. Even when the properties would work without problems, Lit expects us to list reactive properties in
a static function like `static properties = {foo: {type: String}}`. In theory, you can simply define Javascript
statics in Kotlin by writing a companion object. That doesn't work with the ecma script compilation, the methods
are simply not generated.

Ok, we're not beginners here, we can mitigate all that by using one of the single greatest features only Kotlin
offers: [delegated properties](https://kotlinlang.org/docs/delegated-properties.html#property-delegate-requirements).
In theory that will make our classes look like this:

```kotlin
class SimpleGreeting: LitElement() {
    var foo by State("everyone")
    fun render() {}
}
```

with a simple delegate implementation like this

```kotlin
data class State<T>(@JsName("underlying") var underlying: T) {
    operator fun getValue(thisRef: LitElement, property: KProperty<*>): T = underlying
    operator fun setValue(thisRef: LitElement, property: KProperty<*>, value: Any?) {
        underlying = value as T
        thisRef.requestUpdate()
    }
}
```

But it also doesn't work. It complains about some methods on undefined blah blah. No way to get it to work.

### Closing words

There are too many things that don't work with Kotlin compilation to Javascript classes. Other than that, I have
high hope that using the standalone-template approach of Lit works much better. The custom string literals
in Kotlin work very well with a framework like Lit, that doesn't need too much more than that.

I misused the repository from last time, it is [here](https://github.com/hannomalie/ktx), but you have to take a look at the branch 
[__lit__](https://github.com/hannomalie/ktx/tree/lit).
