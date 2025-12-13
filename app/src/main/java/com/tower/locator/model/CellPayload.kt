package com.tower.locator.model

data class CellPayload(
    val mcc: Int,
    val mnc: Int,
    val tac: Int,
    val cid: Long,
    val rsrp: Int?
)
