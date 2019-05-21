import java.io.File

fun main(args: Array<String>) {
    val (jarPath, targetClassPath, outputDir) = args
    val engine = KtsScriptEngine.create(jarPath)

    val kclass = engine.loadClass(targetClassPath)
    val typeSpecList = TableFileGenerator(kclass).generate()

    typeSpecList.forEach { it.writeTo(File(outputDir)) }
}



