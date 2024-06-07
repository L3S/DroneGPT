package com.l3s.dronegpt.ui

//import kotlinx.android.synthetic.main.dronegpt_main.test_flight_updates
import ExperimentAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.l3s.dronegpt.FlightUtility
import com.l3s.dronegpt.ScriptErrorListener
import com.l3s.dronegpt.ScriptManager
import com.l3s.dronegpt.data.database.Experiment
import dji.sampleV5.aircraft.R
import dji.sdk.keyvalue.value.camera.GeneratedMediaFileInfo
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import kotlinx.android.synthetic.main.dronegpt_main.btn_create_all_experiments
import kotlinx.android.synthetic.main.dronegpt_main.btn_create_experiment
import kotlinx.android.synthetic.main.dronegpt_main.btn_disable_virtual_stick
import kotlinx.android.synthetic.main.dronegpt_main.btn_enable_virtual_sticks
import kotlinx.android.synthetic.main.dronegpt_main.btn_execute_script
import kotlinx.android.synthetic.main.dronegpt_main.btn_experiment_height
import kotlinx.android.synthetic.main.dronegpt_main.btn_export_data
import kotlinx.android.synthetic.main.dronegpt_main.btn_get_heading
import kotlinx.android.synthetic.main.dronegpt_main.btn_initialize_all
import kotlinx.android.synthetic.main.dronegpt_main.btn_land
import kotlinx.android.synthetic.main.dronegpt_main.btn_move_backward
import kotlinx.android.synthetic.main.dronegpt_main.btn_move_forward
import kotlinx.android.synthetic.main.dronegpt_main.btn_move_left
import kotlinx.android.synthetic.main.dronegpt_main.btn_move_right
import kotlinx.android.synthetic.main.dronegpt_main.btn_return_home
import kotlinx.android.synthetic.main.dronegpt_main.btn_rotate_left
import kotlinx.android.synthetic.main.dronegpt_main.btn_rotate_right
import kotlinx.android.synthetic.main.dronegpt_main.btn_save_logcat
import kotlinx.android.synthetic.main.dronegpt_main.btn_take_off
import kotlinx.android.synthetic.main.dronegpt_main.btn_take_photo
import kotlinx.android.synthetic.main.dronegpt_main.experiment_area_description
import kotlinx.android.synthetic.main.dronegpt_main.experiment_display_name
import kotlinx.android.synthetic.main.dronegpt_main.experiment_height
import kotlinx.android.synthetic.main.dronegpt_main.experiment_model
import kotlinx.android.synthetic.main.dronegpt_main.flight_logs_switch
import kotlinx.android.synthetic.main.dronegpt_main.fragment_chat_container
import kotlinx.android.synthetic.main.dronegpt_main.test_flight_updates
import kotlinx.android.synthetic.main.dronegpt_main.updates_scroll_view
import java.io.File
import java.io.IOException
import java.time.LocalDateTime


class DroneGPTActivity : AppCompatActivity(), ScriptErrorListener {
//    protected val msdkCommonOperateVm: MSDKCommonOperateVm by viewModels()
//    private val testToolsVM: TestToolsVM by viewModels()
    private var luajExecution: Boolean = false
    private var updates: StringBuilder = StringBuilder().appendLine("updates:")
    private val viewModel: DroneGPTViewModel by viewModels()
    private var experimentsList = ArrayList<Experiment>()
    private lateinit var selectedExperiment: Experiment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dronegpt_main)
//        window.decorView.apply {
//            systemUiVisibility =
//                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
//                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//        }
//
//        window.decorView.setOnSystemUiVisibilityChangeListener() {
//            if (it and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
//                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
//                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
//                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
//                        View.SYSTEM_UI_FLAG_FULLSCREEN or
//                        View.SYSTEM_UI_FLAG_IMMERSIVE or
//                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
//            }
//        }
//
//        loadTitleView()
//
//        DJIToastUtil.dJIToastLD = testToolsVM.djiToastResult
//        testToolsVM.djiToastResult.observe(this) { result ->
//            result?.msg?.let {
//                ToastUtils.showToast(it)
//            }
//        }
//
//
//        loadPages()
        ScriptManager.setupLuaEnvironment()
        ScriptManager.errorListener = this
//        initRadioButtons()
        initBtnClickListener()
        updateText()
        FlightUtility.setActivityObject(this)

        fragment_chat_container.visibility = View.GONE
        flight_logs_switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updates_scroll_view.visibility = View.VISIBLE
                fragment_chat_container.visibility = View.GONE
            } else {
                updates_scroll_view.visibility = View.GONE
                fragment_chat_container.visibility = View.VISIBLE
            }
        }


        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_chat_container, ChatFragment())
            .commit()
        val adapter = ExperimentAdapter {experiment ->
            // This lambda function is triggered when an experiment is clicked
//            ToastUtils.showToast("experiment ${it.id} selected in activity")
            viewModel.setSelectedExperiment(experiment)
            selectedExperiment = experiment
//            updates_scroll_view.visibility = View.GONE
//            fragment_chat_container.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).post {
//                test_flight_updates.text = updates.toString()
                experiment_display_name.text = "Experiment ${experiment.id.toString()}"
                experiment_model.text = "${experiment.openaiModel}"
                experiment_height.text = "Flight height: ${experiment.flightHeight} meters"
                experiment_area_description.text = experiment.areaDescription
            }
        }

        findViewById<RecyclerView>(R.id.experiments_recycler_view).apply {
            layoutManager = LinearLayoutManager(this@DroneGPTActivity)
            this.adapter = adapter
        }

        viewModel.experiments.observe(this, {
            adapter.submitList(it)
        })
    }

//        private fun initRadioButtons() {
//        radio_java.setOnCheckedChangeListener { buttonView, isChecked ->
//            if (isChecked) {
//                this.luajExecution = false
//                ToastUtils.showToast("Java execution selected")
//                initBtnClickListener()
//            }
//        }
//        radio_luaj.setOnCheckedChangeListener { buttonView, isChecked ->
//            if (isChecked) {
//                this.luajExecution = true
//                ToastUtils.showToast("LuaJ execution selected")
//                initBtnClickListener()
//            }
//        }
//    }

    private fun initBtnClickListener() {
        btn_enable_virtual_sticks.setOnClickListener {
//            FlightUtility.initializeFlight(false)
            FlightUtility.enableVirtualStick()
        }
        btn_disable_virtual_stick.setOnClickListener {
            FlightUtility.disableVirtualStick()
        }
        btn_initialize_all.setOnClickListener {
            FlightUtility.initializeFlight(true)
        }
        btn_export_data.setOnClickListener {
            exportDataToJsonFile()
        }
        btn_take_off.setOnClickListener {
            FlightUtility.takeOff()
        }
        btn_land.setOnClickListener {
            FlightUtility.land()
        }
        btn_return_home.setOnClickListener {
            FlightUtility.returnHome()
        }
        btn_experiment_height.setOnClickListener {
            FlightUtility.setExperiment(selectedExperiment)
            FlightUtility.elevateToExperimentHeight()
        }

        btn_create_experiment.setOnClickListener {
            val intent = Intent(this, ExperimentFormActivity::class.java)
            startActivity(intent)
        }
        btn_create_all_experiments.setOnClickListener {
            //TODO: implement
        }
        btn_save_logcat.setOnClickListener {
            try {
                val pid = android.os.Process.myPid()
                val fileName = "dronegpt_app_logcat_${System.currentTimeMillis()}.txt"
                val outputFile = File(getExternalFilesDir(null), fileName)

                // Only capture logs for this PID
                Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-f", outputFile.absolutePath, "*:V"))
//                    Runtime.getRuntime().exec("logcat -d *:V | grep ' $pid ' > ${outputFile.absolutePath}")
                addUpdate("logcat dumped at ${outputFile.absolutePath}")
            } catch (e: IOException) {
                addUpdate("LogCapture Error saving log file $e")
            }
        }
//        btn_take_off.setOnClickListener {
//            ScriptManager.executeLuaScript("take_off()")
//        }
//        btn_land.setOnClickListener {
//            ScriptManager.executeLuaScript("end_flight()")
//        }
        btn_move_forward.setOnClickListener {
            ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, 23.0,0.0)")
        }
        btn_move_backward.setOnClickListener {
            ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, -23.0,0.0)")
        }
        btn_move_right.setOnClickListener {
            ScriptManager.executeLuaScript("adjust_flight_parameters(23.0, 0.0,0.0)")
        }
        btn_move_left.setOnClickListener {
            ScriptManager.executeLuaScript("adjust_flight_parameters(-23.0, 0.0,0.0)")
        }
        btn_rotate_left.setOnClickListener {
            ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, 0.0, -50.0)")
        }
        btn_rotate_right.setOnClickListener {
            ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, 0.0, 50.0)")
        }
        btn_get_heading.setOnClickListener {
            ScriptManager.executeLuaScript("print(get_compass_heading())")
        }
        btn_take_photo.setOnClickListener {
            ScriptManager.executeLuaScript("take_photo()")
        }
        btn_execute_script.setOnClickListener {try {
            ScriptManager.executeLuaScript("""
                adjust_flight_parameters(0.0, 10,0.0)
                pause_script_execution(0.30)
                adjust_flight_parameters(0.0, -10.0,0.0)
                pause_script_execution(10)
                take_photo()
                adjust_flight_parameters(0.0, 0.0, -50.0)
                pause_script_execution(10)
                adjust_flight_parameters(0.0, 0.0, 50.0)
            """)
            } catch (e: Exception) {
                addUpdate("Error executing script: ${e.localizedMessage}")
            }
        }
    }

    fun exportDataToJsonFile() {
        viewModel.exportDataToJsonFile(getExternalFilesDir(null))
    }

    private fun updateText() {
        Handler(Looper.getMainLooper()).post {
            test_flight_updates.text = updates.toString()
        }
    }

    fun addUpdate(update: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updates.append("> ${LocalDateTime.now()} ")
        } else {
            updates.append("> ")
        }
        if (this::selectedExperiment.isInitialized) {
            Log.d("FlightUtility", "Experiment ${selectedExperiment.id}: $update")
            updates.appendLine("Experiment ${selectedExperiment.id}: $update")
        } else {
            Log.d("FlightUtility", update)
            updates.appendLine(update)
        }

        updateText()
    }

//    fun setLogsPathToExperiment(experimentId: Int, flightRecordPath: String, flightCompactLogsPath: String) {
//        viewModel.setLogsPath(experimentId, flightRecordPath, flightCompactLogsPath)
//    }

    fun createImage(experiment: Experiment, generatedImageInfo: GeneratedMediaFileInfo?, location3D: LocationCoordinate3D) {
        if (generatedImageInfo != null) {
            viewModel.createImage(experiment.id, generatedImageInfo, location3D)
            addUpdate("image from ${generatedImageInfo.createTime.hour}:${generatedImageInfo.createTime.minute} saved successfully")
        }
    }

    fun saveAndResetFlightLogs(experimentId: Int) {
        viewModel.setExperimentFlightLogs(experimentId, updates.toString())
        updates = StringBuilder().appendLine("updates:")
    }

    override fun onError(error: String) {
        runOnUiThread {
            addUpdate("encountered error: $error")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ScriptManager.errorListener = null
//        FlightUtility.setActivityObject(null)
    }
//
//    open fun loadTitleView() {
//        supportFragmentManager.commit {
//            replace(R.id.main_info_fragment_container, MSDKInfoFragment())
//        }
//    }
//
//    abstract fun loadPages()
}