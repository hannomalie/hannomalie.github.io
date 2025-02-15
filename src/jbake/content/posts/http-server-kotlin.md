title=HTTP server from scratch
date=2025-02-15
type=post
tags=code,web
status=published
headline=HTTP server from scratch
summary=I implemented an http server with a simple router from scratch only using TCP in Kotlin.
image=images/Kotlin_Icon.png

~~~~~~

### Foreword

HTTP is really one of __the__ fundamentals in web development. I think it's not necessary
to explain why lerning the fundamentals is always beneficial. But HTTP is huge, I don't think anyone
has enough spare time to implement the complete standard. Yet, implementing the most common stuff
"from scratch" is surprisingly doable. It takes a couple of hours and maybe a programming language you're not
completely new to.

## TCP

Of course one can go even lower than that, but starting with TCP seems to be sane for me. On the JVM, you
can use an ages old part of the standard library which is similar to posix sockets but not quite as low
level as them. But in essence, you get a [socket where you can rad bytes from](https://www.baeldung.com/a-guide-to-java-sockets).

Network programming can be realy interesting. With TCP, the only thing that you can rely on is that you
dont have to care (much) about connection handling and that your server gets bytes that were send by a
client (and made it over the network). Not how many, not how fast, maybe not in one chunk, maybe in one chunk,
maybe in 20.
So when someone sends "hello" your server might receive "h", then "el" and then "lo". How can one deal with that?
How do you know when the message is finished and isn't "hello you" instead?

## HTTP

Well, we're not creating a custom protocol and for HTTP the answer is simple: You will receive a newline
at some point and before the newline, you received the length of the rest message. Tada, the first important
thing of HTTP was already cleared up: the content length header.

So I came up with this piece of code:

```kotlin
val serverSocket = ServerSocket(port)
val clientSocket = serverSocket.accept()
val outputStream = clientSocket.getOutputStream()
val out = PrintWriter(outputStream, true) // true means auto flush
val inputStream = clientSocket.getInputStream()
val bufferedReader = BufferedReader(InputStreamReader(inputStream))

var emptyLineIndex = -1
var line: String

while(emptyLineIndex == -1) {
    line = bufferedReader.readLine() // this one will block
    lines.add(line)
    emptyLineIndex = lines.indexOf("")
}
```

And as soon as this point is reached, you can start extracting all the stuff from the message that HTTP offers:
The method, the path, query parameters etc:

```kotlin
val methodLine = lines.first()
val methodLineParts = methodLine.split(" ")
val method = methodLineParts.first()
val pathAndParams = methodLineParts[1]
val pathAndParamsString = pathAndParams.split("&")
val path = pathAndParamsString.first()
val paramsString = pathAndParamsString.subList(1, pathAndParamsString.size);
val params = if(paramsString.isNotEmpty()) {
    paramsString[0].split("?").map {
        val keyValue = it.split("=")
        Param(keyValue[0], keyValue[1])
    }
} else emptyList()
```

One of these lines is the optional content length header, extracting it is easy:

```kotlin
val headerLines = lines.subList(1, emptyLineIndex)
val headers = headerLines.map {
    val (key, value) = it.split(": ")
    Header(key, value)
}
val contentLength = headers.firstOrNull { it.key.lowercase() == "content-length" }?.value?.toInt()
val body = if(contentLength == null) {
    ""
} else {
    val bodyUntilNow = lines.subList(emptyLineIndex, lines.size).joinToString("").toCharArray()
    val remainingBodySize = contentLength - bodyUntilNow.size
    val restBody = CharArray(remainingBodySize)
    bufferedReader.read(restBody)
    String(bodyUntilNow) + String(restBody)
}
```

Keep in mind what I wrote about TCP above. It's possible that you already received part of the actual body
or even the whole message on the first read. Thats why one can't just assume that the first byte we then
read from the socket (the buffer to be precise) is really the beginning of the body.
Since I Used a buffered reader in order to conveniently read terminated lines from the buffer,
I in turn need to use CharArray instead of ByteBuffer
when reading the rest body, which feels a bit clunky, but it's what it is I guess.

Composing the response is rather trivial then.

## Request handler

What should your server respond, when there are not route definitions? Hence, I added a very tiny router
which might remind you of existing routers in javalin or express. It can then be used like this:

```kotlin
HttpServer(
    port,
    RouteDefinition("PUT", "/body") { request ->
        Response(
            200,
            listOf(Header("Content-Type", "text/html")),
            request.body
        )
    },
).runOnSocket()
```

It now works when you access the http server from your browser and testing can be easily done with
any http client, like the one that is included in the Java std lib for some time already:

```kotlin
@Test
fun putBody() {
    val client = HttpClient.newHttpClient()
    val request: HttpRequest = HttpRequest.newBuilder()
        .method("PUT", HttpRequest.BodyPublishers.ofString("some body", Charsets.UTF_8))
        .uri(URI.create("http://localhost:9909/body"))
        .build()
    val response: HttpResponse<String> = client.send(request, BodyHandlers.ofString())
    assertEquals(response.statusCode(), 200)
    assertEquals("some body", response.body())
}
```

## Closing words

Of course this is just a fun experiment. I probably made some mistakes and wasted some efficiency, especially
with the conversion between CharArray and String instead of using raw bytes. Maybe I change that one day and
make a comparison. Other than that, there's a ton of stuff left in HTTP to implement. And there is
async implementations and multithreading.
As always, [code is public](https://github.com/hannomalie/ht-http-kt) - but feel free to find a
better introduction and better code elsewhere on the internet.