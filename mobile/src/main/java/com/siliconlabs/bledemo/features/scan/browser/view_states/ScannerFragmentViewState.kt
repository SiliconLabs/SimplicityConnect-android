package com.siliconlabs.bledemo.features.scan.browser.view_states

import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel

data class ScannerFragmentViewState(
        val isScanningOn: Boolean,
        val devicesToShow: List<ScanFragmentViewModel.BluetoothInfoViewState>
)
