package com.botdue.orae

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.botdue.orae.engine.DemoScenario
import com.botdue.orae.engine.Advice
import com.botdue.orae.engine.Condition
import com.botdue.orae.engine.ConditionAdvisor
import com.botdue.orae.engine.ConditionEvent
import com.botdue.orae.engine.ConditionRecord
import com.botdue.orae.engine.NextStepAdvisor
import com.botdue.orae.engine.Trip
import com.botdue.orae.ui.AdviceScreen
import com.botdue.orae.ui.CARE_CENTERS
import com.botdue.orae.ui.ChangeScreen
import com.botdue.orae.ui.DRIVER_AGE
import com.botdue.orae.ui.DrivingScreen
import com.botdue.orae.ui.FamilyScreen
import com.botdue.orae.ui.GreetingScreen
import com.botdue.orae.ui.LocalFont
import com.botdue.orae.ui.PLACES
import com.botdue.orae.ui.ParkedScreen
import com.botdue.orae.ui.Summary
import com.botdue.orae.ui.astaSans
import com.botdue.orae.ui.toPlace

enum class Screen { Greeting, Advice, Driving, Parked, Change, Family }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OraeApp() }
    }
}

@Composable
fun OraeApp() {
    val assets = LocalContext.current.assets
    val font = remember { astaSans(assets) }

    // 시연은 7주째(최근 3주 우회전 깜빡이 놓침 증가)로 시작한다
    var scenario by remember { mutableStateOf(DemoScenario.CHANGED) }
    val summary = remember(scenario) { Summary(scenario) }
    var screen by remember { mutableStateOf(Screen.Greeting) }
    var place by remember { mutableStateOf(PLACES.first()) }
    var conditions by remember { mutableStateOf(emptySet<Condition>()) }
    val reminderOn = Condition.REMIND in conditions
    // 시연용 출발 시각: 아침 8시 또는 저녁 7시
    var evening by remember { mutableStateOf(false) }
    var restStop by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // 조건이 작동할 때마다 남긴다. 실제로는 로컬 DB에 쌓는다
    var events by remember { mutableStateOf(emptyList<ConditionEvent>()) }

    fun depart(p: com.botdue.orae.ui.Place) {
        val trip = Trip(p.name, p.distanceKm, p.usesHighway, hour = if (evening) 19 else 8, restStop = p.restStop)
        val advice = ConditionAdvisor().advise(trip, conditions)
        place = p
        notice = null
        restStop = advice.filterIsInstance<Advice.AddRestStop>().firstOrNull()?.restStop
        screen = if (advice.any { it is Advice.SuggestDaytime }) Screen.Advice else Screen.Driving
    }

    BackHandler(enabled = screen != Screen.Greeting) {
        screen = when (screen) {
            Screen.Change, Screen.Family -> Screen.Parked
            else -> Screen.Greeting
        }
    }

    CompositionLocalProvider(LocalFont provides font) {
        when (screen) {
            Screen.Greeting -> GreetingScreen(
                summary = summary,
                reminderOn = reminderOn,
                evening = evening,
                notice = notice,
                onPlace = { depart(it) },
                onSwitchScenario = {
                    scenario = if (scenario == DemoScenario.CHANGED) DemoScenario.STABLE else DemoScenario.CHANGED
                },
                onSwitchTime = { evening = !evening; notice = null },
            )
            Screen.Advice -> AdviceScreen(
                place = place,
                onLater = {
                    events = events + ConditionEvent(Condition.NEAR_AT_NIGHT, followed = true)
                    notice = "${place.name}은 내일 아침에 다시 여쭤볼게요."
                    screen = Screen.Greeting
                },
                onGoAnyway = {
                    events = events + ConditionEvent(Condition.NEAR_AT_NIGHT, followed = false)
                    screen = Screen.Driving
                },
            )
            Screen.Driving -> DrivingScreen(place, reminderOn, restStop) {
                // 시연: 도착하면 휴게소 경유지를 지난 것으로 본다. 실제로는 NaviHelper 경유지 통과로 판단
                if (restStop != null) events = events + ConditionEvent(Condition.REST_ON_HIGHWAY, followed = true)
                screen = Screen.Parked
            }
            Screen.Parked -> ParkedScreen(
                summary = summary,
                onDone = { screen = Screen.Greeting },
                onDetail = { screen = Screen.Change },
                onFamily = { screen = Screen.Family },
            )
            Screen.Change -> {
                val report = summary.change
                if (report == null) {
                    screen = Screen.Parked
                } else {
                    ChangeScreen(
                        summary = summary,
                        report = report,
                        center = NextStepAdvisor().freeCheck(report, DRIVER_AGE, CARE_CENTERS),
                        onAccept = { conditions = conditions + Condition.REMIND; screen = Screen.Greeting },
                        onDecline = { screen = Screen.Greeting },
                        onGuide = { depart(it.toPlace()) },
                    )
                }
            }
            Screen.Family -> FamilyScreen(
                summary = summary,
                conditions = conditions,
                kept = ConditionRecord(events).kept(),
                onToggle = { c -> conditions = if (c in conditions) conditions - c else conditions + c },
                onConfirm = { screen = Screen.Greeting },
                onBack = { screen = Screen.Parked },
            )
        }
    }
}
