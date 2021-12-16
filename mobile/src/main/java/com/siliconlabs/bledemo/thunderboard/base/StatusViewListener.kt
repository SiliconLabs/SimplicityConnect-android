package com.siliconlabs.bledemo.thunderboard.base

import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice

interface StatusViewListener {
    fun onData(device: ThunderBoardDevice?)
}