package com.l3s.dronegpt

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction


class MyKotlinLib: TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val result = addNumbers(arg1.toint(), arg2.toint())
        return LuaValue.valueOf(result)
    }

    private fun addNumbers(a: Int, b: Int): Int {
        return a + b
    }
}