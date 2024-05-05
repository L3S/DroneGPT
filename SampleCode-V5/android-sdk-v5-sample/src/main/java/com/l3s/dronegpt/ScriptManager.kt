package com.l3s.dronegpt

import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform

//takeoff, land, takePhoto 0 -> 0
//getLat, getLong, getAlt, getCurrentHeading 0 -> 1
//adjustFlightParameters

//TODO: class or object?
object ScriptManager {
    private lateinit var globals: Globals

    fun setupLuaEnvironment(): Globals {
        globals = JsePlatform.standardGlobals()
        // Setup other global settings or libraries
        globals.set("take_off", FlightLib.TakeOff())
        globals.set("land", FlightLib.Land())
        globals.set("take_photo", FlightLib.TakePhoto())
        globals.set("get_latitude", FlightLib.GetLatitude())
        globals.set("get_longitude", FlightLib.GetLongitude())
        globals.set("get_altitude", FlightLib.GetAltitude())
        globals.set("get_current_heading", FlightLib.GetCurrentHeading())
        globals.set("adjust_flight_parameters", FlightLib.AdjustFlightParameters())
        // add more function sets here
        return globals
    }

    fun executeLuaScript(script: String) {
        globals.load(script).call()
    }
}