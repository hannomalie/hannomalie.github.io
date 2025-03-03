title=Domain modeling made functional (Part 2)
date=2025-03-03
type=post
tags=code,web
status=published
headline=Domain modeling made functional (Part 2)
summary=I compare F# code of domain modeling made functional to my Kotlin conversion of it. 

~~~~~~

## TLDR

In [part 1](https://hannomalie.github.io/posts/domain-modeling-made-functional-1.html) we took a look at
functional domain modeling in general. Now we focus on a comparison of F# and Kotlin in order to apply it.

F# is a really nice language and as most of its proponents on twitter rightfully complain, it's a bit underrated. Kotlin
has the same problem: Incredibly well designed language, but always fighting to eat into the host language's
cake. While type definitions in Kotlin can't match that of F#, they are very close. Other things like
constructor functions or asynchronous functions are better in Kotlin. Features like monad comprehensions are on par.   

## Type definitions

Let's start with the simple type definitions.
[Here in F#](https://github.com/swlaschin/DomainModelingMadeFunctional/blob/master/src/OrderTakingEvolved/Common.SimpleTypes.fs)
and [here in Kotlin](https://github.com/hannomalie/dmmf-kt/blob/master/src/main/kotlin/SimpleTypes.kt). F# is probably
unbeatable here, there is not much more to strip from type definitions like these:

```F#
type WidgetCode = private WidgetCode of string
type GizmoCode = private GizmoCode of string
type ProductCode =
    | Widget of WidgetCode
    | Gizmo of GizmoCode
```

To get the equivalent Kotlin code we do:

```kotlin
sealed interface ProductCode
data class WidgetCode(val value: String): ProductCode
data class GizmoCode(val value: String): ProductCode
```

Quite close, hm? F# get's bonus points for using the term _type_. And some personal bonus points for inverting
the place where we define which types are actually part of the ProductCode type. By just taking a look at it
with a few meters distance, I would say that F# is easier to read for non-programmers than Kotlin. But it's quite close.

Here is an example video how efficient you can rewrite data class-like types from F# to Kotlin:

<video width="100%" controls>
  <source src="../videos/fsharp_type_to_kotlin_data_class.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>

### Complete type definitions

Those definitions are incomplete though, you can find a lot more lines of code about those types in the links above.
In F# we can find the following functions:

```F#
module WidgetCode =
    let value (WidgetCode code) = code
    let create fieldName code =
        let pattern = "W\d{4}"
        ConstrainedType.createLike fieldName WidgetCode pattern code
```

First, the declaration of _value_ is not necessary in Kotlin, it's already included in the _val_ declaration in the
data class. The closest possible code in Kotlin looks like:

```kotlin
object WidgetCode {
    fun create(fieldName: String, code: String) {
        private val pattern = Regex.fromLiteral("W\\d{4}")
        fun create(fieldName: String, code: String?) = ConstrainedType.createLike(fieldName, ::WidgetCode, pattern, code)
    }
}
```

But we can move it to the WidgetCode class and change it a bit

```kotlin
@ConsistentCopyVisibility
data class WidgetCode private constructor(override val value: String): ProductCode {
    companion object {
        private val pattern = Regex.fromLiteral("W\\d{4}")
        operator fun invoke(fieldName: String, code: String?) = ConstrainedType.createLike(fieldName, ::UsStateCode, pattern, code)
    }
}
```

And made three things better: 1. Data classes in Kotlin by default expose a copy method which would bypass validation,
which is prevented with the private constructor (and the annotation for now). 2. We moved the factory method
into the class, they belong together and should live in the same place as a coherent module. 3. Using operator
function, we can still call `WidgetCode("foo", "myCode")` like a constructor and get a nice `Result` object back.

## Function declarations

Well, I have to admit as someone who never used F# before, I had my trouble deciphering the lambda-oriented function
declarations with type inference, especially when they were higher order functions. Let's take for example this one

```F#

type TryGetProductPrice =
    ProductCode -> Price option
    
let internal getPromotionPrices (PromotionCode promotionCode) :TryGetProductPrice =

    let halfPricePromotion : TryGetProductPrice =
        fun productCode ->
            if ProductCode.value productCode = "ONSALE" then
                Price.unsafeCreate 5M |> Some
            else
                None

    let quarterPricePromotion : TryGetProductPrice =
        fun productCode ->
            if ProductCode.value productCode = "ONSALE" then
                Price.unsafeCreate 2.5M |> Some
            else
                None

    let noPromotion : TryGetProductPrice =
        fun productCode -> None

    match promotionCode with
    | "HALF" -> halfPricePromotion
    | "QUARTER" -> quarterPricePromotion
    | _ -> noPromotion
```

we see the getPromotionPrices function which takes a promotionCode and returns a function which takes
a productCode and returns an option of price. The fact that those two declarations are in different files
with a lot of space between them made it harder to understand for me. In the function we have three local functions, 
each of them a possible return value. The return value is then determined by simple switching over the promotionCode.

The most similar Kotlin code I could come up with is

```kotlin
internal fun getPromotionPrices(promotionCode: PromotionCode): GetPromotionPrices {
    val halfPricePromotion: TryGetProductPrice = { productCode ->
        if(productCode.value == "ONSALE") {
            Price.unsafeCreate(5f)
        } else {
            null
        }
    }

    val quarterPricePromotion: TryGetProductPrice = { productCode ->
        if(productCode.value == "ONSALE") {
            Price.unsafeCreate(2.5f)
        } else {
            null
        }
    }

    val noPromotion : TryGetProductPrice = { null }

    return when(promotionCode.value) {
        "HALF" -> halfPricePromotion
        "QUARTER" -> quarterPricePromotion
        else -> noPromotion
    }
}
```

which I consider close to identical, no better, no worse here.

## Result handling

Monads are everywhere in functional programming. Option or Result types are monads. It's very helpful when
your language has support to map over them, so that you don't have to unwrap them strangely. Let's take a
look at this F# code:

```F#
let toValidatedOrderLine checkProductExists (unvalidatedOrderLine:UnvalidatedOrderLine) =
    result {
        let! orderLineId =
            unvalidatedOrderLine.OrderLineId
            |> toOrderLineId
        let! productCode =
            unvalidatedOrderLine.ProductCode
            |> toProductCode checkProductExists
        let! quantity =
            unvalidatedOrderLine.Quantity
            |> toOrderQuantity productCode
        let validatedOrderLine : ValidatedOrderLine = {
            OrderLineId = orderLineId
            ProductCode = productCode
            Quantity = quantity
            }
        return validatedOrderLine
    }
```

the `let!` here is a [computation expression](https://fsharpforfunandprofit.com/posts/let-use-do/#let-and-use-and-do),
which is a bit of a unusual concept, but basically it depends on the context you are in, what it actually does. In
the given code, we're in a `result` block, the let binding understands that those four functions used over there
return a result object. So they can return, when an error is returned, and short-curcuit. To be honest, I didn't
look up what _exactly_ the used expressions do, but usually it's something like that.

In Kotlin, there is a built-in result type, but it isn't generic over error types (and uses exception), so it's not
very helpful in most cases. But Kotlin offers three other extremely nice things: Lambdas with receivers, extensions and suspending
functions. With that, it's easy for libraries to build such a functionality with seamless integration. Like
[this excellent, small library of a result type](https://github.com/michaelbull/kotlin-result), that I use whenever
I can in Kotlin. With that, we get code quite similar:

```kotlin
fun toValidatedOrderLine(checkProductExists: CheckProductCodeExists, unvalidatedOrderLine: UnvalidatedOrderLine) = binding {
    val orderLineId = toOrderLineId(unvalidatedOrderLine.orderLineId).bind()
    val productCode = toProductCode(checkProductExists, unvalidatedOrderLine.productCode).bind()
    val quantity = toOrderQuantity(productCode, unvalidatedOrderLine.quantity).bind()
    val validatedOrderLine = ValidatedOrderLine(
        orderLineId = orderLineId,
        productCode = productCode,
        quantity = quantity,
    )
    validatedOrderLine
}
```

Here's another video how efficient and easy F# sharp code that uses computation expressions can be converted to
monad comprehensions in Kotlin:

<video width="100%" controls>
  <source src="../videos/fsharp_kotlin_monad_comprehensions.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>

## Composition

Well. I think there are two things I could complain about the code of the book.

First thing is the overly heavy
usage of anonymous functions. When possible, I prefer regular funtion declarations over lambdas that are bound
to a reference. Might be personal preference though.

Second thing is, that the code is overly focused on composition. I think the book doesn't exactly make a mistake by assuming
that there might be a _public_ api and a private implementation. But in most of my projects that distinction is simply
overkill. Just don't do the _interface layer_ unless your module(s) are consumed by other projects. Just do the
implementation. The distinction makes everything a bit weird. The strategy pattern creeps in everywhere - if there
is only one real implementation and only that one is necessary, why not just hardcode it where it's used. Why pass it as
dependency, for example here:

```F#
let placeOrderApi : PlaceOrderApi =
    fun request ->
        // following the approach in "A Complete Serialization Pipeline" in chapter 11

        // start with a string
        let orderFormJson = request.Body
        let orderForm = deserializeJson<OrderFormDto>(orderFormJson)
        // convert to domain object
        let unvalidatedOrder = orderForm |> OrderFormDto.toUnvalidatedOrder

        // setup the dependencies. See "Injecting Dependencies" in chapter 9
        let workflow =
            Implementation.placeOrder
                checkProductExists // dependency
                checkAddressExists // dependency
                getPricingFunction // dependency
                calculateShippingCost // dependency
                createOrderAcknowledgmentLetter  // dependency
                sendOrderAcknowledgment // dependency

        // now we are in the pure domain
        let asyncResult = workflow unvalidatedOrder

        // now convert from the pure domain back to a HttpResponse
        asyncResult
        |> Async.map (workflowResultToHttpReponse)
```

Composition is not a goal per se, but a technique to achieve other things. Like reuse of code. But what for if code
is already clean and concise. Or the possibility to exchange implementations. But what for, if there is only one.

I have the impression that there are more moving parts in the code than there need to be. The more moving parts,
the more variants are in your code.

While that complaint is really not that important, I at least wanted to be honest about my view after the code
conversion. Following the indirections and compositions was by far the most challenging part, all in all.

What is very refreshing for me is the simple way how the whole API is assembled. No dependency injection hell,
just pass in some parameters and done. That's how it should be. We can learn a lot from that.

## Conclusion

Intended or not, the result of applying what the book shows
results in very data-oriented code. Which is nice, because I think it's really lost wisdom, that our programs
are at the end only data-transformation-pipelines. Some of Kotlin's language features are a must-have there.
It's hard to live without them, once one discovered their effective usage. Like data classes and smart constructors.
Result types with monad comprehensions. Sealed types. Suspending functions. Local functions. Expression functions.
Nullability. And the upcoming [context parameters](https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md)
if they ever land.

I once had the pleasure to use the shown style with the shown result library and coroutines
for a rewrite of an existing project professionally and it was very enjoyable, allthough our domain was quite
small. I am a bit sad, that functional domain modelling and data oriented/pipeline oriented programming doesn't
get applied more often.