# Tanzawa: Tsurugi command line tools written in Java

## Available Tools

* [tgsql](./modules/tgsql) - Text based SQL client program.
* [tgdump](./modules/tgdump) - Dump Tool.

## Requirements

* JDK `>= 17`

**Note:** The build is configured with `release = 11` in Gradle, which means the compiled bytecode is compatible with Java 11 or later. While JDK 17 is required for the build environment, the resulting JAR files can run on Java 11 and above.

## How to build

```sh
./gradlew assemble
```

### for Eclipse Buildship users

If you work with [Eclipse Buildship](https://github.com/eclipse/buildship), the following [Gradle initialization script](https://docs.gradle.org/current/userguide/init_scripts.html) avoids the conflict of each project name on the Eclipse workspace.

```gradle
allprojects { project ->
    if (project != project.rootProject) {
        project.tasks.matching { it.name == 'eclipseProject' }.each { task ->
            task.projectModel.name = (project.rootProject.name + project.path).replace(':', '-')
        }
    }
}
```

## How to test

```sh
./gradlew test
```

## How to run SQL console CLI

see [modules/tgsql/README.md](modules/tgsql/README.md).

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
