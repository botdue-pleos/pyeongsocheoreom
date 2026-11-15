package com.botdue.orae

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.botdue.orae.engine.DemoScenario
import com.botdue.orae.engine.Advice
import com.botdue.orae.engine.Condition
import com.botdue.orae.engine.ConditionAdvisor
import com.botdue.orae.engine.ConditionEvent
import com.botdue.orae.engine.ConditionRecord
import com.botdue.orae.engine.NextStepAdvisor
import com.botdue.orae.engine.Trip
import com.botdue.orae.ui.AdviceScreen
import com.botdue.orae.ui.C
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
import com.botdue.orae.ui.FitWindow

enum class Screen { Greeting, Advice, Driving, Parked, Change, Family }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 화면 캡처·영상용: adb shell am start ... --es screen Family --ez conds true --ez kept true --ez clean true --ez night true
        val start = StartAt(
            screen = intent.getStringExtra("screen")?.let { runCatching { Screen.valueOf(it) }.getOrNull() } ?: Screen.Greeting,
            place = intent.getIntExtra("place", 0),
            evening = intent.getBooleanExtra("evening", false),
            conds = intent.getBooleanExtra("conds", false),
            kept = intent.getBooleanExtra("kept", false),
            clean = intent.getBooleanExtra("clean", false),
            night = intent.getBooleanExtra("night", false),
            wander = intent.getBooleanExtra("wander", false),
        )
        // 첫 장면부터 밤 색으로 그리도록 미리 정한다 (흰 화면이 한 번 번쩍이지 않게)
        val systemNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        C.night = start.night || systemNight
        setContent {
            FitWindow {
                // 시스템 야간 모드가 바뀌면(uiMode는 configChanges라 액티비티가 다시 만들어지지 않는다) 여기서 따라간다
                val night = start.night || isSystemInDarkTheme()
                SideEffect {
                    C.night = night
                    window.statusBarColor = C.Ground.toArgb()
                    window.navigationBarColor = C.Ground.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !night
                        isAppearanceLightNavigationBars = !night
                    }
                }
                OraeApp(start)
            }
        }
    }
}

data class StartAt(
    val screen: Screen = Screen.Greeting,
    val place: Int = 0,
    val evening: Boolean = false,
    /** 가족과 정한 조건을 모두 켠 상태로 시작 */
    val conds: Boolean = false,
    /** 조건을 지킨 기록(밤 출발 미룸·휴게소 경유)이 있는 상태로 시작 */
    val kept: Boolean = false,
    /** 캡처 모드: "시연:" 전환 글자와 주행 화면 시연 버튼을 숨긴다 */
    val clean: Boolean = false,
    /** 시스템 설정과 상관없이 밤 색으로 */
    val night: Boolean = false,
    /** 주행 화면을 경로 재탐색 두 번 뒤(길 헤맴)로 시작. --ei place 2(큰딸 집)와 함께 */
    val wander: Boolean = false,
)

@Composable
fun OraeApp(start: StartAt = StartAt()) {
    val assets = LocalContext.current.assets
    val font = remember { astaSans(assets) }

    // 시연은 7주째(최근 3주 우회전 깜빡이 놓침 증가)로 시작한다
    var scenario by remember { mutableStateOf(DemoScenario.CHANGED) }
    val summary = remember(scenario) { Summary(scenario) }
    var screen by remember { mutableStateOf(start.screen) }
    var place by remember { mutableStateOf(PLACES[start.place.coerceIn(0, PLACES.lastIndex)]) }
    var conditions by remember { mutableStateOf(if (start.conds) Condition.entries.toSet() else emptySet()) }
    val reminderOn = Condition.REMIND in conditions
    // 시연용 출발 시각: 아침 8시 또는 저녁 7시
    var evening by remember { mutableStateOf(start.evening) }
    var restStop by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // 조건이 작동할 때마다 남긴다. 실제로는 로컬 DB에 쌓는다
    var events by remember {
        mutableStateOf(
            if (start.kept) listOf(
                ConditionEvent(Condition.NEAR_AT_NIGHT, followed = true),
                ConditionEvent(Condition.NEAR_AT_NIGHT, followed = false),
                ConditionEvent(Condition.REST_ON_HIGHWAY, followed = true),
            ) else emptyList()
        )
    }

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
                clean = start.clean,
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
            Screen.Driving -> DrivingScreen(place, reminderOn, restStop, clean = start.clean, wander = start.wander && start.screen == Screen.Driving) {
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
