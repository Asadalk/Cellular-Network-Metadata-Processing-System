package com.tower.locator.model

data class LocateResponse(
    val lat : Double?,
    val lon : Double?,
    val radius : Double?,
    val error : String?
)