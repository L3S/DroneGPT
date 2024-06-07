package com.l3s.dronegpt

import org.luaj.vm2.Globals

//DEPRECATED!!
class ManagedThread(private val globals: Globals, private val script: String) : Thread() {
    private var shouldPause: Boolean = false
    private var pauseDuration: Long = 0

//    @Synchronized
//    fun pauseExecution(millis: Long) {
//        if (millis > 0) {
////            Log.d("setupLuaEnv", "pauseExecution called")
//            pauseDuration = millis
//            shouldPause = true
//        }
//    }

//    override fun run() {
//        val pattern = Regex("pauseThread\\((\\d+)\\)")
//        val matches = pattern.findAll(script)
////        val parts = pattern.split(script).filterNot { it.isBlank() }
//        val scriptParts = pattern.split(script).filterNot { it.isBlank() } // Assuming your script parts are split by "pauseThread"
//        try {
//            scriptParts.forEachIndexed { index, part ->
//                if (index > 0 && shouldPause) {  // Check if a pause was requested
//                    Log.d("ManagedThread", "going to pause")
//                    sleep(pauseDuration)  // Handle the pause
//                    Log.d("ManagedThread", "paused")
//                    shouldPause = false  // Reset the pause flag
//                }
//                globals.load(part).call()  // Execute each part of the script
//            }
//        } catch (e: InterruptedException) {
//            println("Thread interrupted during pause: $pauseDuration ms")
//        } catch (e: Exception) {
//            println("Error during script execution: ${e.message}")
//        }
//    }

    override fun run() {
        // Regex pattern to extract script commands and pause durations
        val pattern = Regex("pause_script_execution\\((\\d+)\\)")
        val matches = pattern.findAll(script)
        val parts = pattern.split(script).filterNot { it.isBlank() }

        // Iterate over script parts and associated pauses
        parts.forEachIndexed { index, part ->
            globals.load(part).call()  // Execute current script part
            if (index < parts.size - 1) {  // Check if there are more parts that need a pause before execution
                val match = matches.elementAt(index)
                pauseDuration = match.groupValues[1].toLong()  // Extract the pause duration from regex match
                shouldPause = true
                if (shouldPause) {
                    sleep(pauseDuration)  // Pause execution for the specified duration
                    shouldPause = false  // Reset pause flag after pausing
                }
            }
        }
    }
}

