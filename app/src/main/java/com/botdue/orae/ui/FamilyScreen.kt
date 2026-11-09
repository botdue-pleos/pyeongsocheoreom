package com.botdue.orae.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import com.botdue.orae.engine.Condition
import com.botdue.orae.engine.Kept

/**
 * 가족과 같이 보기 (주차 중, 가족이 동승했을 때).
 * 가로 2칸 창이면 좌우로, 세로 1칸이면 위아래로 놓는다.
 */
@Composable
fun FamilyScreen(
    summary: Summary,
    conditions: Set<Condition>,
    kept: List<Kept>,
    onToggle: (Condition) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(C.Ground)
    ) {
        val wide = maxWidth > maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 72.px, end = 72.px, top = 64.px, bottom = 56.px)
        ) {
            val (from, to) = summary.recentRange
            Header("오래운전 가족과 같이 보기", if (wide) "${from.korean()}부터 ${to.korean()}까지" else null)
            Spacer(Modifier.height(if (wide) 36.px else 48.px))
            if (wide) {
                // 가로 2칸: 펼친 앨범처럼 왼쪽 장은 기록, 오른쪽 장은 습관·조건·버튼
                Row(horizontalArrangement = Arrangement.spacedBy(20.px)) {
                    Column(Modifier.weight(1f).background(C.Card, RoundedCornerShape(18.px)).padding(48.px)) {
                        Weeks(summary)
                        Spacer(Modifier.height(32.px))
                        T(DISCLAIMER, 26.tx, color = C.Sub)
                        if (kept.isNotEmpty()) {
                            Spacer(Modifier.height(48.px))
                            KeptRecord(kept)
                        }
                    }
                    Column(Modifier.weight(1f).background(C.Card, RoundedCornerShape(18.px)).padding(48.px)) {
                        Habits(summary)
                        Spacer(Modifier.height(36.px))
                        Conditions(conditions, onToggle)
                        Spacer(Modifier.height(28.px))
                        Buttons(onBack, onConfirm, confirmWeight = 1f)
                    }
                }
            } else {
                Weeks(summary)
                Spacer(Modifier.height(56.px))
                Habits(summary)
                if (kept.isNotEmpty()) {
                    Spacer(Modifier.height(48.px))
                    KeptRecord(kept)
                }
                Spacer(Modifier.height(48.px))
                Conditions(conditions, onToggle)
                Spacer(Modifier.height(36.px))
                T(DISCLAIMER, 26.tx, color = C.Sub)
                Spacer(Modifier.height(28.px))
                Buttons(onBack, onConfirm, confirmWeight = 2f)
            }
        }
    }
}

private const val DISCLAIMER = "진단 결과가 아니에요. 평소와 달라진 점만 보여드려요."

@Composable
private fun Buttons(onBack: () -> Unit, onConfirm: () -> Unit, confirmWeight: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
        OutlineButton("돌아가기", Modifier.weight(1f), onBack)
        PrimaryButton("이 조건으로 정하기", Modifier.weight(confirmWeight), onConfirm)
    }
}

@Composable
private fun ColumnScope.Weeks(summary: Summary) {
    val changed = summary.change != null
    T(
        if (changed) "4주 동안 회전할 때\n깜빡이 놓침이 늘었어요" else "4주 동안 운전은\n평소와 비슷했어요",
        56.tx, weight = FontWeight.Bold, lineHeight = 1.25f,
    )
    Spacer(Modifier.height(20.px))
    T("칸 하나가 회전 한 번이에요. 빈 칸은 깜빡이를 놓친 회전이에요.", 28.tx, color = C.Sub, lineHeight = 1.5f)
    summary.contextLine?.let {
        Spacer(Modifier.height(12.px))
        T(it, 28.tx, color = C.Sub, lineHeight = 1.5f)
    }
    if (summary.otherDrives > 0) {
        Spacer(Modifier.height(12.px))
        T("다른 분이 운전하신 ${summary.otherDrives}번은 운전석 위치로 알아보고 기록에서 뺐어요.", 28.tx, color = C.Sub, lineHeight = 1.5f)
    }
    Spacer(Modifier.height(36.px))
    val recent = summary.weeks.takeLast(4)
    val labels = listOf("첫째 주", "둘째 주", "셋째 주", "넷째 주").takeLast(recent.size)
    Column(verticalArrangement = Arrangement.spacedBy(24.px)) {
        recent.zip(labels).forEachIndexed { i, (w, label) ->
            Column(verticalArrangement = Arrangement.spacedBy(14.px)) {
                Row(Modifier.fillMaxWidth()) {
                    T(label, 26.tx, Modifier.weight(1f), color = C.Sub)
                    val latest = i == recent.lastIndex && changed
                    T("${w.turns}번 중 ${w.missed}번", 26.tx, weight = if (latest) FontWeight.Bold else FontWeight.Normal, color = if (latest) C.GoldText else C.Ink)
                }
                TurnDots(turnCells(w.turns, w.missed), columns = 15, gap = 9.px, description = "$label 회전 ${w.turns}번 중 ${w.missed}번 놓침", ring = 5.px, radius = 7.px)
            }
        }
    }
}

/** 함께 정한 조건이 작동했을 때 어떻게 하셨는지. 지키지 않은 것을 탓하지 않고 횟수만 */
@Composable
private fun KeptRecord(kept: List<Kept>) {
    T("함께 정한 조건은 이렇게 지키셨어요", 30.tx, weight = FontWeight.Bold)
    Spacer(Modifier.height(20.px))
    Divider()
    kept.forEach { k ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.px),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            T(k.condition.label, 28.tx, Modifier.weight(1f))
            T(k.phrase(), 28.tx, Modifier.padding(start = 24.px), FontWeight.Bold)
        }
        Divider()
    }
}

private fun Kept.phrase(): String {
    val did = when (condition) {
        Condition.NEAR_AT_NIGHT -> "낮에 가셨어요"
        Condition.REST_ON_HIGHWAY -> "들르셨어요"
        Condition.REMIND -> "들으셨어요"
    }
    return when {
        followed != suggested -> "${suggested}번 중 ${followed}번 $did"
        suggested == 1 -> "1번 $did"
        else -> "${suggested}번 모두 $did"
    }
}

@Composable
private fun Habits(summary: Summary) {
    val changed = summary.change != null
    T("다른 운전 습관", 30.tx, weight = FontWeight.Bold)
    Spacer(Modifier.height(20.px))
    Divider()
    HabitRow("한 주 기준", "평소", "최근", "달라진 점", header = true)
    // 깜빡이 켜는 때와 좌회전 비율은 기록에서 계산한다. 처음 4주는 기준선을 만드는 중
    val lead = summary.lead
    if (lead == null) {
        HabitRow("깜빡이 켜는 때", "기록 중", "-", "-")
    } else {
        HabitRow(
            if (changed) "${summary.directionNoun} 깜빡이" else "깜빡이 켜는 때",
            "${summary.seconds(lead.usualMs)} 전", "${summary.seconds(lead.recentMs)} 전",
            if (lead.later) "늦어졌어요" else "같아요", highlight = lead.later,
        )
    }
    val left = summary.leftShare
    if (left == null) {
        HabitRow("회전 중 좌회전", "기록 중", "-", "-")
    } else {
        HabitRow("회전 중 좌회전", summary.percent(left.usual), summary.percent(left.recent), if (left.fewer) "줄었어요" else "같아요", highlight = left.fewer)
    }
    // 시연용 값: 실제로는 NaviHelper 재탐색, Door·기어, Safety 경고등, 전조등 신호로 센다
    HabitRow("다니던 길 다시 찾기", "0번", if (changed) "1번" else "0번", if (changed) "조금 늘었어요" else "같아요", highlight = changed)
    HabitRow("P단 전에 문 열기", "0번", "0번", "같아요")
    HabitRow("경고등 확인까지", "2분", "2분", "같아요")
    HabitRow("해 진 뒤 운전", "3번", "2번", "줄었어요")
}

@Composable
private fun HabitRow(a: String, b: String, c: String, d: String, header: Boolean = false, highlight: Boolean = false) {
    val size = if (header) 24.tx else 28.tx
    val color = if (header) C.Sub else C.Ink
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (header) 16.px else 24.px),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(a, size, Modifier.weight(1.6f), color = color)
        T(b, size, Modifier.weight(1f), color = color)
        T(c, size, Modifier.weight(1f), color = color)
        T(d, size, Modifier.weight(1.1f), if (highlight) FontWeight.Bold else FontWeight.Normal, if (highlight) C.GoldText else color)
    }
    Divider()
}

@Composable
private fun Conditions(selected: Set<Condition>, onToggle: (Condition) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(C.Card, RoundedCornerShape(18.px))
            .padding(40.px),
        verticalArrangement = Arrangement.spacedBy(14.px),
    ) {
        T("같이 정해 볼 운전 조건", 36.tx, weight = FontWeight.Bold)
        Spacer(Modifier.height(10.px))
        Condition.entries.forEach { c ->
            val on = c in selected
            val shape = RoundedCornerShape(14.px)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(92.px)
                    .then(if (on) Modifier.background(C.Ink, shape) else Modifier.border(BorderStroke(3.px, C.Ink), shape))
                    .clickable(role = Role.Checkbox) { onToggle(c) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 28.px),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(30.px)) { if (on) Check() }
                T(c.label, 30.tx, Modifier.padding(start = 18.px), FontWeight.SemiBold, if (on) C.Ground else C.Ink)
            }
        }
    }
}

@Composable
private fun Check() {
    Canvas(Modifier.size(30.px)) {
        val u = size.width / 24f
        drawPath(
            Path().apply { moveTo(5 * u, 12.5f * u); lineTo(10 * u, 17 * u); lineTo(19 * u, 7 * u) },
            C.Ground, style = Stroke(3 * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
