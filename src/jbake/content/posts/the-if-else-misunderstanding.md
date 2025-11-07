title=Let's talk about if-else
date=2025-11-07
type=post
tags=design,coding
status=published
headline=Let's talk about if-else
summary=I found an X post about if-else that bothered me. Let's talk about the aspects of if-else, the good and the bad.
~~~~~~
Man. I wrote a post about if-else with not 5 lines, not 10 lines, but with a gazillion lines.
Who will ever read that?!

## TLDR

- there is surprisingly much to possibly know about condition checking
- it's worth to not forget that we can actually discuss details of code and time pressure doesn't need to always win and prevent that
- judging other people without being an expert who really knows the topic in and out doesn't make a good impression on me
- the old debate whether to use inheritance hierarchies or if-else-chains should be about the expression problem and exhaustiveness
- we need to normalize questioning common beliefs
- judgement needs to have a solid base that shows understanding of pros and cons, and not arbitrary phrases with no meaning

## The X post

There was a post on X I can currently not embed here because the functionality is broken for me. Additionally,
I don't think X is particularly good for having a sane conversation and I also have no subscription, making it even
more unrealistic. And that's why I will just use my echo chamber as a kind-of reaction - I also had discussions on the given topic so often in real life,
that I hope I can use this post to just "tap the sign".
Feel free to see the "someone is wrong on the internet" meme in your mind's eye.

Here's the code and the text of the post.

```
In real world production code, using if-else condition becomes costly as one extra else could crash the application. Knowing proper replacements separates junior devs from seniors.

if (paymentType.equals("creditcard")) {
    processCreditCard(payment);
} else if (paymentType.equals("upi")) {
    processUpi(payment);
} else if (paymentType.equals("crypto")) {
    processCrypto(payment);
}

If you’re still writing giant chains of if-else statements in your core logic, you’re holding your code hostage.
They're simple, but they don't scale.

Once logic gets complex, your code becomes hard to test, extend, and read.

So what do senior Java devs do? They often replace them with the Strategy Pattern.

The Strategy Pattern is like having different ways to get to the airport. Instead of one giant function with if-else for "take bus," "take taxi," or "take train," you create a separate "strategy" for each. Your main code simply picks the right strategy for the situation and says, "Go."

Instead of endless else if blocks, you: 
-> Define a strategy interface (PaymentStrategy). 
-> Create concrete classes for each case (CreditCardPayment, UpiPayment).
-> Use a Map to pick the right strategy at runtime.
strategyMap.get(type).processPayment(payment);
Boom. Now you can add new logic without touching the old code. Clean, scalable, and maintainable.

interface PaymentStrategy {
    void processPayment(Payment payment);
}
class CreditcardPayment implements PaymentStrategy {
    void processPayment(Payment payment) { ... }
}
class UpiPayment implements PaymentStrategy {
    void processPayment(Payment payment) { ... }
}
class PaymentService {
    private Map<String, PaymentStrategy> strategyMap;
    public PaymentService() {
        strategyMap = new HashMap<>();
        strategyMap.add("creditcard", new CreditcardPayment());
        strategyMap.add("upi", new UpiPayment());
    }
    public void process(String type, Payment payment) {
        PaymentStrategy strategy = strategyMap.get(type.toLowerCase());
        if(strategy == null) {
            throw new IllegalArgumentException("Unknown payment type: " +  type);
        }
        strategy.processPayment(payment);
    }
}
```

For your reference, this code was posted by an account which claims to be a Java interviewer. What botheres me about it
is the arrogance about how to judge who is a senior or a junior, as well as the drawing of arbitrary conclusions
from data that doesn't even back those conclusions.

And now let's go step by step into it.

### Know your stuff: if-else
The very first sentence establishes a _wrong prerequisite_. It references an "_if-else_ condition"
but the example contains none. In the example there is _no else_. We only have _if_ and _else-ifs_.

That's not a nitpick. The fundamental difference is that even though in Java it's backed by the same statement (the language construct, ordinarily called _if statement_),
usage differs in important aspects. When you have an else in the statement, the statement is exhaustive, it means you literally handle __all possible cases__.
Whether it's a good idea to do it and what to actually do in the else clause is rather unimportant, the point is that you as a programmer literally wrote
the code that handles all known cases. We need to be careful about the else though, as it's the container for all stuff we haven't treated in the other clauses, but
the point still stands.

I cannot find the source, but somewhere I read that an _if without an else is a forgotten code branch_.

The fact that one can omit the else at all is a consequence of Java (and too many other languages) being _statement-oriented_.
If the if-else would be an expression rather than a statement, we would not talk at all, because then you would need to provide the else clause no matter what.

> Note that it would also not really work without actually using the result values, instead of for example using exceptions.
And if we want to nitpick, it would theoretically be possible to have an if-without-else statement as an expression, for example
by having it return a nullable value. Well, Java also has no support for nullability and I know no language actually doing such a thing at all,
because it is probably a dumb idea. So let's not be pedantic, post is long enough already.

The same goes for Java's "new" switch expression. It was added for exactly those reasons. Given if-else is only a special form of pattern matching,
so the switch statement/expression in Java (see [here](https://docs.oracle.com/en/java/javase/17/language/switch-expressions-and-statements.html) and search for exhaustiveness),
we could as well go ahead and only ever use switch instead of if-else.

So the equaivalent example to the strategy implementation would have been

```java
if (paymentType.equals("creditcard")) {
    processCreditCard(payment);
} else if (paymentType.equals("upi")) {
    processUpi(payment);
} else {
    throw new IllegalArgumentException("Unknown payment type: " +  paymentType);
}
```

also omitting the third case (crypto), because it was omitted in the strategy as well.

### What about the extra else??

> using if-else condition becomes costly as one extra else could crash the application

What a strange point.
If I understand correctly, the expectation is that an additional else would do the equivalent of what the strategy implementation does: throwing an exception.
Like I wrote a few lines above.

The author's critique would then be that the else could crash the application because of that exception. Well, bad news, that is because of the _exception_ and not
because of the _else_. In the strategy, it's exactly the same - an unhandled case throws that exception that could then lead to a crash.
There is literally _nothing_ won in this regard by applying the strategy.

### The scaling nonsense

> If you’re still writing giant chains of if-else statements in your core logic, you’re holding your code hostage.
They're simple, but they don't scale.

This statement is so full of ignorance and misinformation that I don't know what to professionally answer.
It's the _simple_ code that scales. Complicated stuff never scales to anywhere.

First of all the _need to "scale"_ anything (whatever that means) turns out to be more often absent then present.
That's the reason why every project you join is more complicated than it needs to be.
Would we all remind us please that doing measures for stuff that we don't even know will happen, let alone _when_ it could happen is a bad idea?

> Note that some people mean _scaling of people_ when they mean scale. So becoming a bigger team. They want to express, that certain code is easier to
change when there are more and more people. For example I can imagine some people fear that multiple people can change the same lines of code, causing conflicts.
First, please read my post about co-creation. Then, The original example suffers from the same problem, because you need to alter the
service. Last, I think when we can't organize ourselves to that we can efficiently do multiple changes in an if-else, then we should question our
abilities in general.

Furthermore. An if-else statement can easily be extended in place by just adding another else-if branch. When the actual differentiation between cases is encoded in that
if-else statement, than that's _exactly_ where it should reside, the branches are _colocated_ because they are _related_, highly _cohesive_. What you _maybe_ want to move out
is the actual stuff that is executed in the block inside a branch. Like it already is, more or less, because it's a named function that gets called.
Again the question. Do we want to extract it? Is the processing in the branches related, do they share a topic,
a domain, owners, vector of change? If so, they are fine as they are. If they are non-trivial they could be extracted to what would be the next best simple primitive of
programming: a function. Is it worth the indirection, which is increasing the cognitive overhead? Then do so. If not, it's fine to keep it inline. Should the function
be extracted and moved to a different file because it's too big? Fine, do it. Congratulations, you have successfully "scaled" your code. There is absolutely nothing that prevents
us from achieving the whished-for characteristics with the if-else.

### Oh the irony

> Once logic gets complex, your code becomes hard to test, extend, and read.

#### What complexity?

Again a statement (no pun intended) that bothers me. What do you mean by "logic" that gets "complex"? By definition, your if-else can't get any more complex than
what we already saw. Your patterns could get more complex, but the patterns still exist in the strategy version, just in a different place - when they get more complex,
they do so in both examples. So there won't be any differences in complexity in either version.

#### What testing?

I have to anticipate what the author means by "hard to test" - he's probably referring to unit-testability of the actual implementation of the
branches themselves. In the strategy example, each branch implementation is extracted into a class each. And here we have to talk.

What exactly should be tested?

When you now start to test every strategy in isolation than you couple your tests to the structure - aka an implementation detail - of the strategy. This is a bad practice because
you can not change the structure of the code anymore without making the tests useless/having to change them.

Where you should rather [test is at your boundary](https://hannomalie.github.io/posts/dissecting-spring-petclinic-blackbox-tests.html).
In the given case that would be the service class. Which probably should not even exist, because it's mostly doing
indirection and aggregation boilerplate - there is probably a controller which could serve us as a boundary. The test should then model the actual use case, which
is a user interaction and it could possibly be property-based testing about our defined payment methods. That would be the business relevant stuff.

So when people accuse stuff to have "bad testability" what they mostly really mean is "I can't unit test on arbitrary structure, so it's bad".

#### Extendability

I don't know what to say. Of course both versions can be extended quite easily. Strategy needs addition of a file and a change in the service to include the new strategy.
We can already identify that we need to adjust two completely unrelated places in code in order to add a single piece of functionality to our project. That should ring
the alarm bells. For the if-else it's actually only a single file and a single place we need to change. How is it not completely obvious what is the objectively better solution
for adding a new payment strategy.

#### Readability

Here we go again, same issue: If you want to understand what's relevant for your "system" regarding a specific payment method, what's better: The need to look in two different,
dislocated places, or when you have a single one place where you don't have to follow any indirections? Again, it needs to be painfully obvious that indirections
don't enhance readability. Additionally, as written before, extracting a method when the flight-level of the code starts to diverge is easily possible in both implementations,
so it can't be a disadvantage of the if-else.

### Oh the irony #2

> Boom. Now you can add new logic without touching the old code. Clean, scalable, and maintainable.

Boom. No, no, no and maybe yes.

First point is simply a _lie_. When you add a new strategy you have to adjust the service, where the strategy map now resides.
If the author means that you don't have to touch the code that was before in the branch blocks, then yes, one doesn't have to touch it.
But that's again true for both examples. You have to touch the old code, otherwise the system won't know the new strategy and therefore won't work.

Second, what means _clean_? The amount of code and indirections added is not clean, it's less clean, because it objectively increases the ratio
between noise and relevant code, increasing the cognitive overhead.

Scalable? We had that already. It's either as scalable as the if-else or it's less scalable because you need to change two places in code instead of one.

What does it mean, maintainable? Maintainability means easy to read, easy to understand, easy to extend. All criteria became worse, so yes, the code might be
maintainable in absolute terms, but it's less maintainble then before.
Doing such a thing hundret times in a project is death by thousand cuts and the reason all projects suck.

## What we rather should talk about

The author is someone who is judging who is a senior or a junior developer. The name of the account implies it's professional, an interviewer, so probably
someone who is involved in hiring, maybe is in a managing or a leading position or so.

And that honestly bothers me. I think a senior developer should be better at understanding and judging the given tools. The statements made are arbitrary, incomplete
or blatantly wrong. Additionally, the whole "interfaces vs if-else/pattern matching" is about a very different question.

It's about the the [__expression problem__](https://en.wikipedia.org/wiki/Expression_problem). That could be a thing a senior knows about, while a junior doesn't,
and I am not even sure about that.

So we're asking the question whether we want someone else being able to extend our stuff without the need to change some of our other stuff. Historically the "someone else" was really
someone else, like in a different division or so, who is not allowed to change our code base or at least not to change our module. That's why there is an interface
implemented the other guy can compile his code against. But the point is, that most of what we do doesn't have any such requirement. It's rather so that we have a static
(but still extendable) set of things we need to model. So we _don't_ need to allow arbitrary addition of implementations. Imagine if Java would have had sealed types from the
beginning instead of adding them in 2020 and having the interface construct sealed by default. That would have made ADTs the default choice in Java and nowadays 99%
of the application projects wouldn't even bother with the topic.

As soon as the values are known, we are free from the necessity to even put our operations into the interface, which often even leads to bloated interfaces. We
can do data oriented design and decouple our data structures from the use cases or at least limit the use cases to where they are needed, for example in a service class
while still getting the great compile time safety and exhaustiveness. The people designing the Java language even [promote that heavily nowaydays](https://www.youtube.com/watch?v=8FRU_aGY4mY).

## Talk is cheap, let's make it better

So let's reimagine the given code.

### Only doing small changes

First of all, we start by enumerating all the known algorithms we have.
Then we identify parsing user input as something we seemingly need to do.

```java
enum PaymentStrategy { 
    CreditcardPayment("creditcard"), UpiPayment("upi");
    
    public String identifier;
    private PaymentStrategy(String identifier){ this.identifier = identifier; }
    
    public static PaymentStrategy parse(String type) {
        return Arrays.stream(PaymentStrategy.values()).filter(it => it.identifier.equals(type.toLowerCase())).findFirst().orElse(null);
    }
}

class PaymentService {
    public void process(String type, Payment payment) {
        Object foo = switch(PaymentStrategy.parse(type)) {
            case CreditcardPayment: processCreditCard(payment); yield null;
            case UpiPayment: processUpi(payment); yield null;
            case null: throw new IllegalArgumentException("Unknown payment type: " +  paymentType);
        }
    }
    public void processCreditCard(Payment payment) {
        System.out.println("processCreditCard");
    }
    public void processUpi(Payment payment) {
        System.out.println("processUpi");
    }
}
```

So what do we gain with that.

1. You cannot create a new strategy without passing the identifier value in the constructor, so there is no way
to omit the type string we use to determine the strategy. This is a win compared to the original, where it was
possible to create a strategy, but forget to put it in the strategy map, rendering the whole implementation useless
because it's never used. Explicitly parsing untyped input at an outer ring of your system and getting something more well-defined
to use it in the rest of the code is also what most would agree is a good idea.
2. As soon as you add a new strategy to the enum, you will get a compile error that the switch expression does
not cover all the possible values anymore. That means it's equally impossible to forget to implement the actual payment processing,
which is one of the properties people argue are interfaces needed for.
3. We have decoupled the operation from the datatype, making it possible to have a) multiple operations that still benefit
from the exhaustiveness criteria and b) to have different signatures for the functions we need. Imagine the processing
for creditcard needs an additional optional security number parameter. People jump through hoops in order to fit stuff like that
into their inheritance hierarchie. Concrete, nominal things and static dispatch are simpler to understand than generic ones and dynamic dispatch.

So our code is now safer. But we also have some downsides, which are more or less weaknesses in Java, the language and I would consider of minor relevance.
You need to have the local variable foo, or otherwise it's not using switch as an expression and it is not exhaustive
anymore. Second, you have to use the yield keyword in order to return something in the switch branches. The PaymentStrategy
enum is wordy because Java only has primary constructors for records and because stream api is mediocre.

### Some more fundamental changes

The original post opens with the claim that one extra else could crash the application. Well, let's get rid of the crash
entirely by removing the exception in favour of a result type. And since I want to show how elegant stuff could be, I will
switch to Kotlin instead of Java.

```kotlin
enum class PaymentStrategy(val identifier: String) { 
    CreditcardPayment("creditcard"), UpiPayment("upi");
    
    companion object {
        fun parse(type: String): PaymentStrategy? = PaymentStrategy.entries().firstOrNull { it.identifier == type.toLowerCase() }
    }
}

class PaymentService {
    fun process(type: String, payment: Payment): Result = when(PaymentStrategy.parse(type)) {
        CreditcardPayment -> processCreditCard(payment)
        UpiPayment -> processUpi(payment)
        null -> Result.failure(IllegalArgumentException("Unknown payment type: " +  paymentType))
    }
    fun processCreditCard(payment: Payment): Result {
        System.out.println("processCreditCard")
        return Result.success(Unit)
    }
    fun processUpi(payment: Payment): Result {
        System.out.println("processUpi")
        return Result.success(Unit)
    }
}
```

Of course processing doesn't always return success results realistically. And I would recommend using a proper
Either type instead of the kotlin built in Result type, which lacks generic type parameter for the failure.
But we gained a pretty significant new property: We don't use exceptions anymore and have the failure case encoded
in our service's process function, which means the caller doesn't get an invisible exception thrown in the face,
but is forces to handle the unhappy path. That's better than pretending to have and use a return value just
to have exhaustiveness guaranteed by the compiler.

### Mixing both versions

```kotlin
sealed interface PaymentStrategy {
    val identifier: String
        
    companion object {
        fun parse(String type): PaymentStrategy? = PaymentStrategy.sealedSubclasses().map { it.objectInstance } firstOrNull { it.identifier == type.toLowerCase() }
    }
}

object CreditcardPayment: PaymentStrategy { override val identifier = "creditcard" }
object UpiPayment: PaymentStrategy { override val identifier = "upi" }

class PaymentService {
    fun process(type: String, payment: Payment): Result = when(PaymentStrategy.parse(type)) {
        CreditcardPayment -> processCreditCard(payment)
        UpiPayment -> processUpi(payment)
        null -> Result.failure(IllegalArgumentException("Unknown payment type: " +  paymentType))
    }
    fun processCreditCard(payment: Payment): Result {
        System.out.println("processCreditCard")
        return Result.success(Unit)
    }
    fun processUpi(payment: Payment): Result {
        System.out.println("processUpi")
        return Result.success(Unit)
    }
}
```

We need all implementations of the sealed interface to be objects, so that we can know all possible values and can
parse the input automatically somehow. This is only possible with reflection and I would write a test preventing
any declaration of a non-object instance. That's also the reason why this version would not be my first choice.
Note that we could even use the object types as receivers and make the process functions extension functions so
that they become `fun CreditcardPayment.process()`.

### Closing words

Do we have to always discuss five hours trivial things like if-else or strategy? No. Would I recommend doing it from
time to time in your team? Yes. Becaue it helps getting deep understanding of concepts and getting aspects on the
table you haven't thought of before, a better understanding of each eryone's values and beliefs.
This in turn helps to stay away from categorizing people in inappropriate ways.

My personal view is that I prefer having a sophisticated discussion on eye level even though when the decision would
then be not in favour of my explanations and opinions. If someone understand all the ups and downsides of a solution
and is then strong enough to admit that he still prefers a certain way due to personal reasons, I have a much
easier job in supporting that.
