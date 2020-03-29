package org.coepi.android.cen

import io.realm.RealmObject
import java.util.Date

open class RealmCenReport(
    var id: Int = 0,
    var report: String = "",
    var key: String = "",
    var L: String = "",
    var period: Int = 0,
    var reportMimeType: String = "",
    var date: Date = Date(),
    var isUser: Boolean = false
): RealmObject()
