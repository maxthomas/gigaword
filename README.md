Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights
reserved. See LICENSE in the project root directory.

Gigaword
========

A small Clojure/Java API for the [Gigaword](https://catalog.ldc.upenn.edu/LDC2011T07) corpus.

## Dependency info
[![Clojars Project](http://clojars.org/gigaword/latest-version.svg)](http://clojars.org/gigaword)

### Leiningen
```lein
[gigaword "2.0.1"]
```

### Maven
```xml
<dependency>
  <groupId>gigaword</groupId>
  <artifactId>gigaword</artifactId>
  <version>2.0.1</version>
</dependency>
```

## API

The main API is exposed in the `gigaword.api` package. The class is
`GigawordDocumentConverter`. These functions produce `GigawordDocument`
objects, either individually or with an `Iterator`.

```java
// Single ctor.
GigawordDocumentConverter gdc = new GigawordDocumentConverter();

// SGML string -> GigawordDocument
String sgmlString = ...
GigawordDocument gigadoc = gdc.fromSGMLString(sgmlString);

// SGML path on disk -> GigawordDocument
String sgmlPath = ...
GigawordDocument gigadoc = gdc.fromPathString(sgmlPath);

// Stream from a .gz file of .sgml docs
String pathToSgmlGZ = ...
Iterator<GigawordDocument> iter = gdc.iterator(pathToSgmlGZ);
```

At a future date, the Clojure functions will be exposed into a proper API.

## License

Copyright (C) 2015 Johns Hopkins University HLTCOE

Distributed under the Eclipse Public License, the same as Clojure.
