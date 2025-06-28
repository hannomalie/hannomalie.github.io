title=Tests (and reviews) in the age of AI
date=2025-06-28
type=post
tags=workflow,architecture,design
status=published
headline=Tests (and reviews) in the age of AI
summary=While implementation code loses importance in AI driven development, tests (and reviews) gain some.
~~~~~~
## TLDR

- (Generative) Artificial Intelligence will play a key role in software development from now on
- Developers will need to take governance over AI generated code somehow
- Review work will be what's left over for the developer the more AI can do the implementation
- Tests and specifications will be the most important tool of a developer
- Blackbox and acceptance tests are best suited for use with AI implementations
- Classic "unit tests" will be rendered useless

## Introduction

Uargs, one of those AI posts...

But I have to hurry. Originally I planned to write down my observations and thoughts end of last year already, 
before the point of view becomes mainstream and it's nothing special anymore. But frankly, I was a) too distracted
by other topics I enjoyed more, b) I was and am still annoyed by the AI hype curve and c) I wasn't even aware
that I have anything to say of value that not everyone else already knows. Alas. A lot of conversations with
colleagues about AI turned out to still surprise me a lot even today.
I often have the impression that people are overly enthusiastic about AI
being able to generate mostly boilerplate code that should not need to exist in the first place. But more surprised that
very few people realize the significance about another topic in that context, which I push for ~ a decade: __tests__.

> **_NOTE:_**  We're not talking about AI in general here - it's about generative AI, so AI that is used to generate
code. Of course AI can also greatly enhance search results or summarize API documentations or whole codebases - that's
not the usage scenario I am referring to here.

## Tests

Depending on whom I talk to and what my surroundings are, I often enter discussions with polarizing statements, of the sort
_"Your tests are more important then your main code. Without high quality tests, you can not change your code, making
it a risk instead of an asset"_. Oftentimes I continue to motivate BDD and acceptance test driven development. Talk
about specifications and the need to have the developers write them and not strictly use them to encode pure user stories only, but also
for property based testing in the host language to get the best quality out of a project. I argue for high fidelity
that doesn't cost much nowadays and for tests that are as decoupled from your main code as possible - which means
black box tests are preferred. I wrote some posts about good approaches to testing [here](https://hannomalie.github.io/posts/ditch-unit-tests.html)
and [here](https://hannomalie.github.io/posts/test-at-your-boundaries.html).

One important aspect is __test first__. In the past, it helped getting a clear picture whether we actually know
what we want to achieve at the end or not. Additionally, in the spirit of TDD, that we only implement what's necessary.

Now we have generative AI. Let's imagine we either bootstrap a quick project from a template ourselves or even have the AI do it.
Then what should the AI do? We need to feed it information, but which one? Well, when we now directly write our executable
specifications as tests, we have maximum security that the AI doesn't mess up (as long as we instruct it to not alter the test
code and also check that regularly), because we can always see when our build (the test execution step) is red.

Here's another bold statement from me: The implementation starts to not matter at all anymore, a lot of developers don't even
look at the implementation code anymore already. And why should they? Someone is not happy with it somewhen later? Well, throw
the AI on it and tell it to refactor the code. But for what reason exactly? When humans read code, they care about
cognitive overhead, readability. The AI doesn't care, it brute forces its way through the problem, it doesn't need
good code at all.

That would mean the whole job is done now, is it? Well, no.

Think when people tried to sell you TDD with the simple example of implementing an _add_ function for numbers. You can write a 
test that asserts 1 and 1 results in 2. 1 and 2 results in 3. You can hardcode those numbers in the implementation and
the tests pass. TDD shows you, that there is the _refactoring_ step, which will ultimately dominate and force you to make
the code abstract so that it works for all numbers. But you _could_ (and should!) also boost your tests a bit, so that we
advance from a specifictation for some conrete numbers to a range of numbers. For that, we have tools like property
based testing and generators or sequences. For algorithmic problems, that's always a bit silly, because ultimately the
tests and implementations look like duplications. For other problems though, it's mostly being as exhaustive as possible
over the input data of the system under test. That works.

This idea becomes essential when using generative AI. Because you are not the implementer anymore.

__Before__: It is your responsibility
to deliver a correct implementation (that also fulfills some other goals, like being performant, readable etc).
You did so by working concentrated while doing the implementation and proofing
your work with tests, before it was (most likely) finally proofed by a merge request reviewer.

__Now__: It is your responsibility to proof the correctness of the implementation of someone else, the AI. What's
left over is two things: executable specifications, so that the result of the AI can be proofed somehow. And then, whatever
quality is left over to be assured, is done by the new form of code review. Automated tests are still more important
than review, for the same reason they were more important before AI came into play: they can be repeated cheaply.

You can twist it as you whish, but one thing is clear: Ai is meant to gain us __efficiency__. Resource efficiency.
More precise: __human resources efficiency__. Given that generative AI will at some point in
time actually be able to be the sole implementer, it is only a matter of time, until the only left over work for the
human is to not write fluffy and sluggish prompts but professional specifications. Either as we know them today, as tests or
maybe even as prompts, allthough I am not sure what exactly we would gain by doing it like that, because it would then
again need to be tested automatically and given it's non deterministic, that's a bit hard to do.

> **_NOTE:_**  There is a voice in my head that tells me it's quite possible I see it wrong: Maybe there is effectively
no difference at all in the end. For example traditionally, tests don't need to be _perfect_, but hit the sweet spot
of value for effort. You know, because the developer is at the same time the implementer and knows he didn't mess
it up so hard. However, that very risk that is created, the reliance on the human factor that is not encoded into
automated tests is probably what comes back to haunt you when none of the original developers are on the project anymore
and new people try to apply changes - because what's left for them is only the automated tests, the human factor is gone.

I've seen people have AI also generate unit tests and I started to shiver. Unit tests are those tests developers
write that are always coupled tightly to the implementation (aka structure of your code) instead of the behaviour.
Those tests are the ones that always need to be touched, even though one is only changing implementation details.
In AI scenario, this is problematic: We cannot rely on refactorings anymore, because when we don't prevent
coupled tests and have the blackbox approach, we need to change specifications, so we alter the safety net! It was
always the same problem, even before AI, but now it's amplified, because it's not the developer anymore who applies
the changes, naturally slowly and carefully. It's now a robot doing it at twenty times the speed. And no human is able
to afford the concentration to carefully review those changes, it would be exhausting.

So in _summary_: Assuming gen AI will be the sole implementer in the future, __unit tests become completely obsolete__.
The developer still need a tool to feed the AI info what the system should do, so either above mentioned __blackbox acceptance
tests__ will remain the best tool or we find a different, better way to do it with prompts. Which I currently doubt,
but developers will do everything besides the right thing, not? ;)

## Reviews

With gen AI, generally, the _reasons_ to actually
do a code review _vanished_. What should a review by a human being achieve now? Or further, shouldn't it be a review
by an AI? That we then just... well, review? 
A second AI can review the tests that were written by the human and maybe additionally the code that
was written by the other AI as well.

As mentioned above, AI is about _efficiency_ and bringing cost down. Not that we didn't have plenty of other ways to
make software development efficient and cheaper, but we developers and managers created so much _bureaucracy and process overhead,
false concepts of quality_, that everything is super inefficient and the result oftentimes just some garbage nobody can efficiently work with.

Especially reviews in their most dominant form (async, blocking mr reviews) are a giantic waste of resources:
Developers request to have four-eyes-principle,
yet nobody wants high-frequency feedback in pairing or ensemble, because you know, they are shy and uncomfy when someone
watches them code and they have to talk with people about different points of views,
or they can't concentrate aka watch netflix or do laundry at the same time. In lean words, much
waste is created. For what? So that late feedback can be rejected easily because it would be too much work to refactor everything.
Because most developers don't want to hear that actual quality can only be built in when as many people from the team as possible
participate with high frequency communication in the development ([this is a must read](https://www.infoq.com/articles/co-creation-patterns-software-development/)).

So let's assume we're over all the unimportant things: typos, small mistakes, variable names, size of functions, use
of MVC pattern or not. All those things that are in the end just favours, just one end of a spectrum of advantages and
disadvantages, created by and for _humans_ and their _organization into communication structures_.

In the past 10 years, I went through a lot of review styles. All of them of course discussed with the colleagues before,
so no, I didn't mean to play the nitpicking asshole. For example I had MRs with 30 comments, 28 of them nitpicks.
Or we did rough review - even nonblocking, so after merge - and only if something is super duper wrong it was commented.
I also worked in a team where we found out review gives us exactly nothing, so we ditched it.

But one thing was always the same: The tests protected us. Ultimately, the code and the code quality was not important.
When it was too low, we just realized it and then went to enhance it. You can always anhance it, as long as you have
the tests protecting you (or as long as you have an other afforable tool that lets you do it, like
efficient manual testing, but different story). And as mentioned above, _good_ or _bad_ is highly subjective.

So why should we at all review the AI generated code if it fulfills what we specified? It can be enhanced by telling
the AI to do so in minutes. And when we realize that there are bugs in the code that our tests didn't cover, the same
thing as before is true: Your tests are insufficient.

On the plus side: The implementer is now super fast, the review can happen exactly afterwards, there is no need anymore
for a handover and a blocking wait between two humans. That - finally - is the best bet to eliminate the waste
in the default development process I see people use. I can't wait for that :)

## Responsibility

There is this famous discussion about who is responsible when an AI driving assistance rolls over the child instead
of the senior when there is no other option. It doesn't matter whether a human driver is a much higher risk than
the AI. A human can be made reponsible for actions, while an AI cannot (currently). So when using genertive AI,
who is responsible for the work and its effects? In case it's not clear yet, it's the developer who is made
responsible. So ultimately, this is a very uncomfortable situation. Theoretically, we can let the robot to our work now.
Yet, we need to proof the work. How do we proof the work? And even more important: How do people proof the work when
they don't understand the work anymore. You senior developer with 20 years of experience can probably read and correct
the AI code, allthough you might be surprised how fast you get rusty. Imagine a junior who now grows up by heavily
using AI, he will inevitably not have the skills to be able to take over that responsibility anymore.

People often say we had this problem before already, with people copy-pasting from stackoverflow without understanding it.
The difference is, that now our AI can be _instructed_ to do things and it can be _passed an individual context_ to work with.
That means we have different approach to input compared to the stackoverflow example. And a much better fit for a given
specific situation. So the amount of transfer work approaches zero. Which will exagerate the effect and instead of people that are moderately good coders who have some
skill left to understand stackoverflow code when necessary, we'll have people who can't get to a point of understanding
anymore in reasonable amount of time. Of course this is just my guess, I don't have any data on it.

Furthermore, the stackoverflow-workflow was always frowned upon. By the developers who did it as well as by the other
developers. Yes, some developers make fun of it in certain stages of their careeer, but that's it. Now I see using
AI _demanded_ by managers and lead developers. Which is again a completely different situation than before. It's now frowned upon __not__
using the workflow. It's legit now to not understand the code that "we produce".

I feel people don't see how problematic this development will be for our industry long-term. And
what it means in terms of responsibility that people expect other people to take over. Taking the responsibility
seriously requires carefulness and patience - which is the first things that gets thrown under the bus when time is tight.

## Closing words

Of course AI has great potential to accelerate what we do in software development, and it won't go away anymore.
Generating code is especially helpful for user interface code that can afterwards be reviewed and tested (manually?) by the developer.
For implementation code, I would have loved to see the industry more at a point of [this](https://hannomalie.github.io/posts/domain-modeling-made-functional-1.html)
where barely any enhancement is necessary or possible (is it?).

I see big potential in the idea that the form and structure of implementation code is getting completely irrelevant in the future.
I see that becoming possible in practice only when we __increase the importance and quality of our testing__ and favour blackbox and acceptance testing
styles as described [here](https://hannomalie.github.io/posts/ditch-unit-tests.html) and [here](https://hannomalie.github.io/posts/test-at-your-boundaries.html).

What we should not forget is that we are __trading efficiency for risk__ here: When you generate all your code and it's never
changed by a human anymore, then it will also become impossible to do so for a human, this is our famous __Conway's law__. So
you better make sure you never have to actually touch the code - we have a big risk here that basically all our currently
established ways of working with code try to minimize.