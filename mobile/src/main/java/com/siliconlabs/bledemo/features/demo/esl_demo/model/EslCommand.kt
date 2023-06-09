package com.siliconlabs.bledemo.features.demo.esl_demo.model

enum class EslCommand(val code: Int, val message: String) {

    /**
     * Connect ESL Tag to ESL Access Point
     */
    CONNECT(0, "connect"),

    /**
     * Add ESL Tag to the list of tags maintained by Access Point
     */
    CONFIGURE(1, "config"),

    /**
     * Disconnect ESL Tag from ESL Access Point
     */
    DISCONNECT(2, "disconnect"),

    /**
     * Update new image to be displayed on the ESL Tag device
     */
    UPDATE_IMAGE(3, "image_update"),

    /**
     * Display chosen image on the ESL Tag device
     */
    DISPLAY_IMAGE(5, "display_image"),

    /**
     * Turn the ESL Tag led on/off
     */
    TOGGLE_LED(6, "led"),

    /**
     * Load info about all tags maintained by Access Point
     */
    LOAD_INFO(7, "list s"),

    /**
     * Load info about LED state of tag maintained by Access Point
     */
    PING(8, "ping"),

    /**
     * Delete ESL Tag from the list of tags maintained by ESL Access Point
     */
    DELETE(9, "unassociate");


    companion object {
        fun fromCode(commandCode: Int) : EslCommand? {
            return values().firstOrNull { it.code == commandCode }
        }
    }
}