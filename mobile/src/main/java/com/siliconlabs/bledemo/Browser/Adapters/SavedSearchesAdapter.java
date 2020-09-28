package com.siliconlabs.bledemo.Browser.Adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.Browser.Model.SavedSearch;
import com.siliconlabs.bledemo.Utils.FilterDeviceParams;
import com.siliconlabs.bledemo.Utils.SharedPrefUtils;

import java.util.HashMap;
import java.util.List;

public class SavedSearchesAdapter extends RecyclerView.Adapter<SavedSearchesAdapter.SavedSearchesViewHolder> {
    private List<SavedSearch> savedSearchList;
    private Context context;
    private SavedSearchesCallback savedSearchesCallback;
    private SharedPrefUtils sharedPrefUtils;

    public interface SavedSearchesCallback {
        void onClick(String name);
    }

    public class SavedSearchesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView searchedTextTV;
        LinearLayout savedSearch;
        ImageButton removeBtn;

        public SavedSearchesViewHolder(View itemView) {
            super(itemView);
            searchedTextTV = itemView.findViewById(R.id.textview_saved_search);
            savedSearch = itemView.findViewById(R.id.saved_search);
            removeBtn = itemView.findViewById(R.id.removeBtn);
            savedSearch.setOnClickListener(this);
            removeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeAt(getAdapterPosition());
                }
            });
        }

        @Override
        public void onClick(View v) {
            savedSearchesCallback.onClick(String.valueOf(searchedTextTV.getText()));
        }

    }

    public void removeAt(int position) {
        if (position < 0) return;
        HashMap<String, FilterDeviceParams> map = sharedPrefUtils.getMapFilter();
        map.remove(savedSearchList.get(position).getSearchText());
        sharedPrefUtils.updateMapFilter(map);
        savedSearchList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, savedSearchList.size());
    }

    public SavedSearchesAdapter(List savedSearchList, Context context, SavedSearchesCallback savedSearchesCallback) {
        this.savedSearchList = savedSearchList;
        this.context = context;
        this.savedSearchesCallback = savedSearchesCallback;
        this.sharedPrefUtils = new SharedPrefUtils(context);
    }

    @Override
    public void onBindViewHolder(SavedSearchesAdapter.SavedSearchesViewHolder holder, int position) {
        SavedSearch savedSearch = savedSearchList.get(position);
        holder.searchedTextTV.setText(savedSearch.getSearchText());

        FilterDeviceParams lastFilterDeviceParams = sharedPrefUtils.getLastFilter();
        if (lastFilterDeviceParams != null && lastFilterDeviceParams.getFilterName().toLowerCase().equals(savedSearch.getSearchText().toLowerCase())) {
            holder.searchedTextTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue_selected));
        } else {
            holder.searchedTextTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text));
        }
    }

    @Override
    public SavedSearchesAdapter.SavedSearchesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_saved_searches, parent, false);
        return new SavedSearchesViewHolder(view);
    }


    @Override
    public int getItemCount() {
        return savedSearchList.size();
    }

    public void addItem(SavedSearch savedSearch) {
        savedSearchList.add(0, savedSearch);
        notifyDataSetChanged();
    }
}