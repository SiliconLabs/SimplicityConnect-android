package com.siliconlabs.bledemo.features.configure.advertiser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.features.configure.advertiser.models.Advertiser
import com.siliconlabs.bledemo.features.configure.advertiser.models.AdvertiserData
import com.siliconlabs.bledemo.features.configure.advertiser.utils.Translator
import com.siliconlabs.bledemo.features.configure.advertiser.views.AdvertiserDetails
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.AdapterAdvertiserBinding
import com.siliconlabs.bledemo.utils.CustomToastManager

class AdvertiserAdapter(
        private val items: ArrayList<Advertiser>,
        private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<AdvertiserAdapter.ViewHolder>() {

    private var isBluetoothOperationPossible = true

    init {
        items.map { it.displayDetailsView = false }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewBinding = AdapterAdvertiserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(viewBinding, itemClickListener)
        setupUiListeners(holder)
        return holder
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun toggleIsBluetoothOperationPossible(isPossible: Boolean) {
        isBluetoothOperationPossible = isPossible
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: Advertiser = items[position]
        holder.bind(item)
    }

    private fun setupUiListeners(holder: ViewHolder) {
        holder.apply {
            viewBinding.ibCopy.setOnClickListener {
                withAdapterPositionCheck(holder) { itemClickListener.onCopyClick(items[adapterPosition]) }
            }
            viewBinding.ibEdit.setOnClickListener {
                withAdapterPositionCheck(holder) { itemClickListener.onEditClick(adapterPosition, items[adapterPosition]) }
            }
            viewBinding.ibRemove.setOnClickListener {
                withAdapterPositionCheck(holder) { itemClickListener.onRemoveClick(adapterPosition) }
            }

            viewBinding.swAdvertiser.setOnCheckedChangeListener(switchListener)

            viewBinding.expandArrow.setOnClickListener {
                withAdapterPositionCheck(holder) {
                    items[adapterPosition].displayDetailsView = !items[adapterPosition].displayDetailsView
                    holder.toggleDetailsView(items[adapterPosition].displayDetailsView)
                }
            }
        }
    }



    inner class ViewHolder(
            val viewBinding: AdapterAdvertiserBinding,
            clickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        private val translator = Translator(itemView.context)

        val switchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            withAdapterPositionCheck(this@ViewHolder) {
                if (isBluetoothOperationPossible) {
                    if (isChecked) clickListener.switchItemOn(adapterPosition)
                    else clickListener.switchItemOff(adapterPosition)
                } else {
                    //Toast.makeText(itemView.context, R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show()
                    val message = itemView.context.getString(R.string.toast_bluetooth_not_enabled)
                    CustomToastManager.show(itemView.context,message,5000)
                    if (isChecked) viewBinding.swAdvertiser.isChecked = false
                }
            }
        }

        fun bind(item: Advertiser) {
            viewBinding.apply {
                handleDetailsView(item)
                populateTextViews(item.data)
                swAdvertiser.isEnabled = isBluetoothOperationPossible

                if (swAdvertiser.isChecked != item.isRunning) {
                    swAdvertiser.apply {
                        setOnCheckedChangeListener(null)
                        isChecked = item.isRunning /* Don't act on this change. */
                        setOnCheckedChangeListener(switchListener)
                    }
                }
            }
        }

        fun withAdapterPositionCheck(holder: ViewHolder, action: () -> (Unit)) {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) action()
        }

        private fun handleDetailsView(item: Advertiser) {
            println("Show Advertiser Data ${item.data}")

            viewBinding.llAdvertisementDetails.apply {
                removeAllViews()

                val advertiserDetailContainer = AdvertiserDetails(itemView.context).getAdvertiserDetailsView(item, translator)
                addView(advertiserDetailContainer.rootView,LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            toggleDetailsView(item.displayDetailsView)
        }

        private fun populateTextViews(data: AdvertiserData) {
            viewBinding.apply {
                tvDeviceName.text = data.name
                tvTxPower.text = itemView.context.getString(R.string.unit_value_dbm, data.txPower)
                tvInterval.text = itemView.context.getString(R.string.unit_value_ms, data.advertisingIntervalMs)
                tvConnectible.text =
                        if (data.mode.isConnectable()) itemView.context.getString(R.string.connectible)
                        else itemView.context.getString(R.string.non_connectible)
            }
        }

        fun toggleDetailsView(displayDetails: Boolean) {
            println("displayDetails:--> $displayDetails")
            viewBinding.llAdvertisementDetails.visibility = if (displayDetails) View.VISIBLE else View.GONE
            viewBinding.expandArrow.setState(displayDetails)
        }
    }

    interface OnItemClickListener {
        fun onCopyClick(item: Advertiser)
        fun onEditClick(position: Int, item: Advertiser)
        fun onRemoveClick(position: Int)
        fun switchItemOn(position: Int)
        fun switchItemOff(position: Int)
    }
}