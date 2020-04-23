package com.siliconlabs.bledemo.adapters;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.mappings.Mapping;
import com.siliconlabs.bledemo.mappings.MappingType;
import com.siliconlabs.bledemo.mappings.MappingsEditDialog;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.interfaces.MappingCallback;

import java.util.List;

public class MappingAdapter extends RecyclerView.Adapter<MappingAdapter.NameMappingViewHolder> {
    private List<Mapping> list;
    private Context context;
    private MappingType type;

    public class NameMappingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView uuidTV;
        TextView nameTV;
        ImageButton deleteIB;
        LinearLayout characServiceTitleLL;
        CardView mappingCV;

        public NameMappingViewHolder(View itemView) {
            super(itemView);
            uuidTV = itemView.findViewById(R.id.text_view_uuid);
            nameTV = itemView.findViewById(R.id.text_view_name);
            deleteIB = itemView.findViewById(R.id.image_button_delete);
            mappingCV = itemView.findViewById(R.id.mapping_card_view);
            characServiceTitleLL = itemView.findViewById(R.id.linear_layout_charac_service);
            characServiceTitleLL.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            Mapping mapping = list.get(position);

            Dialog dialog = new MappingsEditDialog(context, mapping.getName(), mapping.getUuid(), new MappingCallback() {
                @Override
                public void onNameChanged(Mapping nameMapping) {
                    list.set(position, nameMapping);
                    nameTV.setText(nameMapping.getName());
                }
            }, type);

            dialog.show();
        }
    }

    public MappingAdapter(List<Mapping> list, Context context, MappingType type) {
        this.list = list;
        this.context = context;
        this.type = type;
    }

    @Override
    public void onBindViewHolder(MappingAdapter.NameMappingViewHolder holder, final int position) {
        Mapping mapping = list.get(position);

        int margin16Dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
        int margin10Dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());


        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.mappingCV.getLayoutParams();
        if (position == 0) {
            layoutParams.setMargins(margin16Dp, margin16Dp, margin16Dp, margin10Dp);
        } else if (position == getItemCount() - 1) {
            layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin16Dp);
        } else {
            layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin10Dp);
        }
        holder.mappingCV.requestLayout();

        holder.uuidTV.setText(mapping.getUuid());
        holder.nameTV.setText(mapping.getName());

        holder.deleteIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.remove(position);
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public MappingAdapter.NameMappingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.charac_service_mapping_list_item, parent, false);
        return new NameMappingViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}