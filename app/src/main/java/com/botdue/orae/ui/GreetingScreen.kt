package com.botdue.orae.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.botdue.orae.engine.CareCenter

/** 자주 가는 곳. 실제로는 FusedLocation 지오펜스와 NaviHelper 경로 정보로 채운다 */
data class Place(
    val name: String,
    val minutes: String,
    val distanceKm: Int,
    val usesHighway: Boolean = false,
    val restStop: String? = null,
    /** 지금까지 이곳으로 다녀온 횟수 (자주 다니던 길인지 판단) */
    val pastTrips: Int,
)

val PLACES = listOf(
    Place("농협 하나로마트", "12분", distanceKm = 6, pastTrips = 48),
    Place("읍내 정형외과", "18분", distanceKm = 9, pastTrips = 15),
    Place("큰딸 집", "1시간 20분", distanceKm = 82, usesHighway = true, restStop = "여주휴게소", pastTrips = 12),
)

val HOME = Place("집", "", distanceKm = 0, pastTrips = 300)

/** 시연 운전자 나이. 실제로는 본인이 처음 설정할 때 입력한다 */
const val DRIVER_AGE = 74

/** 시연용 주변 치매안심센터 */
val CARE_CENTERS = listOf(
    CareCenter("읍 치매안심센터", distanceKm = 7, minutes = "12분"),
    CareCenter("시 치매안심센터", distanceKm = 15, minutes = "25분"),
)

fun CareCenter.toPlace() = Place(name, minutes, distanceKm, pastTrips = 0)

/**
 * 출발 인사: "오늘 어디 가세요?"를 묻고 자주 가는 곳을 크게 보여준다.
 * 실제 차량에서는 TTS로 묻고 STT로 답을 받아 목적지를 정한다.
 */
@Composable
fun GreetingScreen(
    summary: Summary,
    reminderOn: Boolean,
    evening: Boolean,
    notice: String?,
    onPlace: (Place) -> Unit,
    onSwitchScenario: () -> Unit,
    onSwitchTime: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .page()
            .verticalScroll(rememberScrollState())
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px)
    ) {
        // 수첩 머리는 오늘 날짜. 시연 시각은 아침 8시 12분 또는 저녁 7시 10분
        Header(todayLabel(), if (evening) "오후 7시 10분" else "오전 8시 12분")
        Spacer(Modifier.height(56.px))
        T(if (evening) "좋은 저녁이에요.\n어디 가세요?" else "좋은 아침이에요.\n오늘 어디 가세요?", 68.tx, weight = FontWeight.Bold, lineHeight = 1.22f)
        Spacer(Modifier.height(36.px))
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoiceBars()
            T("듣고 있어요. 말씀하시거나\n아래에서 골라 주세요.", 32.tx, Modifier.padding(start = 20.px), color = C.Sub)
        }

        Spacer(Modifier.height(44.px))
        T("자주 가시는 곳", 28.tx, color = C.Sub)
        Spacer(Modifier.height(16.px))
        PLACES.forEachIndexed { i, place ->
            PlaceButton(place, filled = i == 0) { onPlace(place) }
        }
        if (notice != null) {
            T(notice, 28.tx, weight = FontWeight.SemiBold, color = C.GoldText)
        } else if (reminderOn) {
            T("회전하기 전에 깜빡이를 한 번 더 알려드려요.", 28.tx, color = C.Sub)
        }

        Spacer(Modifier.height(20.px))
        Divider()
        Spacer(Modifier.height(28.px))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            T(
                if (summary.change != null) "이번 주는 깜빡이를 자주 놓치셨어요" else "이번 주 운전은 평소와 비슷해요",
                30.tx, Modifier.weight(1f), FontWeight.Bold,
            )
            T("회전 ${summary.thisWeek.turns}번", 26.tx, color = C.Sub)
        }
        Spacer(Modifier.height(18.px))
        TurnDots(
            turnCells(summary.thisWeek.turns, summary.thisWeek.missed), columns = 15, gap = 8.px,
            description = "이번 주 회전 ${summary.thisWeek.turns}번 중 놓친 회전 ${summary.thisWeek.missed}번",
            ring = 4.px, radius = 5.px,
        )
        Spacer(Modifier.height(18.px))
        T("칸 하나가 회전 한 번이에요. 빈 칸은 깜빡이를 놓친 회전이에요.", 26.tx, color = C.Sub)

        Spacer(Modifier.height(16.px))
        // 시연용: 첫 4주(기준선 만드는 중)와 7주째(변화 감지), 아침과 저녁 출발을 바꿔 본다
        Row(horizontalArrangement = Arrangement.spacedBy(36.px)) {
            DemoLink(if (summary.change != null) "시연: 첫 4주 기록" else "시연: 7주째 기록", onSwitchScenario)
            DemoLink(if (evening) "시연: 아침 출발" else "시연: 저녁 출발", onSwitchTime)
        }
    }
}

@Composable
private fun DemoLink(text: String, onClick: () -> Unit) {
    T(
        text, 26.tx,
        Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.px),
        FontWeight.SemiBold, C.Sub,
    )
}

/** 자주 가는 곳 한 줄: 수첩에 줄 그은 목록처럼. 첫 줄(가장 자주 가는 곳)만 동그라미를 채운다 */
@Composable
private fun PlaceButton(place: Place, filled: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(120.px),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.px)
                    .then(if (filled) Modifier.background(C.Ink, CircleShape) else Modifier.border(BorderStroke(4.px, C.Ink), CircleShape))
            )
            T(place.name, 40.tx, Modifier.padding(start = 28.px).weight(1f), FontWeight.Bold)
            T(place.minutes, 30.tx, color = C.Sub)
        }
        Box(Modifier.fillMaxWidth().height(2.px).background(C.Line))
    }
}

/** 음성 대기 표시: 픽셀 막대 5개, 가운데만 금색 */
@Composable
private fun VoiceBars() {
    Canvas(
        Modifier
            .size(width = 76.px, height = 44.px)
            .semantics { contentDescription = "음성을 듣고 있어요" }
    ) {
        val u = size.width / 76f
        val bars = listOf(16f to 12f, 8f to 28f, 0f to 44f, 8f to 28f, 16f to 12f)
        bars.forEachIndexed { i, (y, h) ->
            drawRoundRect(
                color = if (i == 2) C.Gold else C.Ink,
                topLeft = Offset(i * 16f * u, y * u),
                size = Size(10f * u, h * u),
                cornerRadius = CornerRadius(2.5f * u),
            )
        }
    }
}

/** 수첩 머리용 오늘 날짜: "11월 9일 일요일" */
private fun todayLabel(): String {
    val d = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
    val days = listOf("월", "화", "수", "목", "금", "토", "일")
    return "${d.monthValue}월 ${d.dayOfMonth}일 ${days[d.dayOfWeek.value - 1]}요일"
}
