package com.l3s.dronegpt

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.ZeroArgFunction


// 'function name' 'input argument count' -> 'output value'
//takeoff, land, takePhoto 0 -> 0
//getLat, getLong, getAlt, getCurrentHeading 0 -> 1
//adjustFlightParameters 4 -> 0

// This class defines all Lua functions and their input/output parameters types using LuaJ
// http://www.luaj.org/luaj/3.0/README.html#5
class FlightLib {

    class EndFlight : ZeroArgFunction() {
        override fun call(): LuaValue {
            FlightUtility.returnHomeAndEndFlight()
            return LuaValue.NIL
        }
    }

    class TakePhoto : ZeroArgFunction() {
        override fun call(): LuaValue {
            FlightUtility.takePhoto()
            return LuaValue.NIL
        }
    }

    class GetDistanceToOrigin : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getDistanceToHome())
        }
    }

    class GetXCoordinate : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getCurrentXCoordinate())
        }
    }

    class GetYCoordinate : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getCurrentYCoordinate())
        }
    }


    class GetCompassHeading : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getCompassHeading())
        }
    }

    class AdjustFlightParameters : ThreeArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue {
            if (arg1 != null && arg2 != null && arg3 != null) {
                    FlightUtility.adjustFlightParameters(arg1.checkdouble(), arg2.checkdouble(), arg3.checkdouble())
            }
            return LuaValue.NIL
        }

    }
}