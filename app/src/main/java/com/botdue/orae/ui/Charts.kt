package com.botdue.orae.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.botdue.orae.engine.ContextRate
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 깜빡이를 켜는 때: 회전 지점까지 이어진 눈금자. 눈금 하나가 1초.
 * 평소는 빈 동그라미, 지난주는 찍힌 동그라미로 두고, 늦어진 만큼은 금색 점선으로 잇는다.
 */
@Composable
fun LeadRuler(usualMs: Long, recentMs: Long, summary: Summary, modifier: Modifier = Modifier) {
    val maxSec = maxOf(8, ceil(maxOf(usualMs, recentMs) / 1000.0).toInt() + 1)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(176.px)
            .semantics { contentDescription = "평소 회전 ${summary.seconds(usualMs)} 전, 지난주 ${summary.seconds(recentMs)} 전에 깜빡이를 켬" },
    ) {
        val w = maxWidth
        val end = w - 70.px
        fun at(ms: Long): Dp = end * (1f - ms / 1000f / maxSec)
        val lineY = 50.px
        Canvas(Modifier.fillMaxWidth().height(100.px)) {
            val y = lineY.toPx()
            val x1 = end.toPx()
            drawLine(C.Line, Offset(0f, y), Offset(x1, y), 4.px.toPx(), StrokeCap.Round)
            for (s in 0..maxSec) {
                val x = x1 * (1f - s.toFloat() / maxSec)
                val h = (if (s % 2 == 0) 18.px else 10.px).toPx()
                drawLine(C.Sub.copy(alpha = 0.6f), Offset(x, y - h), Offset(x, y + h), 2.px.toPx())
            }
            val xu = at(usualMs).toPx()
            val xr = at(recentMs).toPx()
            val r = 17.px.toPx()
            // 늦어진 만큼
            if (xr - xu > r * 2) {
                val gw = 5.px.toPx()
                drawLine(C.Gold, Offset(xu + r, y), Offset(xr - r, y), gw, pathEffect = PathEffect.dashPathEffect(floatArrayOf(gw * 1.6f, gw)))
            }
            drawCircle(C.Card, r, Offset(xu, y))
            drawCircle(C.Ink, r - 2.px.toPx(), Offset(xu, y), style = Stroke(4.px.toPx()))
            drawCircle(C.Ink, r, Offset(xr, y))
            // 회전 지점: 오른쪽으로 꺾이는 화살표
            val u = 3.px.toPx() * 0.73f
            val ox = x1 + 18.px.toPx()
            val stroke = Stroke(4.px.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(Path().apply {
                moveTo(ox, y + 8 * u); lineTo(ox, y - 2 * u)
                quadraticTo(ox, y - 7 * u, ox + 5 * u, y - 7 * u); lineTo(ox + 13 * u, y - 7 * u)
            }, C.Ink, style = stroke)
            drawPath(Path().apply {
                moveTo(ox + 9 * u, y - 11 * u); lineTo(ox + 13 * u, y - 7 * u); lineTo(ox + 9 * u, y - 3 * u)
            }, C.Ink, style = stroke)
        }
        val label = @Composable { x: Dp, top: String, bottom: String, strong: Boolean ->
            Column(
                Modifier.width(160.px).offset(x = (x - 80.px).coerceIn(0.px, w - 160.px), y = 92.px),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                T(top, 24.tx, color = C.Sub, align = TextAlign.Center)
                T(bottom, 28.tx, weight = if (strong) FontWeight.Bold else FontWeight.Normal, color = if (strong) C.Ink else C.Sub, align = TextAlign.Center)
            }
        }
        label(at(usualMs), "평소", "${summary.seconds(usualMs)} 전", false)
        label(at(recentMs), "지난주", "${summary.seconds(recentMs)} 전", true)
        T("회전", 24.tx, Modifier.offset(x = end + 6.px, y = 104.px), color = C.Sub)
    }
}

/**
 * 여건별 도장 줄: 맑은 낮·비 오는 날·해 진 뒤마다 최근 회전 10번으로 줄여 찍는다.
 * 늘어난 여건만 굵게, 평소 횟수는 오른쪽에 작게. 최근 기록이 적은 여건은 줄 대신 아래 한 줄로 묶는다.
 */
@Composable
fun ContextStamps(rates: List<ContextRate>, minTurns: Int = 8) {
    val (enough, few) = rates.partition { it.recentTurns >= minTurns }
    Column(verticalArrangement = Arrangement.spacedBy(20.px)) {
        enough.forEach { c ->
            val now = (c.recentRate * 10).roundToInt()
            val usual = (c.baseRate * 10).roundToInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(c.context.label, 28.tx, Modifier.width(170.px), if (c.rose) FontWeight.Bold else FontWeight.Normal, if (c.rose) C.Ink else C.Sub)
                TurnDots(
                    turnCells(10, now), columns = 10, gap = 8.px,
                    description = "${c.context.label} 최근 10번 중 ${now}번 놓침, 평소 ${usual}번",
                    modifier = Modifier.weight(1f), ring = 3.px,
                )
                Column(Modifier.padding(start = 20.px).width(130.px), horizontalAlignment = Alignment.End) {
                    T("${now}번", 28.tx, weight = if (c.rose) FontWeight.Bold else FontWeight.Normal, color = if (c.rose) C.GoldText else C.Ink)
                    T("평소 ${usual}번", 22.tx, color = C.Sub)
                }
            }
        }
        if (few.isNotEmpty()) {
            T("${few.joinToString(", ") { it.context.label }}은 최근 기록이 적어 견주지 않았어요.", 26.tx, color = C.Sub, lineHeight = 1.45f)
        }
    }
}

/** 비율 막대 두 줄: 위는 평소(옅게), 아래는 최근(진하게). 회전 중 좌회전 비율 등에 쓴다 */
@Composable
fun ShareBars(usual: Double, recent: Double, summary: Summary) {
    Column(verticalArrangement = Arrangement.spacedBy(14.px)) {
        listOf(Triple("평소", usual, C.SkyBlue), Triple("최근", recent, C.Ink)).forEach { (label, v, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(label, 26.tx, Modifier.width(90.px), color = C.Sub)
                Box(
                    Modifier
                        .weight(1f)
                        .height(30.px)
                        .background(C.Note, RoundedCornerShape(15.px)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(v.toFloat().coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .background(color, RoundedCornerShape(15.px)),
                    )
                }
                T(summary.percent(v), 26.tx, Modifier.padding(start = 18.px).width(80.px), color = C.Ink, align = TextAlign.End)
            }
        }
    }
}

/** 작은 제목과 오른쪽 상태 말(같아요·늦어졌어요) */
@Composable
fun ChartTitle(title: String, state: String?, highlight: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        T(title, 30.tx, Modifier.weight(1f), FontWeight.Bold)
        if (state != null) T(state, 28.tx, weight = if (highlight) FontWeight.Bold else FontWeight.Normal, color = if (highlight) C.GoldText else C.Sub)
    }
    Spacer(Modifier.height(20.px))
}
