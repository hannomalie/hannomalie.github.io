title=Dissecting Spring petclinic (Part 5): Swap Spring for Javalin
date=2024-12-29
type=post
tags=architecture
status=published
headline=Dissecting Spring petclinic (Part 5)
subheadline=Swap Spring for Javalin
summary=I finally removed all bits of Spring from the project and moved it to Javalin.
image=images/spring_logo.svg
~~~~~~
This is the fifth entry in a series of blog posts about Spring.
Make sure to read [the introduction and my other entries](https://hannomalie.github.io/posts/dissecting-spring-petclinic-intro.html) before this one.

This time, I finally replaced all the bits of Spring by [Javalin](https://javalin.io/), a web framework that's truly simple. 
[This is the commit](https://github.com/hannomalie/petclinic-sandbox/commit/d5c86beca2a39d78b87f7f3eead98a4eb7e70220)
containing all the changes.

## TLDR
- With good preparation, it was surprinsingly simple to do the final conversion, despite of course taking some time.
- I don't see any loss by replacing all the stuff with minimalistic and simple code, only upsides.
- The application components became close to trivial: Routing, request parsing, validation, response application.
- Bean validation is a very nice api, no need to replace it.
- I now feel reassured that Spring projects usually contain a lot of cruft that can simply be removed.

## The endboss

Not sure if this is already the last post of the series, as the goal set at the beginning was to replace
Spring and all that came with it with something else that I see as a better fit for simple and easy web projects.
Which I just did: I removed all dependencies to Spring and translated the rest of the application from Spring
to Javalin. :shrug.

## Removing dependencies
In the build file, there are a bunch of dependencies. As Spring dependencies themselves come with a lot of transitive
dependencies, and also not all Spring dependencies work well with each other, Spring's automatic dependency management
is often used, like here in this project as well, by using the _"io.spring.dependency-management"_ gradle plugin.

When that (as well as the explicit dependency declarations) was removed, first the build complained that the explicitly
used dependencies (without a version declared) need to be adjusted, which is fair. I just picked the latest for all of
them and went ahead, no issue here. I want to stress, that this is thanks to the good design and maintainance
of the remaining projects I depend on. Like jackson, hikari, jte, jdbi, webjars and some more. As everyone always
complains about dependencies as being so evil, because one inevitably lands in a dependency hell: This is not true.
But it rises and falls with the quality of the ecosystem - whereas the core JVM ecosystem and good, simplicist libraries
with strong comaptibility stories really make dependency usage a pleasure. Yes, you need to stay away from the projects
that are not small and simple, which is ironically one of the things this whole series might be about. Because
for example with Spring - or maybe better with _libraries in the Spring ecosystem_ - I also had many projects
where dependencies caused a lot of headaches. Often the root of this evil is that libraries are too feature-rich. They
try to do too many things, therefore they incorporate too many dependencies and the sum of all the tiny risks
is then the big risk that the application developer faces.

### Bean Validation
Bean validation is normally not that of a complicated thing. However, I struggled a bit with the Spring integration (Spring Validation),
which - you guessed it - adds yet again some bits on top of that core library. For example I needed to search
how the actual validator is instantiated, it's not very intuitive. Or I didn't understand how the BindingResult from
Spring is derived from the Set<ConstraintVioloation>. Those things prove my point: Explorability is
not really given, one can hardly find out how that stuff works. Deep documentation is not given because nobody
seems to care about those details and digging in the code open source code of the framework is also not exactly trivial. 
In the end, I just came up with my own class that provides the methods used in the project.

When removing Spring, the implementation of bean validation needs to get added explicitly to the project.
Yet again a lot of wrong or outdated (?) information on how to add it resides on the internet. Some recommend
adding the api artifact without knowing that it will only work in a servlet container environment, where the implementation
is provided by the container. Or they recommend two versions that either don't exist or dont match. I don't want to be
overly critical here, but I indeed had to grin a bit on that api-implementation-seperation, as it is something that
was the norm in the Java EE days, because the implementation of a lot of things was provided by the runtime, yet
you needed to implement against some API, so you needed a seperate dependency for that.

Nowadays, Hibernate - _oh god I love the Hibernate project, it's so good :)_ - is the reference implementation and they got us covered with a
[comparatively simply documentation](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#validator-gettingstarted) on how to set the stuff up.
Using that, I was done quite fast.

But.

Some things around the pet didn't quite work, the type was not set and the application didn't complain and let
the null values through, causing exceptions. I made the mistake not to take a close look that the type property
in the Pet class or the class itself, because otherwise I had realised that the Pet class is the one exception
that doesn't use Bean Validation at all. No annotations in there. So of course the standard validator won't find
any issues with instances of Pet.

And then I found it. There's this PetValidator class in the project that implements some magic interface from
Spring Validation. It's picked up by the classpath scanner in a Spring app. But without Spring, it will just do
nothing at all, it's unreferenced code. Those implicit constructs are really disadvantageous for refacoring or
at least to understand the execution flow of your software.

The reason is also this class is also documented, for which I give kudos here:
> We're not using Bean Validation annotations here because it is easier to define such
validation rule in Java.

but when taking a look at the actual validation, this is a bit hard to believe, the actual validation code
is really simple:
```
// name validation
if (!StringUtils.hasText(name)) {
    errors.put("name", List.of(REQUIRED));
}

// type validation
if (pet.isNew() && pet.getType() == null) {
    errors.put("type", List.of(REQUIRED));
}

// birth date validation
if (pet.getBirthDate() == null) {
    errors.put("birthDate", List.of(REQUIRED));
}
```

The problem is: the type validation cannot be expressed with bean validation out of the box.
You would need a custom validator that operates on the class level. This is doable and also not terribly
complicated, [here's the documentation](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#section-class-level-constraints).
It's possible that this functionality wasn't available back when the petclinic was done. Or maybe I am wrong.
Or maybe the authors didn't know about that feature, or who knows. But the existence of a second mechanism
for validation really surprised me. So yes, it probably was _easier_ like that, but I wouldn't have done it.
And in fact, they wouldn't have either, when Spring hadn't offered it for free. 

I must say I really like to have a class definition and its validation rules in the same spot. Bean validation
is nice in that regard: You can still have mutable data that might be invalid at some point in time (for example
for form input) and you can validate it easily. I only see minor enhancements through libraries like
[this one](https://akkurate.dev/docs/overview.html#showcase).
Or like [what Javalin offers](https://javalin.io/documentation#validation-examples) us.
So keeping been validation is fine with me.

### Models and views

One of the biggest problems I face in projects that change a lot,
 is to understand which template needs which model. not statically verified, only at runtime.
before with the modelmap, a lot was implicit... even with jte, which is very close to normal java code and static typing when
you are __in__ the template, this issue remains. Because at the place where you evaluate the template and pass in
the models, you do it by using a string identifier for a template and a map of objcets.

Not sure if I am up to something most people don't realize, or if everyone else is just yawning, but: This is
really a nasty problem not only to refactor efficiently, but also one big advantage that jsx and tsx silently have: they
don't have that issue. Because the template code is right in the main code itself and the parameters are just the ones
that are passed in as params in the main code (or defined as local properties). Which means you IDE and your compiler
can treat it just like every other piece of code and assist you a lot here.
Since a lot of programming languages lack support for custom string template literals (or macors, compiler extensions etc),
they don't have a nice way to achieve such a solution. In Kotlin, which is superb at DSLs, there are things like
[this](https://ktor.io/docs/server-html-dsl.html) which is also quite nice and gives the same result. But with the
downside that you can't directly use html anymore, which is unacceptable in a lot of cases.

So, this issue got me a lot of times. I should have written a wrapper function for every top level template and mirror
the already defined parameters in code.

One more thing though. __Static typing__. Throughout the whole journey, static typing was the silent
companion that made most of the actions I took enjoyable and efficient.
It's because you can model a tiny thing, like the custom
response class or the view render function and get instant feedback on your fingertips wherever there is something
not integrating into each other. Java is surely not the best language when it comes to the topic,
but programming gets so much better when you approach the "if it compiles, it's correct" style. This is basically also
what this mirror function comes down to.

## RequestMappings

### Routing

In Spring, you have the actual route that corresponds to a controller action right at the method itself, it's
an annotation. I have to say that this is quite nice: When people search for a route, for example by string, they
find the annotation and themselves immediately at the code that matters. Unless it's a nested path. Or contains
a path pattern. Then, it's more usual to search for the right piece of code that lies behind a given path some time
while jumping through the dozens and dozens of controller code, because the routes are scattered everywhere over the whole
code base. Or at least that was how it was for me in that foreign petclinic project after I had to debug a bit, starting
from a given failing test. Well, could be a skill issue again. Or some people would rightfully point out that this
is only a problem when a test is a black box test that gives you just the route it calls, no reference to any structure
it uses, by definition. True. But it doesn't matter for any other case, where you want to associate a request with a
piece of code. Which at least __I__ have to do quite often at work. Or maybe the others are right and I am wrong,
but maybe we start from a different point: Wouldn't it be nice when you have a url or path and you can easily
find what an application actually executes when a request with that path comes in? Wouldn't it be at least helpful?

I tried to do it now for petclinic with the routing capabilities Javalin offers to us. Thw whole application is as simple
as this:

```java
public static Javalin startApplication(int port, Database database) throws IOException {
    database.createTables();
    database.createPetTypes();

    var objectMapper = new ObjectMapper();
    var vetController = new VetController(database, objectMapper);
    var welcomeController = new WelcomeController();
    var crashController = new CrashController();
    var ownerController = new OwnerController(database);
    var petController = new PetController(database);
    var visitController = new VisitController(database);

    var app = Javalin.create(config -> {
            config.staticFiles.enableWebjars();
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.directory = "static/resources";
                staticFileConfig.hostedPath = "resources/";
            });

            config.router.apiBuilder(() -> {
                path("/owners", () -> {
                    get(ownerController::processFindForm);
                    path("/new", () -> {
                        get(ownerController::initCreationForm);
                        post(ownerController::processCreationForm);
                    });
                    path("/find", () -> get(ownerController::initFindForm));
                    path("/{ownerId}", () -> {
                        get(ownerController::showOwner);
                        path("/edit", () -> {
                            get(ownerController::initUpdateOwnerForm);
                            post(ownerController::processUpdateOwnerForm);
                        });
                        path("/pets/{petId}", () -> {
                            path("/edit", () -> {
                                post(petController::processUpdateForm);
                                get(petController::initUpdateForm);
                            });
                            path("/visits/new", () -> {
                                get(visitController::initNewVisitForm);
                                post(visitController::processNewVisitForm);
                            });
                        });
                        path("/pets/new", () -> {
                            get(petController::initCreationForm);
                            post(petController::processCreationForm);
                        });
                    });
                });
                path("vets", () -> get(vetController::showVets));
                path("/", () -> get(welcomeController::welcome));
                path("/oups", () -> get(crashController::triggerException));
            });
        })
        .start(port);

    return app;
}
```

But I am not compeletely satisfied with it. That nesting there might be _dry_ but I think it doesn't help
much in regards of readibility. It would (theoretically) help us when we change some
top level paths like "owners", because then we only need to change it in one place, but come on. How often do
we do it. So I went with the probably unusual and polarizing approach, like always, and compressed the whole
routing definition to

```java
[...]
    .get("/owners", ownerController::processFindForm)
    .get("/owners/new", ownerController::initCreationForm)
    .post("/owners/new", ownerController::processCreationForm)
    .get("/owners/find", ownerController::initFindForm)
    .get("/owners/{ownerId}", ownerController::showOwner)
    .get("/owners/{ownerId}/edit", ownerController::initUpdateOwnerForm)
    .post("/owners/{ownerId}/edit", ownerController::processUpdateOwnerForm)
    .get("/owners/{ownerId}/pets/{petId}/edit", petController::initUpdateForm)
    .post("/owners/{ownerId}/pets/{petId}/edit", petController::processUpdateForm)
    .get("/owners/{ownerId}/pets/{petId}/new", petController::initCreationForm)
    .get("/owners/{ownerId}/pets/{petId}/visits/new", visitController::initNewVisitForm)
    .post("/owners/{ownerId}/pets/{petId}/visits/new", visitController::processNewVisitForm)
    .get("/owners/{ownerId}/pets/new", petController::initCreationForm)
    .post("/owners/{ownerId}/pets/new", petController::processCreationForm)
    .get("/vets", vetController::showVets)
    .get("/find", ownerController::initFindForm)
    .get("/oups", crashController::triggerException)
    .get("/", welcomeController::welcome)
[...]
```
And I think that's much easier to navigate and reason about. Yes, the slightly off alignment because "get" and "post"
have unequal amount of characters bothers me too :) One can also group them by http verb, which also makes sense,
because it's also one of the first facts you will know about a given request, so you can quickly filter
everything else out with that info. In the commit I linked, this is also what I finally kept, it's nice.

### Controller methods and framework integration

In Spring, controlers not only have meta info for routing, but they can also be target to dependency injection.
Since the routing component is somewhere in the framework and the one that calls your controller, it can
implicitly do things for you. For example `@Valid` annotated parameters are - if you configured it correctly - 
validated and you can get the validation result injected as well by just declaring it in the signature.
Or a path parameter can be injected automatically.
Like in `void postPet(@Pathparam("ownerId") int ownerId, @Valid Pet pet, BindingResult result)`.
Well, if you give up on inversion of control, the one in control needs to take over those tasks from the framework.
Congratulations! _Your_ code is the framework now. By changing all signatures to sth like `void postPet(Context ctx)`
I enabled the nice method reference syntax from above.
In the controller, everything that needs to happen happens, right in the code, it's explicitly there. I already
wrote a few times that I prefer that approach because it is so easily explorable, so clear, so natural, no documentation
needed, no quirks that the BindingResult parameter needs to be the next one after the entity that you validated
etc. pp.

```java
public void processCreationForm(@NotNull Context ctx) {
    var owner = getOwnerFromForm(ctx);
    var validationResult = validate(owner);
    var modelMap = new HashMap<String, Object>();
    modelMap.put("owner", owner);
    if (validationResult.hasErrors()) {
        modelMap.put("error", "There was an error in creating the owner.");

        ctx.result(renderView("owners/createOrUpdateOwnerForm", modelMap, validationResult));
    } else {
        owner = database.save(owner);
        modelMap.put("pets", database.findOwnerAndPetsByOwnerId(owner.getId()).pets());
        modelMap.put("message", "New Owner Created");
        
        ctx.result(renderView("owners/ownerDetails", modelMap, validationResult));
    }
    ctx.header("Content-Type", "text/html");
    ctx.status(200)
}
```

And when you for example jump into the `getOwnerFromForm(ctx)` function call,  you'll see simple code like this:

```java
public static @NotNull Owner getOwnerFromForm(Context ctx) {
    var owner = new Owner();
    var idString = ctx.formParam("id");
    if (idString != null && !idString.isEmpty()) {
        owner.setId(Integer.parseInt(idString));
    }
    owner.setTelephone(ctx.formParam("telephone"));
    owner.setCity(ctx.formParam("city"));
    owner.setFirstName(ctx.formParam("firstName"));
    owner.setLastName(ctx.formParam("lastName"));
    owner.setAddress(ctx.formParam("address"));
    return owner;
}
```

### The redirect headaches

I didn't mention it yet, but regarding the validation and BindingResult stuff I wrote about above, there is this
thing that adds flash values to the BindingResult. Effectively, you can add info that is displayed for a short amount of
time for the user and than it's hidden (by some trivial javascript in the templates).
But. All the form handling in this project uses the _post-redirect_ pattern, where you post some form and the server
only sends back a redirect resopnse with for example the owner details page you just posted the owner data for.
I must admit I haven't seen that for a long time, but it seems to be a quite well-known pattern, at least in web development
from 15 years ago. I was wondering what the actual reason is this pattern is used. So that one can basically reuse
controller methods without actually calling them, but calling them through redirecting to the corresponding route?

The thing is, a redirect is a response that will cause the client to send another request then, to access the actual
page that should be shown as a result. You know the big problem? It's a new request, and the "response data" you want
to send back after the data was posted is _gone_. Or in other words: When you want to display a "success" toast message
after someone posted a valid owner through a form, you can trivially do it by sending the page as a response.
But you can't when you send a redirect, because when the owner detail page is requested the next time, you don't know
about the "successful post of the form" from a request ago anymore on the server.
Only if. Only if you somehow keep that state, for example in the session. Compared to the trivial solution,
this needs an extreme amound of machinery and yet again (I know, you're tired of it, me too), Spring hides that
stuff in a way that it's hard or impossible to find out how it works and also it's very hard to replace it or refactor
arount it.

So I decided yet another time to go with simplicity and just removed all the redirects in favour of OK responses.

The nice thing: The browser tests immediately kept working and told me that everything is fine with the implementation.
Yessss.

The bad thing: A lot of the other tests failed because 302 response was expected and 200 was given. Ouch.
Even though I made the tests blackbox tests as far as I could, they broke, because they are coupled to the implementation
detail of the response of single routes. The (browser) user doesn't care whether you send a redirect to the owner
details page, so that it's displayed. The user cares whether he sees the owner details page after he send a form.

True, this is probably again the point where a lot of people claim that you would never apply change so big to a project
that those tests would break and therefore it doesn't matter. Yet, I have to insist, we need to ask
what more do browser based tests cost us? And is it not worth the cost, when we can replace the other tests with it,
so that we don't have tests coupled to the implementation anymore?

I need to write some more click tests and compare the performance of the test execution as well as the readability
of those tests in the project. It will basically come down
to the cost of initializing a framework like Playwright or Selenium and sprinkling
some ids over the templates. Maybe that will be another post in the series.

## Conclusion

This entry could be the last post of the series or at least the last one for some time, so that I can
work on some other things as well. So the conclusion might get a bit more general and not only focues on this entry alone.

All the changes to finally switch the web frameworks can be found in [this commit](https://github.com/hannomalie/petclinic-sandbox/commit/d5c86beca2a39d78b87f7f3eead98a4eb7e70220).
This change was actually not very hard, even though of course it also took some time to get done.
It's probably due to the fact that the change was planned long ago and set as a goal and also well prepared.
The existing blackbox tests served me well this time, as I made some small mistakes here and there.
Once again, I regret that I didn't implement more important clickpaths as browser tests very early.
Especiallly when I had to adjust even the blackbox tests a bit for one change this time.

Throughout the whole endeavour and this time again with the validation stuff, I realized that I really dislike
implicit features, hidden framework behaviour that is nowhere explicitly written down in the actual code.
Some people are happy with it and don't care (until they have to...) and that's fine, but that's not for me.
I think I showed that we have very powerful and much simpler approaches to implement what we need.
Overly relying on a framework might not be objectively bad, but it certainly feels strange for me, because
I don't really see why we _need_ that. Often I have the impression that we just use that stuff, because it's there,
even though there are simpler approaches available. Take the redirect pattern for example. How much
machinery can be ommitted by just doing it in a __simpler__ way.

Now that I reached the finish line, I am not sure what the conclusion really is. Can Spring be replaced in a project?
Certainly. Does it make sense for you? Probably not. Can we nowadays get better projects if we wouldn't use Spring?
Yes, definetly. Maybe the only fair and professional conclusion I can draw right now is, that we must focus
on what we actually want to do, not what we _could_ do when we just introduce framework xyz.
When stuff is made available, it will be used. The more stuff will be used, the more stuff needs to be known,
needs to be taught, needs to be reviewed and enhanced. We must urge ourselves to chose the smaller, simpler solution
whenever it's possible. Yes, there are different opinions and poeple and so on. But for most of the stuff
I did in this series, I think it's rather clear what the simpler solution is after all.

Even though I didn't take an explicit look and experimented with aaaaaaall the stuff one could do with Spring, aaaaaaall
the stuff that was also done in this project - I am sure it can be done with the transformed version as well. Like
graalvm native compilation. Or bean validation integration, as shown. Or micrometer metrics. 
Or using Retrofit. Or using resilience4j. Or provide openapi docs. All that good functionality is already
avilable in high class libraries that can simply be composed into your application. Just for completeness,
there might be those projects, those special cases, where one would tell me that you need all the features of Spring
and you can't live without automatic bean form derivation, and ddd autogeneration
und archunit integration and whatnot because otherwise the project code will surely go to hell. I don't buy it.

So as much as it hurts myself too, I have to write it: **Spring is not worth it anymore from my perspective.**