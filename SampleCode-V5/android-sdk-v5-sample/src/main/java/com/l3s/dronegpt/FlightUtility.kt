package com.l3s.dronegpt

import android.annotation.SuppressLint
import android.location.Location
import com.l3s.dronegpt.data.database.Experiment
import com.l3s.dronegpt.ui.DroneGPTActivity
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FailsafeAction
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.error.RxError
import dji.v5.common.utils.CallbackUtils
import dji.v5.common.utils.RxUtil
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.et.set
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import java.io.IOException
import java.util.Timer
import java.util.TimerTask


// set maxFlightHeight and maxFlightDistance -> enable automatic return to home when battery is low ->
// Set RTH Height = selectedExperiment.flightHeight ->
// Takeoff -> set home to current location
// -> Turn on and setup virtualstick -> adjust virtualStickParam with vThrottle = selectedExperiment.flightHeight ->
// execute ChatGPTs script ->
// Set RTH Height -> return Home -> land -> save all updates to selectedExperiment.flightLogs

//TODO: set up fail safe functionality to "Pull the plug" and take over manually if something goes wrong -> disable VirtualStick
//TODO: manual compass, gimbal and IMU calibration
//TODO: choose max height and distance
object FlightUtility {
    private lateinit var activity: DroneGPTActivity
    private val TAG: String = "FlightUtility"
    private var connected: Boolean = false
    private lateinit var takeOffLocation: LocationCoordinate3D
    var virtualStickEnabled: Boolean = false
    //resets before each experiment
    private var flightParametersTransmitting: Boolean = false
    //resets before each experiment
    private lateinit var experimentInProgress: Experiment
    /*
        pitch: Double       ANGLE: [-30, 30]        VELOCITY: [-23, 23]
        roll: Double        ANGLE: [-30, 30]        VELOCITY: [-23, 23]
        yaw: Double         ANGLE: [-180, 180]      ANGULAR_VELOCITY: [-100, 100] (GROUND: positive ->, negative <-)
        vThrottle: Double   HEIGHT: [0, 5000]       VELOCITY: [-6, 6]
        VerticalControlMode.VELOCITY/POSITION/UNKNOWN
        RollPitchControlMode.VELOCITY/ANGLE/POSITION/UNKNOWN
        YawControlMode.ANGULAR_VELOCITY/ANGLE/UNKNOWN
        FlightCoordinateSystem.GROUND/BODY/UNKNOWN
    */
//    In the virtual stick advanced mode, obstacle avoidance is supported only when
//      the vertical control Mode is velocity mode,
//      the yaw control mode is angular velocity mode,
//      and the roll pitch control mode is velocity mode.
    //resets before each experiment
    private val virtualStickParam = VirtualStickFlightControlParam(
        0.0,
        0.0,
        0.0,
        0.0,
        VerticalControlMode.POSITION,
        RollPitchControlMode.VELOCITY,
        YawControlMode.ANGULAR_VELOCITY,
        FlightCoordinateSystem.GROUND
    )

//     [20, 1500] in meters
    private const val altitudeLimit: Int = 40
//    maximum distance between aircraft and home point [15, 8000] in meters
    private const val defaultDistanceLimit: Int = 320

    //resets before each experiment
    private var distanceLimitReached: Boolean = false

    private var homeLocation: Location = Location("home")

    fun init() {
        //logs https://developer.dji.com/api-reference/android-api/Components/SDKManager/DJISDKManager.html#djisdkmanager_getflyclogpath_inline
        //set maxFlightHeight and maxFlightRadius
        //EU RID? https://developer.dji.com/doc/mobile-sdk-tutorial/en/tutorials/compliance-eu.html#eu-rid
        //obstacle avoidance
        //set takeoff altitude
        //return home when battery is low KeyLowBatteryRTHEnabled

        // connection status
        if (KeyManager.getInstance().getValue(
                KeyTools.createKey(FlightControllerKey.KeyConnection),
            ) == true
        ) {
            connected = true
        }


    }

    private fun resetState() {
        flightParametersTransmitting = false
        virtualStickParam.pitch = 0.0
        virtualStickParam.roll = 0.0
        virtualStickParam.yaw = 0.0
        distanceLimitReached = false

        val homeCoordinates = getLocation3D()
        homeLocation.latitude = homeCoordinates.latitude
        homeLocation.longitude = homeCoordinates.longitude
    }

//    fun testFlightFullInit(obstacleAvoidance: Boolean, takeoffAltitude: Double, altitudeLimit: Int, distanceLimit: Int) {
//        //set home location
//        setHomeToCurrentLocation()
//        //set takeoff altitude
//        KeyTools.createKey(FlightControllerKey.KeyTakeoffLocationAltitude).set(takeoffAltitude, {
//            activity.addUpdate("takeoff altitude set to $takeoffAltitude")
//        }, {
//            activity.addUpdate("could not set takeoff altitude to $takeoffAltitude $it")
//        })
//        //enable automatic return to home when battery is low
//        KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHEnabled).set(true, {
//            activity.addUpdate("lowBatteryRTH enabled")
//        }, {
//            activity.addUpdate("could not enable lowBatteryRTH $it")
//        })
//        //set max flight altitude
//        KeyTools.createKey(FlightControllerKey.KeyHeightLimit).set(altitudeLimit, {
//            activity.addUpdate("max flight altitude set to $altitudeLimit")
//        }, {
//            activity.addUpdate("could not set max flight altitude to $altitudeLimit $it")
//        })
//
//        //enable and set max distance to home point
//        KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled).set(true, {
//            activity.addUpdate("enabled distance limit")
//        }, {
//            activity.addUpdate("could enable distance $it")
//        })
//        KeyTools.createKey(FlightControllerKey.KeyDistanceLimit).set(distanceLimit, {
//            activity.addUpdate("distance limit set to $distanceLimit")
//        }, {
//            activity.addUpdate("could not set distance limit to $distanceLimit: $it")
//        })
//        //TODO: test obstacle avoidance type BYPASS
//        //TODO: test takeoff without any obstacle avoidance
////        if(obstacleAvoidance) {
////            PerceptionManager.getInstance().setObstacleAvoidanceType(
////                ObstacleAvoidanceType.BRAKE,
////                object : CommonCallbacks.CompletionCallback {
////                    override fun onSuccess() {
////                        activity.addUpdate("obstacle avoidance set to brake")
////                    }
////
////                    override fun onFailure(error: IDJIError) {
////                        activity.addUpdate("could not set obstacle avoidance to brake $error")
////                    }
////                })
////        } else {
////            PerceptionManager.getInstance().setObstacleAvoidanceType(
////                ObstacleAvoidanceType.CLOSE,
////                object : CommonCallbacks.CompletionCallback {
////                    override fun onSuccess() {
////                        activity.addUpdate("obstacle avoidance set to close")
////                    }
////
////                    override fun onFailure(error: IDJIError) {
////                        activity.addUpdate("could not set obstacle avoidance to close $error")
////                    }
////                })
////        }
//        //TODO: test if needed -> not possible with this drone
//        //set regulation region and operator id
////        UASRemoteIDManager.getInstance().setUASRemoteIDAreaStrategy(AreaStrategy.EUROPEAN_STRATEGY)
////        UASRemoteIDManager.getInstance().setOperatorRegistrationNumber("", object : CommonCallbacks.CompletionCallback {
////            override fun onSuccess() {
////                activity.addUpdate("Remote ID set successfully")
////            }
////            override fun onFailure(error: IDJIError) {
////                activity.addUpdate("could not set Remote ID $error")
////            }
////        })
//
//        val connectivity = KeyTools.createKey(FlightControllerKey.KeyConnection).get()
//        activity.addUpdate("KeyConnection is currently: $connectivity")
//        val gpsSignal = KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel).get()?.value()
//        activity.addUpdate("GPS signal level is currently: $gpsSignal")
//    }


    fun setActivityObject(activity: DroneGPTActivity) {
        this.activity = activity
    }

    fun setExperiment(experiment: Experiment) {
        experimentInProgress = experiment
        resetState()
        setReturnHomeAltitude()
    }

    fun initializeFlight(fullInitialization: Boolean = false) {
        // setup flight mode listener
        KeyTools.createKey(FlightControllerKey.KeyFlightMode).listen(activity, false) {currentFlightMode ->
            activity.addUpdate("FlightMode changed to: $currentFlightMode")
        }

        if (fullInitialization) {
            // setup listener for generated images
            KeyTools.createKey(CameraKey.KeyNewlyGeneratedMediaFile).listen(activity, false) {generatedImageInfo ->
                activity.createImage(experimentInProgress, generatedImageInfo, getLocation3D())
            }

            // confirm continue landing.
            // When the aircraft is at an altitude of less than 0.7 meters above the ground, the aircraft will stop landing and wait for confirmation.
            // removed for this being unsafe in some cases -> confirm with RC instead
//            KeyTools.createKey(FlightControllerKey.KeyIsLandingConfirmationNeeded).listen(activity, false) {
//                if (it == true) {
//                    FlightControllerKey.KeyConfirmLanding.create().action({
//                        activity.addUpdate("confirmed landing")
//                    }, { e: IDJIError ->
//                        activity.addUpdate("could not confirm landing $e")
//                    })
//                }
//            }

            // testing:

            // set home to current location
            // set maxFlightHeight and maxFlightDistance takOffAltitude -> enable automatic return to home when battery is low ->
            // Set RTH Height ->
            // Turn on and setup virtualstick ->


            //set home location -> after takeoff
            //        setHomeToCurrentLocation()

            //set max flight altitude
            KeyTools.createKey(FlightControllerKey.KeyHeightLimit).set(altitudeLimit, {
                activity.addUpdate("max flight altitude set to $altitudeLimit")
            }, {
                activity.addUpdate("could not set max flight altitude to $altitudeLimit $it")
            })

            //enable max distance to home point
            KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled).set(true, {
                activity.addUpdate("enabled distance limit")
            }, {
                activity.addUpdate("could enable distance $it")
            })

            //moved to a separate fun
//            KeyTools.createKey(FlightControllerKey.KeyDistanceLimit).set(defaultDistanceLimit, {
//                activity.addUpdate("distance limit set to $defaultDistanceLimit")
//            }, {
//                activity.addUpdate("could not set distance limit to $defaultDistanceLimit: $it")
//            })
            //set max distance to home point
            setDistanceLimit(defaultDistanceLimit)

            //setup listener for when the drone reaches distance limit
            KeyTools.createKey(FlightControllerKey.KeyIsNearDistanceLimit).listen(activity, false) {
                if (it == true && !distanceLimitReached) {
                    activity.addUpdate("distance limit of $defaultDistanceLimit meters reached.")
                    distanceLimitReached = true
                } else if (it == false && distanceLimitReached) {
                    distanceLimitReached = false
                }
            }

            //enable automatic return to home when battery is low
            KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHEnabled).set(true, {
                activity.addUpdate("lowBattery return to home enabled")
            }, {
                activity.addUpdate("could not enable lowBattery return to home $it")
            })
            //setup listener for when the battery gets too low
            //aircraft performs auto return home in that case
            KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHInfo)
                .listen(activity, false) { info ->
                    if (info?.lowBatteryRTHStatus.toString() == "COUNTING_DOWN") { // battery level is getting too low
                        activity.addUpdate("Battery is getting low. Initiating auto return home")
                        KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHConfirm).action({
                            activity.addUpdate("confirmed auto return home")
                        }, { e: IDJIError ->
                            activity.addUpdate("could not confirm auto return home $e")
                        })
                    }
                }


            //sets fail safe behavior to auto return home.
            // When the remote controller loses connection with the aircraft, the aircraft will perform according to the set fail safe behavior.
            KeyTools.createKey(FlightControllerKey.KeyFailsafeAction).set(FailsafeAction.GOHOME, {
                activity.addUpdate("successfully set auto return home as FailsafeAction")
            }, {
                activity.addUpdate("could not set auto return home as FailsafeAction $it")
            })

            //setup listener for when the aircraft is about to perform fail safe behavior
            KeyTools.createKey(FlightControllerKey.KeyIsFailSafe).listen(activity, false) {
                if (it == true) {
                    activity.addUpdate("aircraft is out of control. performing fail safe behavior")
                }
            }


            //get takeoff altitude
//            activity.addUpdate(
//                "KeyTakeoffLocationAltitude: ${
//                    KeyTools.createKey(FlightControllerKey.KeyTakeoffLocationAltitude).get()
//                        .toString()
//                }"
//            )
//            activity.addUpdate("Current altitude: ${getAltitude().toString()}")
            //        KeyTools.createKey(FlightControllerKey.KeyTakeoffLocationAltitude).get().toString()

            setReturnHomeAltitude()
        }
    }

    /*
        【Prerequisite】
        The connection between the app and RC is normal.
        The RC is under normal mode.
        The aircraft is not flying with an automated task such as waypointMission or RTH function.
     */
    fun enableVirtualStick() {
        VirtualStickManager.getInstance().init()
        VirtualStickManager.getInstance()
            .enableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    activity.addUpdate("VirtualStick enabled")
                    FlightUtility.virtualStickEnabled = true
                }
                override fun onFailure(error: IDJIError) {
                    activity.addUpdate("could not enable VirtualStick $error")
                }
            })
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true)
    }

    fun disableVirtualStick() {
        VirtualStickManager.getInstance().disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                activity.addUpdate("VirtualStick disabled")
                virtualStickEnabled = false
            }

            override fun onFailure(error: IDJIError) {
                activity.addUpdate("Error disabling VirtualStick $error")
            }
        })
    }

    //TODO: call before each experiment
    private fun setReturnHomeAltitude() {
        if(this::experimentInProgress.isInitialized) {
            //set return home altitude
            KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight)
                .set(experimentInProgress.flightHeight, {
                    activity.addUpdate("return home height set to ${experimentInProgress.flightHeight}")
                }, {
                    activity.addUpdate("could not set return home height to ${experimentInProgress.flightHeight}: $it")
                })
        }
    }

    fun setDistanceLimit(distanceLimit: Int) {
        KeyTools.createKey(FlightControllerKey.KeyDistanceLimit).set(distanceLimit, {
            activity.addUpdate("distance limit set to $distanceLimit")
        }, {
            activity.addUpdate("could not set distance limit to $distanceLimit: $it")
        })
    }


    private fun getLocation3D(): LocationCoordinate3D {
        return KeyManager.getInstance()
            .getValue(
                KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D),
                LocationCoordinate3D(0.0, 0.0, 0.0)
            )
    }

    fun getLatitude(): Double {
        return getLocation3D().latitude
    }

    fun getLongitude(): Double {
        return getLocation3D().longitude
    }

    //called by ChatGPT
    fun getCurrentXCoordinate(): Double {
        val currentXAxisLocation = Location("xAxisLocation")
        currentXAxisLocation.latitude = homeLocation.latitude
        currentXAxisLocation.longitude = getLongitude()
        val distance = homeLocation.distanceTo(currentXAxisLocation)
        return if (currentXAxisLocation.longitude >= homeLocation.longitude) {
            distance.toDouble()
        } else {
            (-distance).toDouble()
        }
    }

    //called by ChatGPT
    fun getCurrentYCoordinate(): Double {
        val currentYAxisLocation = Location("yAxisLocation")
        currentYAxisLocation.longitude = homeLocation.longitude
        currentYAxisLocation.latitude = getLatitude()
        val distance = homeLocation.distanceTo(currentYAxisLocation)
        return if (currentYAxisLocation.latitude >= homeLocation.latitude) {
            distance.toDouble()
        } else {
            (-distance).toDouble()
        }
    }


    //DEPRECATED
//    fun testGetXCoordinate(homeLat: Double, homeLong: Double, currentLongitude: Double): Double {
//        val homeLocation = Location("homeTestLocation")
//        homeLocation.latitude = homeLat
//        homeLocation.longitude = homeLong
//
//        val currentXAxisLocation = Location("xAxisLocation")
//        currentXAxisLocation.latitude = homeLocation.latitude
//        currentXAxisLocation.longitude = currentLongitude
//        val distance = homeLocation.distanceTo(currentXAxisLocation)
//        return if (currentXAxisLocation.longitude >= homeLocation.longitude) {
//            distance.toDouble()
//        } else {
//            (-distance).toDouble()
//        }
//    }
//
    //DEPRECATED
//    fun testGetYCoordinate(homeLat: Double, homeLong: Double, currentLatitude: Double): Double {
//        val homeLocation = Location("homeTestLocation")
//        homeLocation.latitude = homeLat
//        homeLocation.longitude = homeLong
//
//        val currentYAxisLocation = Location("yAxisLocation")
//        currentYAxisLocation.longitude = homeLocation.longitude
//        currentYAxisLocation.latitude = currentLatitude
//        val distance = homeLocation.distanceTo(currentYAxisLocation)
//        return if (currentYAxisLocation.latitude >= homeLocation.latitude) {
//            distance.toDouble()
//        } else {
//            (-distance).toDouble()
//        }
//    }

    //called by ChatGPT
    fun getDistanceToHome(): Double {
        val threeDLocation = getLocation3D()
        val currentLocation = Location("currentLocation")
        currentLocation.latitude = threeDLocation.latitude
        currentLocation.longitude = threeDLocation.longitude
        return homeLocation.distanceTo(currentLocation).toDouble()
    }

    //called by ChatGPT
    fun getCompassHeading(): Double {
        val heading = FlightControllerKey.KeyCompassHeading.create().get(200.0)
        if (heading == 200.0) {
            activity.addUpdate("Could not retrieve heading. Receiving default value instead")
        }
        return heading
    }

    //DEPRECATED
//    fun getGimbalAngle(): Double {
//        return KeyTools.createKey(GimbalKey.KeyGimbalAttitude).get()?.pitch ?: 0.0
//    }


    private fun preCommandCheck(): Boolean {
        //check connectivity
        //check battery
        //check GPSSignalLevel
        return (KeyTools.createKey(FlightControllerKey.KeyConnection).get() == true &&
                KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel).get()?.value()!! >= 2)

//        return true
    }

    fun getConnectionAndGpsSignalLevel() {
        activity.addUpdate("KeyConnection: ${
            KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection))
        }")
        activity.addUpdate("GPSSignalLevel: ${
            KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel))
        }")
        activity.addUpdate("GPSSatelliteCount: ${
            KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyGPSSatelliteCount))
        }")
    }


    fun takeOff() {
//        takeOffLocation = KeyManager.getInstance()
//            .getValue(
//                KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D),
//                LocationCoordinate3D(0.0, 0.0, 0.0)
//            )

        //takeoff
        FlightControllerKey.KeyStartTakeoff.create().action({
            activity.addUpdate("takeoff successful")
            //Set home location??

        }, { e: IDJIError ->
            activity.addUpdate("could not takeoff $e")
        })
    }

    fun elevateToExperimentHeight() {
        if (!virtualStickEnabled) {
            enableVirtualStick()
        }
        virtualStickParam.verticalThrottle = experimentInProgress.flightHeight.toDouble()
//        adjustFlightParameters(0.0, 0.0, 0.0, experimentInProgress.flightHeight.toDouble())
        if (!flightParametersTransmitting) {
            flightParametersTransmitting = true
            transmitFlightParameters()
        }
        activity.addUpdate("elevating to experiment's flight height")
    }

    private fun setHomeToCurrentLocation() {
        FlightControllerKey.KeySetHomeLocationUsingAircraftCurrentLocation.create().action({
            activity.addUpdate("home location set successfully.")
        }, { e: IDJIError ->
            activity.addUpdate("could not set home location,$e")
        })
    }

    fun land() {
        FlightControllerKey.KeyStartAutoLanding.create().action({
            activity.addUpdate("landing successful")
        }, { e: IDJIError ->
            activity.addUpdate("could not land $e")
        })
    }

    fun returnHome() {
        if (virtualStickEnabled) {
            disableVirtualStick()
        }
        FlightControllerKey.KeyStartGoHome.create().action({
            activity.addUpdate("returning home")
        }, { e: IDJIError ->
            activity.addUpdate("could not return home $e")
        })
    }

    private fun endFlight() {
        activity.saveAndResetFlightLogs(experimentInProgress.id)
        activity.exportDataToJsonFile()
    }

    //called by ChatGPT
    //VirtualStickEnabled acts as the switch for this function to reduce unwanted images
    fun takePhoto() {
        if (virtualStickEnabled) {
            capture(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    activity.addUpdate("took a photo successfully")
                }

                override fun onFailure(error: IDJIError) {
                    activity.addUpdate("could not take a photo $error")
                }
            })
        } else {
            activity.addUpdate("could not take a photo. please enable VirtualStick first.")
        }
    }

    @SuppressLint("CheckResult")
    private fun capture(callback: CommonCallbacks.CompletionCallback) {
        RxUtil.setValue(
            KeyTools.createKey<CameraMode>(
                CameraKey.KeyCameraMode
            ), CameraMode.PHOTO_NORMAL
        )
            .andThen(RxUtil.performActionWithOutResult(KeyTools.createKey(CameraKey.KeyStartShootPhoto)))
            .subscribe({ CallbackUtils.onSuccess(callback) }
            ) { throwable: Throwable ->
                CallbackUtils.onFailure(
                    callback,
                    (throwable as RxError).djiError
                )
            }
    }

/*
    pitch: Double       ANGLE: [-30, 30]        VELOCITY: [-23, 23]
    roll: Double        ANGLE: [-30, 30]        VELOCITY: [-23, 23]
    yaw: Double         ANGLE: [-180, 180]      ANGULAR_VELOCITY: [-100, 100]
    vThrottle: Double   HEIGHT: [0, 5000]       VELOCITY: [-6, 6]
 */
    //called by ChatGPT
    fun adjustFlightParameters(pitch: Double, roll: Double, yaw: Double) {
        // check if pitch value is within the allowed range
        if (pitch < ChatGPTUtility.minPitchRollValue.toDouble()) { // pitch value is smaller than minValue allowed -> set to minValue and addUpdate
            virtualStickParam.pitch = ChatGPTUtility.minPitchRollValue.toDouble()
            activity.addUpdate("pitch value was outside the allowed range")
        } else if (pitch > ChatGPTUtility.maxPitchRollValue.toDouble()) { // pitch value is larger than maxValue allowed -> set to maxValue and addUpdate
            virtualStickParam.pitch = ChatGPTUtility.maxPitchRollValue.toDouble()
            activity.addUpdate("pitch value was outside the allowed range")
        } else { // pitch value is within the allowed range
            virtualStickParam.pitch = pitch
        }

        // check if roll value is within the allowed range
        if (roll < ChatGPTUtility.minPitchRollValue.toDouble()) { // roll value is smaller than minValue allowed -> set to minValue and addUpdate
            virtualStickParam.roll = ChatGPTUtility.minPitchRollValue.toDouble()
            activity.addUpdate("roll value was outside the allowed range")
        } else if (roll > ChatGPTUtility.maxPitchRollValue.toDouble()) { // roll value is larger than maxValue allowed -> set to maxValue and addUpdate
            virtualStickParam.roll = ChatGPTUtility.maxPitchRollValue.toDouble()
            activity.addUpdate("roll value was outside the allowed range")
        } else { // roll value is within the allowed range
            virtualStickParam.roll = roll
        }

        // check if yaw value is within the allowed range
        if (yaw < ChatGPTUtility.minYawValue.toDouble()) { // yaw value is smaller than minValue allowed -> set to minValue and addUpdate
            virtualStickParam.yaw = ChatGPTUtility.minYawValue.toDouble()
            activity.addUpdate("yaw value was outside the allowed range")
        } else if (yaw > ChatGPTUtility.maxYawValue.toDouble()) { // yaw value is larger than maxValue allowed -> set to maxValue and addUpdate
            virtualStickParam.yaw = ChatGPTUtility.maxYawValue.toDouble()
            activity.addUpdate("yaw value was outside the allowed range")
        } else { // yaw value is within the allowed range
            virtualStickParam.yaw = yaw
        }

        if (!virtualStickEnabled) {
            activity.addUpdate("could not adjust parameters. enable VirtualStick first.")
        } else {
            if (!flightParametersTransmitting) {
                flightParametersTransmitting = true
                transmitFlightParameters()
                activity.addUpdate("transmission started")
            }
            activity.addUpdate("parameters adjusted to pitch: ${virtualStickParam.pitch}, roll: ${virtualStickParam.roll}, yaw: ${virtualStickParam.yaw}, vThrottle: ${virtualStickParam.verticalThrottle}")
        }
    }
    private fun transmitFlightParameters() {
        Timer().scheduleAtFixedRate( object : TimerTask() {
            override fun run() {
                if (virtualStickEnabled) {
//                    Log.i(TAG, "transmitting")
//                    activity.addUpdate("transmitting: $virtualStickParam.pitch")
                    try {
                        VirtualStickManager
                            .getInstance()
                            .sendVirtualStickAdvancedParam(virtualStickParam)
//                        activity.addUpdate("transmitting pitch: ${virtualStickParam.pitch}, roll: ${virtualStickParam.roll}, yaw: ${virtualStickParam.yaw}, vThrottle: ${virtualStickParam.verticalThrottle}")
                    } catch (e: IOException) {
                        activity.addUpdate("error sending parameters: $e")
                    }
                }
            }
        }, 0, 200)
    }

    //called by ChatGPT
    fun returnHomeAndEndFlight() {
        returnHome()
        endFlight()
    }

    //DEPRECATED
//    fun planarDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
//        val earthRadius = 6378137 // Radius of the earth in meters (WGS84)
//        val dLat = Math.toRadians(lat2 - lat1) // Delta latitude in radians
//        val dLon = Math.toRadians(lon2 - lon1) // Delta longitude in radians
//        val latAvg = Math.toRadians((lat1 + lat2) / 2.0) // Average latitude in radians
//
//        val meterPerLat = Math.PI * earthRadius / 180.0 // meters per degree of latitude
//        val meterPerLon = meterPerLat * cos(latAvg) // meters per degree of longitude at average latitude
//
//        val x = dLon * meterPerLon // Distance in meters on the x-axis
//        val y = dLat * meterPerLat // Distance in meters on the y-axis
//
//        return sqrt(x * x + y * y) // Pythagorean theorem for final distance
//    }
}
