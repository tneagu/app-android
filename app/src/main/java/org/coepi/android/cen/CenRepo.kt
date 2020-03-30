package org.coepi.android.cen

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import org.coepi.android.system.log.log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.MessageDigest
import kotlin.random.Random.Default.nextBytes

// CenRepo coordinates local Storage (Realm) and network API calls to manage the CEN protocol
//  (A) refreshCEN: CEN generation every 7 days, CEN Generation every 15 minutes  (stored in Room)
//  (B) insertCEN: Storage of observed CENs from other BLE devices
class CenRepo(private val cenApi: CENApi, private val cenDao: RealmCenDao, private val cenkeyDao: RealmCenKeyDao, private val cenReportDao: RealmCenReportDao)  {
    // CEN Management
    // currentCENKey contains S, L, ... which are used to generate K1, ...K_i and then K_i are used to generate CENs
    var currentCENKey : CenKey? = null

    // the latest CEN (ByteArray form), generated using currentCENKey
    var CEN : MutableLiveData<ByteArray> = MutableLiveData<ByteArray>()

    var CENLifetimeInSeconds = 15   // every 15 secs a new CEN is generated (will be some random number centered around 15 mins to not have identifiability

    private val periodicGetCENReportFrequencyInSeconds = 30 // run every 30s (should be every hour)

    // lastGetCENReport is the last time (unix timestamp) the CENReports were requested
    var lastGetCENReport = 0

    init {
        CEN.value = ByteArray(0)

        // load CENKey from local storage, if it exists
        val r = cenkeyDao.lastCENKey()
        r?.let {
            currentCENKey = CenKey(it.S.toByteArray(), it.L.toByteArray(), it.K0.toByteArray(), it.K0timestamp, it.currentKey, it.currentPeriod)
        }

        // Setup regular CEN refresh, which powers CEN broadcast
        refreshCEN()

        // Setup regular CENReports
        periodicGetCENReport()
    }

    // generateK0 generates the S, L=H(S), and K0 ; this is done upon initialization and upon "finishing" a case.
    private fun generateK0() : CenKey {
        val S = nextBytes(ByteArray(32), 0, 32)  // S, a secret nonce ; this could be the hash of the public key for the session
        val L = computeHashFromBytes(S)  // L = H(S)
        val K0Timestamp = (System.currentTimeMillis() / 1000L).toInt()
        val K0 = nextBytes(ByteArray(32), 0, 32) // a fresh secret key for the session
        val K1 = computeHashFromBytes( K0.plus(L) )
        return CenKey(S, L, K0, K0Timestamp, K1,1)
    }

    private fun computeHashFromBytes(bytes : ByteArray) : ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).toUByteArray().asByteArray()
    }

    private fun refreshCEN() {
        // if there is no CENKey at all, generate one
        if ( currentCENKey == null ) {
            val cenKey = generateK0()
            cenkeyDao.insert(cenKey)
        }
        generateNextKey()

        Handler().postDelayed({
            refreshCEN()
        }, CENLifetimeInSeconds * 1000L)
    }

    // generateNextKey will generate the next Key Ki ← H(Ki-1, L), if needed
    private fun generateNextKey() {
        currentCENKey?.let {
            val originalTimestamp = it.K0Timestamp
            val currentTimestamp = (System.currentTimeMillis() / 1000L).toInt()
            val currentPeriod = ( currentTimestamp - originalTimestamp ) / CENLifetimeInSeconds

            // move the hash chain a fixed number of steps
            val steps = ( currentPeriod - it.currentPeriod )
            if ( steps == 0 ) {
                return
            }
            var curKey = it.currentKey
            for ( i in 0 .. steps ) {
                curKey = computeHashFromBytes( curKey.plus(it.L) )
            }
            cenkeyDao.updateKey(CenKey(it.S, it.L, it.K0, it.K0Timestamp, it.currentKey, currentPeriod))
            // CENi = H(Ki, periodi)
            val cen = computeHashFromBytes(curKey.plus(IntToByteArray(currentPeriod)))
            CEN.value = cen
        }
    }

    // when a peripheral CEN is detected through BLE, it is recorded here
    fun insertCEN(CEN: String) {
        val c = Cen(
            CEN,
            (System.currentTimeMillis() / 1000L).toInt()
        )
        cenDao.insert(c)
    }

    // CEN API Calls: mapping into Server Endpoints via Retrofit
    // 1. Client publicizes report to /cenreport along with 3 CENKeys (base64 encoded)
    private fun postCENReport(report : CenReport) = cenApi.postCENReport(report)

    // 2. Client periodically gets a list of CENReveals from /cenreport/<timestamp>
    private fun getCENReport(timestamp : Int) = cenApi.getCENReport(timestamp)

    // doPostSymptoms is called when a ViewModel in the UI sees the user finish a Symptoms Report
    // Reveal: upon a positive diagnosis, the app broadcasts to a health authority database:
    // the short label L, a key Kj, and the initial period j.
    // Other application users download the key Kj, and the initial period j, and can from those two precompute
    // all subsequent CENs from that initial period j and compare them with the ones seen on phone.
    // The comparison here is purely string comparison, and therefore efficient string search algorithms can be used.
    fun doPostSymptoms(report : CenReport) {
        currentCENKey?.let {
            report.L = it.L.toString()       // short label L from CenKey
            report.key = it.K0.toString()    // Key_{j-1}
            report.j = 1                     // initial period j
            val currentTimestamp = (System.currentTimeMillis() / 1000L).toInt()
            report.jMax = ( currentTimestamp - it.K0Timestamp ) / CENLifetimeInSeconds // number of periods
            // TODO: what if ( report.jMax > MAX )?
            postCENReport(report)
        }
    }

    fun checkCENReportMatch(reports : Array<CenReport>) : List<CenReport?> {
        // load all the CENs into memory: these are the ones actually seen by the device
        val allCENS = cenDao.all()
        val map = hashMapOf<ByteArray, Boolean>()
        for ( i in allCENS.indices) {
            map[allCENS[i].cen.toByteArray()] = true
        }

        // for all the reports received, regenerate the CENs, and see if there is a match:
        var matches = MutableList<CenReport?>(0) { _ -> null }
        for ( i in reports.indices ) {
            // Reveal
            // var L: String,     -- short label L from CenKey
            // var key: String,   -- Key_{j-1}
            // var j : Int,       -- initial period j
            // var jMax : Int     -- number of periods
            val r = reports[i]
            val L = r.L.toByteArray()
            var key = r.key.toByteArray()
            for (period in 0..r.jMax) {
                val k = computeHashFromBytes(key.plus(L))
                val cen = computeHashFromBytes(k.plus(IntToByteArray(period)))
                if ( map.containsKey(cen) ) {
                    // we have a match!
                    matches.add(reports[i])
                }
            }
        }
        return matches
    }

    // periodicGetCENReport fetches a report
    fun periodicGetCENReport() {
        val call = getCENReport(lastGetCENReport)
        call.enqueue(object :
            Callback<Array<CenReport>> {
            override fun onResponse(call: Call<Array<CenReport>?>?, response: Response<Array<CenReport>>) {
                val statusCode: Int = response.code()
                if ( statusCode == 200 ) {
                    val r: Array<CenReport>? = response.body()
                    r?.let {
                        if ( r.size > 0 ) {
                            // compute matches
                            val matches = checkCENReportMatch(it)
                            // insert them into the database
                            for ( i in matches.indices ) {
                                cenReportDao.insert(matches[i]!!)
                            }
                            // TODO: do something in the UI
                        }
                    }
                    lastGetCENReport = (System.currentTimeMillis() / 1000L).toInt()
                } else {
                    log.e("periodicGetCENReport $statusCode")
                }
            }

            override fun onFailure(call: Call<Array<CenReport>?>?, t: Throwable?) {
                // Log error here since request failed
                log.e("periodicGetCENReport Failure")
            }
        })
        Handler().postDelayed({
            periodicGetCENReport()
        }, periodicGetCENReportFrequencyInSeconds * 1000L)
    }


}
