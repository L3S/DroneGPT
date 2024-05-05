package com.l3s.dronegpt

import android.annotation.SuppressLint
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
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
import dji.v5.et.set
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.perception.PerceptionManager
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import java.util.Timer
import java.util.TimerTask

//TODO: set up fail safe functionality to "Pull the plug" and take over manually if something goes wrong
//TODO: implement heading calibration or leave it manual?
//TODO: location/speed logging has to have the starting and end location
object FlightUtility {
    private lateinit var activity: TestFlightActivity
    private val TAG: String = "FlightUtility"
    private var connected: Boolean = false
    private lateinit var takeOffLocation: LocationCoordinate3D
    var virtualStickEnabled: Boolean = false
    private var flightParametersTransmitting: Boolean = false

    //TODO: test if roll comes before pitch
    /*
        pitch: Double       ANGLE: [-30, 30]        VELOCITY: [-23, 23]
        roll: Double        ANGLE: [-30, 30]        VELOCITY: [-23, 23]
        yaw: Double         ANGLE: [-180, 180]      ANGULAR_VELOCITY: [-100, 100]
        vThrottle: Double   HEIGHT: [0, 5000]       VELOCITY: [-6, 6]
        VerticalControlMode.VELOCITY/POSITION/UNKNOWN
        RollPitchControlMode.VELOCITY/ANGLE/POSITION/UNKNOWN
        YawControlMode.ANGULAR_VELOCITY/ANGLE/UNKNOWN
        FlightCoordinateSystem.GROUND/BODY/UNKNOWN
    */
    private val virtualStickParam = VirtualStickFlightControlParam(
        0.0,
        0.0,
        0.0,
        0.0,
        VerticalControlMode.VELOCITY,
        RollPitchControlMode.VELOCITY,
        YawControlMode.ANGLE,
        FlightCoordinateSystem.BODY
    )
//    private const val takeoffAltitude: Double = 5.0
    // [20, 1500] in meters
//    private const val altitudeLimit: Int = 20
    //maximum distance between aircraft and home point [15, 8000] in meters
//    private const val distanceLimit: Int = 15
    //set speed

    //longitude
    //latitude
    //altitude

    fun init() {
        //logs https://developer.dji.com/api-reference/android-api/Components/SDKManager/DJISDKManager.html#djisdkmanager_getflyclogpath_inline
        //set maxFlightHeight and maxFlightRadius
        //EU RID? https://developer.dji.com/doc/mobile-sdk-tutorial/en/tutorials/compliance-eu.html#eu-rid
        //obstacle avoidance
        //set takeoff altitude
        //return home when battery is low KeyLowBatteryRTHEnabled
        //calibrate compass https://developer.dji.com/doc/mobile-sdk-tutorial/en/basic-introduction/basic-concepts/flight-controller.html#compass

        // connection status
        if (KeyManager.getInstance().getValue(
                KeyTools.createKey(FlightControllerKey.KeyConnection),
            ) == true
        ) {
            connected = true
        }


    }

    fun testFlightFullInit(obstacleAvoidance: Boolean, takeoffAltitude: Double, altitudeLimit: Int, distanceLimit: Int) {
        //set home location
        setHomeToCurrentLocation()
        //set takeoff altitude
        KeyTools.createKey(FlightControllerKey.KeyTakeoffLocationAltitude).set(takeoffAltitude, {
            ToastUtils.showToast("takeoff altitude set")
            activity.addUpdate("takeoff altitude set to $takeoffAltitude")
        }, {
            ToastUtils.showToast("could not set takeoff altitude $it")
            activity.addUpdate("could not set takeoff altitude to $takeoffAltitude $it")
        })
        //enable automatic return to home when battery is low
        KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHEnabled).set(true, {
            ToastUtils.showToast("lowBatteryRTH enabled")
            activity.addUpdate("lowBatteryRTH enabled")
        }, {
            ToastUtils.showToast("could not enable lowBatteryRTH $it")
            activity.addUpdate("could not enable lowBatteryRTH $it")
        })
        //set max flight altitude
        KeyTools.createKey(FlightControllerKey.KeyHeightLimit).set(altitudeLimit, {
            ToastUtils.showToast("max flight altitude set")
            activity.addUpdate("max flight altitude set to $altitudeLimit")
        }, {
            ToastUtils.showToast("could not set max flight altitude $it")
            activity.addUpdate("could not set max flight altitude to $altitudeLimit $it")
        })

        //enable and set max distance to home point
        KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled).set(true, {
            ToastUtils.showToast("enabled distance limit")
            activity.addUpdate("enabled distance limit")
        }, {
            ToastUtils.showToast("could enable distance $it")
            activity.addUpdate("could enable distance $it")
        })
        KeyTools.createKey(FlightControllerKey.KeyDistanceLimit).set(distanceLimit, {
            ToastUtils.showToast("distance limit set")
            activity.addUpdate("distance limit set to $distanceLimit")
        }, {
            ToastUtils.showToast("could not set distance limit $it")
            activity.addUpdate("could not set distance limit to $distanceLimit: $it")
        })
        //TODO: test obstacle avoidance type BYPASS
        //TODO: test takeoff without any obstacle avoidance
        if(obstacleAvoidance) {
            PerceptionManager.getInstance().setObstacleAvoidanceType(
                ObstacleAvoidanceType.BRAKE,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("obstacle avoidance set to brake")
                        activity.addUpdate("obstacle avoidance set to brake")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("could not set obstacle avoidance to brake $error")
                        activity.addUpdate("could not set obstacle avoidance to brake $error")
                    }
                })
        } else {
            PerceptionManager.getInstance().setObstacleAvoidanceType(
                ObstacleAvoidanceType.CLOSE,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("obstacle avoidance set to close")
                        activity.addUpdate("obstacle avoidance set to close")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("could not set obstacle avoidance to close $error")
                        activity.addUpdate("could not set obstacle avoidance to close $error")
                    }
                })
        }
        //TODO: test if needed
        //set regulation region and operator id
//        UASRemoteIDManager.getInstance().setUASRemoteIDAreaStrategy(AreaStrategy.EUROPEAN_STRATEGY)
//        UASRemoteIDManager.getInstance().setOperatorRegistrationNumber("", object : CommonCallbacks.CompletionCallback {
//            override fun onSuccess() {
//                ToastUtils.showToast("Remote ID set successfully")
//                activity.addUpdate("Remote ID set successfully")
//            }
//            override fun onFailure(error: IDJIError) {
//                ToastUtils.showToast("could not set Remote ID $error")
//                activity.addUpdate("could not set Remote ID $error")
//            }
//        })

        val connectivity = KeyTools.createKey(FlightControllerKey.KeyConnection).get()
        activity.addUpdate("KeyConnection is currently: $connectivity")
        val gpsSignal = KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel).get()?.value()
        activity.addUpdate("GPS signal level is currently: $gpsSignal")
    }

    fun testFlightMinimalInit() {
        VirtualStickManager.getInstance().init()
        VirtualStickManager.getInstance()
            .enableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("VirtualStick enabled")
                    activity.addUpdate("VirtualStick enabled")
                    FlightUtility.virtualStickEnabled = true
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("Error enabling VirtualStick $error")
                    activity.addUpdate("Error enabling VirtualStick $error")
                }
            })
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true)
        activity.addUpdate("VirtualStickAdvancedMode enabled")
    }

    private fun preCommandCheck(): Boolean {
        //check connectivity
        //check battery
        //check GPSSignalLevel
        return (KeyTools.createKey(FlightControllerKey.KeyConnection).get() == true &&
                KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel).get()?.value()!! >= 2)

//        return true
    }

    fun takeOff() {
//        takeOffLocation = KeyManager.getInstance()
//            .getValue(
//                KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D),
//                LocationCoordinate3D(0.0, 0.0, 0.0)
//            )

        //takeoff
        FlightControllerKey.KeyStartTakeoff.create().action({
            ToastUtils.showToast("takeoff successful")
            activity.addUpdate("takeoff successful")
            //Set home location
//            setHomeToCurrentLocation()
            //TODO: add delay until takeoff is complete
        }, { e: IDJIError ->
            ToastUtils.showToast("could not takeoff $e")
            activity.addUpdate("could not takeoff $e")
        })
    }

    private fun setHomeToCurrentLocation() {
        FlightControllerKey.KeySetHomeLocationUsingAircraftCurrentLocation.create().action({
            ToastUtils.showToast("home location set successfully.")
            activity.addUpdate("home location set successfully.")
        }, { e: IDJIError ->
            ToastUtils.showToast("could not set home location,$e")
            activity.addUpdate("could not set home location,$e")
        })
    }

    fun land() {
        FlightControllerKey.KeyStartAutoLanding.create().action({
            ToastUtils.showToast("landing successful")
            activity.addUpdate("landing successful")
        }, { e: IDJIError ->
            ToastUtils.showToast("could not land $e")
            activity.addUpdate("could not land $e")
        })
    }

    fun takePhoto() {
        capture(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("took a photo successfully")
                activity.addUpdate("took a photo successfully")
                //TODO: handle and upload image https://developer.dji.com/mobile-sdk/documentation/android-tutorials/MediaManagerDemo.html
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("could not take a photo $error")
                activity.addUpdate("could not take a photo $error")
            }
        })
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

    fun getAltitude(): Double {
        return getLocation3D().altitude
    }

    fun getCurrentHeading(): Double {
        //return KeyManager.getInstance()
        //    .getValue(KeyTools.createKey(FlightControllerKey.KeyCompassHeading), Double())
        activity.addUpdate("lat: ${getLatitude()}, long: ${getLongitude()}, alt: ${getAltitude()}")
        val velocity = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity))
        activity.addUpdate("Vx: ${velocity?.x}, Vy: ${velocity?.y}, Vz: ${velocity?.z}")
        return FlightControllerKey.KeyCompassHeading.create().get(200.0)
    }

    /*
        【Prerequisite】
        The connection between the app and RC is normal.
        The RC is under normal mode.
        The aircraft is not flying with an automated task such as waypointMission or RTH function.
     */
    private fun initVirtualStick() {
        VirtualStickManager.getInstance().init()
        VirtualStickManager.getInstance()
            .enableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("VirtualStick enabled")
                    activity.addUpdate("VirtualStick enabled")
                    FlightUtility.virtualStickEnabled = true
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("Error enabling VirtualStick")
                    activity.addUpdate("could not enable VirtualStick $error")
                }
            })
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true)
        Thread {
            //TODO: non infinite loop
            //TODO: send parameters at a frequency between 5Hz and 25Hz (5Hz recommended)
            //while (true) {
                VirtualStickManager
                    .getInstance()
                    .sendVirtualStickAdvancedParam(FlightUtility.virtualStickParam)
            //}
        }
    }

/*
    pitch: Double       ANGLE: [-30, 30]        VELOCITY: [-23, 23]
    roll: Double        ANGLE: [-30, 30]        VELOCITY: [-23, 23]
    yaw: Double         ANGLE: [-180, 180]      ANGULAR_VELOCITY: [-100, 100]
    vThrottle: Double   HEIGHT: [0, 5000]       VELOCITY: [-6, 6]
 */
    fun adjustFlightParameters(pitch: Double, roll: Double, yaw: Double, vThrottle: Double) {
        //TODO: Assert min max values
        assert(pitch >= -30 && pitch <= 30 &&
                roll >= -30 && roll <= 30 &&
                yaw >= -180 && yaw <= 180 &&
                vThrottle >= -6 && vThrottle <= 6)
        virtualStickParam.pitch = pitch
        virtualStickParam.roll = roll
        virtualStickParam.yaw = yaw
        virtualStickParam.verticalThrottle = vThrottle
    //    if (!FlightUtility.virtualStickEnabled) {
    //        initVirtualStick()
    //    }
        if (!flightParametersTransmitting) {
            flightParametersTransmitting = true
            transmitFlightParameters()
            ToastUtils.showToast("transmission started")
            activity.addUpdate("transmission started")
        }
        ToastUtils.showToast("parameters adjusted")
        activity.addUpdate("parameters adjusted")
        iterations = 0

    }
    private var iterations: Int = 0
    private fun transmitFlightParameters() {
        Timer().scheduleAtFixedRate( object : TimerTask() {
            override fun run() {
                //TODO: log position and speed vector
//                ToastUtils.showToast(iterations.toString())
                if (iterations < 10) {
                    iterations++
//                    Log.i(TAG, "transmitting")
                    activity.addUpdate("transmitting virtualStickPara")
                    VirtualStickManager
                        .getInstance()
                        .sendVirtualStickAdvancedParam(virtualStickParam)
                }
            }
        }, 0, 200)
    }

    fun setActivityObject(activity: TestFlightActivity) {
        this.activity = activity
    }
}
