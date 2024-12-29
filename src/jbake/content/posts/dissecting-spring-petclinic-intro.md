title=Dissecting Spring petclinic
date=2024-11-05
type=post
tags=architecture,design
status=published
headline=Dissecting Spring petclinic
summary=I try to apply various refactorings to Spring's example project, the petclinic. Let's then take a look at pros and cons.
image=images/spring_logo.svg
~~~~~~
## TLDR
- Spring's existence is to a big part responsible for the steady and healthy JVM ecosystem
- but its omnipresence also hinders evolution and growth
- and its programming model and sub-ecosystem impose a lot of solutions that have potentially better alternatives nowadays
- it's worth to explore other solutions, which I do by taking the Spring petclinic as an example project

## Praising Spring

10 years JVM guy here.
I made the experience that it is usually very hard to argument against the usage of Spring.
_"And why would anyone do that at all?"_ may some people ask, understandably.
Spring is a superb project. It's of high quality, it's as battle proven as it could get.
It's very cautious regarding breaking evolution. Yet, it evolved quite a lot.
It can do __everything__. It is supported by __everything else__.
It has an insanely large and durable community which made it the de-facto standard (web?) framework on the JVM
over a decade ago, which it still is.
The fact that every Spring project looks and works more or less the same is an unprecedented advantage
for onboarding and hand-overs.
Other ecosystems experience the same situation: Node with React, there is Laravel, Django, Rails and so on and so forth.
And that's great and nowadays I could just live with that and live a happy programmer's life and go on doing
Spring projects like it's 2015.

But.

## Time goes on

There is competition now.
There's plenty of content on the internet showing viable alternatives to Spring. On the JVM alone we have Vertx, Micronaut,
Quarkus, Javalin, Ktor, all coming just from the top of my dome. What was exceptional quality development 10 years
ago is common nowadays. All those projects have solid backing, community, design and project lifecycle management
and so on. There is no chance anymore for Java EE complexity, so no competitor is in such a bad light that Spring
will look especially "simple" next to it.

Additionally, JVM fell a bit out of fashion, because it's not primarily suited for the new need: fast startups, low
resource consumption. Which is not really a new need, but for the last 15 years we didn't care, because
we didn't have any hope that something can be as good as JVM development and also deliver on the other front.
But there is Go now. And there is Rust. And there is Node with TS. And PHP evolved.

Now usually someone drops by and answers _GraalVM_. And those kind of answers are exactly part of the problem.

## Local optimum

Because all the answers. To all the criticisms. To all the questions.
They are justifications.
They are never the "best" possible solutions. They are answers that are sometimes really
good, considering we are in a context that we can't change. Sometimes they are just okay.
And sometimes they could be a lot better.
They are local optimums.

This becomes painfully obvious once you talk to non-JVM people about that stuff. Like, everyone outside there
__hates__ Spring for its complexity. Hates Java for its clumsyness. When you observe non-junior programmers who didn't do Spring before,
trying to get into a Spring project, they will __curse__ because there is 15 years legacy Spring brings with it,
20 years legacy Java brings with it and Spring is everything, but not _easily explorable_. Which a lot
of developers rightfully see as one of the most important things about projects, frameworks and code in general.

## The curse of knowledge
When it comes to me personally, I suffer the burden that I cannot make things unseen. I found Kotlin and can't
get back to Java without feeling pain in every second line of code, because I _know_ how much better it could be done
with that other tool at close to no more cost if any. And I used a lot of other frameworks and libraries for web application development, seeing 
the same effect: I see how much better in different regards stuff could get with a different tool without losing any of the strengths the other tool had.
I see how many ways to code and do projects are never explored at all, because people only stay in the same
narrow context, doing stuff like they are doing it for 10 years - _it works, is good enough, why should we change?_
But like I introduced in the first paragraph, it's insanely hard to talk about it, people get angry, they try to deny or
falsify everything, often without any backing knowledge or experience. It's religious. And I hate it.

Yes, true, a company, team or person that invested in Spring does probably okay. Furthermore, switching away from it
faces us with the _status-quo problem_: We already have 15 Spring projects. And now we add a new one that is not
Spring. What cost do we all pay for the ugly wart a single project with a different framework would be. I saw too many
motivated people's ideas and discussions die exactly at that early point, to realize that innovation, growth and learning
will always lose that battle. This is partly because it is difficult to _anticipate_ what the value of such an approach
could really be, what different ways of working can make out of those projects everyone accepted to be complicated,
slow, expensive and whatnot.

## Dissecting the petclinic
So I need to do another thing for my sanity and my mental well-being and maybe it can help
any reader out there too when I write it down. I take the official example project
from Spring, the petclinic, and apply various changes to it. I will do the _"show, don't tell"_ game. It's much easier
to just __do__ the change in a representative project and then show the advantages and disadvantages on the living
subject.

> **_NOTE:_** 
Make no mistake, One could as well transform some Node projects to Spring or even some Spring projects to better
Spring projects. There's always things to enhance. It's just not __me__ doing it, because as I motivated above, I made
some particular experiences in my career and want to work with it. That doesn't mean what I do is the most important thing for
anyone else.

And we'll start with a benchmark for a topic I posted about recently: 
[Test at your boundaries](https://hannomalie.github.io/posts/test-at-your-boundaries.html)!
So enjoy [__Part 1__ of the series __Dissecting Spring petclinic__](https://hannomalie.github.io/posts/dissecting-spring-petclinic-blackbox-tests.html),
where I refactor all the tests in the petclinic project to blackbox system tests!

> 
## All series entries 
1. https://hannomalie.github.io/posts/dissecting-spring-petclinic-blackbox-tests.html
2. https://hannomalie.github.io/posts/dissecting-spring-petclinic-persistence.html
3. https://hannomalie.github.io/posts/dissecting-spring-petclinic-templating.html
4. https://hannomalie.github.io/posts/dissecting-spring-petclinic-browsertests.html
5. https://hannomalie.github.io/posts/dissecting-spring-petclinic-replace-spring.html

<!-- -->

> **_CHANGELOG:_** </br>
29.11.2024 - Added section for all series entries<br>
29.11.2024 - Decreased pub date of this post by a day to fix the order of posts in the overview :)<br>
23.12.2024 - Added new entries to list. Fix changelog dates.<br>
29.12.2024 - Added new entry to list.<br>