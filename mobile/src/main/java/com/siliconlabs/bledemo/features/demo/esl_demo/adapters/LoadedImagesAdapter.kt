package com.siliconlabs.bledemo.features.demo.esl_demo.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        return imageUris.size
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
                showBorder()
                callback.onSlotClicked(adapterPosition)
            } }
        }

        fun bind(position: Int) {
            val imageInfo = imageUris[position]

            _binding.apply {
                imageInfo?.let {
                    Glide
                        .with(itemView.context)
                        .load(it)
                        .centerCrop()
                        .into(eslImageLoaded)
                }

                root.strokeWidth =
                    if (position == chosenSlot) itemView.context.resources
                        .getDimensionPixelSize(R.dimen.rv_loaded_images_stroke_width)
                    else 0
            }
        }

        private fun showBorder() {
            val oldChosenSlot = chosenSlot ?: adapterPosition
            chosenSlot = adapterPosition

            notifyItemChanged(oldChosenSlot)
            notifyItemChanged(chosenSlot!!)
        }
    }

    interface Callback {
        fun onSlotClicked(index: Int)
    }
}