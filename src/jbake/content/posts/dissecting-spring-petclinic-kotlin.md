title=Dissecting Spring petclinic (Part 6 Bonus): Swap Java for Kotlin
date=2024-12-31
type=post
tags=architecture,code
status=published
headline=Dissecting Spring petclinic (Part 6 Bonus)
subheadline=Swap Java for Kotlin
summary=I converted all the Java code to Kotlin and did some comparison.
image=images/Kotlin_Icon.png
~~~~~~
This is the sixth entry in a series of blog posts about Spring.
Make sure to read [the introduction and my other entries](https://hannomalie.github.io/posts/dissecting-spring-petclinic-intro.html) before this one.
However, this is somewhwat of a __bonus entry__. It's not really
connected to the original goal, which is anyway already achieved.
Nor has it anything to do with Spring. So I will not continue working on this code branch
if I ever make another entry in the series, but will use the latest Java state.

This time, I converted the project from Java to Kotlin.
[This is the merge request](https://github.com/hannomalie/petclinic-sandbox/pull/2/files)
containing all the changes.

## TLDR
- Reduction in lines of code of roughly 13%
- The difference in this project are not really big, Java does quite okay here
- Kotlin has a lot of quality of life features that enhance the code in close to every line
  of the project
- Initial conversion took close to no time, making it more idiomatic took ~ 2-3 hours and was a lot of fun
- No issues worth mentioning
- Biggest difference I found was in nullability handling throughout the whole codebase
- Compilation times are roughly the same, project handling the IDE as well

## Gradle Kotlin DSL
The build file in the project was written in Groovy, something that I don't like much anymore nowadays.
But explaining why would be too much for this post.
Converting it to Kotlin took a few minutes only, the build file is rather simple. Changing some infix
function calls to normal ones, change some single quotes to double quotes and done. I don't know
why I write this here at all.

## Project auto conversion
In case you don't know yet, there is a auto-conversion from Java to Kotlin in IntelliJ. Can be done per file.
Applying it to the project took and 15 minutes. I had to solve some compilation errors afterwards, that were
all about some nullability issues, where it was a bit hard to decide what to do.
The Kotlin code out of the box was good, sometimes not too ideomatic. So I spend a couple of hours going through all the files
and changed everything to how I would have written it myself.

## Comparison

First of all, there is not much code in this project. And the few bits we have is quite simple. 
In this project, all in all, __I don't see the big difference between the two languages__.
Kotlin vastly outshines Java in a hand full of cases, yes. It is better, without a doubt.
But it's also not too dramatic. I could perfectly live with Java in this case and I am a bit surprised by that myself,
because as you might know, I love Kotlin.

Let's take a look at some situations that showed some meaningful differences.

### First class properties

Java records were kind of a disappointment for me on the petclinic journey, you can read about that in an earlier
post. First class language support for a property abstraction is so damn helpful for writing readable
and concise code and Kotlin just nailed it. If you can't use records, then converting POJO to data classes
or even regular classes with just properties reduces that code by 90%. 90% boilerplate.

The dumbest conversion of the Person class is getting

```kotlin
open class Person : BaseEntity() {
	@get:NotBlank var firstName: String? = null
	@get:NotBlank var lastName: String? = null
}
```

instead of
```java
public class Person extends BaseEntity {
	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
```

And all the other model classes are not any better. For the Owner class it is 47 lines of code against 4.
That's close to __12 times the code__. Don't tell me it's dumb code that doesn't matter. It's code, it has to be
maintained, needs to be read, understood and changed over time.

Properties can also be used in interfaces - Java people passionately hate that feature, because with
getters and setters it feels strange and is super unergonmic. In a scenario where data might be partial,
because it comes in from a form and needs to be validated first, one could even come up with a solution like this:

```kotlin
interface Person {
    val firstName: String?
    val lastName: String?
}
data class FormPerson(override var firstName: String?, override var lastName: String?): Person
data class ValidatedPerson(override var firstName: String, override var lastName: String): Person
```
And suddenly with six lines of code you have a nice abstraction that your validator can use like
`fun FormPerson.validate(): Either<ValidatedPerson, Violations>`, and which ensures that all your
code can rely on data being already successfully validated, because it's ensured by the typesystem from now on.

### Nullability

Sometimes I think this is Kotlin's biggest advantage over not only Java, but so many other languages. The design
and support for nullability in Kotlin is so nice, it's so smoothly integrated into the language instead of bolted
on somehow. Nullability is omnipresent and having good language support helps in every second line.

Take a look at this example

```kotlin
fun Context.getPageParamOrDefault(): Int {
    val page = queryParam("page") ?: "1"
    return page.toInt()
}
// or even

fun Context.getPageParamOrDefault() = (queryParam("page") ?: "1").toInt()
```

The elvis operator saves us from 
```java
public static int getPageParamOrDefault(Context ctx) {
    var page = ctx.queryParam("page");
    if(page == null) {
        page = "1";
    }
    return Integer.parseInt(page);
}
```
And even leveraging the latest Java syntax it would be
```java
public static int getPageParamOrDefault(Context ctx) {
    var page = ctx.queryParam("page");
    return Integer.parseInt(
        switch(page) {
            case null -> "1";
            default -> page;
        }
    );
}
```

And I bet my Java colleagues will have to talk to me, as they had to when I started using lambdas in Java back then.

Or in the PetTypeFormatter class we can write

```kotlin
override fun parse(text: String, locale: Locale): PetType = database.findPetTypes().firstOrNull {
    it.name == text
} ?: throw ParseException("type not found: $text", 0)
```

instead of

```java
@Override
public PetType parse(String text, Locale locale) throws ParseException {
    Collection<PetType> findPetTypes = this.database.findPetTypes();
    for (PetType type : findPetTypes) {
            if (type.getName().equals(text)) {
                    return type;
            }
    }
    throw new ParseException("type not found: " + text, 0);
}
```

which is just more straight forward, more concise and causes less cognitive overhead cause we don't need
to follow three to five statements containing assignments, iterations, conditions and returns.

### Primary constructors

This is another feature that is so unbelievably nice in Kotlin. And so well integrated, it just works for
all the classes. In Java, only records have them and people already start abusing records just to get rid
of the constructor boilerplate every single normal constructor in Java introduces.

```kotlin
class BaseAppTest(
    ds: HikariDataSource = getHikariDataSource("jdbc:h2:mem:testdb", "sa", "password"),
    databaseType: DatabaseType = DatabaseType.H2,
    port: Int = 0
) {
    val database = Database(ds, databaseType)
    val app = startApplication(port, database)
    val port = app.port()

    constructor(container: MySQLContainer<*>): this(getHikariDataSource(container.jdbcUrl, container.username, container.password), DatabaseType.MySQL)
    constructor(container: PostgreSQLContainer<*>): this(getHikariDataSource(container.jdbcUrl, container.username, container.password), DatabaseType.Postgres)

[...]

}
```

whereas in Java I quickly [created a mess](https://github.com/hannomalie/petclinic-sandbox/blob/replace-spring/src/test/java/org/springframework/samples/petclinic/BaseSpringBootTest.java#L32-L50) because I started with a parameterless constructor,
then needed a configurable one, than an overloaded one.
But one or the other way around, the marked lines in the commit would be the boilerplate I need.
Not to speak about overloads for passing a port.
Note how in Kotlin, I can just assign properties like database in the class body directly, as well as instantiating
the app, thanks to the primary constructor. The port is also part of that and can simply be optionally
passed in without any overload crazyness. Default parameters are a big win here.

### Extensions

This

```kotlin
ctx.setResponse(
    ResponseEntity(renderView("pets/createOrUpdatePetForm", model, result), headers, 200)
)
```

is more natrual than this:

```java
setResponse(
    ctx, 
    new ResponseEntity<>(renderView("owners/ownerDetails", modelMap, result), htmlHeaders, 200)
);
```

And this

`val owner = ctx.getOwnerFromForm()`

reads nicer as this

`var owner = getOwnerFromForm(ctx)`

Most of the functions that operate on the request context, can be written in a nicer way with extensions.
Allthough in this project, I haven't used them that dramatically often.

### Scope functions

```kotlin
fun getHikariDataSource(
    jdbcUrl: String,
    username: String,
    password: String
) = HikariDataSource(HikariConfig().apply {
    this.jdbcUrl = jdbcUrl
    this.username = username
    this.password = password
    addDataSourceProperty("cachePrepStmts", "true")
    addDataSourceProperty("prepStmtCacheSize", "250")
    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
})
```

is more readable and lass cognitive load than this:

```kotlin
public static @NotNull HikariDataSource getHikariDataSource(String jdbcUrl, String username, String password) {
    HikariConfig hikariConfig = new HikariConfig();
    HikariDataSource ds;
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);
    hikariConfig.addDataSourceProperty( "cachePrepStmts" , "true" );
    hikariConfig.addDataSourceProperty( "prepStmtCacheSize" , "250" );
    hikariConfig.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
    ds = new HikariDataSource( hikariConfig );
    return ds;
}
```

I don't want to say it's the biggest deal in history. But it's an example for so many other spots in the
project where I was able to make the code less noisy and removed a lot of temporary variables thanks
to scoping functions. Which are really a few standard higher order functions, which is the great deal about
them - once you roughly understand how higher order functions and receivers work, you understand them and
they will be natural for you and help you.

### Files

One of the from my pov more important things is organization of code in files. Java's strict approach
with a file per public class is outdated. Might have made sense a long time ago, but at least today something
else is more important: __cohesion__. Things that belong together should reside in the same place, for example
one file.

For the _Pet_ type, I moved _Pet, PetType, PetTypeFormatter_ and _PetValidator_ into one file. Ended up with _Pet.kt_ and
_PetController.kt_. Compared to five files before. Same vor the _Vet_ type. Two files instead of four.
All in all, it's a great reduction in file count - less indirections, less distraction when you want to understand how things
work. Having a lot of files that all contain only two, three lines of code doesn't make sense. It makes
the file tree basically unnavigateable.

### Collections apis

The standard library and collections API is Kotlin's underestimated superpower. Complicated operations
are boiled down to a single line of code. Those transformations on collections of different kinds are used
too often in code to always do something like this in java:

`model.put("pet", pets.stream().filter({ it: Pet -> it.id == petId }).findFirst().get())`

when instead we can do this

`model["pet"] = pets.first { it.id == petId }`

17 times in this small project did I use `.stream()` just to apply some filtering on a collection.

Also note, how Kotlin operator functions let you put elements into a map with the assign syntax.
This is another example of a tiny thing that is better readable and is omnipresent. 60 occurrences
in the project, without me taking a too close look at the spots now.

Another nice example is that I was able to convert this code:

```java
fun getSpecialties(): List<Specialty> {
    val sortedSpecs: List<Specialty> = ArrayList(specialtiesInternal)
    PropertyComparator.sort(sortedSpecs, MutableSortDefinition("name", true, true))
    return Collections.unmodifiableList(sortedSpecs)
}
```

to this much nicer Kotlin code:

```kotlin
fun getSpecialties(): List<Specialty> = specialties.sortedBy { it.name?.lowercase() }
```

Kotlin has read-only collection types as first class citizen and sort functions of every kind you can imagine
in the std lib, that works with standard lambda syntax. Then using expression body syntax and there
you have your less noisy code that encodes concisely what is done without any distractions.

## Build times

I was able to apply the latest version of Kotlin, which includes the brand new compiler. That one is
much faster then the old one. Since increase of compilation times is for years one of the arguments used
against Kotlin adoption (from a Java shop point of view), I gave it a shot. __6 seconds__ takes `./gradlew jar` after
before `./gradlew clean` was executed, so it's built from scratch. And __5 seconds__ for the same thing in Java on
the branch before I converted to Kotlin.

Note that this was done completely unsientific - I just ran the commands ten times each.
This only gives us the information that for this project or any project of comparable size, you will have
around one second slower compilation time when doing a compilation from scratch. When you apply
a single change in a file, both Kotlin and Java take __~4 seconds__ for the task. Recompiling after nothing
was changed results in 3 seconds. So I have the strong suspicion that it doesn't make much sense to compare
the compilation times in such a small project at all.

## Conclusion

I opened a [MR](https://github.com/hannomalie/petclinic-sandbox/pull/2/files) that will be kept in draft state containing all the changes.
I only invested a very tiny amount of time after the auto-conversion to make the code a little bit more idiomatic,
and the result is already a reduction of around 400 lines of code. Which is roughly 13% of the overall code.

In this project, the difference between Java and Kotlin is fairly small. This is because we didn't have too many ugly warts
like checked exceptions, capturing lambdas, lambdas at all and so on in the project. When __records__ can't be used,
there's a big difference and __data classes__ can still reduce so much boilerplate. The same goes for usage of __scope functions__
and __Kotlin's collections api__.

For me _personally_, those things make coding a lot more anjoyable. It's easier
to do the "right" thing, instead of often accept that Java is what it is and just do it tha way it wants you to.
Like for example with the `var` keyword. That lacks a corresponding `val`. And can only be used for local variables,
whereas most of the time it's just fine to use it in function signatures as well,
because type inference is exatly what you inteded to use.
Or expression bodies for functions, a feature that is nnnowadays at least planned for Java as well.
Being concise often times enhances readability. It's really uncomfy when
you are used to that luxury and suddenly you cannot do it and have to type empty curly braces even though the 
record constrcutor body is empty, just for the sake of it.

Kotlin is just a nicer language and a nicer overall experience for an ambitious developer. The whole topic
is only difficult when the question is "Why Kotlin?" instead of "Why not Kotlin?". It always ends with the argument
that Java has the bigger talent pool. And that is despite all Java shops I worked for also use Kotlin without telling anybody
and the fact that average Java developers can be productive in Kotlin within two days. So what difference does it
make? I feel it's exactly the same controversy that led me to writing this series at all: "Why don't use Spring?"
instead of "Why use Spring?". I think this series and 
the code comparisons showed a lot of reasons why "biggest talent pool" cannot be the answer all the time.
When the discussion ends here, we'll be forever stuck in the `.stream().filter(it -> it.isBar()).findFirst().get()` hell.
Or doing `if(foo.bar == null) throw new RuntimeException("bar is null")` everywhere in the code.

I will be fair, in this project, __Kotlin doesn't make a big difference. Java does great here.__ If I could only chose
a single thing to take over, than it would be nullability handling. It is used too often to just be "undefined" all the time.