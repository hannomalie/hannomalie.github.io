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
see as more beginner friendly and better documented and supported than what I describe here. For whomever likes to work with the JVM ecosystem and Git, my way might be interesting.

## Template
> **_Assumption_:** You know the basics of html and css and what web servers do.

The most important part of your blog is neither the bake tool nor the build tool but the template. Now I have said it 
and I am a developer. So my template can be found on [html5up](https://html5up.net/), just as some other nice ones, but of course you can
chose an arbitrary one from the internet, as long as it's suitable for a static site.  

## Git
Initialize your git project and use [Java's default .gitignore file](https://github.com/github/gitignore/blob/main/Java.gitignore). You will need a foder named _docs_ under your root 
folder, so that GitHub Pages knows where to find your pages. For now, create an empty index.html file within that folder and check in your first working version.

## GitHub Pages
Create a new Repository in your GitHub account, name it exactly like your username, as also described [here](https://pages.github.com/).
Go to _settings -> Pages_ and chose _branch: master_ and folder: _/docs_ so that your pages are goingto be served from this very folder on your master branch.
When you push your new project containing an index.html file, you can already visit it on _https://yourusername.github.io_. From now on, you only have to do some _release_, which means you need to update 
the contents of the docs folder and push it on the master branch.

## Gradle Part 1
> **_Assumption_:** You have Java installed (at least 8, better is 11 or higher) or you know how to install it.

Initialize gradle within your root folder. You can do so by either installing an arbitrary gradle version on your machine and call _gradle init_ on the cmd, or by using any gradle repository as a starting point. You could use [mine](https://github.com/hannomalie/hannomalie.github.io) 
and delete the _docs_ and _src_ folders. This way, you don't need to install gradle. Afterwards, you can use the executables to launch gradle commands.

> **_Assumption_:** You use Kotlin as your language for gradle and you use IntelliJ idea (community edition is sufficient, it's [free](https://www.jetbrains.com/idea/download/) and [open source](https://github.com/JetBrains/intellij-community))

Now you need to add JBake as a dependency to the dependencies of your gradle build script (*not* to the application dependencies). You do so by adding

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

When you write the task definition like in the image above, you only need to create the default JBake source folder _src/jbake_

and declare source and destination folders above your task definition like so

```kotlin
val sourceFolder = project.rootDir.resolve("src/jbake")
val destinationFolder = project.buildDir.resolve("jbake")
val docsSubFolder = rootProject.rootDir.resolve("docs") // we need that later
```