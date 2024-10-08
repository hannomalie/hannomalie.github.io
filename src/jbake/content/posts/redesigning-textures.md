title=Redesigning textures in my engine
date=2024-10-08
type=post
tags=rendering
status=published
headline=Redesigning textures in my engine
subheadline=To support streaming better and to have nicer code :)
summary=When implementing texture streaming in my engine, I realized some not-so-nice bits in the existing code textures. So I changed it.
image=images/default.png

~~~~~~
__TLDR:__

- I now differentiate between the actual allocation of a texture on the GPU and some typed handle for it
- The new design has cleaner state handling and therefore supports streaming better
- It also enables pooling of texture resources
- Creating resources also got a lot simpler

## First implementation of textures in my engine

I've posted about my shenanigans with textures in my rendering engine quite a few times.
As I always try to, in the beginning, I started with the most simple implementation - which was an OpenGL implementation
specific object per texture that was uplaoded in a blocking manor immediately when created.
Over time, I added the properties I needed to have - like the dimension, the filter configuration for mipmapping,
bindless texture handles etc.

## Loading state bolted on top of it

When I started working on texture streaming for dynamic loading and unloading (this is a trick...), I also just added
whatever was needed on top of the basic implementation I had.
For example I needed a loading state, which I made a sealed type of the form  

```kotlin
sealed interface UploadState {
    data class Unloaded(val mipMapLevelToKeep: Int?) : UploadState
    data class Uploading(val mipMapLevel: Int): UploadState
    data class MarkedForUpload(val mipMapLevel: Int?): UploadState
    data object Uploaded : UploadState
}
```

So that I am able to differentiate between an unloaded and fully loaded texture, but also to see if a mipmap is currently
in process of being uploaded or to see if the texture is currently still in the priority queue because all asynchronous
upload streams to the GPU are busy (the pixel buffer objects in OpenGL).
Additionally, I added a mutable float property to all textures which keeps the current mipmap level bias.
Given the info which is the lowest currently loaded mipmap, I was able to implement the smooth fade in of textures
by adjusting the current mipmap bias and have it never be lower than the lowest mipmap loaded.

## The issues

First thing I noticed Even though I was only interested in 2D textures of my models to be streamable, I paid the price
of not being concrete and added it also to CubeMap textures, CubeMap array textures and 3D textures I used for voxelization.
That surely didn't feel right.
Furthermore, besides the textures of the actual scene's models, I had a lot of textures I knew for sure will never
be unloaded, for example texture storage for renderbuffers, noise textures or static fallback textures.
Yet, I needed to fill in all those properties in a gazillion places and - given the nature of switching over sealed types when you want
to have any benefits from them - also typing out a lot of switch branches that actually never happen for most of
the texture types I use.
Not getting better.

Even further, my first attempt was to not acutally unload a texture _(because I don't want to delete the GPU resource,
because it would block the graphics API and cause stuttering)_, but to only set the mipmap bias accordingly and "simulate"
the unlaoded state for now.
In reality one needs to ask the question what to actually display when the texture is unloaded completely - _nothing_
is not an option, or objects will pop in visibly. So some low level mipmap should always be available, but more on that later.
So for now, I have a static fallback texture that will be used as a replacement for unloaded textures.
It's the colored checkerboard texture you see in the video below.
For starters, every time I found a texture with `Unloaded` state, I used that fallback texture - but I was still able
to use the unloaded texture as well, because the object was still there, right at hand.
Which is kind of dangerous, because what does it even mean? It's probably used for some other texture to stream
in already, so for sure my other texture data is not in there anymore.

## Adding handles

Long story short, the upload state should not be part of the texture allocaiton on the GPU at all, the GPU doesn't care
what data I upload to it. Instead I added an indirection, I mean what can't be solved by an indirection? I added a 
handle abstraction that is just a reference to a texture allocation.

```kotlin
sealed interface TextureHandle<T: Texture> {
    val texture: T?
    var uploadState: UploadState
    var currentMipMapBias: Float
}
interface StaticHandle<T: Texture>: TextureHandle<T> {
    override val texture: T
}
class StaticHandleImpl<T: Texture>(override val texture: T,
                                   override var uploadState: UploadState,
                                   override var currentMipMapBias: Float
): StaticHandle<T>
interface DynamicHandle<T: Texture>: TextureHandle<T> {
    override var texture: T?
}
```
I bet I was the only one in the world not having such a thing, everyone else in graphics or game development probably knew it's
better to use such handles to some kind of resource. The thing is, until now I didn't need it :shrug.

Sealed types make the compiler force me to handle all possible branches again. The simplest possible implementation
is just a wrapper around a texture and some upload state. Note that even textures that get never unloaded still have
an upload state and therefore could have a mipmap bias when used on models. Since static handles are not of any further
relevance, one can instantiate them freely, doesn't really matter, what matters more is the underlying texture.

Well, of course handles open up a lot of possibilities to have invalid state. For example two handles can use
the same texture to upload different data. I mitigated that by having a TextureManager class that is in charge of those
handles and that is the gatekeeper to upload data. Combined with the sealed type for upload state, I built
a minimal state machine with _when_ statements to prevent issues, which works well so far.

## Pooling

So we can remove a texture from a handle and reuse it for some other handle.
Now what is a solution where I don't have to delete the gpu texture objects (for now)? Right, a pool.
So when a handle transitions to unloaded state, the handle is returned to the TextureManager's texture pool.
The next handle to upload can then draw from the pool - but here's an issue. Not all the texture objects
are the same. Hey have different dimensions, types, internal formats, filters and wrapping modes.
With this insight, I extracted all those properties characterizing a texture into this:

```kotlin
sealed interface TextureDescription {
    val dimension: TextureDimension
    val internalFormat: InternalTextureFormat
    val textureFilterConfig: TextureFilterConfig
    val mipMapCount: Int get() = if(textureFilterConfig.minFilter.isMipMapped) dimension.getMipMapCount() else 1
    val wrapMode: WrapMode
}
data class Texture2DDescription(
    override val dimension: TextureDimension2D,
    override val internalFormat: InternalTextureFormat,
    override val textureFilterConfig: TextureFilterConfig,
    override val wrapMode: WrapMode,
) : TextureDescription
```

and some more types for different specializations. Note how a _Texture2DDescription_ refines the type of
dimension to _TextureDimension2D_, because it's annoying to already have a Texture2D but unknown dimensionality.

> **_NOTE:_**  TextureDescription could be an abstract class and have all the concrete properties,
 instead of abstract ones, but in Kotlin that would prevent me from implementing data classes as subclasses,
 because their primary constructors can only have val/var properties.

The texture itself can now be as simple as

```kotlin
sealed interface Texture {
    val description: TextureDescription
    val id: Int
    val handle: Long
    val target: TextureTarget // This could probably be part of description
    // the following properties could be extension properties
    val dimension: TextureDimension get() = description.dimension
    val internalFormat: InternalTextureFormat get() = description.internalFormat
    val textureFilterConfig: TextureFilterConfig get() = description.textureFilterConfig
    val wrapMode: WrapMode get() = description.wrapMode
    val srgba: Boolean get() = description.internalFormat.isSRGBA
}
interface Texture2D: Texture {
    override val description: Texture2DDescription
    override val dimension: TextureDimension2D get() = description.dimension
}
```

and some more texture types - and the same type refinment as before applies here as well.
Which reduces the actual properties of a texture to an id and a gpu handle, whereas both of those things
are implementation details of OpenGL I let leak into the interface because it is handy as long as I don't
yet implement any other backend _(And then I would remove it at the cost of some abstraction complexity)_.

FileBased textures (2D) are then again a wrapper around the introduced texture handles that enrich
the reference with a file, so that it is all one needs to fully stream in the texture data. It looks kind of like
that:

```kotlin
class DynamicFileBasedTexture2D(
    val path: String, // used as a speaking identifier
    val file: File,
    override var texture: Texture2D?,
    val description: Texture2DDescription,
    override var uploadState: UploadState,
    override var currentMipMapBias: Float = mipMapBiasForUploadState(uploadState, description.dimension)
): DynamicHandle<Texture2D>, FileBasedTexture2D {
    private val bufferedImage: BufferedImage get() = ImageIO.read(file) ?: throw IllegalStateException("Cannot load $file")
    private val ddsImage: DDSImage get() = DDSImage.read(file)
    override fun getData(): List<ImageData> = createAllMipLevelsImageData()
}
```

> **_NOTE:_**  I cheated a bit here, actually I hid a lot of stuff in `createAllMipLevelsImageData()`, for example
 distinction between dds and other file types, because dds files can loaded to the gpu directly and used compressed internally.

The whole process made the factory functions of the OpenGL backend implementation quite lean:

```kotlin
override fun Texture2D(
    description: Texture2DDescription,
): OpenGLTexture2D {
    val textureAllocationData = allocateTexture(
        description,
    )

    return OpenGLTexture2D(
        description,
        textureAllocationData.textureId,
        textureAllocationData.handle,
    )
}
```
because all the functions now understand a texture description and have all the info they need.

And finally, the description is the only thing we need to get a texture from the pool, so the pool itself
can be a `MutableList<Pair<Texture2DDescription, Texture2D>>` and the access can be as simple as

```kotlin
private fun returnToPool(
    texture2DDescription: Texture2DDescription,
    texture: Texture2D
) {
    texturePool.add(Pair(texture2DDescription, texture))
}

private fun getFromPool(textureDescription: Texture2DDescription) = texturePool.firstOrNull {
    it.first == textureDescription
}?.apply {
    texturePool.remove(this)
}?.second
```

Better idea would be to create pools per texture description, but for now it does the job.

## Unloading condition

When to unload? Well, if a texture is not used anymore. I just hacked in a usage timestamp for each model
texture and when unused for two seconds, it gets unloaded (the texture returned to the pool).

> **_NOTE:_**  In this video, everything is still artificially slowed down in order to make it visible.
 I am neither loading from SSD full speed, nor am I uploading to the GPU in full speed. I am not even
 adjusting the mipmap bias at full speed, it can probably be done much faster without creating a pop-in effect.

<video width="1280" height="920" controls>
  <source src="../videos/unload-out-of-view.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>

Instead of the checkerboard texture as a replacement, I will keep a low level mipmap texture alive for
every texture, which is then used when the real texture is unloaded. Alternatively, I could
just use the highest mipmap texture's (1x1) texel color as uniform color value, I will see whether that's sufficient.

## Changeset and some words about workflows

The commit that made everything better can be found [here](https://github.com/hannomalie/hpengine/commit/2e38eb8cc917648b1161d9e5310f7dfa29a12f45).

I have to admit that I __love__ statically typed languages, that enable me to pull off those big refactorings
by just requiring me to repeatedly satisfy the compiler. I don't have noticable tests in that project - that's because
I don't want to have rigid boundaries within the project, which most tests create. I am faster without them - sometimes, I temporarily create
one or two tests where it fits. Often, they indeed show where the code is maybe not too easily testable - one
of the big benefits of tests! I then enhance the code as much as I can and delete the tests. The statically
typed code is enough and the tests already did what they where intendet to do: guide the code design. They don't need
to cement much else anymore and prevent me from chaning the code further in the future.

I find that quite interesting, because it's a point of view on code that not many people seem to share with me. And maybe
I am also a bit too far on the left side and will course correct a bit somewhen.

The other commit, the earlier one, is [this one](https://github.com/hannomalie/hpengine/commit/abe7c2743643f0d72d01efba15028112d0a2ab37), 
which introduced some bits of the upload state in the simplest possible way. I don't think it's necessarily good,
but it was the first state I was willed to commit without being ashamed too much, so cheers.