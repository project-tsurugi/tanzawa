# Tanzawa: Tsurugi command line tools written in Java

## Available Tools

- [tgsql](./modules/tgsql/cli) - Text based SQL client program.

## Requirements

* JDK `>= 11`

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
./gradle test
```

## How to run CLI

see [modules/cli/README.md](modules/cli/README.md).

## SQL language

see [docs/grammar-rule.md](docs/grammar-rule.md).

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
