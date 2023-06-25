package com.google.ar.core.smartmaintenance.webrtc.utils

import com.google.ar.core.smartmaintenance.webrtc.models.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}