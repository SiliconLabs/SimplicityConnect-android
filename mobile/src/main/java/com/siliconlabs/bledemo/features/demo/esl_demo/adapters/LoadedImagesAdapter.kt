package com.siliconlabs.bledemo.features.demo.esl_demo.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.AdapterEslLoadedImageBinding
import com.siliconlabs.bledemo.utils.RecyclerViewUtils

class LoadedImagesAdapter(
    imageArray: Array<Uri?>,
    private val callback: Callback
) : RecyclerView.Adapter<LoadedImagesAdapter.LoadedImageViewHolder>() {

    private val imageUris: MutableList<Uri?> = imageArray.toMutableList()
    private var chosenSlot: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadedImageViewHolder {
        val binding = AdapterEslLoadedImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoadedImageViewHolder(binding).apply {
            setupOnClickListener()
        }
    }

    override fun getItemCount(): Int {
        return 2
    }

    override fun onBindViewHolder(holder: LoadedImageViewHolder, position: Int) {
        holder.bind(position)
    }

    fun isChosenSlotNotEmpty() : Boolean {
        return chosenSlot?.let { imageUris[it] != null } ?: false
    }

    inner class LoadedImageViewHolder(
        private val _binding: AdapterEslLoadedImageBinding
    ) : RecyclerView.ViewHolder(_binding.root) {

        fun setupOnClickListener() {
            _binding.root.setOnClickListener { RecyclerViewUtils.withProperAdapterPosition(this) {
                showBorder(it)
                callback.onSlotClicked(it)
            } }
        }

        fun bind(position: Int) {
            val imageInfo = imageUris[position]

            _binding.apply {
                imageInfo?.let {
                    tvImageNoSpecifiers.visibility = View.GONE
                    eslImageLoaded.load(it) {
                        scale(Scale.FILL)
                        transformations(RoundedCornersTransformation())
                    }
                } ?: run {
                    eslImageLoaded.dispose()
                    eslImageLoaded.load(R.color.silabs_click_grey)
                    eslImageLoaded.setBackgroundColor(ContextCompat.getColor(eslImageLoading.context, R.color.silabs_click_grey))
                    tvImageNoSpecifiers.text = "Image ${position + 1}"
                }

                root.strokeWidth =
                    if (position == chosenSlot) itemView.context.resources
                        .getDimensionPixelSize(R.dimen.rv_loaded_images_stroke_width)
                    else 0
            }
        }

        private fun showBorder(position: Int) {
            val oldChosenSlot = chosenSlot ?: position
            chosenSlot = position

            notifyItemChanged(oldChosenSlot)
            notifyItemChanged(chosenSlot!!)
        }
    }

    interface Callback {
        fun onSlotClicked(index: Int)
    }
}