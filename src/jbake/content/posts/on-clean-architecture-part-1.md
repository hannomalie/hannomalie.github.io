title=On Clean Architecture (Part 1)
date=2024-09-06
type=post
tags=architecture
status=published
headline=On Clean Architecture (Part 1)
subheadline=On "The advice to keep options open"
summary=Clean Architecture and its author are under fire nowadays. I see some reasons for that and try to elaborate on the situation.
image=images/soap.jpeg

~~~~~~
## TLDR

- Clean Architecture is quite a good read and contains a lot of good stuff to think about
- I don't agree that seperating the policy of a system is a good idea in most projects
- People don't care about policy - but about behaviour of a system. That's more important from my pov
- I consider pretending existing decisions in the company don't exist as a bland mistake
- I see preparing code to change the major application framework as a waste of resources, needlessly overcomplicating the project
- Sticking to what the rest of the company or division does beats snowflaking and growing tool zoo
- Don't abstract over something until you don't at least know a few concretions of it


## Clean architecture and Bob Martin under fire

For people that are rightfully avoiding the internet and drama around all the things, my short summary: Clean Architecture
was a foundational opus for around 2 decades. I've seen so many software projects written with the stuff from that book
in mind. Yet, in the recent years, not only the ideas behind Clean Architecture got questioned more and more, even
the integrity, qualification or trustworthyness of the author (Bob Martin) gets attacked nowadays. The latter one is
probably a consequence of the former one.

As always, the way most people critisize is probably too harsh or even unfair and unprofessional. However, there are some
takes that are quite reasonable and justified. Like the ones from Casey Muratori with "Clean Code, Horrible Performance"
or Dan North's "CUPID principles", allthough even those takes are a bit salty, let's not be too sensible here.

While I sympathize with almost all of the takes from both sides, I can't lie: Yes, I also experienced the majority
or projects that applied Clean Architecture as overengineered. I didn't enjoy working on most of them until they
were changed quite a lot - often exactly by removing whatever Clean Architecture ideas were applied.

So I wanted to find out why that is and gave the book a reread. And I was somehow surprised. Not only is the book really
nice to read. It's concise, contains tales as examples, some history. It's thoughtfully written. It almost never
deals in absolutes, emphasizes the pros and cons of ideas. For almost every point in the book I though "Oh yes,
that makes sense". So what actually is the issue!?

I can only pick a few spots form the book and try to find out why they might be the reason we have that controversy now,
or what applied ideas led to unenjoyable projects for me.

Since I can't get anything big done in my limited time, I will always pick only a single random piece from the book
and do multiple posts over time. Hopefully. And we start with:

## The advice of "Keeping options open"

In chapter 15 there is a paragraph that I don't want to quote completely, but some parts:
> All software systems can be decomposed into [...] policy and details.
The policy is where the true value of the system lives. [...] The goal of an architect is to create a shape
for the system that recognizes policy as the most essential element of the system while making the details irrelevant
to that policy. This allows decisions about those details to be delayed and deferred.

Then come a lot of reasons why it should be that way. The thing is, this whole point of view does only
make sense when one accepts the arguments prior to that chapter, the prerequisites. Why is the policy where the true
value of the system lives? Nobody really cares about the policy, people care about the behaviour of a system,
not it's policy. When you work at a company that does microservices and has one or two frameworks typically used for
all their services, then __yes__, it __is__ necessary to adopt exactly those two things: HTTP apis and for example
Spring with Java. The "business rules", often times very simple compared to whatever tech stuff you need to write
a service, are not that important that it trumps choice of framework or programming language. So arguing that
a specific, pure encoding of the business rules is the most valuable thing for a company is not meaningful, or at
least not in all cases - I would even say close to never in the projects I worked on.

What baffles me is, that the chapter even handles that exact point, I cite again:
> What if your company has made a committment to a certain database, web server, or a certain framework? A good
architect pretends that the decision has not been made, and shapes the system such that those decision can still
be deferred or changed for as long as possible.

It baffles me, because this is such a bad advice! How can the decision which framework is used be done already
at company level, yet we pretend it's not done? Why should we write our code in a way that is flexible to
use another framework, yet this is almost certainly not needed? The flexibility and that decoupling comes at
a significant cost that we will never get repaid, because we never change the framework.
And even __if__ that decision is questioned many years in the future somewhen - our code can then _still_ be
changed to that changing the framework is possible. Yes, it will be a bit more effort, but it's better to
not pay high upfront cost (useless abstraction) for something that never happens, than to pay somewhat
higher cost when it _actually_ becomes necessary.

The same argument gets even more clear when we talk about
databases. Who ever changes databases? And even _if_, the change is made between two somewhwat similar database
systems. Yes, sure, you can have the snowflake counter examples from the 2000s, but nowadays we have postgres.
I have done both: Just as an experiment changed the web framework and also changed the database, without
preparing anything for that. It's not as difficult as people claim it will be, not nowadays.

Otoh when some snowflake engineer picks the file system as a database, while the rest of the company uses managed
postgres, then he imposes significant risk and effort onto the company. The advice in the book is simply not
sensible advice. There might be reasons to write some very simply repository abstraction
(read my post https://hannomalie.github.io/posts/test-with-the-database.html) but that's about it. And when you
dogmatically keep even JPA annotations out of your so called "business rules" for the sake of it, then you will
create exactly the unnecessary complexity that all our architecture and design attemps were originally trying to solve.

## The advice of "Keeping options open", again

In the last paragraph, I basically argumented for __YAGNI__ - You ain't gonna need it. Don't introduce an abstraction
that you won't need. A close friend to that is doing the _wrong_ abstraction, letting alone the idea that
every abstraction is leaky.

Have you ever been on a project where people desinged an abstraction for which they
didn't know the usecases? I mean where not at least a single one - better two - usecases where on the table and the design
was at benchmarked and validated against those two usecases? Then you probably know what I am after. How can you
anticipate what your design needs to look like when you don't have the usages yet? In all those "business rules first"
projects you have a form of that problem: 

People design their abstractions because they insist thay don't need
to know how the persistence is actually implemented, just that whatever implements it fulfills some interface (the language construct).
But what the persistence really needs to do is fulfill a _contract_ that can't be reliably encoded with any programming
language I know of (take a look at https://hannomalie.github.io/posts/test-with-the-database.html to see a glimpse of
what I mean). So when you take a relational database, you pretty much end up using primary keys. When you take an
object store, you not necessarily have any identifiers at all. When you have an in-memory implementation and few example data,
having a "getAll" method might be fine, but on a real database, that method will almost certainly blow up.
How do you write an API that accounts for that?

Sure, sure. Its possible of course, there are plenty of ways to do that. I won't write down a gazillion implementation 
options now because the important point is: Whatever it is you want to abstract over, complexity is introduced that
is not needed otherwise. Additionally, if you don't know the details of what you want to abstract over, your design
will be insufficient. In further addition, when we assume all abstraction to be leaky, even if you have perfectly anticipated the unknown
and a good design, your abstraction will cause damage to your code that wouldn't have happened otherwise.


So far, see you next time!