package org.coepi.android.cen

import java.util.Date

data class CenReport(
    var id: Int,
    var report: String,
    var reportMimeType: String,
    var date: Date,
    var isUser: Boolean,

    // Reveal
    var L: String,     // short label L from CenKey
    var key: String,   // Key_{j-1}
    var j : Int,       // initial period j
    var jMax : Int     // number of periods
)


