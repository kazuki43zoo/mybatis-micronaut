# mybatis-micronaut

[![Build Status](https://travis-ci.org/kazuki43zoo/mybatis-micronaut.svg?branch=master)](https://travis-ci.org/kazuki43zoo/mybatis-micronaut)
[![Coverage Status](https://coveralls.io/repos/github/kazuki43zoo/mybatis-micronaut/badge.svg?branch=master)](https://coveralls.io/github/kazuki43zoo/mybatis-micronaut?branch=master)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


The mybatis-micronaut is the bridge module to use MyBatis easily on Micronaut Framework.

The mybatis-micronaut's standard feature are as follows:

* Support the bean definition for `SqlSessionFactory`
* Support to scan mapper interfaces from specified packages
* Support multiple configuration properties (such as `mybatis.*.mapper-packages`)

If you enable the declarative Spring based transaction management and add the `mybatis-spring` module into your classpath, following optional features can be used.

* Support the bean definition for `SqlSessionTemplate` (Thread-safe transactional the `SqlSession` implementation provided by `mybatis-spring` module)
* Support to export mapper beans to the Micronaur's `ApplicationContext`

> **NOTE : Additional feature candidates**
>
> * Support to use the GraalVM (Will plan to provide `native-image.properties`, `reflect.json` and so on ...)
> * Support to inject mapper bean without the `mybatis-spring` module(= without declarative spring based transaction management). This feature is requested via the [MyBatis's user mailing list](https://groups.google.com/forum/#!topic/mybatis-user/-sW62H_1Quw).
> * etc ...

## How to install

This artifact does not deploy to the public maven repository yet because under developing.
Therefore, you need to install to the your local repository if you want to try this artifact.

```
$ git clone https://github.com/kazuki43zoo/mybatis-micronaut.git
$ cd mybatis-micronaut
$ ./mvnw install
```

## Standard usage

### Build tool settings

Maven:

```xml
<dependencies>
  <dependency>
    <groupId>org.mybatis.micronaut</groupId>
    <artifactId>mybatis-micronaut-config</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```

Gradle:

```groovy
dependencies {
  compile 'org.mybatis.micronaut:mybatis-micronaut-config:1.0.0-SNAPSHOT'
}
```

### Application configuration

You need to configure the datasource and the MyBatis configuration properties as follow:

```yaml
datasources:
  default:
    url: jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: ''
    driverClassName: org.h2.Driver

mybatis:
  default:
    mapper-packages: [] # Indicate to scan mappers from application package
```

### Application code

You need to add the `@org.apache.ibatis.annotations.Mapper` on your mapper interface for scanning mapper interfaces from classpath.

```java
import org.apache.ibatis.annotations.Mapper;
// ...
@Mapper // Add annotation
public interface City {
  // ...
}
```

You can inject a `SqlSessionFactory` bean and use it.

```java
// ...
@Singleton
public class CityService {

  private final SqlSessionFactory sqlSessionFactory;

  public CityService(SqlSessionFactory sqlSessionFactory) { // Inject an SqlSessionFactory bean
    this.sqlSessionFactory = sqlSessionFactory;
  }
  
  public void update(City city) {
    try (SqlSession sqlSession = sqlSessionFactroy.openSession()) { // Retrieve a new SqlSession (begin a new transaction)
      CityMapper cityMapper = sqlSession.getMapper(CityMapper.class);  // Retrieve a mapper object
      cityMapper.update(city); // Call a mapper method
      sqlSession.commit(); // Commit a existing transaction
    }
  }
  
}
```

## Using Spring based transaction management

### Build tool settings

Maven:

```xml
<dependencies>
  <dependency>
    <groupId>org.mybatis.micronaut</groupId>
    <artifactId>mybatis-micronaut-config</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </dependency>
  <!-- Add following artifacts for enabling the Spring based transaction management -->
  <dependency>
    <groupId>io.micronaut</groupId>
    <artifactId>micronaut-spring</artifactId>
    <version>1.1.2</version>
  </dependency>
  <dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.1.7.RELEASE</version>
  </dependency>
  <dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.0.1</version>
  </dependency>
</dependencies>
```

Gradle:

```groovy
dependencies {
  compile 'org.mybatis.micronaut:mybatis-micronaut-config:1.0.0-SNAPSHOT'
  // Add following artifacts for enabling the Spring based transaction management
  compile 'io.micronaut:micronaut-spring:1.1.2'
  compile 'org.springframework:spring-jdbc:5.1.7.RELEASE'
  compile 'org.mybatis:mybatis-spring:2.0.1'
}
```

### Use `SqlSession` on application code

You need to add the `@io.micronaut.spring.tx.annotation.Transactional` that perform declarative transaction management 
using spring based transaction management and you can inject an `SqlSession` bean.

```java
import io.micronaut.spring.tx.annotation.Transactional;
// ...
@Singleton
@Transactional // Add annotation
public class CityService {
  
  private final SqlSession sqlSession;

  public CityService(SqlSession sqlSession) { // Can inject an SqlSession(SqlSessionTemplate)
    this.sqlSession = sqlSession;
  }
  
  public void update(City city) {
    sqlSession.update("com.example.CityMapper.update", city); // Call an SqlSession method
  }
  
}
```

### Use mapper on application code

You need to add the `@io.micronaut.spring.tx.annotation.Transactional` that perform declarative transaction management 
using spring based transaction management and you can inject a mapper bean.

```java
import io.micronaut.spring.tx.annotation.Transactional;
// ...
@Singleton
@Transactional // Add annotation for performing declarative transaction management
public class CityService {
  
  private final CityMapper cityMapper;

  public CityService(CityMapper cityMapper) { // Can inject a mapper bean
    this.cityMapper = cityMapper;
  }
  
  public void update(City city) {
    cityMapper.update(city); // Call a mapper method
  }
  
}
```

## Samples

* [The sample for standard usage with only MyBatis core module APIs](./mybatis-micronaut-samples/mybatis-micronaut-samples-standard)
* [The sample for advanced usage integrating with the Spring based transaction management and the `mybatis-spring`](./mybatis-micronaut-samples/mybatis-micronaut-samples-springtx)
