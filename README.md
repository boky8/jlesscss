jlesscss
========

Less CSS compiler for Java, based on official lesscss.js


Heavily inspired by:
* http://mojo.codehaus.org/lesscss-maven-plugin/
* https://github.com/ultraq/lesscss-filter
* https://github.com/marceloverdijk/lesscss-java


Why did I do it? Simply because most Java compilers rely on the outdated version of Less and it's difficult
to keep up with the latest less developments.

This method allows you to use the latest version of less by simply dropping the `less-rhino.js` file into the
appropriate directory.

What's wrong with the [official LESS compiler](https://github.com/marceloverdijk/lesscss-java)? Well, two things:
* it reads files from disk and does not use HTTP servlet API (hence, your *.less files must be static files and
  cannot be dynamically generated)
* always creates temporary files when compiling


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

Example usage:

```xml
		<plugin>
			 <groupId>org.lesscss</groupId>
			 <artifactId>lesscss-maven-plugin</artifactId>
			 <configuration>
				  <sourceDirectory>${project.basedir}/src/main/webapp/_common/css</sourceDirectory>
				  <outputDirectory>${project.build.directory}/${project.build.finalName}/_common/css</outputDirectory>
				  <compress>true</compress>
				  <includes>
						<include>common.less</include>
				  </includes>
			 </configuration>
			 <executions>
				  <execution>
						<goals>
							 <goal>compile</goal>
						</goals>
				  </execution>
			 </executions>
		</plugin>
```



Requirements
============

* Java 7
* A Servlet 3.0 compliant servlet container

