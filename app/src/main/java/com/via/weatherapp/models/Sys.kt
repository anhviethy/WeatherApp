package com.via.weatherapp.models

import java.io.Serializable

data class Sys(
    val type: Int,
    val country: String,
    val sunrise: Int,
    val sunset: Int
) : Serializable
