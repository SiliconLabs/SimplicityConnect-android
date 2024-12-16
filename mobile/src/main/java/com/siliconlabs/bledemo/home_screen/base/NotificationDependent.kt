package com.siliconlabs.bledemo.home_screen.base

interface NotificationDependent {
    fun onNotificationStateChanged(isNotificationOn: Boolean)
    fun onNotificationPermissionsStateChanged(arePermissionsGranted: Boolean)
    fun setupNotificationBarButtons()
    fun setupNotificationPermissionBarButtons()
}