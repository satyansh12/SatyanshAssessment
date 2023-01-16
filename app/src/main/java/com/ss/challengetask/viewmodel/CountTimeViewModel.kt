package com.ss.challengetask.viewmodel

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ss.challengetask.datastore.TimerPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CountTimeViewModel @Inject constructor(
    private val timerPreference: TimerPreference
) : ViewModel()  {
    private var countDownTimer: CountDownTimer? = null

    fun updatePrefMin(min : Int){ viewModelScope.launch { timerPreference.saveMin(min) }}
    fun updatePrefHour(hour : Int){ viewModelScope.launch { timerPreference.saveHour(hour) }}
    fun updatePrefSec(sec : Int){ viewModelScope.launch { timerPreference.saveSec(sec) }}

    suspend fun getHour() = timerPreference.getHour()
    suspend fun getMins() = timerPreference.getMin()
    suspend fun getSecs() = timerPreference.getSec()

    private var onFinishCallback : (() -> Unit)? = null
    private var onRestartCallback : (() -> Unit)? = null
    fun addOnFinishCallback(callback : () -> Unit) {
        onFinishCallback = callback
    }
    fun addOnRestartCallback(callback : () -> Unit) {
        onRestartCallback = callback
    }
    val hourFlow = timerPreference.hourFlow
    val minFlow = timerPreference.minsFlow
    val secFlow = timerPreference.secsFlow

    val isRunning: LiveData<Boolean>
        get() = _isRunning

    private val _seconds = MutableLiveData(0)
    val seconds: LiveData<Int>
        get() = _seconds

    private val _minutes = MutableLiveData(0)
    val minutes: LiveData<Int>
        get() = _minutes

    private val _hours = MutableLiveData(0)
    val hours: LiveData<Int>
        get() = _hours

    private val _isRunning = MutableLiveData(false)

    private val _progress = MutableLiveData(1f)
    val progress: LiveData<Float>
        get() = _progress

    private val _time = MutableLiveData("00:00:00")
    val time: LiveData<String>
        get() = _time

    var totalTime = 0L

    init {
        // get the default timer duration from the timerPreference
        initHMSDetails()
    }

    private fun initHMSDetails() {
        viewModelScope.launch {
            val hour = timerPreference.getHour()
            val min = timerPreference.getMin()
            val sec = timerPreference.getSec()
            withContext(Dispatchers.Main) {
                _hours.postValue(hour)
                _minutes.postValue(min)
                _seconds.postValue(sec)
            }
        }
    }

    fun startCountDown() {
        if (countDownTimer != null) {
            cancelTimer()
        }
        initHMSDetails()
        totalTime = ((getSeconds() * 1000).toLong())
        Log.d("timerDuration", "totalTime -> ${totalTime}")
        countDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisecs: Long) {
                // Seconds
                val secs = (millisecs / MSECS_IN_SEC % SECS_IN_MINUTES).toInt()
                if (secs != seconds.value) {
                    _seconds.postValue(secs)
                }
                // Minutes
                val minutes = (millisecs / MSECS_IN_SEC / SECS_IN_MINUTES % SECS_IN_MINUTES).toInt()
                if (minutes != this@CountTimeViewModel.minutes.value) {
                    _minutes.postValue(minutes)
                }
                // Hours
                val hours = (millisecs / MSECS_IN_SEC / MINUTES_IN_HOUR / SECS_IN_MINUTES).toInt()
                if (hours != this@CountTimeViewModel.hours.value) {
                    _hours.postValue(hours)
                }

                _progress.postValue(millisecs.toFloat() / totalTime.toFloat())
                _time.postValue(formatHourMinuteSecond(hours, minutes, secs))
            }

            override fun onFinish() {
                _progress.postValue(1.0f)
                _isRunning.postValue(false)
                onFinishCallback?.invoke()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    startCountDown()
                }, 500)
            }
        }
        countDownTimer?.start()
        _isRunning.postValue(true)
    }

    fun modifyTime(timeUnit: TimeUnit, timeOperator: TimeOperator) {
        var seconds = seconds.value ?: 0
        var minutes = minutes.value ?: 0
        var hours = hours.value ?: 0

        when (timeUnit) {
            TimeUnit.SEC -> {
                seconds = updateTime(seconds, timeOperator).coerceIn(0, 59)
                viewModelScope.launch { timerPreference.saveSec(seconds) }
            }
            TimeUnit.MIN ->{
                minutes = updateTime(minutes, timeOperator).coerceIn(0, 59)
                viewModelScope.launch { timerPreference.saveMin(minutes) }
            }
            TimeUnit.HOUR ->{
                hours = updateTime(hours, timeOperator).coerceIn(0, 23)
                viewModelScope.launch { timerPreference.saveHour(hours) }
            }
        }

        _seconds.postValue(seconds)
        _minutes.postValue(minutes)
        _hours.postValue(hours)

        _time.postValue(String.format("%02d:%02d:%02d", hours, minutes, seconds))
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
        _progress.postValue(1.0f)
        _isRunning.postValue(false)
    }

    private fun formatHourMinuteSecond(hours : Int,minutes : Int,seconds : Int) =
        String.format("%02d:%02d:%02d", hours, minutes, seconds)

    fun cancelTimer() {
        countDownTimer?.cancel()
        _isRunning.postValue(false)
    }

    private fun getSeconds() = ((hours.value ?: 0) * MINUTES_IN_HOUR * SECS_IN_MINUTES) + ((minutes.value
        ?: 0) * SECS_IN_MINUTES) + (seconds.value ?: 0)


    private fun updateTime(currentValue: Int, timeOperator: TimeOperator): Int {
        return when (timeOperator) {
            TimeOperator.INCREASE -> currentValue + 1
            TimeOperator.DECREASE -> currentValue - 1
        }
    }

    companion object {
        enum class TimeOperator {
            INCREASE, DECREASE
        }

        enum class TimeUnit {
            SEC, MIN, HOUR
        }
        const val MINUTES_IN_HOUR = 60
        const val SECS_IN_MINUTES = 60
        const val MSECS_IN_SEC = 1000
    }

}