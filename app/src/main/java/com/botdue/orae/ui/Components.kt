package com.botdue.orae.ui

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@Composable
fun T(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    color: Color = C.Ink,
    lineHeight: Float = 1.4f,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = LocalFont.current,
            fontSize = size,
            fontWeight = weight,
            color = color,
            lineHeight = lineHeight.em,
            letterSpacing = if (size.value >= 30) (-0.02).em else 0.em,
            textAlign = align ?: TextAlign.Unspecified,
            // 한국어를 어절 단위로 줄바꿈 (기기 언어와 상관없이 ko로 지정해야 적용된다)
            localeList = LocaleList("ko-KR"),
            lineBreak = LineBreak(
                strategy = LineBreak.Strategy.HighQuality,
                strictness = LineBreak.Strictness.Strict,
                wordBreak = LineBreak.WordBreak.Phrase,
            ),
        ),
    )
}

/** 3x3 픽셀 로고. 오른쪽 가운데 금색 칸이 방향지시등 */
@Composable
fun PixelLogo(sizeDp: Dp = 34.px) {
    Canvas(Modifier.size(sizeDp)) {
        val cell = size.width * 7f / 26f
        val step = size.width * 9.5f / 26f
        val colors = listOf(C.Ink, C.Ink, C.Line, C.Ink, C.Ink, C.Gold, C.Ink, C.Ink, C.Line)
        colors.forEachIndexed { i, c ->
            drawRoundRect(
                color = c,
                topLeft = Offset((i % 3) * step, (i / 3) * step),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(cell * 0.2f),
            )
        }
    }
}

/**
 * 수첩 머리: 날짜·제목은 왼쪽, 보조 정보는 오른쪽, 아래에 진한 줄 하나.
 * [onGround]면 종이 밖 바탕 위에 쓰므로 밤에도 읽히는 [C.OnGround]로 쓴다.
 */
@Composable
fun Header(title: String, trailing: String? = null, onGround: Boolean = false) {
    val ink = if (onGround) C.OnGround else C.Ink
    val sub = if (onGround) C.OnGround.copy(alpha = 0.8f) else C.Sub
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            T(title, 30.tx, Modifier.weight(1f), FontWeight.Bold, ink)
            if (trailing != null) T(trailing, 28.tx, color = sub)
        }
        Spacer(Modifier.height(18.px))
        Box(Modifier.fillMaxWidth().height(3.px).background(ink))
    }
}

/** 하늘색 바탕 위에 흰 수첩 지면 한 장 */
fun Modifier.page(): Modifier = this
    .background(C.Ground)
    .padding(28.px)
    .background(C.Card, RoundedCornerShape(12.px))

@Composable
fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.px)
            .background(C.Line)
    )
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = C.Ink) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 26.px),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        T(label, 32.tx, color = C.Sub)
        T(value, 32.tx, weight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(116.px)
            .background(C.Ink, RoundedCornerShape(58.px))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, 36.tx, weight = FontWeight.Bold, color = C.OnInk) }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(116.px)
            .border(BorderStroke(3.px, C.Ink), RoundedCornerShape(58.px))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, 34.tx, weight = FontWeight.SemiBold) }
}

/** 놓친 회전을 전체에 고르게 흩어 놓는다 (true = 깜빡이 켬) */
fun turnCells(total: Int, missed: Int): List<Boolean> {
    if (total <= 0) return emptyList()
    val m = missed.coerceIn(0, total)
    return (0 until total).map { i -> (i + 1) * m / total == i * m / total }
}

/**
 * 회전 칸. 칸 하나가 회전 한 번이다.
 * 채운 남색 칸 = 깜빡이를 켠 회전, 금색 테두리만 있는 빈 칸 = 놓친 회전 (모양으로도 구분된다).
 * [others]만큼 뒤에 붙는 옅은 회색 칸은 다른 분이 운전한 회전이다. 기록에서 빼고 셌다는 표시.
 */
@Composable
fun TurnDots(
    cells: List<Boolean>,
    columns: Int,
    gap: Dp,
    description: String,
    modifier: Modifier = Modifier,
    ring: Dp = 5.px,
    radius: Dp = 8.px,
    others: Int = 0,
) {
    val all: List<Boolean?> = cells + List(others) { null }
    Column(
        modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        all.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { on -> Cell(on, Modifier.weight(1f), ring, radius) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 도장 한 칸: 찍힌 도장은 남색 동그라미, 놓친 회전은 금색 점선 동그라미, 다른 분 운전(null)은 옅은 회색.
 * 점선이 가늘면 흰 종이 위에서 잘 안 보여서, 선은 4.5px 아래로 내리지 않고 점선 마디를 길게 한다.
 */
@Composable
private fun Cell(on: Boolean?, modifier: Modifier, ring: Dp, radius: Dp) {
    Canvas(modifier.aspectRatio(1f)) {
        val r = size.minDimension / 2
        when (on) {
            true -> drawCircle(C.Ink, r)
            null -> drawCircle(C.Other, r * 0.86f)
            false -> {
                val w = maxOf(ring.toPx(), 3.dp.toPx() /* 4.5px */).coerceAtMost(r * 0.34f)
                drawCircle(
                    C.Gold, r - w / 2,
                    style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 1.8f, w * 0.8f))),
                )
            }
        }
    }
}

@Composable
fun Legend(others: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.px)) {
        listOf<Pair<Boolean?, String>>(true to "깜빡이 켠 회전", false to "놓친 회전")
            .plus(if (others) listOf(null to "다른 분 운전, 빼고 셈") else emptyList())
            .forEach { (on, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Cell(on, Modifier.size(28.px), 4.px, 0.px)
                    T(text, 26.tx, Modifier.padding(start = 12.px), color = C.Sub)
                }
            }
    }
}
