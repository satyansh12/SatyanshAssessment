import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ss.challengetask.viewmodel.MainViewModel
import kotlinx.coroutines.*

@Composable
fun TimerTask(mainViewModel: MainViewModel = viewModel()) {
    val repeatDuration = remember {
        mutableStateOf<Int>(900)
    }
    LaunchedEffect(key1 = true, block = {
        repeatDuration.value = 0
    })
    val remainingTime = remember { mutableStateOf("") }
    var timerJob: Job? = null
    LaunchedEffect(true) {
        timerJob = launch {
            startTimer(repeatDuration, onFinishCallback = {}) {
                remainingTime.value = it
            }
        }
    }
    Column {
        Text(
            text = "Remaining Time: ${remainingTime.value}",
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val newDuration = showDurationPickerDialog()
            repeatDuration.value = newDuration
            // set Timer Duration
            /**/
        }) {
            Text("Set Repeat Duration")
        }
    }
}

private suspend fun startTimer(duration: MutableState<Int>, onFinishCallback: () -> Unit, updateRemainingTime: (String) -> Unit) {
    while (true) {
        val startTime = System.currentTimeMillis()
        val remainingTime = (duration.value - (System.currentTimeMillis() - startTime) / 1000)
        updateRemainingTime("${remainingTime / 60}:${remainingTime % 60}")
        delay(1000)
        if (remainingTime <= 0) {
            onFinishCallback()
            //Restart timer after capturing
            startTimer(duration,onFinishCallback,updateRemainingTime)
            break
        }
    }
}

private fun showDurationPickerDialog(): Int {
    // Code to show a dialog to pick a new repeat duration and return the selected duration
    return 0
}


