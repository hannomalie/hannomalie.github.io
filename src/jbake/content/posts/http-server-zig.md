title=HTTP server from scratch in Zig
date=2025-02-24
type=post
tags=code,web
status=published
headline=HTTP server from scratch in Zig
summary=I implemented an http server with a simple router from scratch in Zig only using posix sockets.
image=images/zig-logo-light.svg

~~~~~~

### HTTP servers again

Well, HTTP, I mean who doesn't know about it already? You could take a look 
at my [last post](https://hannomalie.github.io/posts/http-server-kotlin.html) for motivation and insights
about why and how to implement an http server from scratch on top of sockets only.

Doing it in a language you are not too familiar with is then another special experience. And of course
I chose Zig. Don't think you can get more hipster then that, especially in times of Bun and Ghostty.
The [last time I used Zig](https://hannomalie.github.io/posts/ray-tracing-in-zig.html) lies already nearly
freaking 3 years in the past and the truth is, I found it cool before it was cool. Jk. Alas, using
a system programming language can be a good idea for a network application like an http server, so
that's why I gave it a try.

### Zig tooling evolved

I am very happy to see that Zig tooling advanced in the last years. You might know that I am mostly using
Jetbrains IDEs and therefore have an incredible high expectation towards everything. Well, the Zigbrains
Zig plugin for IntelliJ is surprisingly good nowadays. It benefits from the excellent simple toolchain around
zig and has all of the expected core features and is stable. The zig language server doesn't crash anymore and
is not laggy anymore, so that I can say it's really a pleasent development now. Yes yes, it's still miles
behind posterchilds like Java or Kotlin or even Go, but I would say niveau is recommendable for production
usage. Big kudos. What is noticable though is lack of a lot of refactoring patterns, like extraction of local variable
etc.

### Zig is still weird

The language gets hyped a lot and praised for a lot of stuff regarding its design, but I still think there are
just some super ugly warts in the language. Everyone might know them and everyone probably complained about them,
nonetheless, they really stand out for me.

__Empty array.__ This is how you declare an empty array: `&[0]Header{}` and ideally this is how it should look like: `[]`.

__Config paramerers.__ Zig heavily relies on its own syntax for struct initialization when creating and configuring
objects. For example an allocator takes a config so that you can instantiate it like `std.heap.GeneralPurposeAllocator(.{}){};`.
Ignore the second pair of curly braces, it's because the function doesn't really return an allocator but only a type.
The problem is that there are no default parameters in Zig and so for an empty config you always have to pass an empty
object. That's ugly :shrug. Given example is especially interesting because the constructor is in fact deprecated and
the `init` method should be used instead. This avoids the requirement to pass in config and makes it effectively optional
like we wanted, but at the cost of introducing necessity of mutability. I don't think that's a good deal.

__Lambdas.__ There are function pointers in Zig. So you can declare a function type and use it as type for parameters.
But you can't inline an implementation for those types, you always have to declare functions on top level. Or use
an [anonymous struct with a member function](https://ziggit.dev/t/anonymous-functions-lambdas/1087/5), but I think that's
also a wart. And I also haven't tried to use the closure with the struct hack - for example I needed to pass in a request scoped arena
for allocation, but impossible with top level functions. But I will find out.

__Catch-break.__ Error unions are reeeeally cool. Exhaustive switches over them are too. What I don't like is that catch
clause are not expressions. They are... I don't even know, some weird attachment to function calls?! For example
you can use `return` in them, but it will return from the outer scope. When you want to return from the catch block
you need to add a label to it and do `break :blk "";` to return an empty string from a ctach block labeled "brk". Weird.
And then there is a special form of that pattern called catch binary operator that you can use to have default values
when an error occurs. Weird. Yes, yes, there are probably some reasons for that design yada yada yada.
Coming from expression oeriented languages that stuff just feels completely unnecessary and overly un-generic.

__Strings.__ No other part a standard library hurts so much when missing as string operations. Zig pretty much
leaves you with `[] u8` in the desert - surprisingly good general representation of strings, yet most of the functionality
you'll need is missing nonetheless. It screams to include something like [this](https://github.com/JakubSzark/zig-string)
in the standard library. This could probably said for a lot of things, like collecions and http (I think that's either
currently worked on officially or already integrated), but relevant for me in the given experiment was only strings.


That's what comes to mind for now. There is a ton of good stuff to say about Zig, but you'll find that yourself
when you just use it. You can be surprisingly productive with Zig, which is amazing, because Zig also fulfills requirements
like (fast) native compilation, cross compilation, low resource consumption, fast runtime performance etc.

### Some code

Many words, few code. Actually most of my code is too ugly to show here, but if you're brave you can find it
[here](https://github.com/hannomalie/ht-http-zig). This is how you can define routes, similar to my Kotlin http
server implementation:

```zig
fn handleIndex(_: Request) Response {
    const response = Response {
        .status = "200",
        .headers = &[0]Header{},
        .body = "<html><body>index</body></html>",
    };
    return response;
}

const routeDefinitions = [_]RouteDefinition {
    RouteDefinition {
        .method = "GET",
        .path = "/",
        .handler = handleIndex,
    },
};
```

Having that written, there's a ton of stuff left to learn. Allocator usage, restructuring with functions,
using tests, do some benchmarks, multithreading, take a look at resource consumption etc.