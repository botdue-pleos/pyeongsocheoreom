package com.botdue.orae.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.botdue.orae.engine.WanderingDetector
import kotlinx.coroutines.delay

/**
 * 주행 중: 큰 글씨 회전 안내만 보여준다.
 * - 깜빡이 알림을 켰으면 회전 200m 앞부터 "오른쪽 깜빡이를 켜 주세요"가 뜬다 (실제 차량에서는 TTS로도 말한다).
 * - 함께 정한 조건으로 휴게소를 경유지에 넣었으면 그 사실을 보여준다.
 * - 자주 다니던 길에서 경로 재탐색이 반복되면 집으로 안내할지 묻는다.
 * 시연에서는 남은 거리가 1초마다 줄어들고, "시연: 경로 재탐색"이 NaviHelper onReRouteEnded를,
 * "시연: 도착(P단)"이 기어 P 신호를 대신한다. [clean](캡처 모드)이면 두 시연 버튼을 숨긴다.
 * [wander]면 경로 재탐색 두 번 뒤로 시작한다 (캡처 모드에서 길 헤맴 화면을 열기 위해).
 */
@Composable
fun DrivingScreen(
    place: Place,
    reminderOn: Boolean,
    restStop: String?,
    clean: Boolean = false,
    wander: Boolean = false,
    onPark: () -> Unit,
) {
    var distance by remember { mutableIntStateOf(400) }
    var target by remember { mutableStateOf(place) }
    val reRoutes = remember {
        mutableStateListOf<Long>().apply {
            if (wander) System.currentTimeMillis().let { addAll(listOf(it - 60_000, it)) }
        }
    }
    val wandering = WanderingDetector().isWandering(target.pastTrips, reRoutes)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            distance = if (distance <= 0) 400 else distance - 25
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .page()
            .padding(64.px)
    ) {
        Header("${target.name.toward()} 가는 중", target.minutes.ifEmpty { null })
        if (restStop != null && target == place) {
            Spacer(Modifier.height(16.px))
            T("가는 길에 ${restStop.obj()} 들러요", 28.tx, color = C.Sub)
        }

        if (wandering) {
            WanderingCard(
                onHome = { target = HOME; reRoutes.clear(); distance = 400 },
                onContinue = { reRoutes.clear() },
            )
            return@Column
        }

        Spacer(Modifier.height(if (restStop != null) 76.px else 120.px))
        // 회전 화살표는 남은 거리 숫자 옆, 같은 높이에. 한눈에 "어디서·어느 쪽"을 같이 읽게 한다
        Row(verticalAlignment = Alignment.CenterVertically) {
            RightTurnArrow()
            Spacer(Modifier.width(24.px))
            T("${distance}m", 168.tx, weight = FontWeight.ExtraBold, lineHeight = 1f)
        }
        Spacer(Modifier.height(28.px))
        T("오른쪽으로 도세요", 60.tx, weight = FontWeight.Bold)
        if (reRoutes.isNotEmpty()) {
            Spacer(Modifier.height(16.px))
            T("길을 다시 찾았어요.", 30.tx, color = C.Sub)
        }

        Spacer(Modifier.weight(1f))
        if (reminderOn && distance in 1..200) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.Note, RoundedCornerShape(18.px))
                    .padding(40.px),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BlinkerIcon()
                Column(Modifier.padding(start = 28.px)) {
                    T("오른쪽 깜빡이를\n켜 주세요", 50.tx, weight = FontWeight.Bold, lineHeight = 1.2f)
                    Spacer(Modifier.height(12.px))
                    T("말로도 한 번 알려드렸어요.", 28.tx, color = C.Sub)
                }
            }
            Spacer(Modifier.height(32.px))
        }
        // 시연용: 실제 차량에서는 NaviHelper 재탐색 이벤트와 기어 P 신호가 대신한다. 캡처 모드에서는 숨긴다
        if (!clean) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
                OutlineButton("시연: 경로 재탐색", Modifier.weight(1f)) { reRoutes += System.currentTimeMillis() }
                OutlineButton("시연: 도착(P단)", Modifier.weight(1f), onPark)
            }
        }
    }
}

/** 자주 다니던 길에서 헤맬 때: 탓하지 않고 집 안내를 제안한다 */
@Composable
private fun ColumnScope.WanderingCard(onHome: () -> Unit, onContinue: () -> Unit) {
    Spacer(Modifier.height(120.px))
    T("길을 몇 번\n다시 찾고 있어요", 64.tx, weight = FontWeight.Bold, lineHeight = 1.22f)
    Spacer(Modifier.height(28.px))
    T("천천히 가셔도 괜찮아요.\n집으로 안내해 드릴까요?", 36.tx, color = C.Sub, lineHeight = 1.5f)
    Spacer(Modifier.weight(1f))
    PrimaryButton("집으로 가기", Modifier.fillMaxWidth(), onHome)
    Spacer(Modifier.height(16.px))
    OutlineButton("이대로 계속 갈게요", Modifier.fillMaxWidth(), onContinue)
}

@Composable
private fun RightTurnArrow() {
    Canvas(
        Modifier
            .size(190.px)
            .semantics { contentDescription = "우회전" }
    ) {
        val u = size.width / 24f
        val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply {
            moveTo(7 * u, 21 * u); lineTo(7 * u, 12 * u)
            cubicTo(7 * u, 9.8f * u, 8.8f * u, 8 * u, 11 * u, 8 * u); lineTo(19 * u, 8 * u)
        }, C.Ink, style = stroke)
        drawPath(Path().apply {
            moveTo(15 * u, 4 * u); lineTo(19 * u, 8 * u); lineTo(15 * u, 12 * u)
        }, C.Ink, style = stroke)
    }
}

/** 방향지시등: 도장 같은 동그라미 셋이 오른쪽으로 갈수록 진해지며 깜빡인다 */
@Composable
private fun BlinkerIcon() {
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Canvas(
        Modifier
            .size(96.px)
            .semantics { contentDescription = "오른쪽 깜빡이" }
    ) {
        val u = size.width / 30f
        listOf(4f to 0.35f, 15f to 0.65f, 26f to 1f).forEach { (x, a) ->
            drawCircle(C.Gold.copy(alpha = a * blink), radius = 4.2f * u, center = Offset(x * u, 15f * u))
        }
    }
}
