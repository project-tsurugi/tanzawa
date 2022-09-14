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

[Engine]:src/main/java/com/tsurugidb/console/core/executor/engine/Engine.java
[SqlProcessor]:src/main/java/com/tsurugidb/console/core/executor/sql/SqlProcessor.java
[ResultProcessor]:src/main/java/com/tsurugidb/console/core/executor/result/ResultProcessor.java
[BasicEngine]:src/main/java/com/tsurugidb/console/core/executor/engine/BasicEngine.java
[BasicSqlProcessor]:src/main/java/com/tsurugidb/console/core/executor/sql/BasicSqlProcessor.java
[BasicResultProcessor]:src/main/java/com/tsurugidb/console/core/executor/result/BasicResultProcessor.java
[SqlParser]:src/main/java/com/tsurugidb/console/core/parser/SqlParser.java

## Language

### Notation

* `<symbol>` - a non-terminal symbol or token
* `"image"` - a token
* `<left> ::= <right-rule>` - derivation rule
* `<first> <second>` - rule conjunction
* `<first> | <second>` - rule disjunction
* `( <first> <second> )` - rule grouping
* `<rule> ?` - the rule appears just zero or one time
* `<rule> *` - the rule appears zero or more times
* `<rule> +` - the rule appears one or more times

### Grammar

```bnf
<script> ::= <statement>+

<statement> ::= <statement-body> <statement-delimiter>
             | <special-statement>

<statement-body> ::= <SQL-statement>
                  |  <start-transaction>
                  |  <commit-statement>
                  |  <rollback-statement>
                  |  <call-statement>
                  |  /* empty statement */

<SQL-statement> ::= (any text until <statement-delimiter>)

<start-transaction-statement> ::= "START" ( "LONG" )? "TRANSACTION" <transaction-option>*
                               |  "BEGIN" ( ( "LONG" )? "TRANSACTION" )? <transaction-option>*

<transaction-option> ::= "READ" "ONLY"
                      |  "READ" "ONLY" "DEFERRABLE"
                      |  "READ" "WRITE"
                      |  "WRITE" "PRESERVE" <table-list>
                      |  "READ" "AREA" "INCLUDE" <table-list> ( "EXCLUDE" <table-list> )?
                      |  "READ" "AREA" "EXCLUDE" <table-list> ( "INCLUDE" <table-list> )?
                      |  "EXECUTE" ( "PRIOR" | "EXCLUDING" ) ( "DEFERRABLE" | "IMMEDIATE" )?
                      |  "AS" <name-or-string>
                      |  "WITH" <key-value-pair-list>

<table-list> ::= <name-or-string> ( "," <name-or-string> )*
              |  "(" ")"
              |  "(" <name-or-string> ( "," <name-or-string> )* ")"

<key-value-pair-list> ::= <key-value-pair> ( "," <key-value-pair> )*
                       |  "(" ")"
                       |  "(" <key-value-pair> ( "," <key-value-pair> )* ")"

<key-value-pair> ::= <name-or-string> ( "=" <value> )?

<name-or-string> ::= <name>
                  |  <character-string-literal>

<read-area-option> ::= "INCLUDE" <table-list>
                    |  "EXCLUDE" <table-list>

<commit-statement> ::= "COMMIT"
                    |  "COMMIT" "WAIT" "FOR" <commit-status>

<commit-status> ::= "ACCEPTED"
                 |  "AVAILABLE"
                 |  "STORED"
                 |  "PROPAGATED"

<rollback-statement> ::= "ROLLBACK"

<call-statement> ::= "CALL" <identifier> "(" <call-parameter-list>? ")"

<call-parameter-list> :: <value> ( "," <value> )*

<value> ::= <literal>
         |  <name>

<literal> ::= <numeric-literal>
           |  <character-string-literal>
           |  <boolean-literal>
           |  "NULL"

<name> ::= <identifier> ( "." <identifier> )*

<special-statement> ::= "\EXIT"
                     |  "\HALT"
                     |  "\STATUS"
                     |  "\HELP"

<statement-delimiter> ::= ";"
                       |  EOF
```

### Tokens

```bnf
<whitespace> ::= ( " " | "\t" | "\r" | "\n" )

<comment> ::= "/*" (any character except "*/")* "*/"
           |  ( "//" | "--" ) (any character except line-break)*

<identifier> ::= <letter> ( <letter> | <digit> )*
              |  "\"" ( any character except "\\" and "\"" | "\\" . )* "\""

<numeric-literal> ::= ( "+" | "-" )? <coefficient-part> <exponent-part>?

<coefficient-part> ::= <digit>+ ( "." <digit>* )?
                    |  "." <digit>+

<exponent-part> ::= "E" ( "+" | "-" )? <digit>+

<character-string-literal> ::= "'" ( any character except "\\" and "'" | "\\" . )* "'"

<boolean-literal> = ( "TRUE" | "FALSE" )
```

## Implementation note

### Logger names and levels

* `com.tsurugidb.console.core.ScriptRunner`
  * `ERROR` - print critical runtime error messages
  * `WARN` - print runtime error messages
  * `INFO` - program start/finish message
  * `DEBUG` - print program parameters
* `com.tsurugidb.console.core.executor.engine.BasicEngine`
  * `DEBUG` - print engine progress
* `com.tsurugidb.console.core.executor.sql.BasicSqlProcessor`
  * `DEBUG` - print actual SQL requests
* `com.tsurugidb.console.core.executor.engine.ExecutorUtil`
  * `WARN` - print warnings
* `com.tsurugidb.console.core.parser.Segment`
  * `TRACE` - print each tokens
* `com.tsurugidb.console.core.parser.SegmentAnalyzer`
  * `DEBUG` - print analyze target
  * `TRACE` - print analyze progress
