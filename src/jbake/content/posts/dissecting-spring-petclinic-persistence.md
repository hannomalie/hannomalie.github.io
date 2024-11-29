title=Dissecting Spring petclinic (Part 2): Bending the persistence
date=2024-11-29
type=post
tags=architecture,design
status=published
headline=Dissecting Spring petclinic (Part 2)
subheadline=Bending the persistence
summary=I converted all tests in Spring's example project to use a different persistence layer. Let's take a look at pros and cons.
image=images/spring_logo.svg
~~~~~~
This is the second entry in a series of blog posts about Spring.
Make sure to read my [entry post and previous entries](https://hannomalie.github.io/posts/dissecting-spring-petclinic-intro.html) before this one.

This time, I replaced Spring data with [JDBI](https://jdbi.org/#_obtaining_a_managed_handle),
close to pure SQL and some simple POJOs to represent persisted data

## TLDR
- Spring data and Hibernate (or JPA as well) do a lot of necessary work for you. It's really great when you are okay with its requirements.
- Staying pure, simple and data-centric when it comes to persistence is not as easy as one thinks, at least not for applications that need a lot of data creation
- I don't think I changed the project for the better with my latest changes. I can only see them as a necessary step towards the
greater goal.

## How it started

When I explored the petclinic project, I realized that in most of the tests, I didn't
immediately have an idea what exactly is tested. I tackled this problem with the stuff I wrote about in Part 1 of the series.
I also realized, that I don't consider the existing tests to be particularly good for a set of reasons. Even though I
enhanced a few of those concerns, some are still left in there. For example a good test should make clear what
data is arranged, what functionality is triggered and what behaviour is expected. The arrangement of the data is
ideally done right in the test to keep locality of reasoning high and to have tests being independent of each other.
Let's take a look at an existing test.

[Here](https://github.com/hannomalie/petclinic-sandbox/blob/blackbox/src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java)
you can find the OwnerControllerTest. There is a test

```java
@Test
void testShowOwner() throws Exception {
    var httpResponse = get("http://localhost:" + port + "/owners/" + TEST_OWNER_ID);

    assertEquals(200, httpResponse.statusCode());
    assertThat(httpResponse.body().toString()).containsSubsequence("<tr>","<th>","Name","</th>","<td>","<b>","George Franklin", ...
    assertThat(httpResponse.body().toString()).containsSubsequence("<thead>","<tr>","<th>","Visit Date","</th>","<th>","Description","</th>","</tr>","</thead>","<tr>","<td>","2024-10-10", ....
}
```
Let's ignore for a second that the actual assertions (that I wrote, not the petclinic authors) could be more speaking and concise.
Why should the get request in this test lead to George Franklin being present on the owners page? We can try to find out
what this "TEST_OWNER_ID" is that is referenced. But it will only lead us to a top level declaration without any
further info. What happened is, that I worsened the code in this class, because [before](https://github.com/hannomalie/petclinic-sandbox/blob/main/src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java#L67-L103),
there was some class scoped init code before, that actually made explicit what data was initialized before each test.
On the upside, I removed a lot of boilerplate. Well.
Since I had [something like this](https://github.com/hannomalie/testing-with-repositories/blob/master/src/test/java/ApplicationTest.java#L68-L75)
in my mind when I applied the first changes to the project, I converted all the tests
to use a real database - and then, i realized that there was actually a set of default data already present in 
[an sql file](https://github.com/hannomalie/petclinic-sandbox/blob/main/src/main/resources/db/postgres/data.sql).
This is one of those magic, unexplorable things Spring does for you. You can not find the answer to why this data
is created for the test in the test anywhere. [Here's](https://www.baeldung.com/spring-boot-data-sql-and-schema-sql)
a good read about the topic, how to explicitly override it etc. pp, but the point stands: a feature that
favours convenience over actual readability and explorability.

> **_NOTE:_**  This statement is not unique to Spring, a lot of tools have a mechanism like that, some name the file
a bit different or place it in a different location.

This is not only a common critique towards Spring Data in particular, but it's also something I stumbled over
in a lot of Spring projects: By default, most of them start off using Spring Data.
Then again, most of them kept it, if not all of them. Then, all of them made me and my colleagues struggle again and
again with the implicitness of how the data looks like, how it is organized, how it is accessed. In a lot of projects,
we had to get creative in order to solve the eager/lazy loading issue when entities reference each other.

So let's see if we have just "skill issue" here and find out whether there could be a simpler approach at all.
__Let's remove Spring data completely and replace it with some very simple mapping library like JDBI.__

## How it went

Now, to get closer to what I consider the ideal solution in those tests, I need to get some aspects of the old code back - the local
repository reference and the ability to add things to it and clear it. Since the goal is to replace the existing implementations,
this is where I added an interface layer and created repository classes for all existing entity types that are independent of
Spring (well, almost, the PageableImpl class is handy).

### Deactivating the default implementations

A first gotcha is something that would be very simple with good old code: How to disable the existing persistence completely
and switch to the new, in-memory implementation? It _could_ be `val ownerRepository = InMemoryRepository()` instead of 
`val ownerRepository = SpringDataOwnerRepository()`. But the latter doesn't exist. We only have an interface, while
the implementation is created by the container, out of our control. [Here](https://stackoverflow.com/questions/36387265/disable-all-database-related-auto-configuration-in-spring-boot)
you have a stackoverflow post with a wall of text that can now be said to this seemingly simple change of the project.
Now, when the real implementation is deactivated, the tests will complain that a bean definition can't be found,
so we have to provide the new implementations somehow. [Here's](https://stackoverflow.com/questions/35742920/overriding-beans-in-integration-tests)
the next wall of text what could be said to the topic when Spring comes into play - I wil just refer to the single line
of code a few lines above what the ideal solution could look like.

Furthermore. There is something that is a helper to launch an application in combination with a databse locally without
much hassle, just a main method: [MysqlTestApplication.java](https://github.com/hannomalie/petclinic-sandbox/blob/main/src/test/java/org/springframework/samples/petclinic/MysqlTestApplication.java).
It's in the test sources so that
Testcontainers can be used to run the database. When I override the beans, right in this @Configuration-annotated
class, then those definitions affect the whole rest of the source set - for example I tried to just disable
the Spring data stuff and define my in-memory implementations with @Bean declarations and suddenly my OwnerControllerTest
failed with a duplicate bean definition error. Yes, I know why this happens __technically__. But it's just another example
of needless entanglement of things that should naturally just work completely independent. Again, let's compare it to
the vanilla version in terms of simplicity and robustness.

Let's continue. I would like to move this _@TestConfiguration_ with my new repositories
into the super class, so that all tests can simply reuse them and so that I can abstract over that stuff a bit
to counter Java's verbosity. And then of course I can also inject the repositories so that all tests can just freely use
them. Boom. Unsatisfied dependency error at runtime. Okay okay, then I will move the field declarations into the test, we make
a compromise. Boom. Doesn't work. Okay, okay, I will create a seperate class, then use @Import on the base
test and then I can finally inject the stuff in the base test. Phew.

The next thing that somewhat surprised me is the duplication of the dummy data initialization. I would
have expected those simple things to just use core sql and then apply the same script to all database implementations.
But not the case, there were three different implementations. This might very well be for demonstration purposes, as
the petclinic supports multiple database implemenations, like Postgres or MySQL, I haven't tried to unify it yet.

### Records: the disappointment
I was looking forward to removing the entity classes code bloat alltogehter and replace them with Java records.
This is not possible, because records are immutable and entities need to be mutable, otherwise they won't work
with jpa or the whole model bindings for the views. I mean I knew that, but I ran into it again nonetheless.
I continued to go in that direciton by at least using records for the new queries I replaced the entities with,
but it turned out to be just aweful because of all the mapping and conversion bloat. At the given point, I can't
convert eveything to be data-centric, rather than state-centric. Maybe at some later iteration. In comparison
with Kotlin's data classes, records have really bad ergonomics, I have to state. And I am surprised what an effect
worse usability of a vital thing like data modeling has on (my) coding.

### Actually removing entities
As written above, I didn't replace the old entity classes. But I managed to remove all JPA annotations and made them POJOs.
This was actually super hard, because the whole project relied on their structure. One view had a list of owners, each
with a list of pets, each with a list of vet visits. When removing the list of viits from a pet, the view will simply receive
null values, no static typing with thymeleaf. Those cases had to be replaced with multiple parameters in the model
and for example a map that can be indexed by owner or respectively pet id.

### Switch to JDBI
Besides those connection issues I mentioned above, switching to JDBI was more or less straight-forward. We can
easily reuse the existing datasource that Spring injects into the new database class and instantiate a JDBI instance
from it. In tests, everything works fine, but for regular setup, I get connection refused errors wich I am not
able to track down. Nothing seems to help.
The biggest effort however, was in translating hibernate query language to sql. I would have loved to keep hibernate query language
as it gives us platform independence and convenience, something that I _worsened_ by converting to sql. But unfortunately, hibernate
strictly relies on using entities and explicitly not just database tables directly. I would wish for using hibernate
query language without the entity layer, but [I guess it's not possible](https://stackoverflow.com/questions/49999274/get-data-through-hibernate-without-entity-classes).
By the way, the [Hibernate documentation](https://docs.jboss.org/hibernate/stable/orm/userguide/html_single/Hibernate_User_Guide.html) is __excellent__, a really recommended read.

The conversion showed me, how much stuff JPA, Hibernate or even Spring data do for you behind the scenes.
I won't say it's the best way how they do it, but at least partly it's necessary stuff that you need to do by hand as well.
For example an __upsert__ - have you ever done that with pure SQL? There'slot of stuff to know about that. For example
when you have some owner data. With JPA your entity is always connected to the database context and you can query it,
save your data to a row by calling a simple method and it just saves the stuff for you whether it's new or not.
Without such a tool, you have a lot of options, for example first you could execute a statement to find out if
a row with a given id exists. or you could do an update statement and take a look how many rows are affected and only
if none were execute an insert statement. And then you need to be careful which database implementations can give you
back data as result and how it is actually returned - in one case I got a BigInteger back instead of an Int, for a generated id - maybe it could be
a Long as well, who knows all that? And you will only find out at runtime, when you run with that actual database implementation.
Additionally, I had to duplicate property lists because I needed those two statements for every table where I wanted to do an upsert.
Not pretty and prone to errors when you forget to add a new property to your data/column to your tables.

Take a look at what bloat I created for upserting - it was a simple `owner.save()` before:

```java
public Owner save(Owner owner) {
    jdbi.inTransaction(handle -> {
        var updatedRowsCount = owner.getId() == null ? 0 : handle.createUpdate("""
            update owners
            set first_name = :firstName, last_name = :lastName, address = :address, city = :city, telephone = :telephone
            where id = :id
            RETURNING *
            """)
            .bindBean(owner)
            .execute();
        if(updatedRowsCount == 0) {
            Map<String, Object> id = handle.createUpdate("""
                    insert into owners (first_name, last_name, address, city, telephone) values (:firstName, :lastName, :address, :city, :telephone)
                    """)
                .bindBean(owner)
                .executeAndReturnGeneratedKeys("id")
                .mapToMap().one();
            var idObject = id.values().stream().findFirst().get();
            if (idObject instanceof BigInteger) {
                owner.setId(((BigInteger) idObject).intValue());
            } else if (idObject instanceof Integer) {
                owner.setId((Integer) idObject);
            }  else if (idObject instanceof Long) {
                owner.setId(((Long) idObject).intValue());
            } else {
                throw new IllegalStateException("Don't understand id value: " + idObject);
            }
        }
        return null;
    });
    return owner;
}
```

All in all, I don't think I did any good to the actual queries - I think they were better before. The good thing is,
that by implementing all data handling it got explicit, so there's no question anymore where what is fetched or not.
It's simply written out and you can debug into it.

### Schema handling in general
I saw, that there is schema generation functionality in Hibernate, which can generate your db schema from your entities.
Well, I want to state that I think this is exactly the opposite way it should be done: I strongly believe that the persistence
- which is a boundary between now and the past that you have no chance to remove - should be the dominating element in
this relationship. I consider the approach taken by JOOQ or SqlDelight the best one: Define the schema natively.
Including migrations, they are inevitable. Then derive your types from there. SqlDelight does this just perfectly.
So I am looking forward to applying that conversion at some point in the petclinic project.

### Spring, why you so slow
Not exactly related to the persistence topic, but I have to get it off my chest:
Everything in this project takes so much time, it's annoying. When executing tests in the ide the standard way, the task
`processTestAot` takes 3-4 seconds of time. Every. Single. Time. Maybe there's a way to skip it, maybe it can be simply
excluded by adding -x to the gradle config, or the gradle default config, so that it's also applied to
every new run config that is created when I run a single test - but tbh I don't care.

Another 1,5 - 2 seconds is spent setting up the docker client and starting the db container - this is likely
__my fault__, because I made every test
use a true database. I will eventually remove that, as soon as I removed Spring and the static test annotations,
so that I can easily abstract over starting a container or not.

From the first rendering of the petclinic logo on the command line to the first failing/succeeding test, it takes
22 whopping seconds. The test execution itself is in ms. This got just way out of hand - whatever the reason is
that stuff is so slow. Usually people now start to tell me that stuff should be _unit tested and mocked and Spring's sliced
contexts_ should be used and then everything will be faster, yada yada yada.

No. [This](https://github.com/hannomalie/testing-with-repositories/blob/master/src/test/java/ApplicationTest.java#L68-L75) is 
the benchmark. If worsening the way of working (imo) and making the actual code worse (imo) just to please the framework
that is dominating you, than that's just plain bad. In the professional Spring projects (and other techstack as well),
there are usually always a bunch of people knowing more details about the framework in use and usually do some magic tweaks
here and there and do some education why strange feature xy should be used in order to improve self-imposed issue xyz and so on.

At the current point in my carrer, I dislike that. I see __increasing complexity__ because it's __additional info__ you have
to __process, handle, update, keep and pass__. The onboarding increases. The damage of "uneducated" people increases, because
more people don't know enough to be "stellar" in that area. I always think that there is a different way to do things,
it's just that we need to want it, actually. Curious if I will regret my current point of view in the future, but doesn't
look like it, so far.

### Converting sql dummy data to code
I was a bit puzzled when I saw the amount of sql code, for example for the example data. When I moved all tests
to use real databases, I somehow needed needed to have data setup in every test, whereever it was needed. This was not
possible with the old interfaces and so I just stick to using (and slightly adjusting...) the example data, so
that I can test with that. I now exchanged that with fine granular data initialization per test, so that each test
now has it's arragent-act-assert structure better recognizable. Here's an example.

```java
// In base class, applied to every test subclass
@BeforeEach
void beforeEach() {
    database.clear();
}
// In base class, used by a lot of tests
public @NotNull Owner createGeorge() {
    var george = createOwner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023");
    var milo = createPet(george.getId(), "Milo", database.findPetTypes().get(0));
    var visit = createVisit(milo.getId(), LocalDate.of(2023, 10, 2));
    return george;
}

// In some actual test
@Test
void testInitUpdateOwnerForm() throws Exception {
    var george = createGeorge();

    var httpResponse = get("http://localhost:" + port + "/owners/" + george.getId() + "/edit");

    assertEquals(200, httpResponse.statusCode());
    assertTrue(httpResponse.body().toString().contains("<input class=\"form-control\" type=\"text\" id=\"firstName\" name=\"firstName\" value=\"George\" />"));
    [...]
}
```
So all in all, there was some short amount of time where I misused the example sql files to setup test data.
That didn't then work with the in-memory implementation of the database/repository interfaces and I would have needed
to convert it to create the test data programmatically. That's normally what I prefer, because it's reusable then and
doesn't need to get duplicated per database.

Once again, I miss Kotlin's features like default values, named parameters and data classes - with Java, I needed
to have some ugly factory methods and I am not able to easily override parts of the data for some specific tests.
I also won't go down the rabbit hole of creating a lot of builder pattern boilperplate for that, it's too much noise for
what is aimed to achieve.

## Closing words

[Here's the commit](https://github.com/hannomalie/petclinic-sandbox/commit/398e7d4df958cd73f855f8bfeb37d5ee842d4441) 
containing all the changes connected to this blog post.

I __underestimated__ the __effort__ and knowledge that is needed to replace JPA or similar from a project like petclinic.
I created unnecessary effort by not focusing on the best way to approach the problem, or generally the best way of doing
anything in life: By doing tiniest possbile steps, get to a safepoint and work with the gained experience.
I tried to replace exiting entities with records and created code bloat that didn't work out. I was too stubborn
to accept it early and wasted too much time on it.

My biggest insight in this iteration was how much fun it can be to dissect the __flow of data through an application__.
How is data connected, where is it fetched, what are the states that need to be handled? Whenever I thought I understood
a piece of code, I rewrote its data usage to what I would have liked it to look.

Objectively though, __I don't think I made much - if anything - better in this project with this iteration__.
Just a bit different. And I also have the feeling I just added some errors or made some mistakes here and there.
I personally still prefer explicitness and the close-to-the-core-language aspect of a tool like JDBI, but I can also
definetely see the value of frameworks like JPA, Spring data and especially Hibernate. I wonder whether using Hibernate
without joined entities is the best way to do persistence, because it would give you the simplicity of pure SQL approaches
and simple mappers, but still prevents you from having to deal with the ugly bits of entities and any performance concerns.
And it gives you Hibernate Query language <3.

Up to the next big change in the petclinic. Maybe it will be switching the template engine - I can't say that
I am enjoying Thymeleaf much :)

