package com.l3s.dronegpt.pages;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.l3s.dronegpt.FlightUtility
import com.l3s.dronegpt.ScriptManager
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.pages.DJIFragment
import dji.sampleV5.aircraft.util.ToastUtils
import kotlinx.android.synthetic.main.frag_simple_testing_page.btn_get_heading
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

//import kotlinx.android.synthetic.main.frag_virtual_stick_page.widget_horizontal_situation_indicator

//DEPRECATED
class SimpleTestingFragment : DJIFragment() {

    private var luajExecution: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_simple_testing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        widget_horizontal_situation_indicator.setSimpleModeEnable(false)
        initRadioButtons()
        initBtnClickListener()
        //flig
    }

    private fun initRadioButtons() {
        radio_java.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                this.luajExecution = false
                ToastUtils.showToast("Java execution selected")
            }
        }
        radio_luaj.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                this.luajExecution = true
                ToastUtils.showToast("LuaJ execution selected")
            }
        }
    }
    private fun initBtnClickListener() {
//        btn_initialize_virtual_sticks.setOnClickListener {
//            FlightUtility.testFlightMinimalInit()
//        }
//        btn_initialize_all.setOnClickListener {
//            FlightUtility.testFlightFullInit(takeoff_altitude.text.toString().toDouble())
//        }
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


}
