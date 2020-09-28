package com.siliconlabs.bledemo.MainMenu.Adapters;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.siliconlabs.bledemo.MainMenu.Model.MenuItem;
import com.siliconlabs.bledemo.MainMenu.Model.MenuItemType;
import com.siliconlabs.bledemo.R;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuItemViewHolder> {
    private List<MenuItem> menuItemList;
    private OnMenuItemClickListener onMenuItemClickListener;
    private Context context;

    public class MenuItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView menuIconIV;
        TextView menuTitleTV;
        TextView menuDescrTV;

        public MenuItemViewHolder(View itemView) {
            super(itemView);
            menuIconIV = itemView.findViewById(R.id.imageview_menu_icon);
            menuTitleTV = itemView.findViewById(R.id.textview_menu_title);
            menuDescrTV = itemView.findViewById(R.id.textview_menu_description);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            onMenuItemClickListener.onMenuItemClick(menuItemList.get(position).getMenuItemType());
        }
    }

    public MenuAdapter(List<MenuItem> menuItemList, OnMenuItemClickListener onMenuItemClickListener, Context context) {
        this.menuItemList = menuItemList;
        this.onMenuItemClickListener = onMenuItemClickListener;
        this.context = context;
    }


    @Override
    public MenuAdapter.MenuItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_main_menu, parent, false);
        return new MenuItemViewHolder(view);
    }


    @Override
    public void onBindViewHolder(MenuItemViewHolder holder, int position) {
        MenuItem menuItem = menuItemList.get(position);
        holder.menuTitleTV.setText(menuItem.getTitle());
        holder.menuDescrTV.setText(menuItem.getDescription());

        holder.menuIconIV.setImageDrawable(ContextCompat.getDrawable(context, menuItem.getMenuIcon()));
    }


    @Override
    public int getItemCount() {
        return menuItemList.size();
    }

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItemType menuItemType);
    }

}
