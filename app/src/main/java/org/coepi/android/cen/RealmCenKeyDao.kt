package org.coepi.android.cen

import io.realm.Sort.DESCENDING
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.coepi.android.repo.RealmProvider

class RealmCenKeyDao(private val realmProvider: RealmProvider) {
    private val realm get() = realmProvider.realm

    fun lastCENKey(): RealmCenKey? =
        realm.where<RealmCenKey>()
            .sort("timestamp", DESCENDING)
            .limit(1)
            .findFirst()

    fun insert(key: CenKey) {
        realm.executeTransaction {
            val realmObj = realm.createObject<RealmCenKey>()
            realmObj.S = key.S.toString()
            realmObj.L = key.L.toString()
            realmObj.K0 = key.K0.toString()
            realmObj.K0timestamp = key.K0Timestamp
            realmObj.currentKey = key.currentKey
        }
    }

    fun updateKey(key: CenKey) {
        // TODO, figure out REALM
        realm.executeTransaction {
            val realmObj = realm.createObject<RealmCenKey>()
            realmObj.S = key.S.toString()
            realmObj.L = key.L.toString()
            realmObj.K0 = key.K0.toString()
            realmObj.K0timestamp = key.K0Timestamp
            realmObj.currentKey = key.currentKey
        }
    }


    // @Query("SELECT * FROM cenkey WHERE :first <= timeStamp AND timeStamp <= :last LIMIT 1")
    // fun findByRange(first: Int?, last: Int?): List<CENKey>?

    // @Delete
    // fun deleteBefore(timestamp : Int)
}

