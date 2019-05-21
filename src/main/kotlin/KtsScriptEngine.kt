import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass

class KtsScriptEngine private constructor(scriptEngine: ScriptEngine): ScriptEngine by scriptEngine{
    fun loadClass(path: String): KClass<*> {
        val code = File(path).readText()
        val className = Regex("class ([a-zA-Z]*)").find(code)?.groupValues?.get(1)

        return eval("""
        $code

        ${className}::class
        """.trimIndent()) as KClass<*>
    }

    companion object {
        fun create(jarPath: String): KtsScriptEngine {
            val file = File(jarPath)
            val url = file.toURI().toURL()
            val classLoader = ClassLoader.getSystemClassLoader() as URLClassLoader
            val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
            method.isAccessible = true
            method.invoke(classLoader, url)

            val scriptEngine = ScriptEngineManager(classLoader).getEngineByExtension("kts")

            return KtsScriptEngine(scriptEngine)
        }
    }
}