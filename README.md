#aspectjx Build Plugin
aop scan code plugin
```gradle
aspectjx {
    enabled true
    include 'com.android.*'
}
 ```
##Feature

1. `include` configure the scan code package name
2. `enabled` enable log printing
3. `org.aspectj:aspectjrt:1.9.5` Use with this library

### aspectjrt check the documentation yourself


##Creating a new project
Fill out build.gradle

```gradle
aspectjx {
    enabled true
    include 'com.android.*'
}
 ```

Copy the following into settings.gradle.kts:
```gradle
pluginManagement {
    plugins {
        id("io.github.xiaoyun-sun.aspectjx") version "1.0.0"
    }
}

buildscript {
    repositories {
        maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenLocal()
    }
    dependencies {
        classpath("io.github.xiaoyun-sun.aspectjx:1.0.0")
    }
}

```
