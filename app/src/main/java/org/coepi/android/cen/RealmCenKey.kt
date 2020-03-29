package org.coepi.android.cen

import io.realm.RealmObject
import java.util.Date

open class RealmCenKey(
    var S  : String = "",       // seed
    var L  : String = "",       // L = H(S)
    var timestamp : Int = 0,    // timestamp of period 0, rounded down
    var currentPeriod : Int = 0,
    var key: String = ""
): RealmObject()
