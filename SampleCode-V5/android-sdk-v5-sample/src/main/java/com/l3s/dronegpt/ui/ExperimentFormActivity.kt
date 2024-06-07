package com.l3s.dronegpt.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.l3s.dronegpt.ChatGPTUtility
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.util.ToastUtils
import kotlinx.android.synthetic.main.dronegpt_experiment_form.create_experiment_button

class ExperimentFormActivity : AppCompatActivity() {
    private val viewModel: DroneGPTViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dronegpt_experiment_form)

        val modelSpinner: Spinner = findViewById(R.id.gpt_model_spinner)
        val areaTypeSpinner: Spinner = findViewById(R.id.area_type_spinner)
        val areaDescription: EditText = findViewById(R.id.area_description)
        val flightHeight: EditText = findViewById(R.id.flight_height)

        ArrayAdapter.createFromResource(
            this,
            R.array.gpt_models_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            modelSpinner.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.area_types_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            areaTypeSpinner.adapter = adapter
        }

        areaTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 1) { //rectangle selected
                    areaDescription.setText(ChatGPTUtility.defaultRectangleAreaDescription)
                } else if (position == 2) { //circle selected
                    areaDescription.setText(ChatGPTUtility.defaultCircleAreaDescription)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        create_experiment_button.setOnClickListener {
            val model = modelSpinner.selectedItem.toString()
            val description = areaDescription.text.toString()
            val height = flightHeight.text.toString()
            if (model.isNotEmpty() && description.isNotEmpty() && height.isNotEmpty()) {
                if (model == "ChatGPT 3.5") {
                    viewModel.createGPT3Experiment(description, Integer.parseInt(height))
                } else if (model == "ChatGPT 4") {
                    viewModel.createGPT4Experiment(description, Integer.parseInt(height))
                }
                ToastUtils.showToast("Experiment created!")
                // reset and close form
                modelSpinner.setSelection(0)
                areaTypeSpinner.setSelection(0)
                areaDescription.setText("")
                flightHeight.setText("")
                finish()
            }
        }
    }

}