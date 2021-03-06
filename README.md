# JI project

[![](https://jitpack.io/v/ondrej-nemec/javainit.svg)](https://jitpack.io/#ondrej-nemec/javainit)
[![MIT License](http://img.shields.io/badge/license-MIT-green.svg) ](https://github.com/ondrej-nemec/javainit/blob/master/LICENSE)

Project contains some useful libraries, functions and structures

## Contains

* [Common](ji-common) - Often used logic, useful structures and exceptions.
* [File reading/writing](ji-files) - Factories for Buffered Reader/Writer and JSON stream
* [Access control layer](ji-acl) - For checking permissions. Not for sign in/out.
* [Translator](ji-translate) - Package supports multilingual applications.
* [Logging](ji-logging) - Implementation Logger interface from Common.
* [Database](ji-database) - Contains: SQL Query Builder and database migrations.
* [Database tests](ji-testing) - Extends jUnit for testing with database.
* [Server-Client communication](ji-communication) - Provide server/client communication (secured or unsecured) using Java sockets. Implements web server too. 

## Include in your project

JI uses for publication <a href="https://jitpack.io/">JitPack</a>. It allows you to include this project by using Gradle or Maven.

### Include using Gradle

Add this line to repositories section
```gradle
maven { url 'https://jitpack.io' }
```
And this line to dependencies
```gradle
implementation 'com.github.ondrej-nemec:javainit:Tag'
```