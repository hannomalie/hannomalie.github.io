title=Test at your boundaries!
date=2024-06-03
type=post
tags=testing
status=draft
headline=Test at your boundaries
subheadline=And push the boundaries out as far as you can
summary=I will motivate why you should always define the boundaries of your system as small and as far out as possible and why you should only test them, instead of the rest of your system.
image=images/boundary.jpg

~~~~~~
## TLDR

- Automated tests should test observable behaviour of your system, __no__ internal stuff
- The surface exposed to cause behaviour should be as small as possible
- That will lead to systems you can be highly confident in
- And also give you freedom to refactor everything
- We need to be very careful with exceptions to that rule
- Static typing and functional domain design can greatly help you

## What is hard to change

Let's assume we agree on the fact that __change__ is the most important aspect for any software project 
_(which is not true for every project, like prototypes, but you get the idea)_.
If you don't know why this assumption might be reasonable, watch [this excellent video by Kent Beck](https://www.youtube.com/watch?v=yBEcq23OgB4).
I can give two easy to grasp examples of what is hard to change in projects: _interfaces_ and _persistence_.
Your experience might second that. You might not be aware of, but it's because they are __boundaries__ between things.
The definition of boundaries is ultimately what enables or prevents your software from being
able to get changed over time because they make parts of your system _rigid_. Boundaries get established
to fixate something, while the rest can remain soft and fluid.
A boundary needs to be as small as possible and you must have
as few as possible of them. Things that expose a boundary should access other boundaries as rarely as possible.
I use the unprofessional word "thing" often, because I deliberately don't want to talk about
either classes or functions or services or modules or deployment units or anything specific. It's
about all the things.

Without knowing of it before I started writing about boundaries, Robert Martin described boundaries
in his book _Clean Architecture_. First, please don't dismiss meaningful work of experienced people, even though
you personally either don't like that person or disagree with it. I also don't agree with a lot of his takes
and if it helps keep you reading this post by any means, he's also not my type of personality.
Second, his definition is

> Those boundaries seperate software elements from one another, and restrict those on one side
from knowing about those on the other.

Which goes in the direction of what I mean when I write about boundaries. Yet please don't make the mistake to think
my ideas are exacty the same as Mr. Martin's, or are even based on experience remotely as big as he can offer you.
I consider the idea that elements on both sides of the boundary don't know about each other the goal of 
establishing a boundary. Whereas the fact that that boundary is the fixated area of your system is rather
a side effect - yet the much more important aspect! Why do I think so? Because decoupling things is one of
software engineers trained mantras, one of the things nowadays considered a universally good idea. But I have the
impression people rarely have in mind what actual cost they create by that, what the downsides are. Preventing
them to do a cost-benefit-analysis at all. Invalidating the whole decision making process to a gut feeling decision.

### Boundaries

First of all, the term _interface_ is too overloaded. Some people say API, some say rest interface,
some think of a module they use as a dependency and all its containing types, some think about the programming language 
construct when they talk about interfaces.
Here, I mean __boundaries that are used by other people or seperate services__. It's important to understand that I don't mean
a system's internal modules, even though depending on point of view, those are boundaries as well.
I really mean the __outmost line where your responsibilities end and someone else starts to participate__.
Let's have some examples.

- You have a system, which is a HTTP endpoint delivering json data. Other team's services use it. Your boundary
is the HTTP api of that system. The type signatures of the service classes in your service are not a boundary.
- You have a math library in your favourite programming language. Other people use it as a dependency and compile and
link against it. The exported types and functions of the library are your boundary. The internal functions
that implement bit shifting for super fast vector normalization are not a boundary.
- You have a service where the 3 developers of your team can send a POST via curl and the system publishes
a kafka message that there is cake for free in the office kitchen. The kafka message is a boundary. The HTTP endpoint
for the POST is also a boundary, but here it might be worth stating that it's a different one.
- You have a website which is a server side rendered multi page application, like this blog. The urls and the
content type you use form the boundary. The content can be seen as part of the boundary (but not necessarily). The 
domain package where you implemented the service's functionality is not a boundary.
- You have a library that wraps redis client for some convenience. It's used in 2 projects. It's a boundary.

So it's strictly necessary that we identify our boundaries by looking through a consumer's eyes and
ask for the observable behaviour that is relevant for him.

#### Those boundaries are the place, where you should do the testing of your system.
#### Nowhere else.

And now while the bold statement is out: Depending on the situation, there might be some value deviating from that rule.
It will either cost you ability to refactor. Or it will simply add costs because now you have to differentiate
between tests that break because you broke your contract or because you changed the code. So people need to understand
that fact. And they need to make decisions, probably about code they didn't write etc pp.

The reason I made that statement bold and screaming is that I can't say I __ever__ experienced a situation a developer
has explicitly thought about boundaries when testing, where to set them, what the consequences really are, what cost they introduce
and most important: whether they are worth the cost at all. All developers I worked with (with very few exceptions), 
wrote tests because they think whatever code they themselves implemented, must be protected from getting changed ever
by anyone.

In case you are wondering: I spoke to a couple (maybe a dozen) of them over the years, explicitly asking what they think their
tests' effects will be. The answers were all pretty similar. With some more people, I had some legere talks to get a gut feeling
what their beliefs and priorities are. The impressions I got were also pretty similar. Never was someone talking
about specifications for the system, undeniable facts someone else defined for our system. So not exactly
a scientific method I applied, but I guess more sophisticated than what most people expect and do themselves when they
build an opinion.

I will conclude my motivation with another cite from the clean architecture book, which I find kind of amusing.

> A good architecture makes the system easy to change, in all the ways that it must change, by leaving options open.

> Some principles of architecture are relatively inexpensive to implement and can help balance those concerns,
even when you don't have a clear picture of the targets you have to hit. Those principles help us partition our
system into well-isolated components [remark of me: that means deployable units in the book] that allow us to 
leave as many options open as possible, for as long as possible.

It amuses me partly because I think "easy to change" is one of the cruxes here and partly because I can smell a bit of a contradiction
between the things the book teaches and the essence of this statement. For the reasons I explained
above, the focus on "well-isolation" of things is one of the reason change becomes so painful in most projects.
At least with the common style of unit testing. While writing this post, I came across some other perspectives
from great and well-known software guys, like [this one](https://dannorth.net/cupid-for-joyful-coding/#single-purpose) by Dan North, creator of BDD.
He critisizes the _single responsibility principle_, I cite: _"[...] in my experience, this creates artificial seams [...]"_
and I felt like he hit the nail on the head. What he calls _seams_, I tried to describe as (unnecessary) boundaries,
cemented by unit tests that prevent you from applying changes of a group of things, even though they belong together.

#### A real life example

Let's motivate my idea with an example. Roy Oshervore (you know, the unit testing master) has an example 
[on his blog](https://osherove.com/blog/2005/4/3/when-should-you-remove-or-change-a-unit-test.html).

It states

> A year ago company X needed a feature of their calculator to be able to parse any amount of numbers, as long as they are positive.

So we already know it's about a calculator. But the blog post continues with

> A number of tests were written to satisfy this requirement. Here s one:

followed by the test

```javascript
[ExpectedException(typeof(Exception), Negatives not allowed )]
Void Sum_Negative1stNumberThrowsExcpeiton()
{
     Sum( -1,1,2 );
}
```

The first thing that pops up for me is, that he's talking about requirements for the calculator, yet I can't
see the calculator anywhere here. It's one of the aspects I described above: We lost thinking about the observable
behaviour of the system. The real requirement is probably sth like "When a negative number is typed in, the display
of the calculator shows 'Invalid number'". This is an assumption I find reasonable, because that's similar
to what I usually encounter at work. So the specification could better be encoded as something like (pseudo code):

```kotlin
@Test
fun `calculator shows invalid number when fed negative number`() {
    val calculator = Calculator()
    calcuator.type("-1")
    calcuator.eval()
    assertThat(calcuator.displayText).isEqualTo("Invalid Number")
}
```
Please don't be harsh because you don't like my design of a calculator. I also don't like it, but that's
not the point here. The point is, that it adheres much more likely to sth that a user would specify or at least
contains all the relevant "steps" a user would describe. The best approach most often would be to create a small
internal dsl that acts as a driver, rather than using a calculator class directly. That would move
our boundary another small step to the outside, but not by much.

The second thing is, that for how the unit test works, one would need another test, an integration test, that
somehow verifies that the correct Sum implementation is actually called by the calculator. Such a thing is not needed
with my example.

And now the most important thing: We can freely change the implementation of our Sum functionality and the test will
protect us. We can first remove unnecessary classes and implement it with free functions directly. YES, I know that
the example didn't do it, trust me, other projects do it.
The test will keep working. Then, wen can remove the exception and replace it with a result type that we return, 
because we like errors as values.
The test still works.

While the blog continues its story by introducing a requirement change

> Today, the VP decided that a new feature is required to have negative numbers allowed in the calculator.

it's the next statement I have problems with:

> Alas, the earlier test fails. It broke due to a requirement change.

Yes it did. But it also broke because I enhanced the implementation (yes, that might be subjective, calm down).
First it stopped compiling because the removed class can't be found any more (yes, as said, not the case in this example).
Then, it failed because the expected exception wasn't thrown. Then, I had to add an assertion for the result
of the function. And last, I probably need to fix the integration test we can't see here as well. Either it is green
because it mocked the Sum function by throwing an exception, which is the worst thing to happen, because the test is
now completely wrong. Or I need to change the mock to use some other implementation detail now.

So the unit test doesn't implement any specification that is directly relevant for the user.
It's not good architecture, because it makes changes hard to do.
What does it actually do then?
It creates a boundary that you can not easily move. For what reason? To protect the given design
from being changed. It's the encoding of a selfish, egocentric, overbearing attitude of a developer.
At least __probably__, but that might be my subjective view, based on my experience in projects. Might as well
be the case that people just don't see what I am describing, maybe they just don't know, never thought about what
the actual effect is such tests bring.

#### A word on persistence

There are lots of developers who see persistence as the necessary center of all attention, because it's the hardest
thing to get right and therefore it's okay to let it steer all the other development. We don't need to care about whether
they are _right_ with their conclusion. We need to think about _why_ they are possibly right about the fact
that persistence is the hardest or at least one of the hardest parts of development.

It's hard to deal with persistence, because it's also a boundary. A boundary between now and the past.
Oh that was deep. Maybe another example. When you change non-boundary code, it's simple: You change code,
you deploy, finished. Piece by piece, all your service instances get replaced and the new code just runs. It's
stateless, it doesn't have to care about the past. Your application is exchanged as a whole, there is no
tearing for your application.
Persistence is different. You can not change your data model, because you need to translate the old data to
the new model. Not impossible, but certainly a cost and different from the rest of your code.
You also cannot just remove the old model definition, because
you at least need to have the last one in order to perform the conversion. Usually you have _all_ the old data models,
because that's how relational databases and automated migrations work best.
Now that we have a better feeling why persistence is difficult to change, let's ignore it for the rest of this post
and refocus on the boundaries we can actually influence. I just wanted to give a complete picture about where change in projects might get
difficult, so for sake of completeness.

## Test observable behaviour, nothing else

When you take a look at your system as a consumer and respect the idea of a boundary, this will immediately give
you a big advantage: You instantly have your specifications for the system. Your specifications are purely
based on the features the system should deliver for a consumer. In professional projects,
that's what you usually get when you do behaviour driven development (BDD). Those sepcifications need to be executable,
because you always want to be able to state whether your system fulfills all the specifcations easily.
So they will become tests. Note that often BDD overdoes that topic with "weird" external DSL frameworks like cucumber. That's
completely unnecessary. You can simply write a Selenium test in your language of choice, fetch the html of your endpoint and either
do equals check against a html snippet, or use more sophisticated tools, like screenshot comparison test, partial html
snippet comparison, string contains tests ... whatever fulfills your kind of expectations best.

__Those are the only durable tests.__ That means those are the only tests that are checked in. Read that carefully.
It will give you an easy understanding of what they are. You don't change them and you don't break them.

* When they go red and you changed the main code: Your last change is a mistake, revert.
* When you changed tests and they stay green: You lack test coverage.
* When you changed tests and they go red: You are either changing the specifications of the system after you
clarified that the requirement was accepted, or you have a breaking change for a consumer.
_(Adjusting a test and making it red before doing any changes is the usual flow you implement new features or change
existing ones)_

The second effect is: All the internal workings of your project are elastic. They can freely change. That means
you are free to change close to all code in the project with confidence and freedom. Ever inherited a project that
was done by some OO maniac while you and most of your team prefer to solve with data oriented design and functional
minimalism when it's appropriate? With tests on the boundaries, you are free to do that. With tests for implementation
details, changing the structure of the code will just be a frustrating exercise and will take multiple times
the effort. At this point, I can't stress enough, how important it is for your engineers to shape the code of projects they have
to own and maintain, so that they live up to their expectations and quality standards. So that they can identify with them,
work with it with confidence, without fear. Keep motivation to stay responsible for the project.

## Temporary tests

Now it gets interesting. We won't go into whether test driven development (TDD) is good or bad, it's also mostly
irrelevant, when you follow what I wrote above. The point is:
When TDD helps someone guide the implementation, finding a design, getting the implementation done quicker, with less
code, with more pleasure or whatever reason there might be, then it is probably okay for that person to do so. This is
independent of your personal opinion about TDD. Does it mean that the resulting tests need to get committed and
become part of the project? __Hell no.__ They can very well be a temporary aid for the implementation and deleted right
after the implementation is finished and integrated. Only because everyone and his aunt creates a gazillion of microtests
even without TDD, doesn't mean that committing them is following our intention. We need to stop preventing code - and
that means also test code - to get deleted. When those microtests' intention was to guide the design, fine, they've done their
job. Now they prevent us from refactoring, because they are coupled to the implementation structure, so we remove them.
When they contain any details worth lifting into the boundary tests, even better, now is the time to do it.
As long as you have your boundary tests, ensure that the specifications of the system are clear and fulfilled, you are fine.

Let that sink for a minute. The problem is, this take is _highly controversial_, many people feel offended by the idea to
delete tests and you will be hated by people when you put it on the table.
I can only assume that is, because a lot of experts made statements over the last decades, 
similar to what [unit testing guru Roy Osherove stated](https://osherove.com/blog/2005/4/3/when-should-you-remove-or-change-a-unit-test.html): 

> As a rule, a passing test should never be removed. 
That's because passing tests serve as the regression tests for our maintenance work. 
They are there to make sure that when we change code we don t break anything else that s already working.
[...]
But sometimes we might get some failing tests even though the change was absolutely reasonable.
That usually means we ve encountered conflicting requirements.

And that last sentence made me think. "Usuallly", unit tests fail because of changing requirements.
I am very happy if that's the case for _anyone_ in this world, but in all application projects I worked, it was the
most rare (?) reason for unit tests to fail. Or a different view: Unit tests close to never encode the actual
requirements (of a user) towards a system. Sometimes - especially in DDD projects - they encode some defined
domain logic with some implementation, that can in turn be used by whatever boundary is defined for the system.
For example the actual requirement towards the system is that a user gets an html list page of some items when
he opens the browser, yet the unit test asserts that your developer's service class middle man returns a list
data type and not a set for example. You can't change the implementation of that feature with such a unit test.
So whenever such a unit test guided the design, it can be removed now and replaced by a boundary test.

At this point, I have another controversial take. An alternative to more widely used approaches like DDD, is __functional
domain design__. When you google it, you will see that a lot of smart people give you examples - even language authors of the Java language
(which was mostly OO only in the past) - on how to use algebraic data types and pattern matching to encode a domain
simple, concise, understandable and without any tests needed. The types are the safety guard and are completely sufficient.
The types are close to the bare minimum that one has to articulate in order to implement a domain. That means there is
not really much room to do it in a different way, it basically can't get any simpler and more complete than that. If
you can't imagine how that would look like, watch [this video](https://www.youtube.com/watch?v=2JB1_e5wZmU) by Scott Wlaschin.
In case a requirement changes, you extend your types, get compiler errors when you have unimplemented paths that you
can now easily fix. 

Another word on that, though. When microtests or TDD guide the design of the code, it might very well happen, that
the resulting code is not easy to reason about anymore without those microtests. If that's the case, then your boundary
tests don't deliver on that particular quality. Having a clear idea of what's going wrong in the system
when a test fails is still a valuable property of a test and that quality doesn't have to suffer only because you do
boundary tests. Mind, that the idea is not to have a failing assertion that exactly asserts the single one aspect
that doesn't work anymore. That's one tool to fulfill the need to _clearly see the issue that's causing a test to fail_.
An equally good tool can be a precise error message body in a 400 response of your api. When you want to place an order
through your http api and the response says "order id has invalid format", you know exactly what the issue is that
causes the test to fail. We need to - again - be open to see what the intention is and which tool fulfills the need,
without quickly jumping to conclusions just because there is a way we always did it in the past.

## Static typing
I can't stop with those controversial topics, but since I just mentioned functional domain design, I need to address
the valid question _"Why is static types okay, but unit tests aren't?"_.
It's a good question, because both unit tests as well as static types make your code rigid and encode certain
properties for you. Unit tests (usually, but maybe depending on the language you use) can encode more properties
or oftentimes can at least do it in a more approachable way. When you use static types to encode your domain, the
difference though is, that definition and proof is encoded in the very same code. You just write the definition and
the compiler does the verification for you with instant feedback in your IDE. The fastest feedback you could possibly get.
That approach saves you from syncing two pieces of code (test and implementation), which in fact is the root of the evil
I am writing about in this whole post. Because the tests should be independent of the code (structure), but they aren't.

We can deduce another important aspect: When you don't have static typing, your only option to get the wished for
validation of your code on a micro level is in fact a ton of small unit tests that are coupled to the structure of your code.
The fact that those tests become indistinguishable from the real specification-like tests is one of the major problems
I've seen in too many projects I worked on. Because how do you know that your changes are okay!?

Well, one unusual approach to that problem could be to say: We have _specifications_,
which are behind the red line we don't cross, they define the behaviour of our system.
And then we have tests, which are okay to change more freely.
I have never seen such an approach implemented and when I asked for feedback in real projects,
people would rather prefer not to do it like that. Esentially because they are not used to it and can't see
the value. I personally find it quite suspicious, that the main people disliking my idea were always the code authors,
so it's hard not to get the impression they want to protect their code designs. Otoh that's something
one has to accept when working with other people, I guess.

## What about _architecture_?

As stated above somewhere, people try to come up with an architecture and try to protect it, enforce it, so that
no one overcomes it. In fact, there are tools like ArchUnit, that exist for that sole purpose.
[Here's a superb video by Victor Rentea](https://youtu.be/nuHMlA3iLjY?t=1449) where he demonstrates a highly
modular codebase. I find one little hint at this point in the video quite interesting, which is, that those modules are owned
and maintained by seperate teams (no surprise, it's about modular monoliths as an alternative to microservices).
As someone who is doing distributed services for nearly a decade now, I can greatly appreciate modular monoliths.
So first of all let's again ask the question _why_: When you restrict ownership of repositories or modules inside
a single repository, it's because of your communication structures in your company. I don't like the idea of gatekeeping at all, but
when there is not infinite trust and different values and processes across teams in a company, it seems to be a reasonable take for
everyone to accept those boundaries and then they need to be reflected in the code and enforced, as that's the architecture.
Note how such a boundary immediately makes parts of the code rigid, so that you need to introduce complexity that wouldn't
be necessary otherwise.
The actual boundary is the modules' api code, which is visible to other teams. It's chosen to be rigid, because
changing it is costly, as inter-team communication is costly and usually the most time consuming part of any project
that is executed by multiple teams.

Here it is, where complication start to emerge.

First of all, the quality requirements and though work that needs to go
into an api to be well defined and prepared to get changed never or very slowly is significant. Given the example
with the Sum function from above and exceptions vs. result types, we ussually end up having to support designs that are either
suboptimal, or at least not our preferred ones. Oftentimes, the designs are too much based on personal preferences or
done in isolation without any usecases. No wonder, if you have something that is hard to change, you want to have
it perfect, right? And you will defend whatever design you have to maintain in the future, cough cough, even though you will 
leave the company in about a year, right?

Second, most APIs consist only of interfaces. Rarely is any actual contract encoded or even documented for it. Sounds
abstract? Imagine a service that manages Items. You can save an item for a user and you can retrieve the items
of a user. Have you ever wondered: When you add an item for a user and afterwards retrieve the user's items, is the
newly added item returned in that list? If yes, how is that property encoded in the API? I bet not at all. When
you do distributed services with http apis, there is a bunch of HTTP codes that let you encode those contracts, yet,
often it's not done thoroughly and you end up with the same questions in your head. Why am I writing that? Because
those contract is what I would recommend implementing in the tests, that are - you guessed it - done for that very
boundary, the exposed API, and nowhere else in your module. Hopefully it would be okay in the api-test module
to depend on all the implementation details of our module, because for all implementations of the exposed api, those properties
need to be fulfilled. Take a look at my [preview post about testing with persistence](https://hannomalie.github.io/posts/test-with-the-database.html),
to get an idea how to test contracts of different implementations nicely.

Long story, but hopefully I can convince one or two people out there that even in scenarios like that,
there is a good chance that you can work by testing only at the boundary just fine.

## Closing words: Qualities of a test

There are a lot of properties, aspects, ideas and goals people have in mind when it comes to _good_ automated tests.
I suggest to have tests only at the outer most boundary of whatever your project is. Tests should be as _independend
of the structure_ of your code as possible, to enable the project to get changed over time.

Exceptions to that rule rarely lead to something good, so I can not stress enough to ask the reason _why_ we have certain
tests and what actual value they deliver, what elements of the project they prevent from being changed.

When you really want to model your domain independently from any infrastructure, even though it is very much the actual
requirement of the user, I would recommend to do it with static typing and functional domain design. As that is truly the
most minimalistic, self-verifyable thing you could do in isolation from any IO.


___
_[Title image by Roger](https://www.flickr.com/photos/49015875@N00/2514717782)_