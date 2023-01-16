package com.ss.challengetask.datastore

import kotlinx.coroutines.flow.Flow

interface TimerPreference {
    val hourFlow: Flow<Int>
    suspend fun getHour(): Int
    suspend fun saveHour(hour: Int)

    val minsFlow: Flow<Int>
    suspend fun getMin(): Int
    suspend fun saveMin(min: Int)

    val secsFlow: Flow<Int>
    suspend fun getSec(): Int
    suspend fun saveSec(sec: Int)
}