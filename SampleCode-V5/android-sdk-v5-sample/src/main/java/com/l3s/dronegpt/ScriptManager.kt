package com.l3s.dronegpt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform

// This object handles Lua functions definition and script execution
object ScriptManager {
    private lateinit var globals: Globals
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    var errorListener: ScriptErrorListener? = null

    // Sets up Lua environment by matching Lua functions with their corresponding FlightUtility functions
    fun setupLuaEnvironment(): Globals {
        globals = JsePlatform.standardGlobals()
        globals.set("end_flight", FlightLib.EndFlight())
        globals.set("take_photo", FlightLib.TakePhoto())
        globals.set("get_distance_to_origin", FlightLib.GetDistanceToOrigin())
        globals.set("get_x_coordinate", FlightLib.GetXCoordinate())
        globals.set("get_y_coordinate", FlightLib.GetYCoordinate())
        globals.set("get_compass_heading", FlightLib.GetCompassHeading())
        globals.set("adjust_flight_parameters", FlightLib.AdjustFlightParameters())


        val pauseExecutionCode = """
           function pause_script_execution(duration)
                os.execute("sleep " .. tonumber(duration))
            end
        """
        globals.load(pauseExecutionCode).call()
        globals.set("pause_script_execution", globals.get("pause_script_execution"))

        return globals
    }

    // Executes the Lua script on a separate thread
    fun executeLuaScript(script: String) {
        coroutineScope.launch {
            try {
                globals.load(script).call()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorListener?.onError(e.message ?: "Unknown error occurred while executing Lua script")
                }
            }
        }
    }
}