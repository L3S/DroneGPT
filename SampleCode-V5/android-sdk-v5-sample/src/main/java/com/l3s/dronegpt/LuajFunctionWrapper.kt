package com.l3s.dronegpt

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

//takeoff, land, takePhoto 0 -> 0
//getLat, getLong, getAlt, getCurrentHeading 0 -> 1
//adjustFlightParameters 4 -> 0
class FlightLib {
//    class TakeOff : ZeroArgFunction() {
//        override fun call(): LuaValue {
//            FlightUtility.takeOff()
//            return LuaValue.NIL
//        }
//    }

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

    //DEPRECATED
//    class GetAltitude : ZeroArgFunction() {
//        override fun call(): LuaValue {
//            return LuaValue.valueOf(FlightUtility.getAltitude())
//        }
//    }

    class GetCompassHeading : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(FlightUtility.getCompassHeading())
        }
    }

//    class AdjustFlightParameters : VarArgFunction() {
//        override fun invoke(args : Varargs): LuaValue {
//            //TODO: test if args are correctly converted to double if parameter is int format
//            FlightUtility.adjustFlightParameters(args.checkdouble(1), args.checkdouble(2), args.checkdouble(3), args.checkdouble(4))
//            return LuaValue.NIL
//        }
//    }
    class AdjustFlightParameters : ThreeArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue {
            //TODO: test if args are correctly converted to double if parameter is int format
            if (arg1 != null && arg2 != null && arg3 != null) {
                    FlightUtility.adjustFlightParameters(arg1.checkdouble(), arg2.checkdouble(), arg3.checkdouble())
            }
            return LuaValue.NIL
        }

//        override fun invoke(args : Varargs): LuaValue {
//            FlightUtility.adjustFlightParameters(args.checkdouble(1), args.checkdouble(2), args.checkdouble(3), args.checkdouble(4))
//            return LuaValue.NIL
//        }
    }
}