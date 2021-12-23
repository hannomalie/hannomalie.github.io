title=How to create a blog with JBake and Gradle
date=2021-12-21
type=post
tags=blog,gradle,jbake
status=published
headline=How to create a blog with JBake and Gradle
subheadline=And Git and GitHub Pages
summary=JBake and Gradle are a simple yet powerful duo for everyone who wants to create a static site and automate the workflow around with Git and GitHub Pages.
image=images/jbake_gradle.png
~~~~~~
> **_Disclaimer_**: There are probably ways to create a blog with or without a static site generator that a lot of people will 
see as more beginner friendly and better documented and supported than what I describe here.
For whomever likes to work with the JVM ecosystem and Git, my way might be interesting.

## Template
> **_Assumption_:** You know the basics of html and css and what web servers do.

The most important part of your blog is neither the bake tool nor the build tool but the template.
Now I have said it and I am a developer. So my template can be found on [html5up](https://html5up.net/), just as some other nice ones, 
but of course you can chose an arbitrary one from the internet, as long as it's suitable for a static site.  

## Git
Initialize your git project and use [Java's default .gitignore file](https://github.com/github/gitignore/blob/main/Java.gitignore).
You will need a foder named _docs_ under your root folder, so that GitHub Pages knows where to find your pages.
For now, create an empty index.html file within that folder and check in your first working version.

## GitHub Pages
Create a new Repository in your GitHub account, name it exactly like your username, as also described [here](https://pages.github.com/).
Go to _settings -> Pages_ and chose _branch: master_ and folder: _/docs_ so that your pages are 
goingto be served from this very folder on your master branch.
When you push your new project containing an index.html file, you can already visit it on _https://yourusername.github.io_. 
From now on, you only have to do some _release_, which means you need to update the contents of the docs folder and push it on the master branch.

## Gradle Part 1: JBake
> **_Assumption_:** You have Java installed (at least 8, better is 11 or higher) or you know how to install it.
You can also use JBake standalone as a docker image as described [here](https://github.com/jbake-org/jbake#docker-image) 
and then your only dependency would be Docker.

Initialize gradle within your root folder. You can do so by either installing an arbitrary gradle version on your 
machine and call _gradle init_ on the cmd, or by using any gradle repository as a starting point. 
You could use [mine](https://github.com/hannomalie/hannomalie.github.io) and delete the _docs_ and _src_ folders. 
This way, you don't need to install gradle. Afterwards, you can use the executables to launch gradle commands.

> **_Assumption_:** You use Kotlin as your language for gradle and you use IntelliJ idea 
(community edition is sufficient, it's [free](https://www.jetbrains.com/idea/download/) 
and [open source](https://github.com/JetBrains/intellij-community))

Now you need to add JBake as a dependency to the dependencies of your gradle build script (*not* to the application dependencies). 
You do so by adding

```kotlin
buildscript {
    dependencies {
        classpath("org.jbake:jbake-core:2.6.7")
    }
}
```

simply on the top level. Now you can use JBake's classes from within your build.gradle.kts. Remeber to reload your
Gradle project in IntelliJ after adding the dependency, in order to make auto completion in the script file work:

<img src="../images/buildscript.gif" alt="alt text" width="900"/>

When you write the task definition like in the image above, you only need to create the default JBake source folder 
_src/jbake_ and declare source and destination folders above your task definition like so

```kotlin
val sourceFolder = project.rootDir.resolve("src/jbake")
val destinationFolder = project.buildDir.resolve("jbake")
val docsSubFolder = rootProject.rootDir.resolve("docs") // we need that later
```

Configuring the sourceFolder as an input to the bake task enables caching and usage of 
[Gradle's built-in functionality of doing things continuously](https://blog.gradle.org/introducing-continuous-build).
You can now just do `./gradlew -t bake` and as soon as you save it's baked automatically

<img src="../images/continuous_build.gif" alt="alt text" width="900"/>

I am using a full blown uncached JBake build, which takes not even two seconds to complete and is fast enough for me. 
There are some [caching mechanisms](https://jbake.org/docs/2.6.7/#persistent_content_store) that would probably dramatically 
speed up single document changes, but I heaven't tried that and indeed if I had any more performance requirements, 
I would rather use the JBake executable and the built-in [watch mode](https://jbake.org/docs/2.6.7/#watch_mode) 
that also includes a web server (just after I found out that to be faster of course ;) ).

Last thing, when you want to use markdown, remove the default _HARDWRAPS_ option from the JBake config by
setting _markdown.extensions=AUTOLINKS,FENCED_CODE_BLOCKS,DEFINITIONS_, or you will have a hard time fighting long lines.

## IntelliJ local web server
IntelliJ has a built-in web server, so navigating to the `build/jbake/posts/foo.html` file and clicking open either 
in internal or in default browser will give you a preview of your site immediately. The page can automatically be 
reloaded on changes, but this seems to only work when you a) either edit the html file directly (which is not
practical), or when you b) refocus IntelliJ. So you can have a second window with the browser of your choice 
previewing your page and you need to CTRL-Tab CTRL-Tab in order to have the browser update the page. Odd, but I didn't find a way around that.

## Gradle Part 2: Release workflow
So the simplest workflow would be to just generate the site into _/docs_ for preview and production. 
Then write your content and whenever you think you're done add the content, generate the site, add everything 
with `git add --all`, `git commit` and `git push` and done. You would need to be careful with work in progress commits, 
where you changed the content, but don't want to publish the unfinished compiled site. 
And you need to take care of manually cleaning up the generated content when you
decide _this is my release now_. Releases may be tagged manually so that you can rollback or find out where you 
made a mistake in the past. Would be better to do some more sophisticated things here and Gradle can help.

First, we only generate into _/build/jbake_. Build is a working dir for Gradle and is not checked into version control, 
so we never push a work in progress site. Second, we write a small task that generates into the _/docs_ folder, 
that indeed get checked in. This task can either bake into the build dir and copy the content to docs, 
or it can clear docs and generate into docs. It's close, but not exactly the same. I chose the former.

```kotlin
val compileToDocs by tasks.registering {
    group = "release"
    dependsOn(bake)
    doFirst {
        docsSubFolder.deleteRecursively()
        docsSubFolder.mkdir()
        destinationFolder.copyRecursively(docsSubFolder)
    }
}
```

The [gradle release plugin](https://github.com/researchgate/gradle-release) is the de-facto standard of how you do 
releases with Gradle and is a quite proper tool to abstract over the problem of _doing a release of something_. 
After application, your build contains a `release` task and some hooks. Usually you want to 
treat your codebase as work in progress and only
mark single commits as release of a specific version and immediately go over into wip state again. 
In the JVM world and with Gradle (to be more precise in the Maven standard ...) this is a SNAPSHOT workflow.
So you need to write

```kotlin
release {
    failOnCommitNeeded = false
}
tasks.beforeReleaseBuild {
    dependsOn(compileToDocs)
}
```

to tell Gradle that it needs to compile your site into the docs folder, commit all uncommitted files 
and commit a release version before setting the next snapshot version.
So we only need to `./gradlew release` and afterwards we can push the master branch and released.
