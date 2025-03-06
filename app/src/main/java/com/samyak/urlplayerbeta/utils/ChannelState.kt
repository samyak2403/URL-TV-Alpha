package com.samyak.urlplayerbeta.utils

sealed class ChannelState {
    object Loading : ChannelState()
    data class Success(val message: String) : ChannelState()
    data class Error(val message: String) : ChannelState()
} 