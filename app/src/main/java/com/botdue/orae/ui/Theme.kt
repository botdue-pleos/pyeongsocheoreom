package com.botdue.orae.ui

import android.content.res.AssetManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 현대차 CI 색. 고령 운전자가 읽기 쉽도록 밝은 바탕(Light Sand)에 진한 글자(Hyundai Blue)를 쓴다.
 * 금색은 "깜빡이를 놓친 회전" 한 곳에만 쓴다.
 */
object C {
    val Ground = Color(0xFFF6F3F2)
    val Ink = Color(0xFF002C5F)
    val Sub = Color(0xFF4A5D73)
    val Line = Color(0xFFE4DCD3)
    val Card = Color(0xFFE4DCD3)
    val Gold = Color(0xFFA36B4F)
    /** 금색 글자용: 바탕 대비 4.5:1 이상 */
    val GoldText = Color(0xFF7E4E37)
    val SkyBlue = Color(0xFFAACAE6)
}

/**
 * Pleos 공식 서체 Asta Sans. 가변 글꼴 하나를 굵기별로 불러 쓴다.
 * res/font로 넣으면 굵기(wght) 축이 적용되지 않아 assets에서 읽는다.
 */
@OptIn(ExperimentalTextApi::class)
fun astaSans(assets: AssetManager): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map { w ->
        Font(
            path = "fonts/AstaSans.ttf",
            assetManager = assets,
            weight = w,
            variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)),
        )
    }
)

val LocalFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/**
 * 시안(920x1330 px 창 기준) 수치를 그대로 옮기기 위한 변환.
 * 고령자용이라 글자는 20sp 아래로 내리지 않는다 (Pleos 최소 18sp보다 크게).
 */
val Int.px: Dp get() = (this / 1.5f).dp
val Int.tx: TextUnit get() = maxOf(this / 1.5f, 20f).sp
