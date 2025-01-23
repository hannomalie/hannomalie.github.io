title=Texture streaming with smooth fade-in
date=2024-09-11
type=post
tags=rendering
status=published
headline=Texture streaming with smooth fade-in
subheadline=Utilizing a float mipmap bias
summary=Using a float mipmap bias when dynamically streaming textures can help preventing visible pop-ins.
image=images/bricks_parallax.jpg

~~~~~~
It's been some time since [I posted about texture streaming](https://hannomalie.github.io/posts/persistent-buffers-texture-streaming.html),
and there was this one thing I didn't have enough time back then to implement it: Using a mipmap bias to smoothly
fade in newly loaded textures instead of having that visible pop-in effect.

Well, the idea is super simple: Load textures always beginning with the highest mipmaps. I had that already.
But when a mipmap was loaded, I set the mipmap bias to exactly that mipmap level immediately.
Instead of doing that, one can also use a float value as mipmap bias and first leave the current mipmap bias just as it is.
With a certain rate, let's say one mipmap level per second, one now needs to update the state of the texture
and advance the bias towards the newly available lowest mipmap level until we reached 0 again.

<video width="100%" controls>
  <source src="../videos/mipmap-float-bias.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>

In the video above, I reload all of the scene's textures completely with an artificial decrease of two mipmap levels
per second. (Note that I don't unload the file content's from memory afaicr, so disk read speed is not taken into account here)

When not reloading a gazillion textures at the same time, the transition can look quite good:

<video width="100%" controls>
  <source src="../videos/mipmap-float-bias-closeup.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>

Depending on the speed we want to have for the fade in, which is probably depending on the lowest texture mipmap we want to unload,
the fade-in is now much better then the pop-in. Not showing higher resolution textures that might already been loaded
for a short amount of time can be a good compromise.

In my engine, I simply update the scene's material buffer every tick, which is a persistent mapped buffer and changes
are reflected on GPU side immediately. How I could support not only a single type of textures - currently the diffuse
color textures - but all of them, like normal maps and roughness maps.... I don't know yet. Probably with a flag
per texture type and of lower precision.
