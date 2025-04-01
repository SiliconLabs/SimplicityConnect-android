package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.common.other.ImagesHorizontalDecoration
import com.siliconlabs.bledemo.databinding.DialogEslLoadedImagesBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.adapters.LoadedImagesAdapter

abstract class EslLoadedImagesDialog(
    val imageArray: Array<Uri?>,
    protected val callback: Callback,
) : BaseDialogFragment() {

    protected val binding by viewBinding(DialogEslLoadedImagesBinding::bind)
    protected lateinit var loadedImagesAdapter: LoadedImagesAdapter
    protected var slotIndex: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_esl_loaded_images, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDialogTexts()
        setupUiListeners()
        initLoadedImagesRv()
    }

    private fun initDialogTexts() {
        binding.apply {
            loadedImagesTitle.text = getDialogTitle()
            loadedImagesMessage.text = getDialogMessage()
            loadedImagesNote.text = getDialogNote()
            btnPositiveAction.text = getPositiveButtonText()

            btnPositiveAction.isEnabled = false
        }
    }

    private fun setupUiListeners() {
        binding.apply {
            btnCancel.setOnClickListener {
                onNegativeButtonClicked()
            }
            btnPositiveAction.setOnClickListener {
                onPositiveButtonClicked()
            }
        }
    }

    private fun initLoadedImagesRv() {
        loadedImagesAdapter = LoadedImagesAdapter(imageArray, adapterCallback)

        binding.rvLoadedImages.apply {
            adapter = loadedImagesAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            addItemDecoration(ImagesHorizontalDecoration())
        }
    }

    abstract fun getDialogTitle() : String
    abstract fun getDialogMessage() : String
    abstract fun getDialogNote() : String
    abstract fun getPositiveButtonText() : String
    open fun onSlotClicked(index: Int) {
        slotIndex = index
    }

    protected open fun onPositiveButtonClicked() {
        dismiss()
    }

    protected open fun onNegativeButtonClicked() {
        dismiss()
        callback.onCancelButtonClicked()
    }

    private val adapterCallback = object : LoadedImagesAdapter.Callback {
        override fun onSlotClicked(index: Int) {
            this@EslLoadedImagesDialog.onSlotClicked(index)
        }
    }

    interface Callback {
        fun onUploadButtonClicked(slotIndex: Int, uri: Uri, displayAfterUpload: Boolean)
        fun onDisplayButtonClicked(slotIndex: Int)
        fun onCancelButtonClicked()
        fun onShowingUploadDialogAgain()
    }

}