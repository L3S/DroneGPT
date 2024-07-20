package com.l3s.dronegpt

import com.l3s.dronegpt.data.database.Experiment

object ChatGPTUtility {
    const val gpt3Model: String = "gpt-3.5-turbo"
    const val gpt4Model: String = "gpt-4o"


    val defaultCircleAreaDescription: String = "circle with radius of 100 meters and center at the following coordinates (x=0,y=0)"
    val defaultRectangleAreaDescription: String = "rectangle with vertices at the following coordinates (x=, y=), (x=, y=), (x=, y=), and (x=, y=)"
    var areaDescription: String = ""
    var flightHeight: Int = 0


    var gimbalAngle: Int = -90
    const val cameraFOV = 82.1

    const val flightDuration = 8
    const val minPitchRollValue = -6
    const val maxPitchRollValue = 6
    const val minYawValue = -100
    const val maxYawValue = 100

    val experimentsPreset = arrayOf(
        // Exp1, 3.5, circle, centered
        Experiment(gpt3Model, "circle with radius of 130 meters and center at the following coordinates (x=0,y=0)", 20),
        // Exp2, 4o, circle, centered
        Experiment(gpt4Model, "circle with radius of 130 meters and center at the following coordinates (x=0,y=0)", 20),
        // Exp3, 3.5, rectangle, centered
        Experiment(gpt3Model, "rectangle with vertices at the following coordinates (x=80, y=160), (x=80, y=-160), (x=-80, y=-160), and (x=-80, y=160)", 20),
        // Exp4, 4o, rectangle, centered
        Experiment(gpt4Model, "rectangle with vertices at the following coordinates (x=80, y=160), (x=80, y=-160), (x=-80, y=-160), and (x=-80, y=160)", 20),

        // Exp5, 3.5, circle, offset
        Experiment(gpt3Model, "circle with radius of 130 meters and center at the following coordinates (x=100,y=0)", 20),
        // Exp6, 4o, circle, offset
        Experiment(gpt4Model, "circle with radius of 130 meters and center at the following coordinates (x=100,y=0)", 20),
        // Exp7, 3.5, rectangle, offset
        Experiment(gpt3Model, "rectangle with vertices at the following coordinates (x=140, y=160), (x=140, y=-160), (x=-20, y=-160), and (x=-20, y=160)", 20),
        // Exp8, 4o, rectangle, offset
        Experiment(gpt4Model, "rectangle with vertices at the following coordinates (x=140, y=160), (x=140, y=-160), (x=-20, y=-160), and (x=-20, y=160)", 20)

    )



    val prompt: String
        get() = """
        Imagine you are a drone operator and your job is to develop a Lua script to control a drone during a survey mission over a designated area. The script must be complete and ready for immediate execution, with no adjustments needed post-delivery. The mission requires the drone to stay within defined boundaries and take photographs for analysis.

        Note: All functions listed below are already implemented. You should call these functions directly in your script.
        
        Coordinate System:
        The coordinate system used is a Cartesian plane with the x-axis oriented east (positive) to west (negative) and the y-axis oriented north (positive) to south (negative). The origin of the coordinate system is the drone's starting location. All coordinates are measured in meters.
        
        Flight Objective:
        Thoroughly survey the designated area and capture photographs periodically to ensure adequate area coverage without unnecessary overlap.

        Drone Flight Constraints:
        - The drone must stay within designated boundaries, which for this task is a $areaDescription
        - The total flight duration must not exceed $flightDuration minutes.
        - The drone will maintain a constant height of $flightHeight meters, has a gimbal with a pitch angle of $gimbalAngle degrees and a $cameraFOV FOV camera, which influence the frequency of photo captures.

        Functions:
        - `adjust_flight_parameters(xVelocity, yVelocity, yaw)`: controls the drone's flight direction. `xVelocity` with value range [$minPitchRollValue, $maxPitchRollValue] controls velocity along the x-axis (positive values move the drone east, negative west), and `yVelocity` with value range [$minPitchRollValue, $maxPitchRollValue] controls velocity along the y-axis (positive values move the drone north, negative south). `yaw` with value range [$minYawValue, $maxYawValue] changes the drone's angular velocity (positive values rotate the drone clockwise, negative counterclockwise)
        `xVelocity` and `yVelocity` are both in meters/s and yaw is in degrees/s. The drone maintains these flight parameters until they are changed by another call to this function. 
        - `pause_script_execution(duration)`: pauses the script execution for a specified duration in seconds. This function is typically used after setting flight parameters to maintain the drone’s current direction and speed for the specified period before the next script command is executed.
        - `get_distance_to_origin()`: retrieves the distance in meters from the drone’s current location to the origin of the coordinate system.
        - `get_x_coordinate()`, `get_y_coordinate()`: retrieves the x-coordinate and y-coordinate in meters of the drone's current location.
        - `get_compass_heading()`: retrieves the compass heading as double. The north is 0 degrees, the east is 90 degrees. The value range is [-180,180]. Returns 200 when compass heading value couldn't be retrieved.
        - `take_photo()`: instructs the drone to capture and save a photograph. The script should plan these captures to balance coverage with storage limitations
        - `end_flight()`: instructs the drone to return to its starting point and terminate the flight.

        Instructions:
        Develop the Lua script to ensure the drone remains within the boundary throughout the flight. Select an efficient and effective pattern for surveying the designated area. Implement error handling for failed compass heading retrievals by attempting a retry before any critical operations. The script should respect the $flightDuration minute flight duration limit, utilizing the pause_script_execution function to control the timing of flight adjustments.
    """.trimIndent()

    fun buildPrompt(flightHeight: Int, areaDescription: String): String {
        this.flightHeight = flightHeight
        this.areaDescription = areaDescription
        return prompt
    }

    fun parseCode(chatGptResponse: String): String {
        val regex = "```([\\s\\S]+?)```".toRegex()
        if (!regex.containsMatchIn(chatGptResponse))
            return "Could not find any code in this message"
        val codeLines = ArrayList<String>()
        var inCodeBlock = false
        for(line in chatGptResponse.split('\n')) {
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                if (!inCodeBlock)
                    codeLines.add("\n")
                continue
            }
            if (inCodeBlock)
                codeLines.add(line)
        }
        return codeLines.joinToString(separator = "\n")
    }
}