title=On Clean Architecture (Part 2)
date=2024-09-11
type=post
tags=architecture
status=draft
headline=On Clean Architecture (Part 2)
subheadline=The unspoken cost of dependency inversion
summary=Dependency inversion managed to get very close to becoming everyone's silver bullet. We need to talk about the costs it imposes and why we should use it less.
image=images/soap.jpeg

~~~~~~
The second entry in the series. Find the first one [here](https://hannomalie.github.io/posts/on-clean-architecture-part-1.html).

## TLDR

- Close to always, reusing a costly instance is the real valid reason to use dependency inversion, or better said dependency injection
- Coding against abstractions by default is one of the most harmful things to do
- DI is often applied to enable mockist style testing, which I consider a worst practice

## On the Dependency Inversion Principle

One of those things I experienced people tend to treat as some universal silver bullet and solution to everything:
__Dependecy inversion__...
In one of the earlier chapters _(11, Design Principles)_, the book introduces it as

> [...] tells us that most flexible systems are those in which source code dependencies refer only to abstractions, not to
concretions"

And then starts by showing what kind of dependencies should not be avoided because of their _stability_, for example
standard library or operating system functions. So very good, it emphasizes _stability_ as the main criteria.

And now up to the problems. It continues by stating 

> It is the volatile concrete elements of your system [...]. Those are the modules that we are actively developing,
and that are undergoing frequent change.
 
Ok, what again is "module" in this context? And who is "we"? Essential things for the discussion, but not defined before, or I am blind. When
we are inside one single codebase, we can change all the code, what do we care about stability? When someone changes
a function in a _concrete_ class that I in my class use as a paremeter, then go ahead, it's code, we can change it.
It's possible that you need to fix a test then. Where is the big cost or risk here? The alternative is that I use
some abstraction (aka interface?) and use some other mechanism on top of that to actually create the implementation
(a di container?).
Furthermore, a concrete class can encapsulate things as well, it can have a public surface and make everything
private also by default.
We already introduced enough useless complexity but on top of that, whoever creates the abstraction
needs to ensure that the abstraction is _actually_ stable. Read what I wrote in the first entry of the series
about abstractions and what problems arise when people try to write code that no one expects to ever change.
So what's more risky, more effort now? And we're ignoring
things that can't be encoded in the interface. Or that our class now has an undefined number of invariants, because
we can't know the implementation anymore by definition.

From my pov, __polymorphism__ - or better the inversion of control to instantiate something,
is a rarely substantial reason to use dependency inversion. A more common one nowadays is lifecycle:
When you pass in something, the single one instance can be used in other classes as well. That's a good idea when
you have costly instances, for example database connections or http clients. And that's about it. More
rarely, it's also about removing some amount of code from a class - that could be done by extracting a (concrete) class and
just instantiate it in the class body and use it. This however, seems to be out of fashion, because most time the
di container is right there and then people will just use it, because that's what everyone is used to.
It's overusing of the container because it's easy to do so, instead of thinking about an appropriate location for some
seperated code.
Common arguments say that those things are then "too coupled" and they must be "decoupled". Why though? What makes
the concretion more prone to coupling than let's say an interface between the collaborating classes? Accidental
publicly visible members of the concretion you use? Then what's the deal, make it private and look what your usage site
needs to do to adopt. The signature of a public methods needs to change? Okay - you have the same problem
when you evolve an interface. Even more so, because people expect interfaces to be rigid. It's not your code, but a
dependency? Then you control the version you are using - if you are using a library that gives you trouble upgrading,
maybe it's the wrong dependency to introduce in first place. Alas, an abstraction will hardly help you in any of those
cases.

I see coding against abstractions by default as a big problem - because it creates a boundary that needs to be rigid.
You want to remove those boundaries wherever you can. Only where you can't - and I mean for _very_ good reasons - you
should code against abstractions. And especially not because a SOLID rule says so. I advice to program against
concretions wherever possible. I made the experience that a single implementation is almost always everything you will ever
need. Adding an additional indirection just for the sake of it, is one of the worst things you can do to your software.
Similar to what I wrote in the first post on clean architecture regarding the importance of policy, nobody cares about
whether your code works when targeting the abstraction - they care if the concrete instantiation with the given
configuration works. Reducing the fidelity between "your view" of the world and how it is in reality imposes risk of
mismatch, quite objectively.

## Regarding testability

Back in some long gone days, interfaces where needed for frameworks to be able to _mock_ things when testing.
This is not the case anymore, only in few languages I don't care about much (but if you do, maybe I am not a good
person for you to seek any advice), this might be still relevant today.
Additionally, replacing all dependencies of a class with any sort of test doubles is a preferred technique of
mockists testing - which I consider as harmful as coding against abstractions by default. Without dependency
inversion, this style is not possible at all in most (compiled) languages. So no wonder a lof people think that
DI is strictly necessary for "good" code, because for those folks, "good" means "tested with mocks".

The important thing to notice however is, that _"testability"_ and _"unit testability"_ as most people understand it,
are quite different things. Not using DI doesn't make anything less testable. It just prevents you
from testing things in _isolation_ - or in other words, it forces you to test with __high fidelity__, because you test
everything in its real environment, closest to the real world as possible. And yes, that is sometimes not easily
possible for a few special things, like database and external APIs - in general the bar for "sometimes" and "special
cases" needs to rise here though. For databases, I will once again refer to my post
https://hannomalie.github.io/posts/test-with-the-database.html and state, that I recommend everyone to at least
try how far testing with real databases (and filesystems and what not) gets you until you really must deviate from it.

Also worth a read in that context might be https://hannomalie.github.io/posts/test-at-your-boundaries.html , because
for that approach, whether you use DI or not is close to irrelevant, because there's no coupling between the strucutre
of your code and the tests, eliminating one of its big selling points.
It might be either a disillusional take of mine, or it will be an eye opener for some how much of a relief
it is to let go from the urge to dependency injectify all the code.

> I had a first class example of the effect I describe with a colleague of mine at work, who at some point had to work on a new
project alone, me moving to another project. In our former project together, we experienced severe issues with the 
traditional testing styles, clean architecture style and SOLID principles (unreadable tests, overuse of mocking, 
too many indirections in code, scattered code fragments), so I started proposing alternatives
and kept explaining what the positive effects on the project and our development would be.
We never managed to apply what I suggested, for different reasons.
But he called me a couple of weeks into the new project and reported super happily how well my suggestions worked for them
over there, because they secretly did all that stuff.
He was enthusiastic, encouraged to learn again and a bit in disbelief how he could stubbornly follow those "good practices" for so long. 

## On terminology

_Dependency inversion_ originally means that a piece of code A that depends on some other piece of code B inverts this
dependency. It means source code dependency. In order for that to work, it must declare an interface by itself that
replaces B completely. This is mostly where I have my troubles with - because it introduces those abstractions I
consider harmful. A bit different are interfaces that are not self-defined but for example some interface-only module
or some standard api module. That's not exaclty dependency inversion anymore, because we now just have a dependency to
some other piece of code C - but when that's rock solid stable etc, it might be not worth discussing further.

_Dependency injection_ on the other hand, only means that regardless of the origin of the dependency, we're gonna inject
it where it's needed, instead of instantiating/using it on the spot. This would nowadays be sufficient to fulfill
the needs of mockist testers as mentioned above, but the question must still be: Why? Only when sharing instances
makes sense, I would say.

Not sure if we need to mention _inversion of control_ here also, because I see it as a natural consequence of
dependency injection - someone has to have the control over instantiating the stuff you want to inject. Can only
be the caller. And I don't think it's important whether this is the IoC container or your hand-crafted main method.

The whole terminology however, is problematic - not everyone has the same understanding of those words.
People use them interchangeably, wrong, or rightfully different. Close to no (application) developer would care
about source code dependencies in a non-modular code base (which is probably most often the case). Library
developers care more about api modules and stability. Spring developers do god damn everything with the IoC container
and don't even know anymore how to do it differently.

So I need to ask _why_. Why do you do that stuff. And please don't tell me for _testability_.

## Closing words

I need some closing words. I find it quite difficult to understand what context exactly all those articles and books
about DI where written in. What a module is. Which organizational unit works on which part of the system, on which part of the code.
What the platform is we're developing on. How dependency management can work. What the deployment units are.
What code boundaries we think are important.
Yet, all those things are important.
I am writing from the point of view of someone working for a decade on microservices, custom plugin runtimes and bigger
business applications. The simple argument that "this class should not care about this thing" doesn't work for me anymore,
it's not sufficient. I need to ask _why_? Can we remove it completely? No? Then it's obivously a dependency.
Every thing must care for its dependencies, we can't pretend the coupling isn't there, it _is_ there. An indirection
only obscures that fact.