package com.l3s.dronegpt

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

//takeoff, land, takePhoto 0 -> 0
//getLat, getLong, getAlt, getCurrentHeading 0 -> 1
//adjustFlightParameters 4 -> 0
class FlightLib {
    class TakeOff : ZeroArgFunction() {
        override fun call(): LuaValue {
            FlightUtility.takeOff()
            return LuaValue.NIL
        }
    }

    class Land : ZeroArgFunction() {
        override fun call(): LuaValue {
            FlightUtility.land()
            return LuaValue.NIL
        }
    }

    class TakePhoto : ZeroArgFunction() {
        override fun call(): LuaValue {
            FlightUtility.takePhoto()
            return LuaValue.NIL
        }
    }

    class GetLatitude : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getLatitude())
        }
    }

    class GetLongitude : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getLongitude())
        }
    }

    class GetAltitude : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getAltitude())
        }
    }

    class GetCurrentHeading : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getCurrentHeading())
        }
    }

    class AdjustFlightParameters : VarArgFunction() {
        override fun invoke(args : Varargs): LuaValue {
            //TODO: test if args are correctly converted to double if parameter is int format
            FlightUtility.adjustFlightParameters(args.checkdouble(1), args.checkdouble(2), args.checkdouble(3), args.checkdouble(4))
            return LuaValue.NIL
        }
    }
}