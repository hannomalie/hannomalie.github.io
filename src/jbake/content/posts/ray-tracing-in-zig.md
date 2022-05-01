title=Using Zig
date=2022-05-01
type=post
tags=zig,raytracing,rendering
status=published
headline=Using Zig
subheadline=To implement ray tracing in one weekend
summary=I did the ray tracing in one weekend workshop using the zig language. 
image=images/raytracing_zig.png
~~~~~~
Turns out that I never implemented a simple ray tracer completely from scratch, besides having implemented countless
advanced techniques in computer graphics. It was always that I thought _nah, it's to much effort, you don't have time for that,_
_just use your time for something where you can get some results_.
With [Ray Tracing in one weekend](https://raytracing.github.io/books/RayTracingInOneWeekend.html), you get a decent
chance to implement a basic ray tracer in one weekend - or as I did in ~15 blocks of 20 minutes each.
Additionally, the post contains nice beginner friendly explanations and milestones where you can make a pause.
Side note, it's written by some of **the** masters of computer graphics, [Peter Shirley](https://en.wikipedia.org/wiki/Peter_Shirley),
so many kudos for him. Really a perfect workshop for every coder, no excuses!

To further compress what I can achieve in a very small amount of time, I decided to use the [zig language](https://ziglang.org/) to implement the tracer.
Zig aims to be a better C, by being like C without warts like the preprocessor.

## (Zig) Insights
I initially planned to write about my (not so good) experience with the language, despite being very curious about it.
Then I realized there are a lot of comments and discussions, love letters and rants about Zig all over the internet (surprise),
like [this blog post](https://www.duskborn.com/posts/2021-aoc-zig/) and I concluded the world doesn't nee another (incomplete) take on it.
So I just went for a few key insights the exercise gave me: 

### A language not good for explorative development
For me, the most problematic, most frustrating thing of this little exercise was being unable to deactivate Zig's strict
variable usage checking. Leading to situations like:

I didn't know why my code was rendering garbage. So I commented out some complex calculations and returned a static float instead.
But the code wouldn't compile, because of unused variables.
This problem intensified after I structured the code a bit and introduced a lot of small functions. And boy, I can just say
it's such a downer for a newbie. Given that refactoring support in IDEs is close to non-existent, it's really much worse than it needed to be.
Cherry on top of the cake are long discussions like in [this big ticket](https://github.com/ziglang/zig/issues/3320), containing
a statement from the language creator: "There will be no sloppy mode".
I have a lot of respect for the decision and sticking to the python-like style of a single canonical way of doing things in the language,
but I am also really curious about how such things affect the adoption and success of Zig in the future.

### My development speed depends on the tooling
I am mainly working on the JVM for a decade now. So I got used to having the best-in-class tooling, consisting of flawless
autocompletion, static typing and instant editor feedback, mighty refactorings and dependency management.
Every time I try out a young language like Zig (or Rust before that), I am kind of shocked how much my development speed 
relies on instant, complete editor feedback and auto completion. The (unofficial) Zig IDEA plugin was constantly crashing, the language server
was lacking a *lot* of features I am used to, so no wonder that I felt like a one-legged sloth while programming.
The lack of extensions in Zig makes it worse - for example in Kotlin, using unknown APIs is very nice because of 
[heavily utilizing scopes](https://kotlinlang.org/docs/lambdas.html#function-literals-with-receiver) and 
[extensions](https://kotlinlang.org/docs/extensions.html) (another feature that will [probably never get into Zig](https://github.com/ziglang/zig/issues/1170)).

### Somewhere I already heard these discussions
This could also mean I am getting old and grumpy, but I can't get rid of the feeling of seeing the same discussions about programming
language features over and over again. Let's take operator overloading as yet another example of features Zig won't 
implement (or will it? I don't uderstand the state of [these tickets](https://github.com/ziglang/zig/issues/871) tbh).
I suffered discussions about why operator overloading is bad and so so complicated to understand multiple times when a language came up
and threatend Java: C#, Groovy, Scala, Kotlin etc pp. No, I don't think operator overloading has to be done like in Scala, and no I don't think
operator overloading leads to bad code, the opposite is the case for me. I feel like I reached the end of all these 
discussions long ago (which is probably a bit arrogant) and so I was simply annoyed of Zig lacking operator overloading when implementing something using math.

### comptime reminds me of Scala 3 macros
Even after reading excessively about Scala 3 for a long time, I didn't realize how similar Zig's comptime 
and [Scala 3's inline macros](https://docs.scala-lang.org/scala3/guides/macros/inline.html) are actually.
I am sure that Scala 3 got inspired by Kotlin's inline functions, that enable reified generics (and took that to the next level), allthough
Scala 2 also already had very powerful inlining capabilities with AnyVal subclasses and additional compiler optimizations.
I think these approaches will be how modern languages implement a lot of features that would either require a language in a language
and kill IDE support (preprocessor, macros) or runtime support killing performance (reflection).
Zig's approach seems to be *the* way to remove the biggest wart of C, so really, who had this idea before Zig, there must be someone?

### Build tool in the project language
I am a big advocat of Gradle for years (knowing it is polarizing as hell for sure), because it's just so nice to have an extensible build tool
that can be used for any automation you need in the project context and uses the same language as your project does.
Zig is even the single last missing step ahead that I whish Gradle implements for years: requiring zero dependencies, while still working on every platform.
In order to execute zig build scripts (they are zig files), you need the zig executable. In order to compile your project, you need the zig executable.
It's so small that you can check it into your repository and you get a zero dependency build, which is *awesome*. In Gradle, the last hurdle is
that the repository user must have Java installed (and often in a non-latest version), everything else can be downloaded.
When zig officially has a dependency management system, I consider Gradle officially dethroned :)

### Closing words
Even though I can also complain a lot about Zig, I *love* what it is, how it's done and the pace it's evolving at.
There are so many good things in Zig. 
Even though it also lacks a lot of things I am used to, I think it gives so much development joy compared to languages like C and C++. 
I am looking forward to using it in a few years after it got stable.

