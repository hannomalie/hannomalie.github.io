title=Domain modeling made functional (Part 1)
date=2025-03-02
type=post
tags=code,web
status=published
headline=Domain modeling made functional (Part 1)
summary=Functional domain modeling is an underrated gem that gets you to an excellent design quickly.

~~~~~~

## TLDR

Sealed types or union types in a language are a true game changer, because they allow you to efficiently
model _things_ and _kinds of things_, which resembles how most people see their domains. Additionally,
they enable you to explicitly write out all the branches your code could possibly have, making it safer.
Furthermore, they make you think in results, which in turn helps you writing simple and safe functions
to implement system behaviour. Resulting in a domain encoding that is very close to what domain
people tell you and at the same time is concise and safe.

## Functional domain modeling is awesome

Let me start with bold words, as if I want to sell you something that actually gets me money instead of just
consuming my spare time:

__The book Domain modeling made functional by Scott Wlaschin has the potential to lift
your software development game to the next level. Get. This. book.__

Have you been struggling with software projects that turned bad over time really quickly? Was some initial
design maybe okayish, yet any change made it uglier and uglier? Did functional programming in general not click
for you for valid reasons? I feel you.

And of course nobody can safe us reliably from that situation.
But I belive the approaches and ideas presented in said book are some of the best picks in terms of importance and
effectiveness. I not only read through the book but also took the accompaning source code and translated it from F#
to Kotlin ([see part 2](https://hannomalie.github.io/posts/domain-modeling-made-functional-2.html)).
If you can't get the book, maybe you will get hooked by a 
[compressed presentation that is available on Youtube](https://youtu.be/2JB1_e5wZmU?si=5p-ZDMI6gLe74Bmh).

## Directly translating a domain

Talking to your domain experts will always be the most important thing you can do. As many might know, I am not
exactly the biggest fan of domain driven design, but the fundamental idea is right, I think. Mr. Wlaschin in his book
makes an impressive example how domain knowledge can get __directly__ translated into code, preserving its meaning.
As long as your language knows about ... drum rolls ... category theory.

What I find very important at this point is to remain unbiased towards functional programming and mathematical
concepts. If you have ever used an enum, you probably know most of what you need to know already. Let's get to the
remaining 25%.

We're not going into a scientific definition or stuff, you can find plenty of info on the internet. In my own words,
what the book will show you is, that domain people, business experts talk about __things__. Things of a certain
kind. Things that somehow belong to the same category of things. Well, why not write down exactly that? Create
such a category and simply list the things that experts tell you are in there. We __could__ do that with an enum,
but (in most languages) enum entries do not have associated data, so there is always only ever _one instance_ of such a
_thing_. This is not enough, the domain expert has mutliple instantiations and associated data and functionality in mind
when he talks about his _things_. So let's get straight to the point, your language needs _sealed types_, or you
won't get far with it. Thankfully, even conservative Java has that nowadays, so very few valid excuses are left.

F# is very impressive here, because the type definitions are as concise as they could be. Here's an excerpt from
[the repository where all the code from the book is hosted](https://github.com/swlaschin/DomainModelingMadeFunctional):

```F#
type PersonalName = {
    FirstName : String50
    LastName : String50
}
type CustomerInfo = {
    Name : PersonalName
    EmailAddress : EmailAddress
    VipStatus : VipStatus
}
```
 
For the rest of the post, I will show you how stuff would look like in Kotlin, which is slightly more verbose.

So when your domain experts say "We have customers. And customers can be normal customers or vip customers.
Vip customers are very important for us, as they drive our profit. We differentiate between normal vips and
long-time vips. So when customers order something, they get different prices, vips pay half shipping rates, long-time
vips only 25%".

You can immediately transcribe it while he speaks to

```kotlin
// We have customers
enum class Customer
```

```kotlin
// We have customers. And customers can be normal or vip customers
enum class Customer {
    NormalCustomer, VipCustomer
}
```

```kotlin
// We have customers. And customers can be normal or vip customers. [...] We differentiate between normal vips and
//long-time vips.

sealed interface Customer {
    object NormalCustomer: Customer
    sealed interface VipCustomer: Customer {
        object NormalVip: VipCustomer
        object LongTimeVip: VipCustomer
    }
}
```

```kotlin
// We have customers. And customers can be normal or vip customers. [...] We differentiate between normal vips and
// long-time vips. So when customers order something, they get different prices, vips pay half shipping rates, long-time
// vips only 25%".

sealed interface Customer {
    object NormalCustomer: Customer
    sealed interface VipCustomer: Customer {
        object NormalVip: VipCustomer
        object LongTimeVip: VipCustomer
    }
}

fun Customer.orderSomething(shippingCost: Float) {
    val shippingRateMultiplier = when(this) {
        NormalCustomer -> 1f
        VipCustomer -> when(this) {
            NormalVip -> 0.5f
            LongTimeVip -> 0.25f
        }
    }
    val finalShippingCost = shippingRateMultiplier * shippingCost
    // order something here
}
```

Note the code evolution, step by step. Converting from enum to sealed class in Kotlin with IntelliJ is ALT+Enter and takes
1 second. Note also, how we implemented the bare minimum of what we need to model the domain. With F# even better,
but the above code could very well be shown and discussed with non-programmers in a call. That means it opens the door
for _true collaboration_.

## Superpowers of sealed types or union types

### Exhaustiveness

And while that is already a very solid foundation, it gets even better. With sealed types (in most languages) you
can do exhaustive switches. That means you get a __compiler error__ whenever you switch over a type and don't
handle one of the possible cases. That will make all your functions __total__, there won't be any code branch
that you haven't explicitly written out. Which gives you an enormous safety.

Think of the situation another vip customer type is added, the platinum customer. You extend the sealed type

```kotlin
sealed interface VipCustomer: Customer {
    object NormalVip: VipCustomer
    object LongTimeVip: VipCustomer
    object PlatinumCustomer: VipCustomer
}
```

and immediately get a compiler error in `orderSomething` because then switch is not exhaustive anymore and
the type PlatinumCustomer is not handled. Over the time, there will be lots of such cases and you will never
miss anyone of it.

### Associated data

In the above example I deliberately stick to the most simple solution, for example I used enums first, because
they are the simplest possible implementation to cover the requirements thus far. Or I use object declaration
in the example above - that's rarely sufficient in real world projects, because most of the _things_ domain
people talk about have associated data of some form. So let's again play the game. Your domain expert tells you:
"When customers order something successfully, we ship to their shipping address. Their receipt will be sent to their billing
address. A shipping address always has firstname, lastname, street and postal code. Billing address as well.
A customer has a default shipping address and a default billing address, too. When he doesn't have a default billing
address, it should be the default shipping address. He doesn't need to have a default shipping address though, he can 
put one in for every order, if he likes. He can pass in both also for every order specifically."

```kotlin
data class Address(val firstName: String, val lastName: String, val street: String, val postalCode: Int)
typealias ShippingAddress = Address
typealias BillingAddress = Address

fun Customer.orderSomething(
    shippingCost: Float,
    shippingAddress: ShippingAddress = defaultShippingAddress, 
    billingAddress: BillingAddress = defaultBillingAddress ?: shippingAddress,
) {
    // ...
}

val customer = Customer(Address("Max", "Mustermann", "Teststraße 12", 12345))
val companyBillingAddress = // ...
customer.orderSomething(shippingCost = 10, billingAddress = companyBillingAddress)
```

This is also the first time where a discussion can arise, because how exactly defaults of shipping and billing addresses
work is not 100% clear.

What should have gotten clear here is, that our customer can not longer be an enum or an object declaration, we need
to define the addresses of a customer somehow. Let's try:

```kotlin
sealed interface Customer {
    data class NormalCustomer(val defaultShippingAddress: Address, val defaultBillingAddress: Address?): Customer
    sealed interface VipCustomer: Customer {
        data class NormalVip(val defaultShippingAddress: Address, val defaultBillingAddress: Address?): VipCustomer
        data class LongTimeVip(val defaultShippingAddress: Address, val defaultBillingAddress: Address?): VipCustomer
    }
}
```

The duplication probably bothers you. Yes, the given example could as well be written as a single Customer class
that contains an enum flag. As soon as you add further data, like a date since when the LongTimeVip is vip,
or a vip level for the two vip customer types, that approach would fall short quickly. With the given approach, there
is close to no coupling between what sounded distinct from the domain-perspective.

> NOTE: Of course it's also very much possible to write "traditional oop code" that is good and meets the requirements.
The functional way is only one of your tools and worth to be considered.

## The rest is functions

We talked a lot about the data part of an application, but what about the behaviour? Well, functional programming
embraces immutability and pure functions. So behaviour is best modeled with functions that fulfill those criteria.
I think nowadays that's not controvesial anymore, even other paradigms like object orientated programming embraces
those things naturally. When talking to domain experts or when doing event storming, it becomes clear that most
people tend to think in "events". They describe processes where certain actions are initiated and the system has
some defined paths and at the end there is some observable behaviour. Like "When a customer acknowledes his shopping
cart, the order is send and he gets an acknowledgement email". The customer is the actor, acknowleding the cart is
the action and order and email sending are two results. In functional style, it could be implemented like this:

```kotlin
fun Customer.acknowledgeShoppingCart(
    shoppingCart: ShoppingCart
): List<Event> {
    // reserve items from stock for packaging
    // calculate Price
    // send email
    return listOf(OrderSent(), EmailSent())
}
```

Of course this is a ridiculously simplified version of how to do it. Stuff is async, results have a lot of unhappy paths,
you need to have certain points in the process that can be retried and so on and so forth. All that can of course be
handled.

### Results

First the elephant in the room. Most actions have multiple possible branches, results, happy ones and unhappy ones.
The important thing: Errors are valid outcomes and treated as values. No exceptions. Everything needs to show up in the
signatures. So either you model all your outcomes as sealed types. Or you [embrace result types](https://github.com/michaelbull/kotlin-result)
that differentiate between success and failure cases. The latter enables you to write nice code could look like:

```kotlin
sealed interface Sent
object ItemsSent: Sent
object EmailSent: Sent
typealias Ack = Pair<ItemsSent, EmailSent>
sealed interface StockFailure
sealed interface AckFailure
object ReserveFailure: AckFailure, StockFailure
object SendItemsFailure: AckFailure, StockFailure
object SendMailFailure: AckFailure


suspend fun Customer.acknowledgeShoppingCart( // async function
    shoppingCart: ShoppingCart
): Result<Ack, AckFailure> = coroutineBinding { // monad comprehension block starts here
    val orderSendingResult: ItemsSent = reserveAndSendItems().bind() // can be async, returns error if error
    // calculate Price
    val emailSendingResult: EmailSent = sendMail().bind() // can be async, returns error if error
    
    return Ack(orderSendingResult, emailSendingResult)
}
```

Given such an example, it's not hard to imagine a domain expert's input in the like of "When an order is placed,
stock is checked and either sufficient or insufficient. When insufficient, that causes the ordering process to fail.
This means an error in the UI and no mail is sent." With the given results, the calling code knows exactly what
happened and can handle accordingly. All the benefits of sealed types are brought to bear by result types as well.

> NOTE: Purity of functions remains as the one point that is difficult. There are common approaches in functional
programming to deal with effects in general or IO in specific. However, my current opinion is, that this is exactly
where people stop seeing any benefit of the solution and perceive the abstractions as too complicated and of little value.
When your function uses some external service to send a mail, it automatically has side effects.
[Here is another video](https://youtu.be/P1vES9AgfC4?si=icEJuRZGDYav3kG7&t=2563), also by Scott Wlaschin,
which describes this challenge and possible solutions much better than I could, so I encourage you to watch that video
instead and find your own good compromise.

My experience is, that dependency injection and good result types are enough to address the problem
of IO well enough.

### Data: Make illegal states irrepresentable

Now that we already know about result types, we can use that knowledge to its full potential. You can use it for
the whole of your validation layer. Because certainly your application takes unvalidated input, for example
from the user. A shipping address has a firstname. Your domain expert might only assume it, but certainly that thing
shouldn't be empty. So let's model a data type

```kotlin
@ConsistentCopyVisibility
data class NonEmptyString private constructor(val value: String) {
    companion object {
        operator fun invoke(possiblyEmpty: String): Result<NonEmptyString, String> = if(possiblyEmpty.isBlank()) {
            Err("Empty string")
        } else {
            Ok(NonEmptyString(possiblyEmptyString))
        } 
    }
}
//[...]
data class ShippingAddress(val firstName: NonEmptyString /*[...]*/ )
val shippingAddressOrError = binding {
    val firstName = NonEmptyString("").bind() // will cause a failure and return Err result from binding block
    ShippingAddress(firstName)
}
```

You are now forced to go through the factory method and you cannot have an empty string get along in your code, it will
be prevented right where the input is validated. So your shipping address is inherently safe. Of course you can do
all of your validation like that - Maximum of 50 characters, integer values between 0 and 10, a non empty list and so on
and so forth. What might be a bit unusual for many programmers is simply that: unusual. Take a look at 
[the code from the book](https://github.com/swlaschin/DomainModelingMadeFunctional/blob/master/src/OrderTakingEvolved/Common.SimpleTypes.fs)
or [my conversion of it](https://github.com/hannomalie/dmmf-kt/blob/master/src/main/kotlin/SimpleTypes.kt) 
to see that it's perfectly possible to define custom data types and validations for like everything.

Validations can exist for technical reasons (no more than 50 chars for a database column) or for functional reasons
(a delivery address must always be present). By taking domain expert's input, these can be implemented just
like the types itself.

## Closing Part 1

The introduction summarized everything well enough.
In [part 2](https://hannomalie.github.io/posts/domain-modeling-made-functional-2.html), we can take a closer look
at the differences between F# and Kotlin. 
