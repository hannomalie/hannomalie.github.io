title=Dissecting Spring petclinic (Part 3): From Thymeleaf to jte
date=2024-12-06
type=post
tags=architecture,design
status=published
headline=Dissecting Spring petclinic (Part 3)
subheadline=From Thymeleaf to jte
summary=I converted the existing templates from Thymeleaf to jte
image=images/spring_logo.svg
~~~~~~
This is the third entry in a series of blog posts about Spring.
Make sure to read [the introduction and my other entries](https://hannomalie.github.io/posts/dissecting-spring-petclinic-intro.html) before this one.

This time, I replaced [Thymeleaf](https://www.thymeleaf.org/) with [jte](https://jte.gg/).

## TLDR
- Thymeleaf's api surface is gigantic because of its large feature set. That surface has a significant cost - which is not worth it
in most projects I worked in.
- Thymeleaf's Spring integration is hard to understand and explore. Convenience is favoured over clarity and ease of understanding here.
- Thymeleaf supports and facilitates a lot of arcane usecases that are less relevant nowadays, yet one pays the cost
  with every line of code in the project that touches templating.
- There are many less noisy templating languages with better tooling I would recommend over Thymeleaf that don't share
  the above mentioned downsides.
- The conversion was a lot of effort, more than expected. Allthough I made a bunch of mistakes, it was straight forward work.
- Explicitly assembling the responses in controllers, rendering the templates and passing the whole context without
  any implicit stuff just makes me super happy personally. It's easy to explore and reason about. I assume that there are a lot
  of people alike me who would prefer such an implentation.
- I didn't find a way to use two templating systems at the same time for piecewise, gradual migration without duplicating
  some fragments.
- JTE's seamless interop with Java code makes using static functions a no-brainer, ideal for translations and other
simple conversions.

## Road to ResponseEntity
When the signature of a method in a Spring _@Controller_ annotated class returns a string, it's interpreted as the identifier
of a view file. Depending on how the resolver is configured, it's searched for on the classpath in some configured
folder like _templates_ and with a suffix like _.html_ and then rendered and returned as http resopnse body.
This could easily be replaced by changing the method so that it returns `ResponseEntity<String>`, which would make the
thing explicit, even though that is rarely done under normal circumstances, because usually nobody wants that and instead
enjoys the convenience of auto configuration and concise code.

But we're not normal today - when we want to replace the templating, we want to do it piece by piece. First, because
I don't have much continuous time, but a lot of tiny fragments and want to keep the project in a working state. Second,
because that's what you are normally forced to do in a realworld project, because there's rearely enough continuous time
for a big bang conversion.

## First failure: dependency issues
This is what needs to be done to have Thymeleaf templates resolved as described above

```java
private static TemplateEngine templateEngine = new TemplateEngine();
private static ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
public static String renderView(String viewName, Map<String, Object> variables) {
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setPrefix("templates/");
    templateResolver.setSuffix(".html");
    templateEngine.setTemplateResolver(templateResolver);
    Context context = new Context();
    context.setVariables(variables);
    return templateEngine.process(viewName, context);
}

[...]
    // In the controller method
    return new ResponseEntity<>(renderView(VIEWS_PETS_CREATE_OR_UPDATE_FORM, model), headers, HttpStatus.OK);
[...]
```

When I did that, every execution failed because of `java.lang.ClassNotFoundException: ognl.ClassResolver`.
This is because the ognl dependency is used by some code in Thymeleaf, but excluded so that [Springs expression language
is used instead](https://github.com/thymeleaf/thymeleaf-spring/issues/203). So I added it again in the dependencies
section in gradle. But I got another classpath issue: `java.lang.ClassNotFoundException: ognl.AbstractMemberAccess`.
That's because a [class was moved](https://github.com/orphan-oss/ognl?tab=readme-ov-file#faq) and one has to use a very
specific, older version of ognl.

Can I say something? This sucks. I know how to resolve those issues, but it just still sucks.
This stuff is really too complicated.


## Second failure: configuration issues

When finally done with that, I encountered yet another issue, which is more or less 
[this one](https://stackoverflow.com/questions/67145533/relative-path-for-an-image-in-thymeleaf) and had something to do
with the (yet again invisible) configuration that is used in the project to resolve some resource urls in Thymeleaf
templating engine. I added explicit configuration for static resources and added the _~_ prefix so that server relative
urls are used, but I have the slight feeling I broke something.
Then of course I had to replace some SpringEL statements in the templates by ognl compatible ones - for example
I had to replace safe call operations with null checking ternery operators.

Phew. And then the endboss. There are expressions like _th:with="valid=${!#fields.hasErrors(name)}"_ in the templates.
The template engine told me that something in there is null, which I concluded must be the fields object. Which
makes sense, as I don't provide that in any way by myself.
Initially I assumed those are just some static methods, so how complicated can it be. But when you find the actual
implementation of the _Fields_ class, you realize it takes a parameter that is some _IExpressionContext_. Must
be possible to instantiate one, does it? Well. There are like 10 implementations of that, some
are abstract and none is simple. I start to get the feeling, that absolutely nothing in this project is simple.
Finally I gave up and wanted to resort back using the instance that was provided by the former context which I
intended to replace. But I simply don't know where to search.
[Here](https://stackoverflow.com/questions/50456942/is-fields-object-from-thymeleaf-or-spring) is someone else
desperately asking the question where the actual instance of that fields-thingy is coming from, but he doesn't
get an answer. It's an absolute pity that there seems to be no effing clue anywhere on the internet how that
thing is created. It drives me nuts. It also drives me nuts, that every effing documentation, tutorial or article
on Thymeleaf always uses Spring - like [this one](https://www.thymeleaf.org/doc/tutorials/2.1/thymeleafspring.html#field-errors),
where it's also not explained how that stuff works.

At this point, I will just tap the sign: That stuff is not explorable. And that's bad.

## Replacing the magic fields object

I now came to conclusion that the only way moving forward is to replace the built-in solution for the errors 
by a hand-written fields obejct. Therefore, I created my own _Fields_ implementation and put it as _fields_ into
the model and removed the hashtag from the templates. In order to implement the _hasErrors_ method correctly, I had
to make the model binding result available in each instance of the fields object. So it needs to be a new object
for each request, like that:

```java
class CustomFields {
    private final Map<String, Object> variables;
    private final BindingResult result;

    public CustomFields(Map<String, Object> variables, BindingResult result) {
        this.variables = variables;
        this.result = result;
    }

    public boolean hasErrors(String fieldName) {
        if(result == null) return false;

        return result.hasFieldErrors(fieldName);
    }
}
```
While the actual rendering happens like that:

```java
public static String renderView(String viewName, Map<String, Object> variables, BindingResult result) {
    Context context = new Context();
    context.setVariable("fields", new CustomFields(variables, result));
    context.setVariables(variables);
    return templateEngine.process(viewName, context);
}
```

Tests are green again, but I can't get rid of the feeling that I added some errors in here. I am not sure how the
actual error messages are peeled out of the fields object, because right now, I didn't have anything implemented in
the custom object.

This is the point in time where I realized I made a mistake, by not adding __approval tests__ before changing the templating.
Those tests should have captured the exact content of the responses and compared them character by character with
the app's response. Let's find out how big of an issue this is.

At least some tests got rightfully red. The input element templates now got expanded in a wrong way. Instead
of textfields with name and value, the name now contained the actual value, while the value attribute
was omitted. Like `<input type="text" name="Franklin">`. Yeahh.... how to put it politely. There is somehow no effing
way to find out how that stuff works and what I have to do to make it do what I want. I have the feeling the problem
as well as the solution to the problem is not that complicated, yet the framework manouvered me in an unsolvable,
intransparent situation of defeat and despair not even the internet can help me with. You know what? I will transform
`<input th:case="'date'" class="form-control" type="date" th:field="*{__${name}__}"/>`
into
`<input th:case="'date'" class="form-control" type="date" th:name="${name}" th:value="*{__${name}__}"/>`
and just call it a day. No idea what I broke by that, but I need to move on.

So here we are - all templates explicitly rendered, all responses instantiated explicitly, all data explicitly
passed into rendering. Next step:

## Converting Thymeleaf to JTE

Oh god where to start that. First, I added a _.jteroot_ file to the templates root, so that the IDE is able to 
reason about the templates and supports me with autocompletion and instant feedback on errors. Then, I just went file
by file through all the templates and did the following:

1. rename from .html to .jte
2. search for any variable usages in the template and declare them as paramters in jte syntax
3. replace all Thymeleaf constructs by jte constructs

Sounds simple, but took me round about a few couples of hours. Of course Thymeleaf has some features that jte doesn't, like
inline fragment definitions. For the single one that was in the project, I just extracted a file.

### Classloaders!?

Even though all tests where finally green, running the application (main method) threw. 
The owner class cannot be casted to the owner class, because it's loaded in a different classloader.
Oh how I hate it when there is so much difference between tests and the real application execution.
This is a problem that you get with a technology like jte, because it compiles templates to actual java code, which
is then blazingly fast, but it also needs to for example load classes via a classloader, just as other java code.
Easily solved by passing a classloader the engine should use:
`static gg.jte.TemplateEngine templateEngine = gg.jte.TemplateEngine.create(codeResolver, Paths.get("jte-classes"), ContentType.Html, Templating.class.getClassLoader());`
A bit unexpected nonetheless and certainly another _bit of stuff you need to know_ - keep in mind
that comparably simple templating engines like mustache don't suffer from that problem. Maybe
I should have chosen that one, like I planned initially :)

### Mistakes were made

In the conversion process, I made some mistakes and introduced some bugs.

1. When converting Thymeleaf [relative urls](https://www.thymeleaf.org/doc/articles/standardurlsyntax.html),
I messed up the parent context, that now needs to be provided explicitly. I see the value in having some fancy mechanism
to resolve relative urls for complex usecases, but to be quite honest, I prefer simplicity and just write the urls as they are.
Dead simple. Also, nobody is deploying multiple apps in one container anymore, so why bother supporting those usecases.
One can clearly see Java web application roots in supporting those usecases.
2. Explicit template rendering doesn't use translations out of the box. I added a simple MessageBundle resolve
mechanism, dead simple and easily usable from jte directly as Java code.
But I skipped replacing all the strings but the _welcome_ string - don't want to invest more time fiddling around, I just
wanted to show how it works with an alternate solution and I think I achieved that.
3. I broke the pagination. Must be a very tiny issue with the way I use the alreday existing pagination objects,
but I am running out of time. I am also not sure whether there were any tests for that before, or if I deleted them 
and didn't properly replace them.
4. In the owner details page, I converted the template code to iterate a list of owners, not a list of pets of an owner.
Big fail, one test covered it but it took me quite a while to actually find the issue.
5. Since I wanted to also test a piece-wise, gradual migration you would need in the real world by using two templating systems
at the same time, I have to say: It's not possible without temporarily duplicating some templates. I don't think it's
too comfy of a job when people need to change the templates while the migration is done...

What I regret the most is, that I didn't write down all important click paths as browser based tests before. I though about
it and also wrote it somewhere in the series before if I remember correctly. But for some reason I fell into the trap again
that I won't necessarily need them. Yes they are necessary. And you need them. You need high quality tests with a lot
of coverage that are decoupled as much as possible from the structure of the application.

## Evaluation

Thymeleaf is much more complicated than JTE. Just take a look at the [API surface of JTE](https://jte.gg/syntax/) comapred
to [the one of Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html).
There is simply much more stuff in it, much more things to explain.
Every single bit of funtionality a tool offers needs to be known at some time, because it will be used in the project. 
This is what I believe, it's based on my experience.
I also believe that the success of a project depends on the ability of us developers to leave stuff out
and reject requirements that don't brint big value to the table - using Thymeleaf basically pulls in the possibility
to do too many things that you don't want to do. But now you have that complex beast in your project - which
is either waste or, even worse, you are now tempted to solve all the problems Themleaf provides you solutions for.

For example

1. __Allmighty syntax:__ Thymeleaf chose to be more complicated syntax-wise in order to support [un-evaluated templates that can still be
used, for example as a preview](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#inlining-vs-natural-templates).
When do people need such a thing? How much effort is it to just offer a simple set of dummy data and run the true application, locally?
You need it one or the other way around, because everyone needs that all the time, for testing, simulating, developing,
prototyping. I can' see static templates as a big advantage, yet you pay the price for it in every line of your template.
2. __All the features:__ Thymeleaf is not only templating, but also provides functionality like
[externalization](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#using-thtext-and-externalizing-text).
The way this feature works in Thymeleaf is completely invisible and unexplorable. Here we go again, convenience
over explorability and ease of understanding. You need to read the (long!) documentation in order to know what
happens, how it works and how it can be changed.
Of course a lot of projects also don't use properties files for translations, so probably you need a new implementation 
of the extension points additionally. Take a look at what [JTE recommends us doing](https://jte.gg/localization/).
And then you'll just have some regular function calls to a regular class in your template, you can resolve it in the IDE
by ctrl-clicking it and it's dead simple.
3. __Fragments:__ Those are really the flesh of a templating engine. Thymeleaf again has an astonishingly big documentation
that I again had to consult, because I wasn't able to make sense of some of the arcane usages and syntax twists.
[Here's](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#template-layout) the documentation that is...what,
20 to 25 times as big as [JTE's documentation](https://jte.gg/syntax/#template-calls) of fragments? See the first paragraph:
Every bit of stuff here is a long term cost. What is the point of such an enormous amount of stuff, when it's
really about including a piece of stuff in some other piece of stuff. How complicated can features be made?! JTE's
approach is so simple, effective and satisfying in comparison.
4. __Expressions:__ It doesn't stop. Take a look at the [custom language](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#standard-expression-syntax)
you can use in Thymeleaf to navigate your objects. And that's without it getting replaced by yet another custom
expression language when you use Thymeleaf in Spring. We're doing Java, why not just stick to Java. In JTE you get complete
IDE assistance for it, it's a dream. Or when you want a bit more convenience, maybe go straight to kotlin with this
[JTE extension](https://jte.gg/kotlin/). And while we're on it, we can move the whole project over to Kotlin and have
the nice ergonomics everywhere, yes? :) Back to topic. I will just tap the sign: Too many features, too much
functionality, too many ways to do things, too much information to maintain, it blows up the project.

All in all, I am most happy about the implicitness I made disappear: No hidden response creation, no hidden
templating magic, no hidden context generation, configuration and passing. No hidden translation mechanism you cannot find.
Not a ton of implicit functionality you have to read up in pages and pages of external documentation. 
No hidden content negotiation (allthough I definitly understand when people hate what I did here). I just find most of
the stuff - if not all - now much simpler and easier to understand than before. I am aware that I removed most of
the things that people consider the benefits of Spring. Well, I don't think most of the stuff is a benefit in sum.

Finally, here's at least one inlined comparison of some templating. Before:

```html
<html xmlns:th="https://www.thymeleaf.org"
  th:replace="~{fragments/layout :: layout (~{::body},'owners')}">

<body>

  <h2>Owner</h2>
  <form th:object="${owner}" class="form-horizontal" id="add-owner-form" method="post">
    <div class="form-group has-feedback">
      <input
        th:replace="~{fragments/inputField :: input ('First Name', 'firstName', 'text')}" />
      <input
        th:replace="~{fragments/inputField :: input ('Last Name', 'lastName', 'text')}" />
      <input
        th:replace="~{fragments/inputField :: input ('Address', 'address', 'text')}" />
      <input
        th:replace="~{fragments/inputField :: input ('City', 'city', 'text')}" />
      <input
        th:replace="~{fragments/inputField :: input ('Telephone', 'telephone', 'text')}" />
    </div>
    <div class="form-group">
      <div class="col-sm-offset-2 col-sm-10">
        <button
          th:with="text=${owner['new']} ? 'Add Owner' : 'Update Owner'"
          class="btn btn-primary" type="submit" th:text="${text}">Add
          Owner</button>
      </div>
    </div>
  </form>
</body>
</html>
```

and after conversion:

```jte
@import org.springframework.samples.petclinic.owner.Owner
@import org.springframework.samples.petclinic.system.Templating.CustomFields
@param CustomFields fields

@param Owner owner

@template.fragments.layout(content = @`
    <h2>Owner</h2>
    <form class="form-horizontal" id="add-owner-form" method="post">
        <div class="form-group has-feedback">
            @template.fragments.inputField(fields, "First Name", "firstName",owner.getFirstName(),  "text")
            @template.fragments.inputField(fields, "Last Name", "lastName",owner.getLastName(),  "text")
            @template.fragments.inputField(fields, "Address", "address", owner.getAddress(), "text")
            @template.fragments.inputField(fields, "City", "city", owner.getCity(), "text")
            @template.fragments.inputField(fields, "Telephone", "telephone", owner.getTelephone(), "text")
        </div>
        <div class="form-group">
            <div class="col-sm-offset-2 col-sm-10">
                <button class="btn btn-primary" type="submit">${owner.isNew() ? "Add " : "Update "}Owner</button>
            </div>
        </div>
    </form>
`, menu = "owners")
```
You can find all the changes done within the scope of this post
[here](https://github.com/hannomalie/petclinic-sandbox/pull/1/files)

As always, I am working towards the greater goal - I have some confidence that the project's remaining entirety can get replaced now too.
Stay tuned for the next entry in the series.
