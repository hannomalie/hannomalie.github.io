title=OIDC in your app with Keycloak
date=2025-03-19
type=post
tags=code,architecture,web
status=published
headline=OIDC in your app
subline=made simple with Javalin and Keycloak
summary=I show how the simplest possible integration of OIDC in a web app can look like with Javalin and Keycloak.

~~~~~~

> __TLDR:__ [Here](https://github.com/hannomalie/javalin-keycloak-oidc) I created a repository containing OIDC standard
flow integration in the simplest possible way in a simple Javalin webapp, without any big and complicated frameworks 
that hide stuff from you, so that you can see all the good http happen right there. Contains a completely self contained
Junit test with high fidelity.

It looks like the days where it was okay and sufficient to just use username password authentication in your apps are long over.
Almost all (web) projects I worked on in the last years used token authentication and single sign on. Almost always,
it was a dedicated team that "owned" the auth topic - so very few people had moderate knowledge of the topic, while
the other 95% of the developers knew almost nothing about it. I have no idea why I write this, I don't think that
we are able to drastically change that situation - because there is just too much stuff to know about auth in order
to make everyone the expert we need to own the topic. But as always, I think it's helpful to understand at least
some basics and therefore I wanted to implement a simple __OIDC__ authentication flow with nothing but http. So that
one can see what is necessary to get a basic, yet quite sophisticated base going. If you don't know anything about
the topic, this blog is probably not the right place to read about it, maybe 
[this Microsoft blogpost](https://www.microsoft.com/en-us/security/business/security-101/what-is-openid-connect-oidc)
is a good introduction.

Since nothing is worth a penny without a high-fidelity automated test, I implemented exactly that. Using a __Keycloak__
testcontainer, __Javalin__ and __Playwright__ to actually navigate a true browser like a user.

### Basic OIDC standard flow

Brief summary what the standard flow does:

1. The user enters your web page.
2. He's determined as unauthenticated, so he's redirected to the login page.
3. The login page is a username password mask directly delivered from an auth system
4. The credentials are verified by the auth system and the user gets redirected again
5. When successful, the user's web context is populated with an access token which is now sent with every request the
user's browser performs
6. The access token is verified on each request and the user gets redirected if it's invalid
7. The access token's payload is used to authorize the user's actions

### How to manually configure Keycloak

So far so good. Let's now dive into __the__ open source solution for auth: Keycloak. OIDC is a standard, which means
that when you test with a compliant solution, you automatically achieve a high fidelity and you can be sure that
your app actually works. However, a lot of the end-to-end-experience will depend on the __config__ you apply to the
systems. Simple example: When your prod system doesn't have user Max Mustermann, you won't be able to login
as Max Mustermann. No matter how nice your test Keycloak is configured to have a Max Mustermann and no matter
how nice you test that the login succeeds in that case - in prod, people won't be able to login because their
user is missing. Another example: When your prod auth system doesn't allow to redirect to /foo after successful
login, your app will happiliy do it in your test setup, but will fail to do so in production.

[Here](https://www.keycloak.org/getting-started/getting-started-docker) is a very nice introduction about what
you have to do in Keycloak in order to get basic authentication running. The steps are

1. Run Keycloak
2. Access the admin interface
3. Create a __realm__, which can be understood as a tenant and configure it
4. Create a __user__ in the realm
5. Create a __client__, which can be thought of an "application kind" and configure it

### How to configure Keycloak in code

After doing it manually, you understand what elements and config is needed. In the test setup we can do it in code:

```kotlin
KeycloakContainer().use { keycloak ->
    keycloak.start()

    val realmName = "myrealm"

    var server: Javalin? = null
    val keycloakBaseUrl = keycloak.authServerUrl

    val keycloakAdmin = Keycloak.getInstance(
        keycloakBaseUrl,
        "master",
        keycloak.adminUsername,
        keycloak.adminPassword,
        "admin-cli"
    )
    // thanks to testcontainers we could as well do 
    // val keycloakAdmin = keycloak.keycloakAdminClient

    val serverPort = 8081 // this is the port where my webapp runs locally

    keycloakAdmin.realms().create(RealmRepresentation().apply { // thanks Koltin for scope functions <3
        id = realmName // thanks Koltin for property conventions <3
        realm = realmName
        isEnabled = true
        users = listOf(
            // this is our test user we want to enable to login
            UserRepresentation().apply {
                email = "test@test.de"
                username = "testuser"
                firstName = "Tester"
                lastName = "McTest"
                isEnabled = true
                credentials = listOf(CredentialRepresentation().apply {
                    type = CredentialRepresentation.PASSWORD
                    value = "12345"
                    isTemporary = false
                })
            }
        )
        clients = listOf(
            ClientRepresentation().apply {
                // this is the client, I named it like the web framework i use for the web app, normally
                // you would have something like "orderservice-client"
                id = "javalin"
                clientId = "javalin"
                clientAuthenticatorType = "client-secret"
                isPublicClient = true
                protocol = "openid-connect"
                name = "javalin"
                baseUrl = "/"
                // this is important, you want to restrict the urls where your user will get redirected to
                // after a successful login
                rootUrl = "http://localhost:$serverPort"
                redirectUris = listOf("/auth_redirect")
                isEnabled = true
            }
        )
    })

    val realm = keycloakAdmin.realm(realmName).toRepresentation()
    assertNotNull(realm)
}
```

### How to test with Playwright

And now you can verify that your login is indeed successful, when the correct credentials are passed in:

```kotlin
// [...]

val server = startServer(keycloakBaseUrl, serverPort, realmName)
try {
    val loginUrl = "$keycloakBaseUrl/realms/${realmName}/protocol/openid-connect/auth?" +
            "scope=openid&amp;" +
            "response_type=code&amp;" +
            "client_id=javalin&amp;" +
            "redirect_uri=http://localhost:$serverPort/auth_redirect"
    
    
    Playwright.create().use { playwright ->
        val browser: Browser = playwright.chromium().launch()
        val page: Page = browser.newPage()
        // try to access a secured page without login
        page.navigate("http://localhost:$serverPort/secured")
        // expect to get redirected to the login page
        assertEquals("""<html><head></head><body style="background-color:black"><a href="$loginUrl" target="_self"><button class="btn">Login</button></a></body></html>""", page.content())
    
        // click the big login button ;)
        page.getByText("Login").click()
    
        // on the login page provided by keycloak, fill in credentials and login
        page.locator("#username").fill("test@test.de")
        page.locator("#password").fill("12345")
        page.locator("#kc-login").click()
    
        // expect to be on a page where logout is available
        assertEquals("""<html><head></head><body style="background-color:black"><a href="/logout" target="_self"><button class="btn">Logout</button></a></body></html>""", page.content())
    
        // try to access another secured page
        page.navigate("http://localhost:$serverPort/secured-as-well")
        assertEquals("<html><head></head><body>This is another secured endpoint</body></html>", page.content())
    
        page.goBack()
    
        // logout and expect a redirect to the login button page again
        page.locator(".btn").click()
        assertEquals("""<html><head></head><body style="background-color:black"><a href="$loginUrl" target="_self"><button class="btn">Login</button></a></body></html>""", page.content())
    }
} finally {
    server.stop()
}
```

### How the app looks like

For the given test, a simple app written in Kotlin with Javalin can be as simple as this:

```kotlin
fun startServer(keyCloakBaseUrl: String, port: Int, realmName: String): Javalin {
    // (step 0: get keycloak public key)
    val getPublicKeyRequest = Request.Builder()
        .url("$keyCloakBaseUrl/realms/$realmName")
        .get()
        .build()
    val publicKey = client.newCall(getPublicKeyRequest).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val publicKey = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(response.body!!.bytes(), PublicKeyResponse::class.java).public_key

        val decodedKey: ByteArray = Base64.getDecoder().decode(publicKey)
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decodedKey))
    }

    // (step 2: define routes)
    val app = Javalin.create()
        // (step 2a: define a before filter that requires an access token for all routes but 3 exceptions)
        .exception(InvalidTokenException::class.java) { e, ctx ->
            ctx.redirect("/login")
        }
        // (step 2b: define a before filter that requires an access token for all routes but 3 exceptions)
        .beforeMatched { ctx ->
            if (ctx.path() !in listOf("/unsecured", "/login", "/auth_redirect")) {
                validateAccessToken(publicKey, ctx.accessToken)
            }
        }
        // (step 2c: define a logout endpoint that removes the token and redirects to login page)
        .get("/logout") { ctx: Context ->
            ctx.accessToken = null
            ctx.redirect("/login")
        }
        // (step 2d: on login page we have a button that sends the user over to the keycloak login page)
        .get("/login") { ctx: Context -> ctx.result(getLoginPage(keyCloakBaseUrl, port)).header("Content-Type", "text/html") }
        .get("/unsecured") { ctx -> ctx.result("This is a unsecured endpoint") }
        .get("/secured") { ctx ->
            ctx.result(
                """
                <html><head></head><body style="background-color:black"><a href="/logout" target="_self"><button class="btn">Logout</button></a></body></html>
            """.trimIndent()
            ).header("Content-Type", "text/html")
        }.get("/secured-as-well") { ctx ->
            ctx.result(
                "<html><head></head><body>This is another secured endpoint</body></html>"
            ).header("Content-Type", "text/html")
        }
        // (step 2e: this is the endpoint the user will get redirected to after login, a "code" parameter is send with it)
        .get("/auth_redirect") { ctx ->
            val request = Request.Builder()
                .url("$keyCloakBaseUrl/realms/$realmName/protocol/openid-connect/token")
                .post(
                    FormBody.Builder()
                        .add("client_id", "javalin")
                        .add("redirect_uri", "http://localhost:${port}/auth_redirect")
                        .add("grant_type", "authorization_code")
                        .add("code", ctx.queryParam("code").toString())
                        .build()
                )
                .build()

            // (step 2f: we post the code to keycloak to get an access token and safe it into the user's cookies
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val authenticationData =
                    ctx.jsonMapper()
                        .fromJsonString<AuthenticationData>(response.body!!.string(), AuthenticationData::class.java)
                ctx.accessToken = authenticationData.access_token
                ctx.redirect("/secured")
            }
        }
        .start(port)
    return app
}
```

While we need some helper functions. First, the login page:

```kotlin
private fun getLoginPage(keycloakBaseUrl: String, port: Int): String {
    val url = "$keycloakBaseUrl/realms/myrealm/protocol/openid-connect/auth?" +
            "scope=openid&" +
            "response_type=code&" +
            "client_id=javalin&" +
            "redirect_uri=http://localhost:$port/auth_redirect"
    return """
            <html><head></head><body style="background-color:black"><a href="$url" target="_self"><button class="btn">Login</button></a></body></html>
        """.trimIndent()
}
```

There is a simple a tag that issues a get request when clicked. The target is this keycloak url and we need to pass
in some parameters, for example which client we are or which redirect we want to receive after a login.

Then the validation method for the key.

```kotlin
fun validateAccessToken(publicKey: PublicKey, accessToken: String?) {
    accessToken ?: throw InvalidTokenException(IllegalStateException())

    try {
        val parsed: Claims = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(accessToken).payload
    } catch (e: Exception) {
        throw InvalidTokenException(e)
    }
}
```

No key? No access. Even though I am normally all against exceptions, this is one of the nice usecases where
blowing up the call stack is the desired behaviour. Only thing to ensure is that this single type of exception
is treated properly. As you can see above in _step 2a_, it's very easy to do so.

### Wrap-up

And that's about it. Not that complicated. 
There are some more essential additions like _refresh tokens_ or maybe one or two flows.
But my impression is, that besides the simplest possible application,
there is just a ton of additional stuff and variants, options, configs and facets that blow up this whole auth topic
enourmously. How many different algorithms do we need to verify the integrity of a key. How many flows could we possibly
need. I still remember the good old days where we developed apps with a user table, everyone used password auth
and everyone rolled their own authorization layer. I have to admit I liked that :)

Alas, you can find the repository containing all the code [here](https://github.com/hannomalie/javalin-keycloak-oidc).