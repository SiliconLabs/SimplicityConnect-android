package com.siliconlabs.bledemo.Views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.siliconlabs.bledemo.R

class CharacteristicItemContainer(context: Context) : LinearLayout(context) {

    val characteristicExpansion: LinearLayout
    val propsContainer: LinearLayout
    val characteristicName: TextView
    val characteristicUuid: TextView
    val descriptorsLabel: TextView
    val descriptorContainer: LinearLayout
    val characteristicEditNameIcon: ImageView
    val characteristicEditNameLayout: LinearLayout
    val characteristicSeparator: View

    init {
        LayoutInflater.from(context).inflate(R.layout.list_item_debug_mode_characteristic_of_service, this)

        characteristicExpansion = findViewById(R.id.characteristic_expansion)
        propsContainer = findViewById(R.id.characteristic_props_container)
        characteristicName = findViewById(R.id.characteristic_title)
        characteristicUuid = findViewById(R.id.characteristic_uuid)
        descriptorsLabel = findViewById(R.id.text_view_descriptors_label)
        descriptorContainer = findViewById(R.id.linear_layout_descriptor)
        characteristicEditNameIcon = findViewById(R.id.image_view_edit_charac_name)
        characteristicEditNameLayout = findViewById(R.id.linear_layout_edit_charac_name)
        characteristicSeparator = findViewById(R.id.characteristics_separator)

        characteristicExpansion.id = View.generateViewId()
    }

}