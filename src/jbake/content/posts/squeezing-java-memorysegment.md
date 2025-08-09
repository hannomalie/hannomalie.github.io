title=Squeezing Java's MemorySegment
date=2025-08-09
type=post
tags=architecture,design,rendering
status=published
headline=Squeezing Java's MemorySegment
summary=I tried to get the most out of Java's new foreign memory API in terms of ergonomics and performance.
~~~~~~
## TLDR

- MemorySegment api is obviously nicer as working manually with raw integers as pointers
- It doesn't matter much for my usecase, because it's hidden behind Kotlin properties
- The new memory api is slower than Unsafe for my usecase, likely for most games or rendering usecases

## Introduction

It has been some time since I started experimenting with __native memory access on the JVM__,
shockingly [seven years](https://hannomalie.github.io/posts/2018-09-17-structs-jvm.html), can you believe that?
It's even [five years](https://github.com/hannomalie/kotlin-foreign-memory-access)
since last time I married Java's new foreign memory api with my struct generation library - funny that the api
is still in preview, even after all these years :)

But given it's largely finished and won't change much anymore, I gave it another try. And reated a
very minimalistic implementation of a game engine that implements multithreaded rendering, as I described ...
damn, also [nearly eight years ago](https://hannomalie.github.io/posts/2017-18-14-multithreaded-game-engines.html).
Additionally, it has a very simplistic implementation of an entity component system (yes, yes, I know...) that fits
the situation best.

## Multithreading

The idea of the extractor pattern is easy: Your game world gets updated at an arbitrary rate and at the end
of the cycle, the state relevant for rendering gets extracted and attached to a _frame_. You can create n frames
that can be put in a queue for the render thread to pull them and render them - also in another, arbitrary rate,
but ideally fast enough to keep up with the producing thread, or we need to skip frames. A frame is a complete
immutable snapshot of the world state and can be rendered independently.

But we might not forget that this comes at a cost: A frame needs to be a pure copy of the world state, the update
threads may never touch the data the rendering might still be consuming.

So two things: First, it simply costs memory, that needs to be allocated. Then, the data lives on the CPU side,
but rendering happens on the GPU. When no shared memory is present (which was common until recently) then we need
to transfer a lot of data. Those two things have two consequences: We need a special allocator for frame memory and gpu memory.
And the memory needs to be synchronized/fenced between CPU and GPU.

For that, we need native memory access, there's no way to do efficient gpu stuff without it. Managed heap data cannot
be used directly to feed the GPU, we would need to traverse our object graphs manually and extract the bytes and put them
in a buffer otherwise. That's possible at high speed (I did so in [hpengine](https://github.com/hannomalie/hpengine)).
And maybe it's even a dumb idea to not distinguish between cpu memory layout and gpu memory layout. But _given_
it was a good idea, it would make the whole extracion step just a simple _memcopy_ and therefore much faster and prettier.


## Structs and pointers via MemoryLayout and MemorySegment

While frowned upon, the JVM had a very efficient solution for accessing native memory in the form of _Unsafe_. You dealt
with byte buffers by accessing them through pointers, denoted with an int primitive. Simply. Out of bounds access
resulted in seg faults, hence the word unsafe. The performance is pretty much on par with what C offers, while similarly
risky.

The new official solution for native memory access on the JVM of course _can't_ simply be that unsafe. So there was an API
built that aims to provide safety by compromising a bit of performance. Instead of a construct comparable to void pointer
in C, we get something like a [span](https://stackoverflow.com/questions/45723819/what-is-a-span-and-when-should-i-use-one),
just a bit different. A MemorySegment is only a pointer to memory and the amount of bytes it has. Whether the segment
is a subsegment or a seperate buffer is unknown and irrelevant. The info is used to determine out of bounds access
in order to prevent segmentation faults.

The idea of MemoryLayout however is to describe a bunch of bytes with a layout, like you would type it when defining
a struct in C. And then you either have a simple layout or a sequence layout, containing multiple entries
of simple layout kinds, like an array.

### Components defined with MemoryLayout

In _rebirth_, I modeled a 2D position component with this layout definition

```kotlin
val layout = MemoryLayout.structLayout(
    ValueLayout.JAVA_FLOAT.withName("x"),
    ValueLayout.JAVA_FLOAT.withName("y")
)
```

We can then derive VarHandles from that layout, which are a JVM-native construct that lets you access various
properties, functions, dynamically generated method implementation, or like in this case is t an accessor into some
underlying data structure.

```kotlin
val xHandle = layout.varHandle(groupElement("x"))
val yHandle = layout.varHandle(groupElement("y"))
```

Like in my struct generation library, in Kotlin, we use a sliding window that is like a typed pointer, which in rebirth
looks like this:

```kotlin
class PositionComponent {
    context(segment: MemorySegment)
    var x: Float
        get() = xHandle.get(segment, 0) as Float
        set(value) = xHandle.set(segment, 0, value)

    context(segment: MemorySegment)
    var y: Float
        get() = yHandle.get(segment, 0) as Float
        set(value) = yHandle.set(segment, 0, value)
}
```

Note the context parameter - this is a parameter that gets passed into the function call or is used for the
property access on the call site, without the need to explicitly pass it in. As long as there is a MemorySegment
present on the call site, the propery access will use it automatically, like so:

```kotlin
world.forEach<PositionComponent> { entityId, position ->
    println(position.x)
}
```

on the _world_, this finds the system that contains all the PositionComponent instances and uses a single sliding
window instance, which is the position parameter in the lambda. A component is only valid in the current iteration,
those may never be saved, only the entityId - the handle - may be saved, as usual in ECS or games in general.
The type of the lambda is actually `context(MemorySegment) (Int, T) -> Unit` which means the MemorySegment
gets passed into the property accessor automatically, as described above. Of course, quite unfamiliar for someone
who has never seen Scala (or Kotlin), but I would say quite elegant and ideal for eliminating the need to pass around the base
pointer.

### Components allocated with SequenceLayout

The component array itself is allocated like this:

```kotlin
val baseLayout: layout // the layout from above
var componentsLayout = MemoryLayout.sequenceLayout(entities.size.toLong(), baseLayout)
    private set
var components = arena.allocate(componentsLayout) // This is the global arena from the game world
```

## Performance / Profiling

Prepare for nasty surprises. If there is only a single element ever, it's easy, only one MemorySegment.
When you have a sequence, let's say 100k game objects, then you have... yes, 100k MemorySegments. For iteration
there are nice APIs, could be done like `components.elements(baseLayout).forEach { memorySegment -> ...  }` or like this:

```kotlin
(0 until componentsLayout.elementCount().toInt()).map {
    context(components.asSlice(baseLayout.byteSize() * it, baseLayout)) {
        block(it, componentType as T)
    }
}
```

And while this looks nice, it will _tank_ the performance, because the termporarily needed MemorySegments get
heap allocated and in 10 seconds of runtime, caused at least 10 garbage collections of 10-20ms for me, which means
not suited for soft-realtime applications anymore (for the 100k entities test case).

This might be because the JVM still lacks proper value types, but one would have guessed that escape analysis will
prevent the allocations, because, well, a MemorySegment should not be much more than a _span_ at all, so two int numbers at
best. I countered that by precomputing all MemorySegments once, only when the components change in some way by adding
or remving an entity or a component. This is ugly and wasteful, but stopped any garbage collection from happening.

Then, almost all of the samples in my profiling where somewhere inside the memorysegment accessor apis or in the lambda
machinery of the var handles. The amount of different hot spots give me the impression that whatever is done at runtime
is too complex to get properly inlined and eliminated, even though this should really not be more than a simple
out of bounds check once before an iteration is done over an array.
 
Old material showed a big panelty for the new memory api, latest benchmarks like
[this one](https://inside.java/2025/06/12/ffm-vs-unsafe/) show
that the gap is close to disappearing for at least some applications. My profiling doesn't look like that,
but since I can't deduce any info needed for a true comparison
without having reference implementation with Unsafe as well, I have to not read anything into
the wild profiling results and instead take a look at the runtime data I can measure. Running the implementation
for __120k entities__ had the whole cycle being __~0.0025ms__ running on __16 threads__, consisting of __0.02ms__ for the systems update and __0.005ms__ 
for the extraction of the frame data, aka copying the buffers for rendering. For __1.2 million entities__, we're at
__0.03__ to __0.04 ms__. Running it on a __single thread__ only, brings it up to __~0.125ms__ for __1.2 million__ entities.

This is on an undocked _Ryzen 7 7840U notebook_.

All the update method does is

```kotlin
parallelForEach<PositionVelocity> { index, component ->
    val position = component.position
    val velocity = component.velocity

    var resultingX = position.x + velocity.x * deltaSeconds
    var resultingY = position.y + velocity.y * deltaSeconds

    if(resultingX > dimension.width.toFloat()) {
        resultingX = 0f
    } else if(resultingX < 0) {
        resultingX = dimension.width.toFloat()
    }
    if(resultingY > dimension.height.toFloat()) {
        resultingY = 0f
    } else if(resultingY < 0) {
        resultingY = dimension.height.toFloat()
    }

    position.x = resultingX
    position.y = resultingY
}
```

So only some minor float comparisons against more or less static values per entity and then setting a new position
based on entity's velocity. Note that this goes straight into offheap memory that is then copied over for the gpu.
So while 120k entities take 0.02ms, it takes around 0.2ms for 1.2 million entites - so around 12 million
entities of such a kind can get updated in a 2ms budget, which means 500 times per second. Probably not entirely
true, because memory effects will start showing up, but we cannot really change anything about it one or the 
other way around and we have an idea about the performance now.

The ideal would be to compare the whole implementation against a - let's say - C implementation that is as bare metal
as possible, but that's out of reach for me right now. Instead, I can compare it to an implementation based on
good old Unsafe. Unsafe itself is only a very tiny layer on top of native code, as are ByteBuffers.

ByteBuffers per se have the disadvantage that they contain a position property, so we can't simply use multiple
threads, because the position would be mutated from multiple threads then. So the simplest possible implementation
would be __single threaded__ only:
The same setup with __120k entities__ takes __~0.001ms__. For 1.2 million we're at __0.004__ to __0.015ms__.
So it is a good chunk faster then MemorySegments in my case, roughly an order of magnitude, which makes me a bit sad.

In order to make ByteBuffers usable with multithreading, I created an artificial MemorySegment type that holds
a ByteBuffer and a (mutable) position property. __1.2 mio entities__ on __16 threads__ brings the update time to
__0.03ms__, so it scales roughly in the same way as the native MemorySegment implementation. It's also very close
to the other implementation in terms of performance, only a few percentages difference.

On the other hand ByteBuffers as they are right now have two disadvantages that need to be said. First, they can't
be cleaned up, so I managed to get out of memory because even when finished rendering, the buffers couldn't get freed
manually. Second, they have the allocation limit of 2Gb.

> **_NOTE:_** 
Intersting fun fact: When converting the MemorySegment implementation to raw integer "pointers", of course I made
exactly the mistakes the new API would protect you from: Wrong byte offsets, wrong interpretation of integer types,
confusing bits and bytes etc. Even though I implemented such a thing already at least once before.

## Closing words

The last paragraph more or less says it all, the performance turns out not be on par for both the MemorySegment and
the Unsafe implementation. The gap is not especially big, there's also some variance, so in no way a clean, representable
benchmark, but all in all it gets us a rough idea.

The MemorySegment api is of course nicer than using integers as pointers for the poor people, but it's also not as nice
as I would want it to be. For example the VarHandle has no generic types or has specific implementation.
That results in statements like `xHandle.get(segment, 0) as Float` where unsafe casting is needed all the time.
This would be one of the first things I would write Kotlin property delegates around to make it type safe.
Or the `get` method takes a vararg parameter of type `Object`. Whatever handle it is, one has to pass different things 
into it in correct order and you need to read the javadocs to find out what exactly needs to be done. I really don't
like those kind of APIs.

Other than that, the API might be nice, but I do hide all that in automatically generated code, so it doesn't really
matter at all for me. What matters more is performance. On the plus side, we have the allocation limit of 2Gb that
ByteBuffers come with, removed.

The repository is [here](https://github.com/hannomalie/rebirth) and the branch for the unsafe implementation is
named _unsafe_. All in all, not much difference for me with the new API, but since the current state is the worst it will ever be,
we can expect it to get better over time and at least better and officially supported, contrary to the Unsafe api,
which is already in the process of getting phased out.