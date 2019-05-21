import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

class TableFileGenerator(kclass: KClass<*>) {
    val typeSpecMap: MutableMap<String, TypeSpec> = mutableMapOf()
    val tree = createTree(kclass, TreeElement(kclass))

    fun generate(): List<FileSpec> {
        tree.flatten().sortedByDescending { it.depth }.forEach {
            println(it)
            typeSpecMap.put(it.data.simpleName!!, it.data.createTableTypeSpec())
        }

        return typeSpecMap.values.map {
            FileSpec.builder("", it.name!!).addType(it).build()
        }
    }

    private fun KClass<*>.createTableTypeSpec(): TypeSpec
            = TypeSpec.objectBuilder("${simpleName}Table")
        .superclass(IntIdTable::class)
        .addProperties(memberProperties.map {
            val m = it.toPropertyMaterial()
            PropertySpec.builder(m.name, m.typeName).initializer(m.initValue).build()
        })
        .build()

    private fun <T> KProperty1<T, *>.toPropertyMaterial(): PropertyMaterial {
        val typeName = if(returnType.jvmErasure.isData) {
            val intClassName = Int::class.asTypeName()
            val entityIdClassName = EntityID::class.className.parameterizedBy(intClassName)
            Column::class.className.parameterizedBy(entityIdClassName)
        } else {
            Column::class.className.parameterizedBy(returnType.asTypeName())
        }

        return PropertyMaterial(
            name,
            typeName,
            initValueStr
        )
    }

    private val KClass<*>.className: ClassName get() = ClassName(java.`package`.name, java.simpleName)

    private val <T> KProperty1<T, *>.initValueStr: String get() = when(returnType) {
        String::class.starProjectedType -> "varchar(\"$name\", 255)"
        Int::class.starProjectedType -> "integer(\"$name\")"
        Long::class.starProjectedType -> "long(\"$name\")"
        Char::class.starProjectedType -> "char(\"$name\")"
        Float::class.starProjectedType -> "float(\"$name\")"
        else -> typeSpecMap.get(returnType.jvmErasure.simpleName)?.let { "reference(\"${returnType.jvmErasure.simpleName}\", ${it.name})" } ?: throw RuntimeException(toString())
    }

    private fun createTree(kClass: KClass<*>, treeElement: TreeElement<KClass<*>>): TreeElement<KClass<*>> {
        if(kClass.memberProperties.any { it.returnType.jvmErasure.isData }.not()) return treeElement

        treeElement.children += kClass.memberProperties.map { it.returnType.jvmErasure }.filter { it.isData }.map { createTree(it, TreeElement(it, treeElement.depth + 1)) }

        return treeElement
    }

    data class TreeElement<T>(
        val data: T,
        val depth: Int = 0,
        val children: MutableList<TreeElement<T>> = mutableListOf()
    ) {
        fun flatten(): List<TreeElement<T>> {
            return children.map { it.flatten() }.flatten() + this
        }
    }

    class PropertyMaterial(
        val name: String,
        val typeName: TypeName,
        val initValue: String
    )
}