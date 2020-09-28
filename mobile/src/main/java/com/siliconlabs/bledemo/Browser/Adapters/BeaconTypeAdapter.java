package com.siliconlabs.bledemo.Browser.Adapters;

import android.content.Context;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.siliconlabs.bledemo.Browser.Model.BeaconType;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.BeaconUtils.BleFormat;

import java.util.List;

public class BeaconTypeAdapter extends RecyclerView.Adapter<BeaconTypeAdapter.BeaconTypeViewHolder> {
    private List<BeaconType> beaconTypeList;
    private Context context;

    public class BeaconTypeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView beaconTypeTV;
        ImageView beaconIsCheckedIV;

        public BeaconTypeViewHolder(View itemView) {
            super(itemView);
            beaconTypeTV = itemView.findViewById(R.id.textview_beacon_name);
            beaconIsCheckedIV = itemView.findViewById(R.id.imageview_beacon_is_checked);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            BeaconType beaconType = beaconTypeList.get(position);

            if (beaconType.isChecked()) {
                beaconIsCheckedIV.setVisibility(View.GONE);
                beaconTypeTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text));
                beaconTypeTV.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            } else {
                beaconIsCheckedIV.setVisibility(View.VISIBLE);
                beaconTypeTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue));
                beaconTypeTV.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }

            beaconType.setChecked(!beaconType.isChecked());

            notifyDataSetChanged();
        }
    }

    public BeaconTypeAdapter(List beaconTypeList, Context context) {
        this.beaconTypeList = beaconTypeList;
        this.context = context;
    }

    @Override
    public void onBindViewHolder(BeaconTypeAdapter.BeaconTypeViewHolder holder, int position) {
        BeaconType beaconType = beaconTypeList.get(position);

        holder.beaconTypeTV.setText(beaconType.getBeaconTypeName());

        if (beaconType.isChecked()) {
            holder.beaconIsCheckedIV.setVisibility(View.VISIBLE);
            holder.beaconTypeTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue));
            holder.beaconTypeTV.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        } else {
            holder.beaconIsCheckedIV.setVisibility(View.GONE);
            holder.beaconTypeTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text));
            holder.beaconTypeTV.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }


    }

    @Override
    public BeaconTypeAdapter.BeaconTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_beacon_type, parent, false);
        return new BeaconTypeViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return beaconTypeList.size();
    }

    public List<BeaconType> getBeaconTypeList() {
        return beaconTypeList;
    }

    public void selectBeacons(List<BleFormat> bleFormats) {
        for (BeaconType b : beaconTypeList) {
            boolean isChecked = false;
            if (bleFormats != null) {
                for (BleFormat bf : bleFormats) {
                    if (b.getBleFormat().equals(bf)) {
                        isChecked = true;
                        break;
                    }
                }
            }
            b.setChecked(isChecked);
        }
        notifyDataSetChanged();
    }
}