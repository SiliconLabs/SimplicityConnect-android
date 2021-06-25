package com.siliconlabs.bledemo.main_menu.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.main_menu.adapters.MenuAdapter.MenuItemViewHolder
import com.siliconlabs.bledemo.main_menu.menu_items.MainMenuItem
import com.siliconlabs.bledemo.R

class MenuAdapter(private val list: List<MainMenuItem>, private val listener: OnMenuItemClickListener, private val context: Context) : RecyclerView.Adapter<MenuItemViewHolder>() {

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val  ivMenuIcon = itemView.findViewById(R.id.iv_menu_icon) as ImageView
        private val tvMenuTitle = itemView.findViewById(R.id.tv_menu_title) as TextView
        private val tvMenuDescr = itemView.findViewById(R.id.tv_menu_description) as TextView

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: MainMenuItem) {
            tvMenuTitle.text = item.title
            tvMenuDescr.text = item.description
            ivMenuIcon.setImageDrawable(ContextCompat.getDrawable(context, item.imageResId))
        }

        override fun onClick(v: View) {
            listener.onMenuItemClick(list[adapterPosition])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_main_menu, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val menuItem = list[position]
        holder.bind(menuItem)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(menuItem: MainMenuItem)
    }

}