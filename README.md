# java-toml-parser

A Java parser for TOML 1.0.0.

## Installation

Add to a Maven project with:

```xml
  <repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
  <dependencies>
    <dependency>
      <groupId>com.github.mwilliamson</groupId>
      <artifactId>java-toml-parser</artifactId>
      <version>$VERSION</version>
    </dependency>
  </dependencies>
```

replacing `$VERSION` with the commit hash you want to use.

## Usage

Import `org.zwobble.toml.parser.TomlParser` and read a file using a `Path`:

```java
var table = TomlParser.parseFile(path);
var name = table.get("name");
System.out.println(name);
```

or using an input stream:

```java
var table = TomlParser.parseInputStream(inputStream);
var name = table.get("name");
System.out.println(name);
```

## Questions and Answers

### Why not just use an existing TOML parser?

I wrote this for two reasons:
1) Fun!
2) I wanted to be able to reference the specific locations in the file that
   specific values are read from.

### Why isn't this on Maven Central?

Publishing on Maven Central is a bit of a pain, but if anybody finds this
library useful, let me know and I can see about publishing it. 
