package com.l3s.dronegpt

import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform

object ChatGPTService {
    fun executeScript() {
        /*
        val shell = GroovyShell()
        val script = """
            def result = 'Hello, World!'
            return result
        """.trimIndent()
        val result = shell.evaluate(script)

        ToastUtils.showToast(result.toString())

         */

        val globals = setupLuaEnvironment()
        val luaScript = """
            local result = add_numbers(10, 20)
            print('Result from Kotlin:', result)
        """
        globals.load(luaScript).call()
    }

    fun setupLuaEnvironment(): Globals {
        val globals = JsePlatform.standardGlobals()
        // Register your library; "add_numbers" is the name used in Lua to call the function
        globals["add_numbers"] = MyKotlinLib()
        return globals
    }
}