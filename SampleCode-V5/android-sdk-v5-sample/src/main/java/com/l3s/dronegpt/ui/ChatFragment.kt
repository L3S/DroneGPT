package com.l3s.dronegpt.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.l3s.dronegpt.ChatGPTUtility
import com.l3s.dronegpt.ScriptManager
import com.l3s.dronegpt.adapter.ChatContentAdapter
import com.l3s.dronegpt.data.database.ChatContent
import com.l3s.dronegpt.data.database.Experiment
import dji.sampleV5.aircraft.databinding.FragDronegptChatBinding
import dji.sampleV5.aircraft.util.ToastUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var branch: Int = 1 // # 1 -> First time loading
    private var _binding: FragDronegptChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val mainViewModel: DroneGPTViewModel by activityViewModels()
    private var contentDataList = ArrayList<ChatContent>()
    private lateinit var experiment: Experiment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragDronegptChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Coil with local context
        val imageLoader = ImageLoader.Builder(requireContext())
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
//        Coil.setImageLoader(imageLoader)

//        binding.loading.visibility = View.INVISIBLE
//        binding.loading.load(R.drawable.loading3)

//        val selectedExperimentId = arguments?.getInt("selectedExperimentId", -1)
//
//        if (selectedExperimentId != null && selectedExperimentId != -1) {
//            experimentId = selectedExperimentId
//            viewModel.getContentData(experimentId)
//        }

        setupObservers()

        binding.sendBtn.setOnClickListener {
//            binding.loading.visibility = View.VISIBLE
            if (this::experiment.isInitialized) {
                viewModel.postResponse(experiment, binding.EDView.text.toString())
                viewModel.insertContent(
                    experiment.id,
                    binding.EDView.text.toString(),
                    true
                ) // 1: Gpt, 2: User
                binding.EDView.setText("")
                branch = 2
                viewModel.getContentData(experiment.id)
            }
        }
        binding.buildPromptBtn.setOnClickListener {
            if (this::experiment.isInitialized) {
                binding.EDView.setText(ChatGPTUtility.buildPrompt(experiment.flightHeight, experiment.areaDescription))
            }
        }
    }

    private fun setupObservers() {
        mainViewModel.selectedExperiment.observe(viewLifecycleOwner, Observer { experiment ->
            this.experiment = experiment
            contentDataList.clear()
            viewModel.getContentData(experiment.id)
            setContentListRV(branch)
        })

        viewModel.contentList.observe(viewLifecycleOwner, Observer {
            contentDataList.clear()
            contentDataList.addAll(it)
            setContentListRV(branch)
        })

//        viewModel.deleteCheck.observe(viewLifecycleOwner, Observer {
//            if (it) {
//                viewModel.getContentData(experimentId)
//                branch = 1
//            }
//        })

        viewModel.gptInsertCheck.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.getContentData(experiment.id)
//                binding.loading.visibility = View.INVISIBLE
            }
            branch = 2
        })
    }

    private fun setContentListRV(branch: Int) {
        val contentAdapter = ChatContentAdapter(requireContext(), contentDataList)
        binding.RVContainer.adapter = contentAdapter
        binding.RVContainer.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }

        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            binding.SVContainer.fullScroll(ScrollView.FOCUS_DOWN)
            if (branch != 1) {
                binding.EDView.requestFocus()
            }
        }

        contentAdapter.viewCodeClick = object : ChatContentAdapter.ViewCodeClick {
            override fun onLongClick(view: View, position: Int) {
                if (contentDataList[position].isUserContent) {
                    ToastUtils.showToast("Please choose a ChatGPT message")
                } else {
                    showExtractedCodeDialog(position)
                }
            }
        }
    }

    private fun showExtractedCodeDialog(position: Int) {
        val script = ChatGPTUtility.parseCode(contentDataList[position].content)
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Execute Script?")
            setMessage("""
                Script extracted from ChatGPT:
                
                $script
                
                Are you sure you want to execute this script?
            """.trimIndent())
            setPositiveButton("execute") { _, _ ->
                try {
                    mainViewModel.setExperimentExecutedCode(experiment.id, script)
                    ScriptManager.executeLuaScript(script)
                } catch (e: Exception) {
                    ToastUtils.showToast("Error executing script ${e.localizedMessage}")
                    Log.e("ChatFragment", """
                        Error executing script: $script 
                        Error: $e
                    """.trimIndent())
                }
            }
            setNegativeButton("cancel", null)
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}