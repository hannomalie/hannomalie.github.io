title=Context Receivers in StruktGen
date=2022-05-10
type=post
tags=struct,kotlin
status=published
headline=Context Receivers in StruktGen
subheadline=I can finally remove some ugly syntax
summary=Context Receivers are a preview feature in the latest Kotlin version and allow for additional receivers in declarations. I write about how the feature greatly enhances the syntax StruktGen requires you to use nested strukts. 
image=images/Kotlin_Icon.png
~~~~~~
## The StruktGen usecase

In my last post, I gave some short introduction to extensions and how context receivers could change the way we
structure our code in Kotlin. Roughly a year ago, I wrote a small code generation library on top of KSP that lets you
generate struct-like classes from interfaces on JVM, backing their memory with ByteBuffer instances. I am using
this library mainly in my own render engine project to have efficient data transfer between cpu and gpu via direct buffers.
In case you missed it, it's called [StruktGen](https://github.com/hannomalie/StruktGen).

Without going too much into details, a strukt definition looks like this:

```kotlin
interface Nested: struktgen.api.Strukt {
    var ByteBuffer.a: Int
    val ByteBuffer.b: Int
    companion object
}
```

Every property is defined as member extension property of ByteBuffer. This is because of the use case of StruktGen:
Generate objects that can be shared between the JVM and native code, like OpenGL in my case. There are not many
options to implement such an interop, but ByteBuffers is (the?) one. But then, a ByteBuffer must be present whenever
you do data access. So either a) everything is a function and you pass in a ByteBuffer as parameter, or b) you have 
a ByteBuffer as a member, or c) you build a fancy abstraction layer that destroys that strukts look and feel nearly
the same as regular objects.

## The problem

I went for d), which is abusing member extensions. It's abuse, because I clearly didn't want to model
properties as a property on a ByteBuffer that is available when a strukt is in scope as a receiver. But it was nice enough
at the call site, that I liked the approach nonetheless. But there was one other thing that became a bit ugly: nested
structs.

```
interface Nested: Strukt {
    var ByteBuffer.a: Int
    companion object
}
interface FooStrukt: Strukt {
    val ByteBuffer.d: Nested
    companion object
}

val foo = FooStrukt()
val buffer = ByteBuffer.allocate(FooStrukt.sizeInBytes)
foo.run { // foo is now this
    buffer.run { // buffer is now also in scope
        // boom, compiler error, can't just do d.a because a is a property on ByteBuffer, not on d, an instance of Nested
        assertThat(d.run { a }).isEqualTo(0) 
    }
}
```

## The solution: context receivers

With context receivers, I am able to actually model exactly what I wanted and get rid of the member extension abuse.
Using the new language feature, the example from above is now written as

```kotlin
interface Nested: Strukt {
    context(ByteBuffer) var a: Int
    
    companion object
}
interface FooStrukt: Strukt {
    context(ByteBuffer) val d: Nested
    
    companion object
}

val foo = FooStrukt()
val buffer = ByteBuffer.allocate(FooStrukt.sizeInBytes)
buffer.run { // buffer is now also in scope
    // boom, works as expected. You can now access d as a property of foo,
    // a as a proeprty of d (an instance of Nested), while
    // a buffer is present in the context
    assertThat(foo.d.a).isEqualTo(0) 
}
```

You can now use strukts and nested strukts just like any other property or object.

## Bonus round: type aware allocations

I don't know whether I really need a language feature that fulfills every requiremend a type class imposes,
but in StruktGen (or my rendering engine context), I would definitely love to have static constraints for a type.
Would be nice to have something like C# [static abstract interface methods](https://docs.microsoft.com/en-us/dotnet/csharp/whats-new/tutorials/static-abstract-interface-methods).

Since .NET implements this feature with runtime support (they extend the bytecode for that feature) and Kotlin
is just a guest language on the JVM, I don't see that feature coming to Kotlin soon - or when it comes, it likely has to be
a compile time thing. With context receivers however, my usecase can be modeled quite nicely without such a feature.
Let's take a look at what I am talking about:

Given structs only work when you provide backing storage in form of a ByteBuffer, you must first allocate it. This can
be done with `ByteBuffer.allocateDirect(sizeInBytes)`. I then have a class `TypedBuffer<T: Strukt>` that wraps a raw
buffer. In order to know how large your raw buffer needs to be, you need to pass an element size in bytes and an element
count. Element count times element size in bytes is the size in bytes your buffer needs to have.
The API currently looks like

```kotlin
val buffer: ByteBuffer = allocateDirectBuffer(FooStrukt.sizeInBytes * count) // pass in premultiplied size
val buffer: ByteBuffer = allocateDirectBuffer(FooStrukt, count) // pass in FooStrukt type and element count
val buffer: TypedBuffer<T> = allocateTypedBuffer(FooStrukt, count)  // pass in FooStrukt type and element count
```

and with context receivers it becomes

```kotlin
FooStrukt.run {
    val buffer: ByteBuffer = allocateDirectBuffer(count) // pass in element count
    val buffer: TypedBuffer<T> = allocateTypedBuffer(count)  // pass in element count
}
```

On top of that, the allocation strategy can be thrown into the mix. Because it's not always a simple direct buffer
I need to allocate - sometimes it is a buffer of a certain kind, that for example is automatically synchronized
with the GPU.

```kotlin
context(StruktType<T>, Allocator)
fun <T: Strukt> allocateTypedBuffer(elementCount: Int) = allocate(elementCount * sizeInBytes)

with(PersistentMappedBufferAllocator, FooStrukt) {
    val buffer: PersistentTypedBuffer<T> = allocateTypedBuffer(count)  // pass in element count
}
```

In my rendering engine, the allocator would be part of the graphics backend - every graphics API could then
implement its own allocator and whenever the backend is given as context, the allocator can automatically be used.
Not that I have enough time to implement a second graphics backend, but in theory, I could do it that way :)
I also might be a bit overeager with the allocation API, I will find that out.
