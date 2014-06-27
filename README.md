jlesscss
========

Less CSS compiler for Java, based on official lesscss.js


Heavily inspired by:
* http://mojo.codehaus.org/lesscss-maven-plugin/
* https://github.com/ultraq/lesscss-filter


Why did I do it? Simply because most Java compilers rely on the outdated version of Less and it's difficult
to keep up with the latest less developments.

This method allows you to use the latest version of less by simply dropping the `less.js` file into the
appropriate directory.



Usage of the filter
===================

To use the servlet, simply drop the JAR into your webapp. The Servlet 3.0 should pick up the filter and
attach it to *.less automatically.

Then you only need to to:
```html
   <!-- Notice that file type is text/css -- the filter will compile your stylesheet to CSS automatically -->
   <link rel="stylesheet" type="text/css" href="/css/myfile.less" />
```

Using the maven plugin
======================

As the compilation can take some time (yes, Rhino is slow), there's also an option to use a precompiled version.
For this, please use the maven plugin found here: https://github.com/marceloverdijk/lesscss-maven-plugin





Requirements
============

* Java 7
* A Servlet 3.0 compliant servlet container



Related projects
================


* Worth checking out is also https://github.com/marceloverdijk/lesscss-java
* Official org.less compiler, but the compiler will always create a temporary file and is not suitable for
  our use case of loading files through HTTP Servlet methods.
