package org.coepi.android.cen

import io.realm.RealmObject
import java.util.Date

open class RealmCenReport(
    var id: Int = 0,
    var report: String = "",
    var reportMimeType: String = "",
    var date: Date = Date(),
    var isUser: Boolean = false,

    // Reveal
    var L: String,     // short label L from CenKey
    var key: String,   // Key_{j-1}
    var j : Int,       // initial period j
    var jMax : Int     // number of periods
    ): RealmObject()



