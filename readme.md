# data2Exposed

データクラスからExposedのTableオブジェクトを生成するツールです。

対象のデータクラス
```Hoge.kt
data class Hoge(
  val a: Int,
  val fuga: Fuga
)
```

```Fuga.kt
data class Fuga(
    val a: Int
)
```

生成されるTableオブジェクト

```HogeTable.kt
import kotlin.Int
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

object HogeTable : IntIdTable() {
    val a: Column<Int> = integer("a")

    val fuga: Column<EntityID<Int>> = reference("Fuga", FugaTable)
}
```

```FugaTable.kt
import kotlin.Int
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

object FugaTable : IntIdTable() {
    val a: Column<Int> = integer("a")
}
```

## 実行方法
JDK8でしか動作しません。

```
java -jar data2Exposed-0.0.1.jar {対象データクラスの入ったjarのパス} {対象データクラスのファイルパス} {生成されたファイルを出力するパス}
```

