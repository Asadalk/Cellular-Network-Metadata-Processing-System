package com.tower.locator.model

data class LocateResponse(
    val lat : Int?,
    val lon : Int?,
    val radius : Int?,
    val error : String?
)