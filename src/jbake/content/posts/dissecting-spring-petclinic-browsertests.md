title=Dissecting Spring petclinic (Part 4): Adding browser tests
date=2024-12-24
type=post
tags=architecture,test
status=published
headline=Dissecting Spring petclinic (Part 4)
subheadline=Adding some browser tests
summary=I realized there are no browser based tests, so I added some.
image=images/spring_logo.svg
~~~~~~
This is the fourth entry in a series of blog posts about Spring.
Make sure to read [the introduction and my other entries](https://hannomalie.github.io/posts/dissecting-spring-petclinic-intro.html) before this one.

This time, I just added some browser based tests to the project utilizing 
[Playwright](https://playwright.dev/java/docs/intro).

## TLDR
- Waiting to add browser tests for the most important clickpaths until now was my biggest mistake in this whole endeavour
- I found multiple errors in functionality _(I likely introduced myself)_ by adding them
- Playwright is surprinsingly good, adding some browser based tests was done in no time
- I consider browser based tests as the best bang for the buck, the best thing you can do to your project if you have any meaningful UI

## Why Browser based tests
When I converted the existing tests in the project to blackbox tests in the very first step of this series,
I achieved a pretty high fidelity for those tests: In fact, accessing a server route via HTTP and passing
in the relevant query or form data is as close to the application as you could get.

But.

It doesn't tell the whole story of the user. That's because the user does not manually call http rountes through some cli
and passes in the encoded data for you. He uses the browser - thankfully a hypermedia client, which means
all the elements in a web page have defined semantics and we know for example: When we hand out a landing page with
a navigation, the user will use the links of the anchors in the navigation to navigate the page.
So actually we do not only need to test an http request for the landing page url and one for the items of the navigation
bar entries, but we also need to ensure that the anchors of those elements are actually correct.
Let's talk about _"correct"_ then.

Of course we could do a dumb string comparison like `assertThat(a.href).isEqualTo("/owners")` or something.
That only would couple the test tightly to the actual value of the href attribute, so that it cannot be changed
anymore without the test failing - regardless of the validity of the new value or not. Imagine we want to change
the route from _/owners_ to _list-owners_ for example. So by definition, it would break _refactoring_.

Without being the biggest deal in the example case, people will claim. But on the other hand, our intention
is probably something very different: We want to verify the behaviour of the system: That the user navigates
to the owners list when he clicks on a certain navigation item. Browser tests let us do exactly that, by using the
same system boundaries the user would be exposed to as well: the browser as a client.

This way, we get maximum __fidelity__, while keeping coupling to the application structure as small as possible.
Great recipe for high quality tests, that keep refactorings possible.

## Playwright

Setting up a test with __Playwright__ is a breeze. The above linked documentation shows you all you need.
It requires you to add exactly one dependecy to the test scope of your project. 


> **_Small caveat:_** 
Note that the build then reported
some missing system dependencies and how to install them via apt. This is something I don't like too much.
System dependencies are always a tiny smell and some risk for users of operating systems without a package manager or
which are not well supported. I guess it was some very essential for my case, my system is quite fresh, so not
big of a deal.

A first test then was written within seconds:

```java
@Test
void ownerIsCreated() {
    page.navigate("http://localhost:" + port + "/owners");
    page.locator("#nav-item-search").click();
    page.locator("#lastName").type("asd");
    page.locator("#search-owner-form-submit").click();

    assertThat(page.content()).contains("wurde nicht gefunden");

    // fill in form values
    page.locator("#search-owner-form > a").click();
    page.locator("#firstName").type("asd");
    page.locator("#lastName").type("def");
    page.locator("#address").type("foo");
    page.locator("#city").type("bar");
    page.locator("#telephone").type("1234567890");

    page.locator("#submit-owner").click();

    assertThat(page.content()).contains("asd def");
}
```
In order to be able to use ids as selectors, I had to add some for the important elements in the page. This can
be seen as test-induced damage, as it would not have been necessary without the tests. In fact, they weren't strictly necessary,
but the selectors I had to use alternatively were too much coupled to the structure of the page to find them acceptable.
Would have worked, would have been bad. So adding ids is some small compromise really not worth much discussion in order
to enable this whole approach.

The test takes less then 2 seconds to run. Multiple tests run around three seconds. 
I see big, big value for very small effort and a tiny runtime cost.

## Selenium

Not really related to this series or this entry, but:
I wanted to give good old Selenium a short try as well, just to have some comparison with Playwrite. Since I
had bad experience because of sever setup complexity with other tools like for example Protractor, I was curious.
The setup rof Selenium is quite comparable to Playwright - a single dependency needs to be added to the test scope. Done.
That's how I love it.

Yet, the Selenium documentation is way behind the one of Playwright.
Walls of text, walls of explanations of stuff nobody cares about. Hard to google, whereas Playwright hits are
immediately relevant and on spot. Just to find the _"official"_ place where the selenium dependency one has to use
is written down, try it for yourself and find it, maybe it's a skill issue on my side.
Also a lot of tutorials try to explain how to use selenium without a
build tool, or to be more precise without that driver manager which downloads all necessary dependencies for you, which
is a pain in the butt to do manually. Those tutorials are pretty much obsolete. Playwright doesn't suffer from those problems
as it seems.

The API of Selenium seems to be a bit simpler. Not five different objects to instantiate, only one driver and let's go.
Not something that I would have expected, because of Selenium's age.

But then, the exact test I wrote in Playwrite doesn't work translted for Selenium out of the box. One of the first elements is seemingly not interactive.
Quickly throwing in a sleep didn't help. Using the driver wait api - which is quite unergonomic - didn't help either.
This is why I stop here and conclude that I will use Playwright more often in future projects, because Selenium
lacks quite a lot on the documentation and api quality side of things. How times have changed, you let me down, old friend!
If it's indeed the problem, Playwright's wait-built-in-API is really much nicer overall.

## Conclusion

I added a few other tests so that most of the user facing functionality is covered. No problems with Playwright,
integration was a dream so far, big kudos to the framework!

Those new tests helped me actually find errors I created in the project throughout all the refactorings.
For example I broke the edit functionality of the owners as well as of the pets. And I also broke the views around
visits for a pet of an owner. That's because earlier there were either just no tests covering that functionality
properly or I deleted them _(don't think so, but not entirely sure right now)_. With that few new lines of test code,
I actually created so much coverage of the application's functionality, that I am quite baffled, even though I
have seen that effect in a lot of other projects before. I am tempted to remove some other existing tests that
now became redundant. Or even make more tests redundant, like tests for invalid input and so on. I can only resist
to scratch that itch because I need to manage my time :)

This was expected to become only a short, spontanious, unimportant stop on the road to the next series entry.
But it became one of the bigger successes and satisfactions. Yess. 
[Here's the commit](https://github.com/hannomalie/petclinic-sandbox/commit/dd71a7e6adfe60a43458c96589fc02591c1d1baa). Until next time!