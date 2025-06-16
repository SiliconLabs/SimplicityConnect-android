package com.siliconlabs.bledemo.features.demo.awsiot.listener

interface OnMqttGridItemClickListener {
    fun onItemClick(ledRedButtonControlStatus:Boolean,ledGreenButtonControlStatus:Boolean,ledBlueButtonControlStatus:Boolean)

    fun onCloseButtonClicked(buttonClickedStatus:Boolean)
}