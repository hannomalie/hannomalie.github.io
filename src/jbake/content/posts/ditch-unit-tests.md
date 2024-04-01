title=Ditch unit tests
date=2024-03-29
type=post
tags=Testing
status=published
headline=Ditch unit tests
subheadline=Do developer tests
summary=Unit tests are outdated. Developer tests replace them.
image=images/unit_tests_developer_tests.jpg
~~~~~~
## Motivation

Long time not written down anything for my blog. The truth is: I got knee deep into the software testing swamp 
and constantly read so many books, blog posts, did so many interviews and started so many
takes to write down my most important thoughts and conclusions, constantly overtaking myself over the
last year, that I ditched that all and will now go for smaller portions. We'll start with an evergreen:
The term _unit test_.

## TLDR

Stop doing and saying _unit tests_. Do _developer tests_. Think and discuss the actual value you
want your tests to deliver and the criteria they should fulfill. And _why_ they should fulfill
the criteria. There is really no need to call sth a unit test anymore at all.

## What's bad about _unit tests_?

1. **It's an ambigious term.** Everyone has a different idea what a unit is. Not having a common understanding will lead people
  to simply move in different directions. Results in tests that have not commonly agreed-upon intentions and criteria.
  For sth as important as tests, we can really not afford to stick to an outdated term just for the sake of it.
2. **Their common definition.** Most people follow definitions of unit tests that are ancient. Originating in a time
  where a lot of things were either impossible, not yet known or not practicable. Such a definition leads to downsides
  that might have been acceptable for the costs they prevented back then - but it's not the case anymore.
3. **They lead to silo thinking.** The distinction of unit and non-unit tests alone leads to different people
  implementing either of them. The very least thing that happens is, that developers will overly focus on unit tests,
  because that's what they are expected to care for and that's what they are themselves mostly caring for.
  If you're lucky, there is a person on your team that compensates that by focusing on "other" tests, like integration
  tests, scenario tests, acceptance tests, whatever. Sometimes, this is what the "QA" people do, as if quality assurance is
  not what unit tests would also do. This is just Conway's law - just as tests are split up in your repo, the people
  caring for them will be isolated to some degree.
4. **They create redundancy.** You will have expectations towards your system that are written as assertions in unit tests
  as well as in integration tests, as well as in potentially even more kinds of tests. We don't have to joke about
  "twice holds stronger". Sure it does. Ten times is even better - but at what cost? When you change your system
  and have to adjust x tests of y types, maintained by z people, cost of change in the project just skyrockets.
  Ideally, a specific verification exists only once and can be maintained by a single person.
5. **They distract from what's important.** Instead of any arbitrary line that differentiates one test kind from another
  one, we should talk about what the actual criteria are we need to differentiate for. When was the last time you've
  seen a colleague execute tests continuously on code change? Or in very small steps doing TDD? When was the last time
  your project's compile time, its pipeline's check or queuing time was actually smaller than test execution time? The point I am
  after: People love complaining that a test could run in 2ms instead of 100, yet there is no substantial need for that speedup,
  no real benefit, the exeuction time is never a bottleneck by any means. Is it really necesary to accept other drawbacks
  just so tests run faster? Is it a valid reason to never use dependencies in tests? No.
6. **They prevent behaviour driven testing.** BDT in essence focuses much more on specifying a user's expectation towareds
  a system. It makes sense to use that as a starting point. But it's a top down, blackbox testing approach. It doesn't
  fit well into what classic unit tests do and what people design in their heads when they start coding. So most
  people won't ever add that approach to their reportoire, but they really should.
7. **They prevent test first approaches.** Like the previous point. For most people, unit tests seem to be a bad fit for
  anything other than dependency free tests that get written after the main code. There are exceptions, like
  mocking-based design approaches, but since I haven't seen anything of that in practice in the last ten years,
  I will just pretend it doesn't exist. Classic unit tests seem to be a particularly bad fit for test first for most people.
8. **They create coupling.** Unit tests have commonly a 1:1 relation to code units. This is inevitably coupling,
  your tests are coupled to your code structure and vice versa. They need to be kept in sync. Change in either
  of them will break the project, because either the test is wrong or the code. Ask your self and be honest: 
  Is it intended or accidental coupling? This coupling prevents changes, making one person's code hard to change
  for anyone else as well. So it's just bad for shared code ownership.

## What's better with **developer tests**?

First of all you could label whatever unit tests you do today as developer tests and would be fine. Because
unit tests are developer tests after all, they are written by the developers.
However, that would miss the point.
It would be helpful to also benefit from all the other upsides, which would require some changes to the status quo.

Here's what will be better than before, when you do developer tests.

1. **No ambiguity.** Whatever test you want to write, it can be a developer test, a test written by a developer. You are
  not a developer? Oh, just write a test, then you are one. You need to automatically test some feature? Great,
  here's a free developer, he can implement it. Someone on your team cares more about QA than some others? Great,
  he's a developer, he can write a developer test for it. If there will ever be a better term then developer test,
  we can adopt it, until then, it's more than fitting for everything we need and dimensions better than what we had before.
2. **No harmful common definition.** They don't really need any further definition. No one will ever get to you and seriously
  tell you to always mock all dependencies, because a given test is a unit test and not an integration test. Developer
  tests are just free to test whatever the exepectations are.
3. **No people silos.** You don't need a QA person to write a smoke test that targets a deployed system. You don't need
  a QA person to implement a sceneario test. You don't need a tester to write an integration test. The developer
  writes the test and whatever and whoever can and wants, supports him, end of story. When people on the project change,
  new people get to be developers and they can write the tests, just as anyone else on the team.
4. **No redundancy.** A "integration test" would be best for some specification, but the developer just writes unit tests?
  Not any more! Developer test it is. When a scenario test covers all the code, no need to write any low level unit tests at all.
  And when it's the developer who can write it, he doesn't need to fear that "his" code is not covered.
5. **Focus on what's important.** For a valid reason (!) the test needs to be fast? Replace dependencies with fast
  implementations. Is a given scenario best as documentation for new people on the project? Write it as 
  a story. Is it a very important business case? Use real implementations, even though they are slower. Instead
  of arbitrary rules stemming from outdated classifications, look at certain criteria and fulfill what makes sense.
6. **Focus on business relevance.** By implementing a given requirement or demand as a executable specification,
  you automatically focus on an expectation towards the system that is important. What's not in there, is left out,
  giving everyone the wiggle room to change main code without breaking the specifications.
7. **Always good for test first.** Given the previous point, it's easier to come up with a test, when the test is
  just the demand or the acceptance criteria. No need to be overly creative.
8. **No coupling.** Your tests are independent from the structure of your code. You gain a lot of freedom, code
  can undergo heavy changes, while keeping your confidence that the system behaves as expected.
  It's even quite possible to write the majority of specifications in a way that can be kept, even though the project itself
  was reimplemented in a completely different programming language. That could be made possible by either targeting an api like http
  as a boundary. Or by having any other driver abstraction, maybe just a tiny internal DSL that decouples your tests
  from the system.
9. **No tech silos as well**. A small addition/side effect of point 3.
  When a developer is in charge of writing a sceario test, what technology will he probably
  use? Right, the one that's already in place, because most of the time, it is perfectly capable of delivering what's needed.
  When you do Java, you can easily just use Selenium and its Java API, maybe put a tiny internal DSL on top of it and fine.
  The question whether your team benefits from and can afford (!) adding a Javascript subproject, Cucumber or any other
  fancy technology that might give one or another small advantage and would be favored by a dedicated QA person without
  any developing background will likely be answered differently. Personal side note: Those projects are **fantastic**
  for developers, because they can take over end-to-end responsibility without getting overwhelmed by technology. The
  quality of the project will be very high. (Of course only if you don't have only bad developers who insist to continue
  living under their rocks...)

## Closing words

Unit tests usually have some characteristics that - from my perspective - are almost always unwanted, yet in my teams
there was rearely awareness about them and even more rarely fruitful discussion about them. But that doesn't mean
some doubtful characteristics are **always** bad.

There might be situations where you don't care about coupling between test and implementation.
Imagine a technical library where you need to be very careful about backwards compatibility, because
it's expensive to cause trouble updating its consumers. Besides binary compatibility, source compatibility might
not be possible to verify because of missing tooling. Semantic backwards compatibility might probably only be possible
to guarantee through very precise tests.
Or when you have a special implementation of something with certain performance characteristics that performance
tests might be able to show, but tests of implementation details might do that much cheaper. Given proper explanations
in those tests, that might very well be appropriate and a good solution.

But those characteristics are just what I wrote before: Some characteristics out of many others. One or the
other way around we need to understand **when** we need to use **what** and **why**.
As soon as we start thinking mainly of the intentions of our tests and ditch outdated models that often lead to
wrong intentions by default, tests will become one of our most effective tools.


***


**P.S.** Also don't mistake this _empowerment of developers_ with _we don't need QA_. I don't know exactly _why_ people could
read that out of my words, but it happened in the past, so I'll comment proactively:
QA means quality assurance. You need an **holistic** approach to QA, it's not something that someone somewhere does after development.
Shifting QA left is rightfully a positive trend for years already, so I don't need to motivate it, really.
Besides developer tests, there might as well be a lot of other things that increase the quality of your product.
Of course it makes sense to adopt them when they are worth the cost. Having a dedicated team or guild that 
is responsible for automating important scenarios, that would span multiple services of multiple teams, yet are a coherent
usecase from a user's perspective (maybe called end-to-ed tests?) is probably a great idea. They can have their own
fancy tech stack for that, their own - probably - independent pipelines and alerting for it, while your other
teams remain efficient by not caring about it. Of course it makes sense to have people caring for edge cases a blind
developer might not be able to think of. So maybe involve them when writing down the tickets, eh? Maybe another good
idea is to just have some collaboration between QA focused peolpe and developers in general.
And yes, there can even be value in doing manual testing to some degree. Doing developer tests
absolutely does not prevent any of that.

