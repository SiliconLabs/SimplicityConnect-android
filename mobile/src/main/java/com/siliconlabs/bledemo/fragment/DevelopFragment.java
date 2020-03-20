package com.siliconlabs.bledemo.fragment;


import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.siliconlabs.bledemo.menu.MenuItem;
import com.siliconlabs.bledemo.menu.MenuItemType;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.MenuAdapter;

import java.util.ArrayList;


public class DevelopFragment extends Fragment implements MenuAdapter.OnMenuItemClickListener {

    private ArrayList<MenuItem> list;
    private MenuAdapter.OnMenuItemClickListener menuItemClickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Here prepare all menu items you want to display in Develop view
        list = new ArrayList<>();
        list.add(new MenuItem(R.drawable.ic_icon_browser, getResources().getString(R.string.title_Browser), getResources().getString(R.string.description_Browser), MenuItemType.BLUETOOTH_BROWSER));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            menuItemClickListener = (MenuAdapter.OnMenuItemClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onViewSelected");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_develop, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recylerview_develop_menu);
        MenuAdapter adapter = new MenuAdapter(list, this, getActivity());
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onMenuItemClick(MenuItemType menuItemType) {
        menuItemClickListener.onMenuItemClick(menuItemType);
    }
}
