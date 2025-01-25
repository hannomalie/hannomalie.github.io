title=KTX
date=2025-01-25
type=post
tags=code,web
status=published
headline=KTX
subline=JSX for Kotlin
summary=I did an experimental implementation of sth. like JSX for Kotlin JS.
image=images/Kotlin_Icon.png

~~~~~~
## KTX - JSX for Kotlin

Kotlin's HTML DSLs are extremely nice. But when writing UIs for the web, people are used to HTML.
Not being able to use HTML directly in the code like JSX or TSX offers, is a serious drawback for a lot
of people. So I tried to find a way to offer it in Kotlin JS, so that for example React can be used
more similar to as it is in Javascript projects.

This is what I enabled:

```kotlin
 val CustomButton = KtxFC<Props>("CustomButton") {
     val (count, setCount) = useState(0)

     ktx("<button onClick=${{ setCount(count + 1) }}>$count</button>") ()
 }
 external interface HelloProps : Props {
     var name: String
 }
 val Hello = KtxFC<HelloProps>("Hello") { props ->
     ktx("<div><div>Hello ${props.name}!</div><CustomButton /><CustomButton /><CustomButton /></div>") ()
 }


fun main() {
    createRoot(document.getElementsByTagName("body")[0]).render(
        ktx("<Hello name='you'>").create()
    )
}
```

### How it's done

Work is based on the excellent [Terpal compiler plugin](https://github.com/ExoQuery/Terpal) that allows
you to have advanced string interpolation in Kotlin. It gives you a way to override the default behaviour
of string interpolation - for example when you have a String `"a + b = $c"`, then you can implement an
interpolator function that receives parameters for the string parts and the actual instances of the parameters
you pass in.
Afterwards, you can use it like `myInterpolator("a + b = $c")`. It opens the door to just embed raw HTML
into your code as string, while being able to pass in callbacks and other properties just like into any kotlin DSL.

Okay, now the heavy part. Just like the JSX compiler, you need to process that input somehow. I do it in the
interpolator, where you implement a function of the signature

`override fun interpolate(parts: () -> List<String>, params: () -> List<Any>) = FC<Props> {`

As you can see, it returns an `FC<Prop>`, which is Kotlin's official wrapper for React function components.
In order to get to that from a bunch of strings and some params, we nead heavy machinery.

> **_NOTE:_**  Of course I am not
able to implement a fully functional solution, I can only do some proof of concept that is nowhere near production
usability.

When I started, I implemented a small parser and just parsed the HTML string basically char by char. That's very tedious,
and took too much time, so I switched to using what the dom api already offers out of the box:

`val currentSnippet = web.dom.parsing.DOMParser().parseFromString(completeString, DOMParserSupportedType.textXml)`

But for that to work, we need to feed in actually valid HTML, which our string parts definitly aren't, remember,
all our parameters are missing, they are not yet stirngs but still objects.

So I used a quick hack and replaced different types of properties like this:

```kotlin
parts().forEachIndexed { index, part ->
    completeString += part

    if(index < params.size) {
        when(val param = params[index]) {
            is String -> completeString += param
            is Int -> completeString += param
            is KFunction<*> -> completeString += "\"\""
            // TODO: Support more types
        }
    }
}
```

This naive way will surely hit limitations, but for the example it works.

We can then recursively handle all elements.

```kotlin
currentSnippet.children.iterator().forEach { child ->
    handleChild(child, params)
}
```

While the handle method is

```kotlin
fun ChildrenBuilder.handleChild(child: Element, params: List<Any>) {
    // React/JSX convention has custom components always be uppercase
    val tagIsLowerCase = child.tagName.lowercase() == child.tagName
    if (tagIsLowerCase) {
        if (child.tagName == "button") {
            val onClickOrNull = params.firstOrNull()

            button {
                onClickOrNull?.let { _onClick ->
                    onClick = {
                        (_onClick.unsafeCast<() -> Unit>())()
                    }
                }
                +child.textContent
            }
        } else {
            IntrinsicType<PropsWithClassName>(child.tagName)() {
                // TODO: Passing an empty list is not appropriate here, we need to find out
                // the list of attributes that go to the child
                val hasAnyChildren = handleChildren(child, emptyList())
                if (!hasAnyChildren) {
                    +child.textContent
                }
            }
        }
    } else {
        val element = globalThis[child.tagName]
        ((element.unsafeCast<IntrinsicType<PropsWithClassName>>()) {
            child.attributes.iterator().forEach { attribute ->
                this.asDynamic()[attribute.name] = (attribute.value)
            }
        })
    }
}
```

Again, not code to be proud of for multiple reasons. First, it leaves open a lot of edge cases. Would take time
to implement a fully functional version. Second, the differentation should only be between custom and standard
tags. For _button_, I added a branch because it let me easily support passing and using onClick handler without
having a better implementation of the parameter stuff I mentioned above. Then, the whole project abuses
the Kotlin react wrapper DSL. As you can see, it's not intended to be used in an abstract way, but rather
to just write simple react comopnents. Using the DSL for higher order prposes is a PITA and it took me some time
to figure out how it works.

But you get the idea. It works. And as long as we produce valid Kotlin react wrapper objects, live is not too bad.

### Already known problems

#### I can't hide the react dsl

Noticed how the result of the `ktx` function call needs to be applied once again? That's because the function
returns a functional component and that one needs to be called in the function body that is the function
that builds your function component. Wait, whoot? Yes. It's brainfuck. You need to read it multiple times in order
to understand it. I could easily hide that wart behind a wrapper extension function and it would be completely
invisble, but I experienced some issue with the Terpal compiler plugin, so was not able to pull it off.

#### Can't use delegated properties

Instead of `val CustomButton = KtxFC<Props>("CustomButton") { }` I could normally easily provide possibility to do
`val CustomButton by KtxFC<Props> { }` so that one doesn't need to redefine the name of the tag, as well as have
it automatically block the identifier for the scope and give a compiler warning. In Kotlin JS, there seems to be an
issue with property delegation providers, it just didn't work for me.

#### It's not a compiler

Good news: You don't need the hell of brittle tools that you need in Javscript projects to pull of JSX. Bad news one:
We're not directly embedding HTML syntax in our programming language, so this is not a fair comparison and we would
need to pull of a solution with Javascript custom string literals. Yes, Javascript can do that, officially, without
compiler plugins, 1 : 0 for Javascript. Bad news two: we're doing eveything at runtime, not at compile time 
(Only the transformation from regular string interpolation with custom interpolator functions is actually done at
compile time). That means it will cost you resources at runtime. I have no idea how much, I haven't looked at it.

## Closing words

I know React is popular, but there's also __[Lit](https://lit.dev/docs/)__ which could probably get resembled
in Kotlin with a string interpolation compiler plugin much easier than React. Maybe in the future I can take a closer
look at Lit and try something similar than I just did with the react wrapper. TBH I don't really think there is
a chance a JSX-like solution is able to get to the production readiness people expect when they use anything related
to React - not matter if Kotlin or Javascript or Typescript.

The repository is [here](https://github.com/hannomalie/ktx), in case the above code was not enough to keep you out :)
