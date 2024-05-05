package com.l3s.dronegpt

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.util.ToastUtils
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_get_heading
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_initialize_all
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_initialize_virtual_sticks
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_land
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_move_backward
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_move_forward
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_move_left
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_move_right
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_rotate_left
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_rotate_right
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_take_off
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_take_photo
import kotlinx.android.synthetic.main.frag_simple_testing_page.radio_java
import kotlinx.android.synthetic.main.frag_simple_testing_page.radio_luaj
import kotlinx.android.synthetic.main.frag_simple_testing_page.test_flight_updates

class TestFlightActivity : AppCompatActivity() {
//    protected val msdkCommonOperateVm: MSDKCommonOperateVm by viewModels()
//    private val testToolsVM: TestToolsVM by viewModels()
    private var luajExecution: Boolean = false
    private var updates: StringBuilder = StringBuilder().appendLine("updates:")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.frag_simple_testing_page)
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
        initRadioButtons()
        initBtnClickListener()
        updateText()
        FlightUtility.setActivityObject(this)
    }
    private fun initRadioButtons() {
        radio_java.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                this.luajExecution = false
                ToastUtils.showToast("Java execution selected")
                initBtnClickListener()
            }
        }
        radio_luaj.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                this.luajExecution = true
                ToastUtils.showToast("LuaJ execution selected")
                initBtnClickListener()
            }
        }
    }
    private fun initBtnClickListener() {
        btn_initialize_virtual_sticks.setOnClickListener {
            FlightUtility.testFlightMinimalInit()
        }
        btn_initialize_all.setOnClickListener {
//            FlightUtility.testFlightFullInit(obstacle_avoidance_switch.isActivated, takeoff_altitude.text.toString().toDouble(), max_altitude.toString().toInt(), max_distance.toString().toInt())
            FlightUtility.testFlightFullInit(true, 10.00, 20, 15)
        }
        if(!luajExecution) {
            btn_take_off.setOnClickListener {
                FlightUtility.takeOff()
            }
            btn_land.setOnClickListener {
                FlightUtility.land()
            }
            btn_move_forward.setOnClickListener {
                FlightUtility.adjustFlightParameters(0.0, 23.0,0.0,2.0)
            }
            btn_move_backward.setOnClickListener {
                FlightUtility.adjustFlightParameters(0.0, -23.0,0.0,2.0)
            }
            btn_move_right.setOnClickListener {
                FlightUtility.adjustFlightParameters(23.0, 0.0,0.0,2.0)
            }
            btn_move_left.setOnClickListener {
                FlightUtility.adjustFlightParameters(-23.0, 0.0,0.0,2.0)
            }
            btn_rotate_left.setOnClickListener {
                FlightUtility.adjustFlightParameters(0.0, 0.0, 90.0, 0.0)
            }
            btn_rotate_right.setOnClickListener {
                FlightUtility.adjustFlightParameters(0.0, 0.0, -90.0, 0.0)
            }
            btn_get_heading.setOnClickListener {
                ToastUtils.showToast("Current Heading = " + FlightUtility.getCurrentHeading())
            }
            btn_take_photo.setOnClickListener {
                FlightUtility.takePhoto()
            }
        } else {
            btn_take_off.setOnClickListener {
                ScriptManager.executeLuaScript("take_off()")
            }
            btn_land.setOnClickListener {
                ScriptManager.executeLuaScript("land()")
            }
            btn_move_forward.setOnClickListener {
                ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, 23.0,0.0,2.0)")
            }
            btn_move_backward.setOnClickListener {
                ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, -23.0,0.0,2.0)")
            }
            btn_move_right.setOnClickListener {
                ScriptManager.executeLuaScript("adjust_flight_parameters(23.0, 0.0,0.0,2.0)")
            }
            btn_move_left.setOnClickListener {
                ScriptManager.executeLuaScript("adjust_flight_parameters(-23.0, 0.0,0.0,2.0)")
            }
            btn_rotate_left.setOnClickListener {
                ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, 0.0, 90.0, 0.0)")
            }
            btn_rotate_right.setOnClickListener {
                ScriptManager.executeLuaScript("adjust_flight_parameters(0.0, 0.0, -90.0, 0.0)")
            }
            btn_get_heading.setOnClickListener {
                ScriptManager.executeLuaScript("print(get_current_heading())")
            }
            btn_take_photo.setOnClickListener {
                ScriptManager.executeLuaScript("take_photo()")
            }
        }
    }

    private fun updateText() {
        Handler(Looper.getMainLooper()).post {
            test_flight_updates.text = updates.toString()
        }
    }
    fun addUpdate(update: String) {
        updates.append(">")
        updates.appendLine(update)
        updateText()
    }
//    override fun onDestroy() {
//        super.onDestroy()
//        DJIToastUtil.dJIToastLD = null
//    }
//
//    open fun loadTitleView() {
//        supportFragmentManager.commit {
//            replace(R.id.main_info_fragment_container, MSDKInfoFragment())
//        }
//    }
//
//    abstract fun loadPages()
}