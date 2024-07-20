package com.l3s.dronegpt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform

//takeoff, land, takePhoto 0 -> 0
//getLat, getLong, getAlt, getCurrentHeading 0 -> 1
//adjustFlightParameters

object ScriptManager {
    private lateinit var globals: Globals
//    private var thread: ManagedThread? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    var errorListener: ScriptErrorListener? = null

    fun setupLuaEnvironment(): Globals {
        globals = JsePlatform.standardGlobals()
        // Setup other global settings or libraries
//        globals.set("take_off", FlightLib.TakeOff())
        globals.set("end_flight", FlightLib.EndFlight())
        globals.set("take_photo", FlightLib.TakePhoto())
        globals.set("get_distance_to_origin", FlightLib.GetDistanceToOrigin())
        globals.set("get_x_coordinate", FlightLib.GetXCoordinate())
        globals.set("get_y_coordinate", FlightLib.GetYCoordinate())
//        globals.set("get_altitude", FlightLib.GetAltitude())
        globals.set("get_compass_heading", FlightLib.GetCompassHeading())
        globals.set("adjust_flight_parameters", FlightLib.AdjustFlightParameters())

//        globals["pauseThread"] = object : OneArgFunction() {
//            override fun call(arg: LuaValue): LuaValue {
////                Log.d("setupLuaEnv", "pauseExecution called")
//                thread?.pauseExecution(arg.checklong())
//                return LuaValue.NIL
//            }
//        }

        val pauseExecutionCode = """
           function pause_script_execution(duration)
                os.execute("sleep " .. tonumber(duration))
            end
        """
        globals.load(pauseExecutionCode).call()
        globals.set("pause_script_execution", globals.get("pause_script_execution"))

        return globals
    }

    fun executeLuaScript(script: String) {
//        if (thread?.isAlive == true) {
//            // Safely stop the current thread before starting a new one
//            thread?.interrupt()
//        }
//
//        thread = ManagedThread(globals, script)
//        thread?.start()
        coroutineScope.launch {
            try {
                globals.load(script).call()
            } catch (e: Exception) {
                // Handle any exceptions, possibly using a callback to notify UI thread
//                Log.e("ScriptManager", "Error executing script", e)
                withContext(Dispatchers.Main) {
                    errorListener?.onError(e.message ?: "Unknown error occurred while executing Lua script")
                }
            }
        }
    }
}