package com.example.rockland.presentation.model

data class UiBanner(
    val text: String,
    val type: UiBannerType
)

enum class UiBannerType {
    Success,
    Error,
    Info
}


