package com.ss.challengetask.utils.timer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import com.ss.challengetask.viewmodel.CountTimeViewModel
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ss.challengetask.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@ExperimentalAnimationApi
@Composable
fun TimerApp(countTimeViewModel: CountTimeViewModel) {
    val secs = countTimeViewModel.seconds.observeAsState()
    val minutes = countTimeViewModel.minutes.observeAsState()
    val hours = countTimeViewModel.hours.observeAsState()
    val resumed = countTimeViewModel.isRunning.observeAsState()

    val context = LocalContext.current
    val progress = countTimeViewModel.progress.observeAsState(1f)
    val timeShow = countTimeViewModel.time.observeAsState(initial = "00:00:00")
    val scope = rememberCoroutineScope()
    LaunchedEffect(true) {
        delay(500)
        countTimeViewModel.cancelTimer()
        countTimeViewModel.startCountDown()
    }

//    countTimeViewModel.addOnRestartCallback {
//        Toast.makeText(context, "restart -> saving to cloud  ",Toast.LENGTH_SHORT).show()
//        if (resumed.value != true) {
//            countTimeViewModel.startCountDown()
//        } else {
//            countTimeViewModel.cancelTimer()
//        }
//    }

    Surface(color = Color.Black) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(82.dp)
                    .clickable {
                        if (!((secs.value ?: 0) == 0 && (minutes.value ?: 0) == 0 && (hours.value
                                ?: 0) == 0)
                        ) {
                            if (resumed.value != true) {
                                countTimeViewModel.startCountDown()
                            } else {
                                countTimeViewModel.cancelTimer()
                            }
                        } else null
                    }
                    .padding(4.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.counter_bg),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(alpha = .3f)
                        .clip(CircleShape) ,
                    contentScale = ContentScale.Crop
                )
                CircularProgressIndicator(
                    color = if(resumed.value != true) Color.Green else Color.Red,
                    modifier = Modifier.fillMaxSize(),
                    progress = progress.value,
                    strokeWidth = 2.dp
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ReusableHeaderText(text = timeShow.value, color = Color.White)
                }
                Box(modifier = Modifier
                    .align(alignment = Alignment.BottomCenter)
                    .padding(bottom = 4.dp)) {
                    if(resumed.value == true) Icon(painterResource(id = R.drawable.icon_pause_svg), null,tint = Color.Red, modifier = Modifier.size(16.dp))
                    else Icon(painterResource(id = R.drawable.icon_play_svg),null, tint = Color.Green,modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                    val hourState = countTimeViewModel.hourFlow.collectAsState(initial = 0)
                    val minFlow = countTimeViewModel.minFlow.collectAsState(initial = 0)
                    val secFlow = countTimeViewModel.secFlow.collectAsState(initial = 0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(4.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerComponent(
                        value = hourState.value,
                    ) {
                        if (resumed.value == true) countTimeViewModel.cancelTimer()
                        countTimeViewModel.modifyTime(CountTimeViewModel.Companion.TimeUnit.HOUR, it)
                    }
                    Text(text = " : ", fontSize = 36.sp)
                    TimerComponent(
                        value = minFlow.value,
                    ) {
                        if (resumed.value == true) countTimeViewModel.cancelTimer()
                        countTimeViewModel.modifyTime(CountTimeViewModel.Companion.TimeUnit.MIN, it)
                    }
                    Text(text = " : ", fontSize = 36.sp)
                    TimerComponent(
                        value = secFlow.value,
                    ) {
                        if (resumed.value == true) countTimeViewModel.cancelTimer()
                        countTimeViewModel.modifyTime(CountTimeViewModel.Companion.TimeUnit.SEC, it)
                    }
                } // row
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimerComponent(
    value: Int?,
    onClick: (CountTimeViewModel.Companion.TimeOperator) -> Unit
) {
    Column(modifier = Modifier.wrapContentSize(align = Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val typography = MaterialTheme.typography
        OperatorButton(
            timeOperator = CountTimeViewModel.Companion.TimeOperator.INCREASE,
            onClick = onClick
        )
        Text(
            text = String.format("%02d", value ?: 0),
            fontSize = 14.sp,
            color = Color.White
        )
        OperatorButton(
            timeOperator = CountTimeViewModel.Companion.TimeOperator.DECREASE,
            onClick = onClick
        )
    }
}

@Composable
fun CustomSpinner(defaultValue: Int, incrementCallback: (Int) -> Unit, decrementCallback: (Int) -> Unit) {
    val currentValue = remember { mutableStateOf(defaultValue) }
    Row(modifier = Modifier.padding(16.dp)) {
        // Up arrow button
        IconButton(onClick = { currentValue.value++; incrementCallback(currentValue.value) }) {
            Icon(Icons.Default.KeyboardArrowUp,null, modifier = Modifier.size(24.dp))
        }
        // Text view displaying the current value
        Text(text = "$currentValue", modifier = Modifier.padding(8.dp))
        // Down arrow button
        IconButton(onClick = { currentValue.value--; decrementCallback(currentValue.value) }) {
            Icon(Icons.Default.KeyboardArrowDown,null, modifier = Modifier.size(24.dp))
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun OperatorButton(
    timeOperator: CountTimeViewModel.Companion.TimeOperator,
    onClick: (CountTimeViewModel.Companion.TimeOperator) -> Unit
) {
        when (timeOperator) {
            CountTimeViewModel.Companion.TimeOperator.INCREASE -> Icon(
                Icons.Outlined.KeyboardArrowUp,
                null,
                Modifier
                    .size(24.dp)
                    .clickable { onClick.invoke(timeOperator) }
                    .background(Color.White)
                    .clip(CircleShape),
                tint = Color.Black
            )
            CountTimeViewModel.Companion.TimeOperator.DECREASE -> Icon(
                Icons.Outlined.KeyboardArrowDown,
                null,
                Modifier
                    .size(24.dp)
                    .clickable { onClick.invoke(timeOperator) }
                    .background(Color.White)
                    .clip(CircleShape),
                tint = Color.Black
            )
        }
}

@Composable
fun ReusableHeaderText(text: String, color: Color) {
    Text(text = text, fontSize= 14.sp, textAlign = TextAlign.Center,style = MaterialTheme.typography.headlineMedium, color = color)
}
