package org.coepi.android.cen

data class CenKey(
    val S: String,
    val L: String,
    val timestamp: Int,
    val currentPeriod: Int,
    var key: String = ""
    )
