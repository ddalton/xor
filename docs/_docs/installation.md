---
layout: docs
title: Installation
permalink: /docs/installation/
---

Getting XOR installed and ready-to-go should only take a few minutes.
If it ever becomes a pain, please [file an issue]({{ site.repository }}/issues/new)
(or submit a pull request) describing the issue you
encountered and how we might make the process easier.

### Requirements

XOR is build using Maven and can be accessed using the following dependency.

## Maven dependency

XOR is build using Maven and can be accessed using the following dependency:

```sh
<dependency>
    <groupId>tools.xor</groupId>
    <artifactId>xor</artifactId>
    <version>1.7.25</version>
</dependency>
```

## Code entry point

If you are using Spring then you can do the following to quickly have XOR integrated into your code.
In a Spring boot application add the following piece of code to the class annotated with `@SpringBootApplication`


```sh
@Bean
public AggregateManager aggregateManager() {
    AggregateManager am = new AggregateManager();
    am.setTypeMapper(typeMapper());
    am.setDasFactory(jpadas());

    return am;
}

@Bean
    public DASFactory jpadas() {
    DASFactory dasFactory = new SpringDASFactory("jpadas", new ArrayList<>());
    return dasFactory;
}

// The below is useful if you would like to interact using JSONObject
@Bean
public TypeMapper typeMapper() {
    MutableJsonTypeMapper tm = new MutableJsonTypeMapper();
    tm.setDomainPackagePath("test");
    return tm;
}
```

Or if using Spring config files, you can find some examples in the test section of the project [here](https://github.com/ddalton/xor/tree/master/src/test/resources).
