# Tsurugi SQL grammar rule

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
                     |  "\HELP" (any character except line-break or ";")*

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

<boolean-literal> ::= ( "TRUE" | "FALSE" )

<word> ::= 
```
