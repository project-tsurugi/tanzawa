# Tsurugi SQL console BLOB/CLOB

Tsurugi SQLコンソール（tgsql）でのBLOB/CLOBを扱い方を説明します。

## DBへのデータ登録

Tsurugi DB内ではBLOB/CLOBデータはファイルで扱われますが、Tsurugi SQLコンソールではSQL実行時にデータファイルを入力とする方法は提供していません。

小さなデータであればリテラル（BLOBなら `X'十六進数'` 、CLOBなら `'文字列'` ）が使用できます。

## select文の実行結果

BLOB/CLOBはサイズが大きいデータのため、select文の実行結果にはデータそのものは表示せず、Tsurugi SQLコンソールが採番したオブジェクト名を表示します。

BLOBの場合は `blob@番号` 、CLOBの場合は `clob@番号` です。

- それぞれの番号はTsurugi SQLコンソール内で一意となります。
- 番号はTsurugi SQLコンソールを再起動するとリセットされます。
- オブジェクト名はselect文を実行してBLOB/CLOBデータが現れる度に採番されます。
  すなわち、複数のselect文の実行結果が実体として同じデータを指していたとしても、異なるオブジェクト名が割り当てられます。
- これらのオブジェクト名は、あくまでTsurugi SQLコンソール上でBLOB/CLOBを扱うためのものであり、Tsurugi DBに渡すことはできません。
  つまり、SQL文の一部（where条件など）に使用することはできません。

なお、 **IPC接続で接続している場合は** これらのオブジェクト名を使用してデータをファイルに出力することができますが、オブジェクト名は **トランザクションが実行中の間のみ有効** です。  
すなわち、デフォルトではSQLを実行する度に暗黙にトランザクションが開始され暗黙にコミットされる（トランザクションが終了する）ので、オブジェクト名を使用することはできません。  
オブジェクト名を使用したい場合は明示的にbegin文を実行してトランザクションを開始してください。

## オブジェクト名の表示（ `\show` ）

`\show` コマンドで、現在のトランザクションで使用可能なオブジェクト名の一覧が表示されます。

このコマンドは、トランザクションが実行中の間のみ使用可能です。

```
\show blob
\show clob
```

## ファイルへの出力（ `\store` ）

`\store` コマンドで、BLOB/CLOBのデータをローカルファイルに出力することができます。

このコマンドは、IPC接続で接続している場合のみ使用できます。  
また、トランザクションが実行中の間のみ使用可能です。

```
\store blob <object-name> </path/to/file>
\store clob <object-name> </path/to/file>
```

object-nameには `blob@番号` や `clob@番号` 、もしくは接頭辞を省略した `番号` が指定できます。

### 例

```
tgsql> begin;
transaction started. option=[
  type: OCC
]

tgsql> select * from blob_example;
[pk: INT4, value: BLOB]
[1, null]
[2, blob@0]
[3, blob@1]
(3 rows)

tgsql> \store blob 0 /tmp/blob-0.bin

tgsql> commit;
transaction commit(DEFAULT) finished.
```

