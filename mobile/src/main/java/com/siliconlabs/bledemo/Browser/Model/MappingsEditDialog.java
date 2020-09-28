package com.siliconlabs.bledemo.Browser.Model;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.Browser.MappingCallback;

public class MappingsEditDialog extends Dialog {

    private Button saveB;
    private Button cancelB;
    private EditText nameET;
    private TextView uuidTV;
    private TextView mappingTypeTitleTV;
    private String name;
    private String UUID;
    private MappingType type;
    private Context context;
    private MappingCallback callback;

    public MappingsEditDialog(@NonNull Context context, String name, String UUID, MappingCallback callback, MappingType type) {
        super(context);
        this.name = name;
        this.UUID = UUID;
        this.callback = callback;
        this.type = type;
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.dialog_characteristic_service_edit);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = (int) (size.x * 0.75);

        getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        saveB = findViewById(R.id.button_save);
        cancelB = findViewById(R.id.button_cancel);
        nameET = findViewById(R.id.edit_text_name_hint);
        uuidTV = findViewById(R.id.text_view_uuid);
        mappingTypeTitleTV = findViewById(R.id.edit_text_mapping_type);

        uuidTV.setText(UUID);
        nameET.setText(name);
        nameET.setSelection(nameET.getText().length());
        if (type.equals(MappingType.CHARACTERISTIC)) {
            mappingTypeTitleTV.setText(context.getResources().getString(R.string.Change_characteristic_name));
        } else {
            mappingTypeTitleTV.setText(context.getResources().getString(R.string.Change_service_name));
        }

        cancelB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        saveB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newCharacServiceName = nameET.getText().toString();

                if (!TextUtils.isEmpty(nameET.getText().toString())) {
                    Mapping mapping = new Mapping(UUID, newCharacServiceName);
                    callback.onNameChanged(mapping);
                    dismiss();
                } else {
                    Toast.makeText(getContext(), context.getResources().getString(R.string.Name_field_cannot_be_empty), Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

}
