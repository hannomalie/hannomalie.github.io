title=The problem with persistence ignorance
date=2025-03-01
type=post
tags=code,web,architecture
status=published
headline=The problem with persistence ignorance
summary=Ignoring technical details might indeed not be the good advice we want it to be.

~~~~~~

## TLDR

A word or warning. I didn't manage to get this post as structured, easy to read, objective as I would have whished.
This really turned out to be more of an opinion than anything else. Over the years I had a hard time accepting
the dominance of the _"I don't care about the implementation details"_ mentality that is behind _persistence ignorance_.
In my experience it didn't do too much good but caused a lot of friction that would not have been necessary with a more
balancecd mindset. Persistence is and will ever be a sepcial topic that comes with a lot of challanges and I don't
think pretending it doesn't helps. Here you can read about some experiences and thoughts on the topic.

## On persistence ignorance

Domain driven design had a huge impact on many of the projects I worked on over the last decade.
In the past, [I have written about DDD](https://hannomalie.github.io/posts/the-ddd-controversy.html) already.
In essence, the idea to closely collaborate with domain experts is one of the biggest benefits of
the approach. Techniques like event storming can also be very helpful to get a first order into chaos.
However, I consider tactical design as waste and strategic design only rarely helpful. Not only DDD but also
other practices like hexagonal architecture and others have one thing in common. Something that became
ubiquitous, something that became the default in most of the projects I worked on:

They motivate that you __only care about the domain and not about any technical details__.

And they are very clear about it. Let's take an excerpt from one of my absolute favourite books,
_Domain Modeling made functional_,
_Chapter 2: Understanding the domain - Fighting the Impulse to do Database-Driven Design_:

> But if you do this, you are making a mistake. In domain-driven-design we let the domain drive the design, not the
database schema.
It's better to work from the domain and to model it without respect to any particular storage implementation.
After all, in a real-world, paper based system, there is no database. The concept of a "database" is certainly
not part of the ubiquitous language. The users do not care about how data is persisted.
[...]
Why is this important? Well, if you design from the database point of view all the time, you often end up 
distorting the design to fit a database model.

My complain about the advice above is that it is __too one-sided__. Of course, when a book is about
_domain modeling_, it will probably advice to focus on ... well, domain modeling. Books about
database-centric design probably tell you to do database-centric design. So at that point I am not sure
what I am arguing about at all, nonetheless I have to scratch that itch.

## How design should be done

Critisizing is cheap, what's a better advice then? My answer:

Let the _more important_ things guide the design of the system.

In most projects I worked on, I perceived the domains as rather simple, compared to the technical complexity
that had to be managed. So it was rarely more challenging to understand the domain than it was to handle
the implementation.

That might partially be because of the microservices trend of the last decade.
Because in distributed services and especially microservice environments, there are already lots of
boundaries created that more or less isolate subdomains, which by definition makes those domains rather small.
And no, you dont strictly need DDD to get at that point. Subdomains or boundaries can also naturally
emerge and they can also be a good fit. Other people might make different experiences, but my point still stands.

What I noticed is that the focus on the domain is driven by the non-developers. That's fine, but you know who
has to maintain and advance the projects, the actual code? The developers. So the developers should
be the ones deciding what element should be the dominating one. Yet, in the environments I worked which found
this domain-focused approach, it became a sakrileg to question it. And that's wrong.

## Irrelevant claim: In the paper based system is no database

> After all, in a real-world, paper based system, there is no database.

Correct. And in which scenario is this claim relevant? Right, in a paper based system. In a paper based system,
there is also no screen, but nonetheless when we're talking about software projects, we're talking about
a screen where the stuff gets displayed, yes? And in a paper based system there is also no remote
execution, but when doing web apps, there's certainly a network between client and server. Pretending we don't
need to care about those crucial elements because they are irrelevant in some other, non-relatable context is
just weird and inappropriate.

Furthermore. In a paper based system there might not be a database, but there is certainly __persistence__.
Because the paper where stuff is written on is the persistence. Has anyone ever talked about the non-functional
requirements of paper as a persistence? (I am sure people have). We don't, because everyone involved - developers
as well as domain experts - know how paper works. Which is not the case for a database. We could do the
experiment and talk about durability. If everyone is happy with using paper as persistence, what would
people do when the requirement is raised that the information needs to be recoverable 20 years long and stored
securely somewhere? Who is going to do that?

It's easy to assume things, it's easy to compare things, yet this approach falls short quickly.
Just as domain experts surely expect a computer system to somehow persist data that was put in. So
there __is__ a database, because that's a reasonable common tool to achieve persistence, which is surely
implicitly required by everyone without saying it. Whether it will be a file stored on disk or an actual database
is an implementation detail, sure. But then again, it would be better to design and implement every single
usecase based on the actual implementation detail from the start, because then it will immediately become
clear when the implementation is insufficient.

## False: Users don't care

> The users do not care about how data is persisted.

Oh they do. They care about the characteristics of how stuff is persisted. They have an expectation about
how long and how fast it is persisted. They have an opinion about how they can access already persisted data.
Most of the time those functional requirements are implicit and nobody talks about it. That doesn't make
it non-existent, it only makes it harder to talk about it.

## Maybe: You often end up distorting the design to fit a database model

Well, __distorting__ is a tautology, it's a negative word. It could as well be __guide__ or __steer__. Your
database model could guide the design of the system.

I would say that either case is too one-sided. When a usecase is on the table, the usecase drives the design.
Your database as well as any other part is just part of implementing that usecase. In most projects
the database and the evolution of the database has been a much havier concern than the few classes that
model the domain.

Let's talk about constraints. When your domain defines some states, those should be ensured somewhere, not?
When you do it in some isolated classes only, than that particular encoding of the domain is fine.
And then you need to persist stuff. Since you already have valid domain models, the persisted data is also
valid. And now you make a change to the domain implementation and the validation changes. New stuff works.
But what about the old data? Well, it's now invalid. And your persistence also doesn't validate it, so
it needs to be migrated. That migration is done in ... in the persistence. So now out of a sudden the
persistence get important, does it? A lot of times the validation ends up to be in the database, as well
as in the domain code. 

Or let's talk about structure. Let's say your code encodes the domain with 3 main entities. And now
things change and you change the code and now it has 5 main entities. In your code, quite simple. Adjusting
the persistence and existing data? Not so much.

What I want to say: One cannot go without the other. They are __coupled__ one or the other way around.
Pretending they are not just doesn't help, because from my point of view it decouples things that live together
and rely on each other, influence each other. I reminds me of the times where databases and schemas were created
and operated by different people then the developers. I don't think that was a good approach.

## Why persistence is hard
 
I briefly mentioned it in the past when I wrote about boundaries and especially testing at the boundaries:
Persistence is a boundary between the past and now. It's natural that changes around a boundary are harder
to apply, that's inherent for a boundary. Running stateless workloads in Kubernetes is so much easier than
running stateful ones, eg databases. Ever wondered why? Because you cannot freely recreate it at your will.
It's inevitable.

Additionally. Persistence has many forms and characteristics. Most of the characteristics are somehow implicit
and "nobody needs to care about it". Yet everyone expects data migrations to be supported. And accessing
lots of data out of a sudden must happen in miliseconds.

I was in a project once which was a greenfield project. Team sat together and did a rough sketch what the service should
look like. It was clear persistence is needed. One colleague clearly came from the "keep it simple stupid" direction,
which normally is __my job__ :P. But I didn't like the situation much.
He proposed to use file system and persist to files. Cant get much simpler than that,
true. A debate started, one colleague asked how we could do migration of data formats with stored files.
The debate got a bit heated because some people claimed that it's not clear that we need migrations at all, hence
we should not prepare for it.

After all I don't like to pick a side on that, because there are valuable ideas on both sides. But the point is
that simply using Postgres as our paved path would have taken minutes to setup, ops knew it and it supported literally
every usecase you could come up with. Rendering the whole discussion close to worthless, because it took longer than
just implementing it with a database and be happy.

## Closing words

Well. Have I been able to make any reasonable point why persistence is always more of a dominating factor then we
admit? I don't know. What I can say for sure: My experience told me it is. And I am not just accepting it
without questioning it just to stick to a mindset that was the status quo at a time I was not even doing software
development. Maybe I am repeating the mistakes of the past. Yet, I cannot help it. Am I sometimes underestimating
domains? Sure. Bet I am not the only one. And it's something that I experienced as very easy to correct in hindsight
as well. Am I sometimes over-focusing on persistence? I bet. All in all, my conclusion could only be that we need
__both__ sides and try to objectively determine which side should be the dominating one based on how hard it is
to handle it.

Having that said, the above mentioned and cited book is pehnomenal. And functional domain modeling
is phenomenal and I will write about that topic soon! Sadly, I cannot tell that I have been able to use that approach
in production projects (not really and long enough at least).
And that very thing could be the missing bit for me: That the approach of modeling the
domain is so much better and causes so much goodness that I can conclude it's the right way to go.
So long for now.