package com.siliconlabs.bledemo.features.demo.channel_sounding.utils

import java.util.UUID

object ChannelSoundingConstant {

    enum class GattState {
        DISCONNECTED,
        SCANNING,
        CONNECTED,
    }

    enum class RangeSessionState {
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
    }

    /**
     * Measurement state to control callback gating for pause/resume functionality.
     * This allows the Channel Sounding session to remain alive while pausing measurements.
     * 
     * IDLE: No active measurement session, initial state
     * RUNNING: Actively collecting and processing measurements
     * PAUSED: Session is alive but measurement callbacks are gated (ignored)
     * 
     * Why this is needed:
     * The RangeSessionState tracks the underlying platform session lifecycle.
     * MeasurementState provides an additional layer to pause/resume measurement processing
     * without terminating the expensive-to-recreate Channel Sounding session.
     * This preserves Kalman filter state and allows seamless resume.
     */
    enum class MeasurementState {
        IDLE,     // No session, ready to start
        RUNNING,  // Actively processing measurements
        PAUSED    // Session alive but callbacks gated, ready to resume
    }

    val OOB_SERVICE: UUID = UUID.fromString("f81d4fae-7ccc-eeee-a765-aaaaaaaaaaaa")

    val OOB_PSM_CHARACTERISTICS: UUID = UUID.fromString("f81d4fae-7ccc-eeee-a765-0aaaaaaaaaaa")


}