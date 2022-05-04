title=Kotlin's Context Receivers
date=2022-05-05
type=post
tags=struct,kotlin
status=published
headline=Kotlin's Context Receivers
subheadline=Might become a game changing feature
summary=Context Receivers are a preview feature in the latest Kotlin version and allow for additional receivers in declarations. I write about my experience with extensions in Kotlin and how context receivers might change the way we program in the language. 
image=images/Kotlin_Icon.png
~~~~~~
I am annoying people in my periphery since 2018 how great 
[*context receivers*](https://blog.jetbrains.com/kotlin/2022/02/kotlin-1-6-20-m1-released/#prototype-of-context-receivers-for-kotlin-jvm) 
will be and how they might change the style we program (in Kotlin). I eagerly read and commented in at least three language enhancement
proposals since back then and so I *had* to write something about the new feature and here it is - even though I 
probably can't tell you anything new or exciting that's not yet explained better by someone else.

At first, I only wanted to write a short reminder what this feature is about, preparing for a more interesting test drive I did with them in a library of mine.
But the more I tried to briefly introduce the feature, the more I had to think about discussions I had with colleagues
over the years, when they first encountered Kotlin's extensions, going from "oh no, this is not good, I like regular functions" to
"oh, this is like a local API, we're doing context driven programming here, it's so readable".

Let's start with a feature Kotlin has since ever:

## Extensions
Extensions do not only let you extend classes you don't own, but also cleanly seperate your data from behaviour,
your core domain from smaller side domains or API from non-API modules. More often than not, I wrote properties 
and functions as extensions, rather than as members. Short reminder how extensions work in Kotlin:
```kotlin
fun Person.getFullName(): String = "$firstname $lastname" // extension function declaration
val Person.fullName: String get() = "$firstname $lastname" // extension property declaration

println(Person().getFullName()) // extension function call
println(Person().fullName) // extension property call
```

Extensions have another interesting property: They automatically propagate. Notice how it's not necessary
to write `this.firstname` in the extension. It works just as we are used to in regular member functions.
Let's take a look at another phenomenal feature of Kotlin:

## Lambdas with receivers

Extension lambdas, so to say.

```kotlin
fun Person.introduceSelf(namePrinter: Person.() -> String) {
    println("Hi everyone! Let me introduce myself, I am ${namePrinter()}, nice to meet you!")
}

Person().introduceSelf { fullName } // we urge the user to decide what name should be used here
```

Of course, slightly odd example, as always, but the point is, that there is not much room to write it differently.
One could write the extension function to accept a parameter rather than a lambda.

```kotlin
fun Person.introduceSelf(name: String) {
    println("Hi everyone! Let me introduce myself, I am $name, nice to meet you!")
}
Person().run { introduceSelf(fullName) }
// or
val person = Person()
person.introduceSelf(person.fullName)
```

The new problem is, that the person introducing herself has to pass in the name, which is not ergonomic.
Of course, we can go further and just remove the extension receiver completely, because it is also
not needed for the `introduceSelf` function.

```kotlin
fun introduce(name: String) {
    println("Hi everyone! Let me introduce myself, I am $name, nice to meet you!")
}

val person = Person()
introduceSelf(person.fullName)
```

But now we lost something. Calling `person.introduceSelf()` is arguably more meaningful and ergonomic than 
`introduceSelf(name)`. There is a reason Kotlin allows for extensions. Because using dot notation and
calling functions *on* objects rather than *with* objects is more suitable in a lot of situations.


## Member Extensions

Member extensions are currently the only way to get at least one collaborateur into the mix when an 
extension receiver is needed.

```kotlin
class DefaultFullNameStrategy {
    val Person.fullName get() = "$firstname $lastname" // member extension property declaration
    fun Person.introduceSelf() { println("Hi everyone! Let me introduce myself, I am fullName, nice to meet you!") }
}
class ReversedFullNameStrategy {
    val Person.fullName get() = "$lastname, $firstname" // member extension property declaration
    fun Person.introduceSelf() { println("Hi everyone! Let me introduce myself, I am fullName, nice to meet you!") }
}

ReversedFullNameStrategy().run { // bring a strategy into scope, 'this' is now of type ReversedFullNameStrategy
    Person().introduceSelf() // member extension function call
}
```
Implementing the `introduceSelf` function in the NameStrategy doesn't make sense, or in other words: Going down this
road leads to unintentional coupling.

Now since the strategies don't have any state, we can simply make them object declarations.
Properties and functions on object declarations can be imported statically.

```kotlin
object DefaultFullNameStrategy {
    val Person.fullName get() = "$firstname $lastname" // member extension property declaration
    fun Person.introduceSelf() { println("Hi everyone! Let me introduce myself, I am fullName, nice to meet you!") }
}
object ReversedFullNameStrategy {
    val Person.fullName get() = "$lastname, $firstname" // member extension property declaration
    fun Person.introduceSelf() { println("Hi everyone! Let me introduce myself, I am fullName, nice to meet you!") }
}

import ReversedFullNameStrategy.introduceSelf // import the 'static' extension function
Person().introduceSelf() // member extension function call
```

Using object declarations for namespacing is convenient, but it doesn't give much: People will now import
the extension to circumvent using a scoping function and the new scope of the extension is your whole file.
For static functionality, that might be sufficient, but then, when is it favorable over a simple top level extension?

## Context receivers to the rescue

Whenever there is functionality that is a dependency for some other functionality we have two options.
Dependency injection or statically referencing the functionality in the implementation (ignoring nasty 
thread local hacks and sorts). With context receivers, declarations can get additional types attached, that are
expected to be present as a context on the call site. Let's take a look at the first example again, using that
feature:

```kotlin
interface NameStrategy {
    val Person.fullName: String
}
class DefaultFullNameStrategy: NameStrategy {
    val Person.fullName get() = "$firstname $lastname" // member extension property declaration
}

context(NameStrategy)
fun Person.introduceSelf() {
    println("Hi everyone! Let me introduce myself, I am $fullName, nice to meet you!")
}
DefaultFullNameStrategy().run {
    Person().introduceSelf()
}
```
We have now fulfilled the interface segregation guideline and no function pollutes the NameStrategy interface.
The call is now ergonomic because we don't have to pass in strange parameters. But we also urge the
user to provide a strategy and limit that to a scope, rather than having him import sth that is used on the file level.
Sure, one can complain about the syntax of bringing the context into scope here, but read on.

In a real world application, there is likely more than one such context. For example
there is almost always a logging context, a config context and maybe a transaction context. Even more interesting
are scopes of effects, for example a scope that can catch IO errors and automatically return a Result<T, IOError>.
So the application would rather look like

```kotlin
with(
    DefaultNameStrategy(),
    LoggingContext(),
    ConfigContext(),
) {
    Person().introduceSelf()
}

context(NameStrategy,LoggingContext,ConfigContext)
fun Person.introduceSelf() {
    val timestamp = if(logTimestamps) now() + " " else "" // uses ConfigContext
    // uses LoggingContext and NameStrategy
    log("${timestamp}Hi everyone! Let me introduce myself, I am $fullName, nice to meet you!")
}
```

As do extension receivers, context receivers automatically propagate.

```kotlin
calss LoggingContext {
    context(ConfigContext)
    fun log(text: String) {
        val timestamp = if(logTimestamps) now() + " " else "" // uses ConfigContext
        log("${timestamp}$text")
    }
}

context(NameStrategy,LoggingContext,ConfigContext)
fun Person.introduceSelf() {
    // uses LoggingContext and NameStrategy, automatically propagates ConfigContext
    log(Hi everyone! Let me introduce myself, I am $fullName, nice to meet you!")
}
```

That means bringing dependencies into scope happens very rarely. Ideally, you only
do it once, in your main method. Reminds you of something? Exactly. This is what
dependency injection frameworks do for you: They take an entry point, gather all
the dependency declarations and wire everything together. Most prominently
via constructor injection. Good news, contextual classes are part of the [language design
document](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md#contextual-classes-and-contextual-constructors)
and even implemented already in the prototype. But I will now stop writing about
this feature and how it might replace runtime dependency injection frameworks
with static injection and instant compiler errors whenever your context misses a depdendency.
Because first, people will claim overusage of the feature and second, I don't know how well
that works in practice until I tried it myself. But this will surely happen and I will probably write about it then.

Last but not least, my example from above would be bad code according to the 
[official design document](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md#designing-context-types).
This is because it suggest that a bunch of contexts should be combined into bigger contexts.
The example uses only interfaces as scopes, so that could be done easily by defining a subinterface.
This problem and solution remind me a lot of regular function parameters and
parameter objects. As long as the merged scope is a subtype of all the other contexts, scope propagation also works 
when only dependency to a sub context is declared, which is nice. Means you can write modules and group things,
but depend on and inject only what's needed. Can't wait to try that.  

But the next post will first be about how context receivers made a library of mine better.

*P.S: No "Scala had it before it was cool" this time!!*