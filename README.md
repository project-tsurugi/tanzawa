# Tanzawa: Tsurugi SQL console

Tsurugi SQL console (Tanzawa) is a text based SQL client program.

## Requirements

* JDK `>= 11`

## How to build

First, set up the following credentials for the GitHub Packages, and build normally.

* Gradle property `gpr.user` or environment variable `GPR_USER` with your GitHub username
* Gradle Property `gpr.key` or environment variable `GPR_KEY` with your personal access token

```sh
./gradlew assemble
```

## How to test

```sh
./gradle test
```

## How to run CLI

see [modules/cli/README.md](modules/cli/README.md).

## SQL language

see [modules/core/README.md](modules/core/README.md#language)

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
