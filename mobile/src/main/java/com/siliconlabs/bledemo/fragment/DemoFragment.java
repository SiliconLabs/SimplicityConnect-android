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

public class DemoFragment extends Fragment implements MenuAdapter.OnMenuItemClickListener {

    private ArrayList<MenuItem> list;
    private MenuAdapter.OnMenuItemClickListener menuItemClickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Here prepare all menu items you want to display in Demo view
        list = new ArrayList<>();
        list.add(new MenuItem(R.drawable.ic_icon_temp, getResources().getString(R.string.title_Health_Thermometer), getResources().getString(R.string.description_Thermometer), MenuItemType.HEALTH_THERMOMETER));

        //BTAPP-711 Keyfob demo tile still present, this is to be removed but only the tile, please keep this in the code base just in case we need to bring it back
        //list.add(new MenuItem(R.drawable.ic_icon_advertiser, "Key Fobs", "Detect and find Key Fobs via intelligent alerts.", MenuItemType.KEY_FOBS));
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
        View view = inflater.inflate(R.layout.fragment_demo, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recylerview_demo_menu);
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
