package com.l3s.dronegpt

import android.annotation.SuppressLint
import android.location.Location
import com.l3s.dronegpt.data.database.Experiment
import com.l3s.dronegpt.ui.activity.DroneGPTActivity
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

// Event sequence for each flight/experiment:
// set maxFlightHeight and maxFlightDistance -> enable automatic return to home when battery is low ->
// Set RTH Height = selectedExperiment.flightHeight ->
// Takeoff -> set home to current location
// -> Turn on and setup virtualstick -> adjust virtualStickParam with vThrottle = selectedExperiment.flightHeight ->
// execute ChatGPTs script ->
// Set RTH Height -> return Home -> land -> save all updates to selectedExperiment.flightLogs


/**
 * This object handles all calls of DJI MSDK functions
 */
object FlightUtility {
    private lateinit var activity: DroneGPTActivity
    private val TAG: String = "FlightUtility"
    private var connected: Boolean = false
    private lateinit var takeOffLocation: LocationCoordinate3D

    // When enabled, allows movement commands of ChatGPT's script
    var virtualStickEnabled: Boolean = false

    //resets before each experiment
    private var flightParametersTransmitting: Boolean = false
    //resets before each experiment
    private lateinit var experimentInProgress: Experiment



    /* parameter stated by DJI MSDK documentation
        pitch: Double       ANGLE: [-30, 30]        VELOCITY: [-23, 23]
        roll: Double        ANGLE: [-30, 30]        VELOCITY: [-23, 23]
        yaw: Double         ANGLE: [-180, 180]      ANGULAR_VELOCITY: [-100, 100] (GROUND: positive ->, negative <-)
        vThrottle: Double   HEIGHT: [0, 5000]       VELOCITY: [-6, 6]
        VerticalControlMode.VELOCITY/POSITION/UNKNOWN
        RollPitchControlMode.VELOCITY/ANGLE/POSITION/UNKNOWN
        YawControlMode.ANGULAR_VELOCITY/ANGLE/UNKNOWN
        FlightCoordinateSystem.GROUND/BODY/UNKNOWN
    */
    // In the virtual stick advanced mode, obstacle avoidance is supported only when
    // the vertical control Mode is velocity mode,
    // the yaw control mode is angular velocity mode,
    // and the roll pitch control mode is velocity mode.
    // resets before each experiment
    // Parameter object transmitted to the drone. 
    // Velocity parameters of this object are adjusted by ChatGPT's script.
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

    // Maximum drone's altitude in meters
    // DJI MSDK value range is [20, 1500]
    private const val altitudeLimit: Int = 40

    // Maximum distance between aircraft and home point in meters
    // DJI MSDK value range is [15, 8000]
    private const val defaultDistanceLimit: Int = 320

    // Resets before each experiment
    private var distanceLimitReached: Boolean = false

    private var homeLocation: Location = Location("home")

    fun init() {
        // connection status
        if (KeyManager.getInstance().getValue(
                KeyTools.createKey(FlightControllerKey.KeyConnection),
            ) == true
        ) {
            connected = true
        }


    }

    // Resets parameters. Called when selecting an experiment
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

    // Sets activity object for logging purposes
    fun setActivityObject(activity: DroneGPTActivity) {
        this.activity = activity
    }

    // Sets the selected experiment
    fun setExperiment(experiment: Experiment) {
        experimentInProgress = experiment
        resetState()
        setReturnHomeAltitude()
    }

    // Calls flight preparation functions and sets up listeners for flight logs
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

            // set max flight altitude
            KeyTools.createKey(FlightControllerKey.KeyHeightLimit).set(altitudeLimit, {
                activity.addUpdate("max flight altitude set to $altitudeLimit")
            }, {
                activity.addUpdate("could not set max flight altitude to $altitudeLimit $it")
            })

            // enable max distance to home point
            KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled).set(true, {
                activity.addUpdate("enabled distance limit")
            }, {
                activity.addUpdate("could enable distance $it")
            })

            // set max distance to home point
            setDistanceLimit(defaultDistanceLimit)

            // setup listener for when the drone reaches distance limit
            KeyTools.createKey(FlightControllerKey.KeyIsNearDistanceLimit).listen(activity, false) {
                if (it == true && !distanceLimitReached) {
                    activity.addUpdate("distance limit of $defaultDistanceLimit meters reached.")
                    distanceLimitReached = true
                } else if (it == false && distanceLimitReached) {
                    distanceLimitReached = false
                }
            }

            // enable automatic return to home when battery is low
            KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHEnabled).set(true, {
                activity.addUpdate("lowBattery return to home enabled")
            }, {
                activity.addUpdate("could not enable lowBattery return to home $it")
            })
            // setup listener for when the battery gets too low
            // aircraft performs auto return home in that case
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


            // sets fail safe behavior to auto return home.
            // When the remote controller loses connection with the aircraft, the aircraft will perform according to the set fail safe behavior.
            KeyTools.createKey(FlightControllerKey.KeyFailsafeAction).set(FailsafeAction.GOHOME, {
                activity.addUpdate("successfully set auto return home as FailsafeAction")
            }, {
                activity.addUpdate("could not set auto return home as FailsafeAction $it")
            })

            // setup listener for when the aircraft is about to perform fail safe behavior
            KeyTools.createKey(FlightControllerKey.KeyIsFailSafe).listen(activity, false) {
                if (it == true) {
                    activity.addUpdate("aircraft is out of control. performing fail safe behavior")
                }
            }
            setReturnHomeAltitude()
        }
    }

    /*
        【DJI MSDK Prerequisite of this function】
        The connection between the app and RC is normal.
        The RC is under normal mode.
        The aircraft is not flying with an automated task such as waypointMission or RTH function.
     */
    // Enables movement commands of ChatGPT's script
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

    // Used to disables all movement commands of ChatGPT's scripts
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

    // Sets altitude of the automated return home flight
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

    // Sets distance limit from home, which is usually the takeoff location
    fun setDistanceLimit(distanceLimit: Int) {
        KeyTools.createKey(FlightControllerKey.KeyDistanceLimit).set(distanceLimit, {
            activity.addUpdate("distance limit set to $distanceLimit")
        }, {
            activity.addUpdate("could not set distance limit to $distanceLimit: $it")
        })
    }

    // Retrieves the full coordinates of the drone
    private fun getLocation3D(): LocationCoordinate3D {
        return KeyManager.getInstance()
            .getValue(
                KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D),
                LocationCoordinate3D(0.0, 0.0, 0.0)
            )
    }

    private fun getLatitude(): Double {
        return getLocation3D().latitude
    }

    private fun getLongitude(): Double {
        return getLocation3D().longitude
    }

    // Called by ChatGPT
    // Retrieves the current X coordinate (East-West) of the drone in meters with takeoff location as 0.
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

    // Called by ChatGPT
    // Retrieves the current Y coordinate (North-South) of the drone in meters with takeoff location as 0.
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

    // Called by ChatGPT
    // Retrieves the drone's distance from takeoff location in meters
    fun getDistanceToHome(): Double {
        val threeDLocation = getLocation3D()
        val currentLocation = Location("currentLocation")
        currentLocation.latitude = threeDLocation.latitude
        currentLocation.longitude = threeDLocation.longitude
        return homeLocation.distanceTo(currentLocation).toDouble()
    }

    // Called by ChatGPT
    // Retrieves the current drone's heading. The north is 0 degrees, the east is 90 degrees. 
    // The returned value range is [-180,180]. Returns 200 when compass heading value couldn't be retrieved
    fun getCompassHeading(): Double {
        val heading = FlightControllerKey.KeyCompassHeading.create().get(200.0)
        if (heading == 200.0) {
            activity.addUpdate("Could not retrieve heading. Receiving default value instead")
        }
        return heading
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
        //takeoff
        FlightControllerKey.KeyStartTakeoff.create().action({
            activity.addUpdate("takeoff successful")
        }, { e: IDJIError ->
            activity.addUpdate("could not takeoff $e")
        })
    }

    // Increases the drone's altitude to the flight height of the selected experiment
    fun elevateToExperimentHeight() {
        if (!virtualStickEnabled) {
            enableVirtualStick()
        }
        virtualStickParam.verticalThrottle = experimentInProgress.flightHeight.toDouble()
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

    // Initiates automated return home flight.
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

    // Saves experiment data
    private fun endFlight() {
        activity.saveAndResetFlightLogs(experimentInProgress.id)
        activity.exportDataToJsonFile()
    }

    // Called by ChatGPT
    // Images are taken only when VirtualStickEnabled is true to reduce unwanted images
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

/*  DJI MSDK min/max parameters:
    pitch: Double       ANGLE: [-30, 30]        VELOCITY: [-23, 23]
    roll: Double        ANGLE: [-30, 30]        VELOCITY: [-23, 23]
    yaw: Double         ANGLE: [-180, 180]      ANGULAR_VELOCITY: [-100, 100]
    vThrottle: Double   HEIGHT: [0, 5000]       VELOCITY: [-6, 6]
 */

    // Called by ChatGPT
    // Adjusts the velocity parameters being transmitted to the drone
    // The allowed velocity range is set in ChatGPTUtility object.
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

    // Transmits the flight parameters to the drone at the 5Hz recommended rate by DJI MSDK documentation
    private fun transmitFlightParameters() {
        Timer().scheduleAtFixedRate( object : TimerTask() {
            override fun run() {
                if (virtualStickEnabled) {
                    try {
                        VirtualStickManager
                            .getInstance()
                            .sendVirtualStickAdvancedParam(virtualStickParam)
                    } catch (e: IOException) {
                        activity.addUpdate("error sending parameters: $e")
                    }
                }
            }
        }, 0, 200)
    }

    // Called by ChatGPT
    // Calls automated return home function
    fun returnHomeAndEndFlight() {
        returnHome()
        endFlight()
    }

}
