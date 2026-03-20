package com.app.quickfun.domain.model

data class Place(
    val id: String,
    val name: String,
    val description: String?,
    val status: String?,
    val categoryName: String?,
    val city: String?,
    val address: String?
)