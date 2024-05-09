package com.l3s.dronegpt.model

data class GptText(
    val index: Int,
    val message: Message,
    val logprobs: Any?, // null in most cases
    val finish_reason: String
)
