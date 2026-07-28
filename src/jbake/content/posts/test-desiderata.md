title=Test Desiderata
date=2026-05-20
type=post
tags=design,coding,testing,qa
status=published
headline=Test Desiderata
summary=Throw away "unit and integration testing", throw away the test pyramid, we have something better.
~~~~~~
We passed the second anniversary of [a blog post of mine](https://hannomalie.github.io/posts/ditch-unit-tests.html) 
about a topic that is the most often
resurfacing one, one I experienced as one of the most controversial ones and personally one of the most annoying ones for me:

Testing.

I also had [a second](https://hannomalie.github.io/posts/test-with-the-database.html) and a 
[third post](https://hannomalie.github.io/posts/test-at-your-boundaries.html) post picking up certain aspects from
the testing topic, going into more details, so they might worth giving a read first before you continue.

What I don't really get: How did I manage to either not get across 
[__Test Desiderata__](https://medium.com/@kentbeck_7670/test-desiderata-94150638a4b3) by Kent Beck or not remember
the specific name at all when I wrote all my posts? It needed 
[a video on the Modern Software Engineering Youtube channel](https://www.youtube.com/watch?v=IBvYFRSw4do) I highly recommend
watching to recall the topic and now I feel an itch to get back to it.


## Test Desiderata

_Awkward_ word for what I consider one of the most _essential_ ideas in software development and testing in particular.
No matter what you already know or think you know about that - _test desiderata_ is our best bet at writing
actual __good automated tests__. I am not talking about the cheap my-idea-of-automated-tests-is-better-than-your-idea way
of the past which I could bet is the only discussion we all know.
I am talking about a way that establishes __objectively desired properties of a test__, so that the whole
discussion that's left over can concentrate about the _remaining non-objective part_. Which is __weighting
the desired properties__ in terms of __cost and value__, leaving achieving an economically viable result as the only
true goal to work towards on the table.

The links and the paragraph above almost say it all already, not much value left for me to add other than repeating all
the properties suggested by Kent (quote):

> * Isolated — tests should return the same results regardless of the order in which they are run.
> * Composable — if tests are isolated, then I can run 1 or 10 or 100 or 1,000,000 and get the same results.
> * Fast — tests should run quickly.
> * Inspiring — passing the tests should inspire confidence
> * Writable — tests should be cheap to write relative to the cost of the code being tested.
> * Readable — tests should be comprehensible for reader, invoking the motivation for writing this particular test.
> * Behavioral — tests should be sensitive to changes in the behavior of the code under test. If the behavior changes, the test result should change.
> * Structure-insensitive — tests should not change their result if the structure of the code changes.
> * Automated — tests should run without human intervention.
> * Specific — if a test fails, the cause of the failure should be obvious.
> * Deterministic — if nothing changes, the test result shouldn’t change.
> * Predictive — if the tests all pass, then the code under test should be suitable for production.

The last 15 years, I mainly worked on enterprise projects in service oriented architectures. Domains tended to be
not too complex, integration effort was quite hight. If the same applies to you, you may agree to the desiderata
I consider the most valuable and most important ones:

* __Predictive__ - Confidence that your project works as intended, is what I see testing's most important job. For me personally,
there is simply no more important aspect of it.
* __Structure-insensitive__ - The idea of tests being predictive in order to tell whether your projects work or not is immediately
connected to the idea of being able to change code and keep that property alive. Coupling between tests and structure
therefore needs to be minimized, otherwise they can't change distinctively.
* __Readable__ - I see this tightly connected to _Inspiring_. A test that is structure-insensitive is ideally written
from the point of view of a user. This will make tests read like a specification and ultimately let's the reader
judge based on the systems defined behaviour, again leading to confidence in the system.

On the other hand, I ususally prioritize the other properties less. But make no mistake, it doesn't mean they
don't get any attention, it's just that we have a limited amount of attention to spend and we need to decide where the
most value lies. For example speed: I never experienced any substantial gain whether a test suite runs 5 seconds instead
of 15 seconds.

Let me contribute some examples from real world projects of mine from the past.

## Example 1: The gateway

I have been working on a service that could be seen as a custom api gateway. There was no business logic
whatsoever in this project, there was simply no domain worth mentioning or anything else in here. The purpose of the gateway
was to be the ingress for another bunch of services internally exposed through a single http endpoint. In order
to do that, authentication was checked and forwarded, rate limit was applied and long-running transactions were recorded
and their state exposed.

So in essence the service needed to connect to Keycloak, forward requests and persist a state of the transaction in postgres, do some
error handling. If you ever wrote a backend-for-frontend, you might have done a similar thing.

### Authentication

When the authentication part was implemented, very nice tests were created, I would say it's even possible a test-first
approach was applied, but idk, I wasn't there. Instead of doing it like 
[I did in my example project](https://github.com/hannomalie/javalin-keycloak-oidc/blob/main/src/test/kotlin/MainTest.kt) 
with maximum fidelity, Spring's built-in test helpers and classes were used,
[like so](https://docs.spring.io/spring-security/site/docs/5.2.0.RELEASE/reference/html/test.html#testing-bearer-authentication).

When using "my approach" with a true authorization server like Keycloak, you can be most confident that the stuff
you developed works. Your production doesn't need to use Keycloak, just some auth server sticking to the solid spec
that is backing the process. You get a similar benefit when using a rock-solid framework like Spring and its test
utilities - even though you are executing against a stub implementation, that implementation gives you nearly
identical confidence, but moves the needle for the other desiderata: your tests will be _faster_ because you don't
use Keycloak over the network but a spec-complient in-memory implementation. On the other hand the test is less
_structure-insensitive_: As soon as you change Spring or Spring security, you have to rewrite the tests completely.
Most people will not think much about that aspect and answer that they will never change frameworks, so it's okay
to couple tightly with it (and I would agree).

### Transaction storage

The transactions triggered through the gateway could be quite long-running. Many minutes, up to hours. They
could involve retries and take different statuses. In order to enable the caller to get that info and request
it repeatedly, persistence was required. Implementation-wise, there is often not much to discuss in enterprise land.
You use the same database technology that we're used to host ourselves, configure in AWS or what the team is
proficient to handle. Like postgres.

With [testcontainers](https://testcontainers.com/), it's a no-brainer
to get a database spun up in your test, being as close to your production setup as possible. Which means
highest possible confidence that stuff works when it finally gets deployed. The desiderata say it's _predictive_.

When the test doesn't cut any corners and instead of using sql scripts to setup test data goes through the public
http api of the service, it wins for other desiderata as well: _Behavioural_, _Inspiring_, _Specific_, _Structure-insensitive_
are all satisfied, while tests are still comparably _fast_. However, the test is not _the fastest_. A pure in-memory,
fully postgres-compliant solution would a charm, though non exists to my knowledge. Now resorting back to something
like H2 comes with different downsides, or in other words, we would prioritize different desiderata.

## Example 2: The server module

Besides the typical services landscapes, I also worked on something (nowadays) more unusual. On extension modules
for a custom application server. The server runtime was completely custom and pretty heavyweight. For example
it took 5 to 10 minutes to start. Company-wise, it was owned and maintained in a dedicated department - the same
goes for the extension modules, they all had their own teams on it.

### Module loading

Without me going into the last details, the server runtime provided a library that can be seen as the api you
could compile against. Only that it wasn't exactly a pure api but also contained implementation classes. The fact
that the api layer also exposed those details and often also made it impossible to use the api without the
implementation details made everything very complicated. The runtime itself additionally came with dependencies
that overrode the ones that were defined in a module, so that module local classes were not loaded but the already
available ones in the server runtime were used for "common dependencies". The server runtime was consisting of different
tiers - there was the backend part, running on a server, then different clients, running somewhere else across the network.
For example there was a remote admin application that was Java Swing based, a web app that was a Javascript client etc.
Stuff not being backwards compatible over time was an additional source of frustration, making maintainance of
multiple versions of a module necessary.

Maybe you get an impression that this environment was exceptionally fragile - it was close to impossible to know
whether the code you wrote works at runtime, even though it did compile. But the zeitgeist of developers was that
tests had to be _mockist tests_ (_isolated_, _deterministic_) and _fast_, they should only run milliseconds. Those desiderata dominated
the development workflows and led to extensive mocking machinery: Developers that were not part of the server team
created masses of mock framework code that imitated some of the server behaviour. Experience showed that there was
substantial drift between the mock implementations and the true one. Experience also showed that implementation errors
were found quite late in the process, often after the ticket was handed over to the QA department.

So what I did was bringing the pain forward. I wrote some utilities that made the true server runtime accessible
for executing tests and enabled automation of what would be a manual test somewhat late in the process.
The server startup time was painful - but it was possible to reuse a server instance to a limited degree. The module
installation took a few seconds, imposing a baseline duration for a test. So tests didn't execute in milliseconds anymore,
they took at least 15 seconds. While it didn't matter whether you executed 1 or 20 tests.

The result was that suddenly there was no need to have the mock implementation anymore, removing a lot of volume
from every project. The feedback cycle was improved, in one way the desiderata _fast_ was even better than before,
when we include the semi-automated testing in the calculation. Allthough the desiderata here would rather be _automated_.
The tests became _structure-insensitive_, because
they used the same interfaces as the user would use which were kept stable, compared to the programmatic apis and
exposed implementation details. They were _inspiring_ and _predictive_.

The most intersting thing in this situation for me was how stubborn new developers insisted in the mockist style testing,
trying to convince everyone that the downsides of that testing style were inevitable. For non-new developers who
never tried to go a different route, the behaviour was the same, everyone kept preaching that making tests slower
would be an unaccaptable trade off.

## Example 3: The llm importer

Different context. I was working on an application that took some unstructured input text and turned it into structured
data. The domain doesn't really matter - only needs to be said that it was not exactly trivial and as with a lot of
those usecases, there were a lot of edge cases. Everyone who worked on an application that does llm prompting
probably knows that the process is inherently undeterminisitic and those models work more or less as black boxes.

The traditional engineers argued that tests should of course mock away all model calls because otherwise the desiderata
_deterministic_ and _fast_ are not fulfilled. Additionally, they were not _cheap_, a desiderata not mentioned by Beck.
Those tests ultimately tested that certain framework methods were called and certain plumbing classes were used and called
as well. Resulting in an exceptionally tight coupling of test and main code, ad maximum degree of _strucutre-sensitivity_.
Furthermore, it was not _predicitve_ - the crucial behaviour of the application was mocked away. Given the behaviour
was mainly determined by the external model call, the tests were not _behavioral_. When the model was updated, the
test didn't reflect any of that.

So nature took its course: Errors were introduced, the service got broken, afterwards the development process
was slowed down by manual testing because there was a high degree of uncertainty when changing anything.

Ultimately, those usecases were translated into tests that did use the external systems. The tests were not _deterministic_,
so sample count had to be increased (kind of a retry). This made the tests _slower_. And still, the gains in
_automation_, _inspiring_, _readable_, _behavioral_, _structure-insensitive_ and _predictive_.

It then made sense to remove a bit of the quality of the _automation_ and only keep a subset of those tests automatically
executed on every merge request, with the option to execute the task manually on demand, for example when prompt
text was changed or model dependencies were updated.

## Closing words

_Test desiderata_ needs to be our framework for crafting tests, period.

Old nomenclature of unit and integration tests
needs to die and discussion need to center around weighting the desiderata instead. Those discussions should consider
the true existing problems that currently exist and an honest, holistic point of view needs to be the base for any
judgement.

In almost every software project I worked, (test execution) _speed_ got way too much attention at the cost of
the remaining desiderata. This is my point of view, because there is never a particular problem that gets adressed
at all by prioritizing it, no developer workflow I have seen actually requires or truly benefits from it.
It seems as if speed is naturally the first thing most developers chose to optimise for because it is the easiest
to write from scratch. The measures to prioritize speed, for example heavy mocking or introduction of interfaces is
entirely in the control of the developer, making it especially tempting.

My believe is that _predictive_ is the most important desiderata by far, usually encoded through a maximum amount
of fidelity in the test. When asking what automated testing in software development is for, everyone usually answers something
like "to verify the behaviour of the system is correct". This answer doesn't include the word "speed". Because speed
itself is only a secondary property of tests - it has to be kept high enough so that test execution does not interfere
with the development workflow in a too negative way.

Bonus points: Given that software development is more and more dominated by agentic coding, we already take a hit
in iteration time, because there is often substantial wait time when collaborating with an agent.
Test execution time of 2 instead of 10 seconds doesn't really matter in a minutes-long agent conversation.
But what indeed matters is the confidence a successful test gives you, given more and more control and knowledge is
transferred over to the agent.