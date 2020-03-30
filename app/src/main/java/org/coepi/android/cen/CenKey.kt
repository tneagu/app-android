package org.coepi.android.cen

// see https://docs.google.com/document/d/1f65V3PI214-uYfZLUZtm55kdVwoazIMqGJrxcYNI4eg/edit#
//  S, L, K0 are generated in the function "generateK0" which is called when there are no keys
//  currentKey + currentPeriod are generated in the function "generateCurrentKey"
data class CenKey(
    val S: ByteArray, // S, a secret nonce, of at least 128 bits
    val L: ByteArray, // L = H(S), a short label
    var K0: ByteArray,  // a fresh secret key for the session
    var K0Timestamp: Int,  // the time of K0
    val currentKey: ByteArray, // current key
    val currentPeriod: Int       // currentPeriod, matching currentKey
    )

// A session ends when a report of an infection is filled