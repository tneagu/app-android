package org.coepi.android.cen

import io.realm.RealmObject
import java.util.Date

open class RealmCenKey(
    var S  : String = "",      // seed
    var L  : String = "",      // L = H(S)
    var K0 : String = "",      // timestamp of period 0, rounded down
    var K0timestamp : Int = 0, // timestamp of period 0, rounded down
    var currentKey: ByteArray, // current key
    var currentPeriod: Int     // currentPeriod, matching currentKey
): RealmObject()


