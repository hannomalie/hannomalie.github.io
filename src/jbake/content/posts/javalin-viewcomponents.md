title=ViewComponents made easy (Javalin, Kotlin, Mustache)
date=2025-03-09
type=post
tags=code,architecture,web
status=published
headline=ViewComponents made easy
subline=with Javalin, Kotlin and Mustache
summary=I show you how the ViewComponents concept can easily be implemented with Javalin, Kotlin and Mustache

~~~~~~

It's been a while since I dveloped Ruby on Rails for money (over ten years ago, my god, time flies). And lately
[this project](https://github.com/tschuehly/spring-view-component) remineded me of something that isn't as popular
on the JVM at all: ViewComponents. As always, I have the impression that everything that gets implemented in the Spring ecosystem
is a bit more complicated than it needs to be (which is absolutely no critique of the linked project, that guy
and the project are totally awesome), so I wanted to take a look at what JVM has to offer in order to get the
concept to fly with as little complications and complexity as possible.

### ViewComponents

I don't think there exists some agreed upon definition of what exactly ViewComponents are, but 
[here](https://viewcomponent.org/) and [here](https://learn.microsoft.com/en-us/aspnet/core/mvc/views/view-components?view=aspnetcore-9.0)
are two examples from RoR and Microsoft what they are about. One example shows to have the actual data passed to
the rendering through one single instance as members, one shows the rendering by passing the data as parameters.

One way or the other, the point is that components are as self contained as possible. So they know how to get rendered,
they know which data is needed to get rendered, the template and the data types reside closely to or inside the component.

### File locations

Let's start with one thing that is super common in Node projects and the developers in those projects also often
complained in talks with me about the (default) folder structure of JVM projects. You know, that Maven standard
directory layout, seperating everything in sources and resources at an upper level in the filesystem. I came to realize
that it's indeed kind of an anti-pattern. We seperate things that should naturally be colocated in the same folder.
For example a controller class and a template file. Why can't they just reside next to each other?

Of course it's for technical reasons and simplicity from a certain point of view. Resources need to get embedded into
the resulting artifact (jar file). So it's easier to say "here's one folder, by convention it contains only resources,
embedd them all". Because when you put resources next to the Java files (or Kotlin, or Scala, you see, it already
gets a bit complicated), you would also need to at least exclude those files, otherwise they will get embedded
into the jar, which is almost never what you want to do.

Fortunately, there is not really much preventing us from ditching the standard Maven directory layout. Gradle is super
awesome to get done what we want:

```kotlin
sourceSets {
    main {
        kotlin {
            val dirsContainingSources = project.rootDir.resolve("src/main/kotlin").listFiles { it ->
                it.isDirectory && it.listFiles()!!.any { it.extension == "kt" }
            } ?: emptyArray()
            setSrcDirs(listOf("src/main/kotlin") + dirsContainingSources)
        }
        resources {
            val dirsContainingTemplates = project.rootDir.resolve("src/main/kotlin").listFiles { it ->
                it.isDirectory && it.listFiles()!!.any { it.extension == "mustache" }
            } ?: emptyArray()
            setSrcDirs(listOf("src/main/resources") + dirsContainingTemplates)
            include { it.file.extension == "mustache" }
        }
    }
}
```

Please someone tell me again how gradle is unnecesary - it enables this usecase quite simply, while other build tools
just can't implement it at all. Kotlin syntax and std library makes the task really plaesent.

Normally I don't think I would need to do the configuration of the source dirs. It's only that IntelliJ treats
subfolders in src as resourc folder only and marks a lot of code in the IDE red, while gradle still builds fine.
So normally, one would only need the second half of the code. Note that one could exclude source files, so blacklisting
instead of whitelisting.

When you do the above, you can easily have a directory layout of

```
src|main|kotlin
        |item|Item.kt
             |Item.mustache
        |feature|Feature.kt
                |Feature.mustache
```

which I sometimes whish would be the default for JVM projects. But the standard layout is for valid reasons different,
so people will be surprised by that. Yet, for ViewComponents, it is ... let's say close to necessary.

### Rendering

We need to render stuff. The above mentioned directory layout and template files at all are not necessary. But let's 
take a look at that option first. With mustache, it's as simple as this:

```kotlin
val mustache: MustacheFactory = DefaultMustacheFactory()

open class Renderable<T>(templateFile: String? = null) {
    val templateFileName: String = templateFile ?: (this.javaClass.simpleName + ".mustache")
    open val templateText: String by lazy { this.javaClass.classLoader.getResource(this.templateFileName)!!.readText() }

    fun render(context: T): String {
        val mustache = mustache.compile(StringReader(templateText), templateText)
        val outputStream = ByteArrayOutputStream()
        val writer = OutputStreamWriter(outputStream)
        mustache.execute(writer, context)
        writer.flush()

        return String(outputStream.toByteArray())
    }
}
```

And there is your convenience class that automatically loads a template by name, based on a convention that ViewComponent
and template have the same one. Can easily be overriden by overriding the templateText. That's where option two comes
into play: Templates in code. Option 2a is simply using Kotlin's raw strings. You could even completely ditch
templating engines all together, if you don't need it. Otherwise here's some base mustache, circumventing partials
by just inlining some other ViewComponent's template string:

```kotlin
class ItemsInline: Renderable<ItemsHolder>() {
    override val templateText: String get() {
        return """
            {{#items}}
${ItemRenderable().templateText}
            {{/items}}""".trimIndent()
    }
}
```

What's especially nice is that you can just change the code in the propertie accessor, recompile and the JVM
can hot reload the change, so you have it visible in the browser in no time, it just works, no tools needed etc.

Option 2b: Kotlin has also the power of nice DSLs, like [this one for html](https://github.com/Kotlin/kotlinx.html), which could
be even nicer.

### Routing

Finally, you need to wire ViewComponents to a path somehow. I always benchmark against Javalin, as it is so simple
and yet so well designed and flexible.
While the path _could_ be part of the component, I don't
see much value in that and left it in the default routing, which then is as simple as that:

```kotlin
[...]
val app = Javalin.create()
    .get("/") { ctx -> ctx.result("Hello World") }
    .get("/items") { ctx -> ctx.result(Items().render(itemsHolder)) } // relevant line is this
    .start(8080)
```

Javalin also has a concept called [CrudHandler](https://javalin.io/documentation#handler-groups) which can
help write components.

### Verdict

See the code [here](https://github.com/hannomalie/javalin-viewcomponents) and as always, keep in mind that it's
only a proof of concept, incomplete and might miss a lot of things.

All in all, it's very tempting to think one doesn't need template files or template engines anymore at all, given powerful tools
like Kotlin raw strings or DSLs and the simplicity of frameworks. The fact that that stuff works with hot reloading out of
the box is nice. Given that DSLs are simply compiled code, they are also super fast. And you can use constructs
of the programming language in them. I think it should be the default and only get replaced when there are solid
reasons for it. Not sure if it needs a fancy name like _ViewComponents_ at all. I think it's just straight-forward code.