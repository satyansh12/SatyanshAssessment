package com.ss.challengetask.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class TimerDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
): TimerPreference {
    companion object{
        val KEY_HOURS = intPreferencesKey(name = "timer_dur_hours")
        val KEY_MINS = intPreferencesKey(name = "timer_dur_minutes")
        val KEY_SECS = intPreferencesKey(name = "timer_dur_seconds")
        val DEFAULT_VALUE_HOURS = 0
        val DEFAULT_VALUE_MINS = 15
        val DEFAULT_VALUE_SECONDS = 0
    }
    override val hourFlow = dataStore.data.map { preference -> preference[KEY_HOURS] ?: DEFAULT_VALUE_HOURS }
    override suspend fun getHour(): Int = hourFlow.mapNotNull { it }.first()
    override suspend fun saveHour(hour: Int) { dataStore.edit { preference -> preference[KEY_HOURS] = hour } }

    override val minsFlow = dataStore.data.map { preference -> preference[KEY_MINS] ?: DEFAULT_VALUE_MINS }
    override suspend fun getMin(): Int = minsFlow.mapNotNull { it }.first()
    override suspend fun saveMin(min: Int) { dataStore.edit { preference -> preference[KEY_MINS] = min } }

    override val secsFlow = dataStore.data.map { preference -> preference[KEY_SECS] ?: DEFAULT_VALUE_SECONDS }
    override suspend fun getSec(): Int = secsFlow.mapNotNull { it }.first()
    override suspend fun saveSec(sec: Int) { dataStore.edit { preference -> preference[KEY_SECS] = sec } }
}
