package com.siliconlabs.bledemo.home_screen.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.home_screen.adapters.DemoAdapter.MenuItemViewHolder
import com.siliconlabs.bledemo.home_screen.menu_items.DemoMenuItem
import com.siliconlabs.bledemo.databinding.AdapterDemoMenuBinding

class DemoAdapter(private val list: List<DemoMenuItem>, private val listener: OnDemoItemClickListener) : RecyclerView.Adapter<MenuItemViewHolder>() {

    private var areItemsEnabled = true

    inner class MenuItemViewHolder(
            private val viewBinding: AdapterDemoMenuBinding
    ) : RecyclerView.ViewHolder(viewBinding.root), View.OnClickListener {

        init {
            viewBinding.root.setOnClickListener(this)
        }

        fun bind(item: DemoMenuItem) {
            viewBinding.apply {
                root.isEnabled = areItemsEnabled
                root.alpha = if (areItemsEnabled) 1f else 0.3f

                tvMenuTitle.text = item.title
                tvMenuDescription.text = item.description
                ivMenuIcon.setImageDrawable(ContextCompat.getDrawable(itemView.context, item.imageResId))
            }
        }

        override fun onClick(v: View) {
            listener.onDemoItemClicked(list[adapterPosition])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val viewBinding = AdapterDemoMenuBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
        return MenuItemViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val menuItem = list[position]
        holder.bind(menuItem)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun toggleItemsEnabled(isBluetoothOn: Boolean) {
        areItemsEnabled = isBluetoothOn
        notifyDataSetChanged()
    }

    interface OnDemoItemClickListener {
        fun onDemoItemClicked(demoItem: DemoMenuItem)
    }

}