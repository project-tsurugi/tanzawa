# Tsurugi SQL console engine

Tsurugi SQL console engine is a core library of Tsurugi SQL console.

Developers can build other SQL scripting tools using following interfaces and classes.

* interfaces
  * [Engine] - SQL scripting engine interface
  * [SqlProcessor] - Handles SQL operations, like start transaction, commit, or execute queries
  * [ResultProcessor] - Handles SQL result set
* classes
  * [BasicEngine] - A basic implementation of [Engine]
  * [BasicSqlProcessor] - A basic implementation of [SqlProcessor], which submits individual SQL operations to the Tsurugi OLTP server
  * [BasicResultProcessor] - Prints result sets as JSON lines style
  * [SqlParser] - Splits SQL script into individual SQL statements

[Engine]:src/main/java/com/tsurugidb/tgsql/core/executor/engine/Engine.java
[SqlProcessor]:src/main/java/com/tsurugidb/tgsql/core/executor/sql/SqlProcessor.java
[ResultProcessor]:src/main/java/com/tsurugidb/tgsql/core/executor/result/ResultProcessor.java
[BasicEngine]:src/main/java/com/tsurugidb/tgsql/core/executor/engine/BasicEngine.java
[BasicSqlProcessor]:src/main/java/com/tsurugidb/tgsql/core/executor/sql/BasicSqlProcessor.java
[BasicResultProcessor]:src/main/java/com/tsurugidb/tgsql/core/executor/result/BasicResultProcessor.java
[SqlParser]:src/main/java/com/tsurugidb/tgsql/core/parser/SqlParser.java

## Language

see [grammar-rule.md](../../docs/grammar-rule.md).

## Implementation note

### Logger names and levels

* `com.tsurugidb.tgsql.core.ScriptRunner`
  * `ERROR` - print critical runtime error messages
  * `WARN` - print runtime error messages
  * `INFO` - program start/finish message
  * `DEBUG` - print program parameters
* `com.tsurugidb.tgsql.core.executor.engine.BasicEngine`
  * `DEBUG` - print engine progress
* `com.tsurugidb.tgsql.core.executor.sql.BasicSqlProcessor`
  * `DEBUG` - print actual SQL requests
* `com.tsurugidb.tgsql.core.executor.engine.ExecutorUtil`
  * `WARN` - print warnings
* `com.tsurugidb.tgsql.core.parser.Segment`
  * `TRACE` - print each tokens
* `com.tsurugidb.tgsql.core.parser.SegmentAnalyzer`
  * `DEBUG` - print analyze target
  * `TRACE` - print analyze progress

### Extra testing options

```sh
# cd /path/to/tanzawa
./gradlew :tgsql:core:test \
    -Ptanzawa.dot=/path/to/dot \
    -Ptest.logLevel=debug
```
