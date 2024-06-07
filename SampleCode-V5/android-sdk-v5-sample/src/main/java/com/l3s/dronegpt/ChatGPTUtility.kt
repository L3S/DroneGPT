package com.l3s.dronegpt

object ChatGPTUtility {
    const val gpt3Model: String = "gpt-3.5-turbo"
    const val gpt4Model: String = "gpt-4-turbo"
    //TODO: optimize description by chatgpt
    val defaultCircleAreaDescription: String = "circle with radius of 100 meters and center 70 meters north and 70 meters east of drone's starting location"
//    val defaultRectangleAreaDescription: String = "rectangle with the boundaries 50 meters to the north, 50 meters to the east, 50 meters to the south and 50 meters to the west of the drones starting location"
    //TODO: ask on Monday if this description is optimal
    val defaultRectangleAreaDescription: String = "rectangle with the following corners: directly at the current location, 50 meters east, 50 meters east and 30 meters north, and 30 meters north"
    var areaDescription: String = ""
    var flightHeight: Int = 0
    //Imagine you are a drone operator and your job is to program a drone using Lua language to perform a survey mission over a designated area. The mission requires the drone to stay within defined boundaries and take photographs for analysis.
    val prompt: String
        get() = """
        Imagine you are a drone operator and your job is to develop a Lua script to control a drone during a survey mission over a designated area. The script must be complete and ready for immediate execution, with no adjustments needed post-delivery. The mission requires the drone to stay within defined boundaries and take photographs for analysis.

        Note: All functions listed below are already implemented. You should call these functions directly in your script

        Flight Objective:
        Thoroughly survey the designated area and capture photographs periodically to ensure adequate area coverage without unnecessary overlap.

        Drone Flight Constraints:
        - The drone must stay within designated boundaries, which for this task is a $areaDescription
        - The total flight duration must not exceed 15 minutes.
        - The drone will maintain a constant height of $flightHeight meters and has a camera with a Field of View (FOV) of 82.1 degrees, which influence the frequency of photo captures.

        Functions:
        - `adjust_flight_parameters(pitch, roll, yaw)`: controls the drone's flight direction. `pitch` with value range [-23, 23] controls east-west velocity (positive values move the drone east, negative west), and `roll` with value range [-23, 23] controls north-south velocity (positive values move the drone north, negative south). `yaw` with value range [-100, 100] changes the drone's angular velocity (positive values rotate the drone clockwise, negative counterclockwise)
        `pitch` and `roll` are both in meters/s and all three parameters are of type double. The drone maintains these flight parameters until they are changed by another call to this function. 
        - `pause_script_execution(duration)`: pauses the script execution for a specified duration in seconds. This function is typically used after setting flight parameters to maintain the drone’s current direction and speed for the specified period before the next script command is executed.
        - `get_latitude()`, `get_longitude()`: retrieves the current latitude and longitude as doubles.
        - `get_compass_heading()`: retrieves the compass heading as double. The north is 0 degrees, the east is 90 degrees. The value range is [-180,180]. Returns 200 when compass heading value couldn't be retrieved.
        - `take_photo()`: instructs the drone to capture and save a photograph. The script should plan these captures to balance coverage with storage limitations
        - `end_flight()`: instructs the drone to return to its starting point and terminate the flight.

        Instructions:
        Develop the Lua script to ensure the drone remains within the boundary throughout the flight. Select an efficient and effective pattern for surveying the designated area. Implement error handling for failed compass heading retrievals by attempting a retry before any critical operations. The script should respect the 15-minute flight duration limit, utilizing the pause_script_execution function to control the timing of flight adjustments.
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