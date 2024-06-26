title=The DDD controversy
date=2024-05-03
type=post
tags=workflow,architecture,design
status=published
headline=The DDD controversy
summary=Domain driven design is as old as some programmer's beards, yet recently it gained some unexpected attention. And hate. I explain my perspective and how we can stay healthy with that topic.
image=images/post-it.png
~~~~~~
## TLDR

- People oversell domain driven design
- Leading to other people dismissing it in its entirety
- Means a lot of people miss out on its good parts

## The domain driven design (DDD) controversy

So first of all the elephant in the room: We will first treat DDD independent of hexagonal architecture. And also
from microservices architecture. Yes, DDD coencepts fit well into those both architecture styles, but they are not
directly related at all. Different implementations might be an equally good fit.

So regardless of what some people said and some other people might or might not have misunderstood: I think
I know why there is a controversy around DDD. It's because some of its parts are great and offer big value for
not much effort and also value that is hard to gain from other tools. But on the other hand some of its parts
are quite complicated and aim to deliver questionable value for a lot of projects at best.

## My background

If you don't know anything about DDD, you will need to read it up. I am not the right person to teach it in any way.
Heck, I am barely capable of applying it somewhat correctly. Yet, I have some honorable experience with it: I had multiple workshops
at multiple companies, a certificate (uhhh!). I applied DDD partially at a company in real projects with real people.
And of course I spoke intensively with people using at least parts of DDD at work throughtout the years.

All I want to say is: I already spent quite some time on that topic that someone had to pay money for (I am not
a freelancer, was almost always internal employee). Given that I am probably a reasonably smart person, that already
tells us something: You have to invest quite a bit of money to get a seat on the table at all. DDD can't be
learned overnight, it can't be learned by a person in isolation, it can't be learned by self education (well).
A lot of newer content derivates from the original ideas. Everyone (including me) wo is not the original author,
a certified trainer or sth. similar, brings in his own perspective, washing it out even further or might be
plain wrong about some things. Additionally, related technology, culture, projects ... that all have changed
a lot since DDD was created. I would consider it close to impossible for a tool to remain relevant in its original form
for so long. Or maybe it's possible and I suffer from repeating the past, because honestly I am also not that old yet.
However, __in essence, DDD is a way to structure communication.__ Communication, which is normally quite unstructured, think
about the communication in your company. That brings us to the first aspect.

## CON: DDD is a language - learning it means effort
DDD introduces a lot of terms and words, ideas and concepts. You can only understand them, if you speak that language.
What about people not speaking your language? They are left behind. This alone can be quite problematic,
because suddenly everyone who wants to work effectively with the others, needs to be taught an additional language.

## CON: DDD is not a simple language
The ideas behind it and the terms of DDD are not that easy to understand. Definitions are sometimes vague, sometimes
abstract. "Real things" can sometimes be mapped to different things in DDD, so there are multiple ways to represent
one single thing. That makes the language complicated and not easy to learn. While reading the internet,
there seem to for example be a lot of discussions what an aggregate is. You can talk about those things endlessly.

## CON: DDD overloads well established terms
Additionally, DDD defines some terms that are also defined in other domains. Take "service" for example. DDD means
something completely different with that word than what any software related person might expect. It's neither
an application service, nor a Spring service, nor a microservice, nor a service of any service oriented architecture.
It's best comparable to a static method in your programming language. Whether the terms are easier to understand
for people of a spezialized department, product people or even users, I can't tell. But to be very honest I doubt it.

## CON: DDD can only work when used pragmatically
Good coaches and consultants helped my companies with DDD always stressed that there are simply cases that need to be
handled pragmatically. For example the costs of keeping a technical identifier out of your domain might be so gigantic,
that it's simply not a good decision to do it, just for the sake of following DDD rules dogmatically. On the other hand,
I had discussions with engineers not sharing that view: You need to keep everything out of your domain for the sake of it
or you are bad. You have to keep third party dependencies out of your core at any cost, not even stable and fundamental
result types that are not part of the standard library are allowed. Okay, cool. We're not caring for intentions and
careful decision making with pro and cons in our context, but applying some technique without any reasoning. Every framework,
every architecture, every tool runs at risk creating those situations and DDD is no different here. You __will__
have those discussions between your people. Decide for yourself whether they are capable of managing them.

## NEUTRAL: DDD is an opinionated structure
Unstructured communication might be bad. But what about structures that doesn't fit you? What if you have developers that
are quite capable to have conversations with stakeholders, get some nice requirements out of them, work in a collaborative
and iterative way and do successful projects that way?

## PRO: Ubiquitous language between different people
That's one of the major goals of DDD. And by putting all the people together and have them model together, they
certainly converge towards the same language for their whole work. Sure, this can also be achieved by comparably
intensive communication, but I would say that rarely happens without applying some tool that demands it.

## PRO: Focus on the business perspective
A similar point. A lot of engineers don't really care for the business perspective when doing projects. There are always
other roles that keep track of that. 
To be fair more often than not engineers also simply don't get any chance to involve themselves in that.
But different perspectives really help engineers making better decisions, it
increases their holistic understanding of their company, their domains, their project landscapes, their sibling teams.
It will create or foster their end-to-end responsibility.

## CON: Not all parts of DDD are equal
There is (ususally) Event Storming, Domain Modeling, Context Mapping, Strategic Design,
Tactical Design and probably some more things I forgot. I can see great value in some, great harm in the other elements.
And I think applying the bad ones leads to what all those anti-DDDlers rightfully hate.

### Event storming is great
Event storming for example is a great way to utilize
swarm intelligence of your people, have different roles communicate with each other and get a common understanding of
one's system landscape. Event storming doesn't introduce a lot of vocabulary, it doesn't take too much
time to execute it and so conclude that it is probably a good investment for everyone. Other tools
like BPMN try to deliver on the same goals, yet are just way more complicated than event storming.
Also, for me personally, it was always a lot of fun, it didn't feel like work for me, which I consider a good sign.

### Domains and subdomains are great
The same goes for identifying your domains, subdomains and classification as generic, support or core domain. You have
a conversation of people with different roles, they will all get an understanding of what domains exist in your company
and how important they are for the business. Not much effort, great value. I don't know how engineers
should get those business related insights elsewhere.

### Problem and solution space concept is not so great
The struggle begins. Let's start with __strategic design__, or for some people easier to grasp: bounded contexts.
Those are parts of a domain where you agree to have a ubiquitous language.
You can have multiple ones in one domain, yet if a bounded context spans multiple domains, you usually have a design
issue which you should solve. Finding out what your bounded contexts are, is not that easy anymore. It would have been
very easy when there would always just be a 1:1 relationship between bounded context and subdomain or that those two are
the same. That's however not possible, because DDD says that domains are the "problem space" and "bounded contexts" are
the "solution space". Even though I spoke to other engineers who share my opinion, I will just speak for myself:
This is unintuitive and it smells like overengineering.

On code level, everyone and their aunt constantly tells
us that simplicity is the key to good projects. When using DDD, at some points you need to use some pragmatic escape
hatches nonetheless, so there's no need to have a 300% flexible solution if the cost is such a complexity. If suitable
for 95% of the projects, subdomain to bounded context should just be a 1:1 mapping, resulting in complete removal
of the distinction between those two spaces at all. Dramatically easier, less bloat, close to no loss. Keep in mind
that I am a software engineer and the perspective might be different for non-engineers. Yet, when software people
need to be open to take other's perspectives, maybe this is a place where the other side can accept the compromise
for using a single concept and share it, instead of having two concepts that need to be mapped to. Especially,
when those contexts are not yet any system or service at all, therefore, another mapping is needed...

### Strategic design is partly good
Furthermore, those bounded contexts have relationships. For two interacting contexts, one is the downstream and one is the
upstream. See the paragraph about overloaded terms - those terms mean sth different than engineers expect. They determine
who of the two is propagating its model. Take for example a contracting context. Legal requirements make it
often impossible to not follow a certain structure, hence such a context will propagate its model. Regarding those
relationships, DDD names some patterns, like Anticorruption Layer, Open Host Service etc. Those are quite helpful,
because you can clearly see and name how two contexts relate to each other. Having that said, context still doesn't mean
"system" or any other technical thing yet. So I really wonder __who__ is actually interested in that information?
Who else than the engineers cares about the relationship of two systems, uhm, pardon, contexts? So even though
the classifications are roughly good, it would just be easier if we could talk about services at that point...
Oh wait. And when it's not about services (it's always about services... jk), it's about code - and then only
the currently pairing engineers need to talk about it, because certainly our code is good enough to be understandable by
navigating it and reading the tests, right? 

### Tactical design is waste
The next stop is tactical design, the thing where a lot of very complicated vocabulary is introduced that
overlaps with tons of stuff engineers are already dealing with and familiar for 20 years. The decomposition
of your contexsts into entities, value objects, aggregates and what not is an aweful lot of work and for me it also
really felt like work, I didn't enjoy it. It felt like applying an abtraction in a foreign language for something
that will at some point become code one or the other way around, using similar but different abstractions on the
code level again. It's up front design in a dimension which I really can't consider as a good idea. Furhtermore,
Who is interested in that level of information? People from a spezialized department? Product owners? I don't think so.
Engineers? I guess they are equalliy interested in designing everything in UML and generate the code afterwards...
So conclusion: This is so much effort for close to no benefit from my point of view. After I saw how unintuitive and
overcomplicated the resulting designs are, I don't want to see that stuff materializing in the code. It's just too complicated.

### Tactical design means Object Orientation (OO)
Might be again my ignorance, but I can't see a way how tactical design would not be about object orientation.
I mean rather the classic OO of Smalltalk or maybe Ruby. Would mean your code will look like that as well.
Even though you do Java and built-in language constructs or established frameworks are not a good fit for it.
Even though object orientation at all might not be a good fit. Sure, sure, one can be pragmatic now and whenever
it makes sense to do data oriented design, sprinkled with some functional abstractions. Will your engineers realize
it when it's a good idea to deviate from the plan? Why make a plan on that level of detail in the first place, then,
what value is left in there?
I will again be honest: Personally, I don't favour OO designs anymore, because in most of my projects
I see a better fit for other approaches. So every design tool that favours OO is something that I am immediately not
after anymore and I expect that there are simply a lot of experienced engineers sharing my view on that.

## The microservices controversy
So DDD doesn't imply any software architecture, yet people immediately jump to hexagonal arch and microservices - why?
First of all because people who teach you about DDD will use both as good examples and always talk about them.
Second, this is because they are a particularly good fit from their perspective. Bounded contexts are 95% of the time exactly
what you would want to span your microservice over. That's because you have a shared language in there and most of
the transactions. And well defined inbound and outbound communication, often directly translated to events that are dispatched
with a middleware. Transactions in a context are not technical transactions, but again, people enjoy mapping
them 1:1, making their lives easier, which seems reasonable to me. There is actually nothing that prevents you from 
having a modulith, making your contexts submodules and your "events" (remember, that's not a technical event...) a
function call on an interface. Indeed, it would be a perfectly fine implementation, yet most people seem to not
see it that way.

So even DDD and microservices are unrelated - microservices didn't even exist back then - we can't blame
people's impression that one often leads to the other, because people jump over to it quickly.

## The hexagonal controversy
Same for hex arch. Yes, the architecture let's you define a domain module and have it isolated from everything else.
Why do you want that, what's your intention to seperate your code like that? If you have a good reason for it, fine.
Tell me about the reason and let's see if I buy it. Most people I spoke to don't have a good reason other than that's
what the architecture says.

And don't confuse my statement with invalidating some of the intentions of hex arch. For example one of the intentions
behind ports and adapters is to move infrastructure code to the very edge of your applications. Other architectures
follow the same goal, like "functional core, imperative shell" or simply by following some proper decisions when working
towards specific goals when implementing. Like speeding up test execution, extracting algorithmic code from
non-algorithmic code etc. pp.

Hex arch might have some of those goals as well, but it also creates a __vocabulary__.
Again, a language that needs to be learned, excluding people who don't speak it.
And it also says that you need to expose adapters for infrastructure components. Aha. You know what? If I don't have a
good reason to abstract my persistence layer, I won't do it. I do that decision on the code level, based on the context
the project is implemented in. And not by some interface-by-default rules an architecture forces on me. And I also don't
create a seperate submodule for the interfaces that are now named ports. I will go on call them interfaces (or whatever the language
I am in calls it),
give them meaningful, non-technical names and have them in the package where the actual data resides that I save and read from whatever
persistence it is. And when all that stuff is not a published api, then I will just keep all that stuff coupled
and evolve it together instead of have it loosely coupled, because the cost of loose coupling is not justified for me
by default and makes our work harder than it needs to be.
Whoever is on the level of applying hexagonal architecture by default, forgot how to be reasonable.

## A bonus perspective - from DDD to BPMN

Recently, I asked an architect colleague of mine for some takes on the topic. He's in a quite large project
that moved from DDD to BPMN. I can't give any info on the reasons for the transition - of course I came to think
that some of my points mentioned above might be relevant for it. Otoh what I _can_ state is a few other things.

- The transition of a project landscape away from DDD seems to be difficult. Everyone keeps using the DDD language.
People seem quite resistant to unlearn it. I guess that's a natural thing for every language, so not 
too big of a surprise. Where's a ramp-up, there's a ramp-down I guess. In this context, it doesn't help
with clarity and frictionless communication though.
- The code of some old contexts is very complicated and difficult to understand. It's explicitly the DDD concepts that make it
so. The point I understood is, that the code is more complicated than it needs to be. Which - so I guess - is
exactly what I wrote above about the tactical design and default architecture choices.
- The proejcts also use Event Sourcing and Hexagonal architecture. Both tools are applied poorly and introduce significant
complexity. It's especially difficult for junior developers to really understand the benefits of those tools, yet
in the project(s) are plenty of junior developers. Note, that even though some more-senior-like developers left the project over the years,
there was never big fluctuation. Now I gave people the best opportunity to argue "yes, applied poorly, that's the isuee!".
Two points: First, if it's incredibly easy to apply those tools poorly, maybe it's a problem with the tool then. _Skill issue_
is an annoying answer for an ofter complex problem involving people and tech. Second, I am not necessarily convinced
that those were really applied poorly. I think it's possible for my colleague that it just _looks_ like that, while
in reality it was applied just fine, but the reasoning to use it was not there at all, so it doesn't make
sense one or the other way around. That a lot of long time developers on the project are neither sufficiently familiar with the 
concepts of event sourcing and hex arch (and microservices), is just another hint, that those things are default choices,
made without asking a lot whether they are reasonable or not. Note that there might be other reasons for that, though.
- BPMN seems to be a much leaner approach and is so far positive for him. BPMN stays on the level of POs and architects.
So the developers don't need to get involved into that stuff and despite, they are able to work
with the artifacts quite well. Thinking in processes and steps is a good model for their domains. Those two points
again prove my view: Simply thinking in good old processes is just much easier for everyone. And having that business stuff
modeled in a simpler language, in a leaner way and with less people and roles involved, also just works better and will
be less intrusive for the resulting code. He's satisfied with the code developers create with that workflow.

It's always interesting to hear perspectives from different people, in different projects, with different experience.
Maybe in the future I will get to know more people and be part of more projects where I see better fit for DDD.

## Conclusion

So there you have it. Useless perspective number 834753674859 on the topic. At least __I__ today understand why DDD
is seen extremely controversial by a lot of people. Or certain architectures. It should be. I doubt I will ever be part of a project
where the domain is of such a different kind that I agree with the upfront design and abstraction effort DDD introduces.
But if I will, and I will be happy to regret what I wrote here and correct my course. But let's see that first.