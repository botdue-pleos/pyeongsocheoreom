package com.botdue.orae.ui

import android.content.res.AssetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 현대차 CI 색. 광고 "Brilliant Moments"의 맑은 낮: 하늘색(Sky Blue)을 옅게 깐 바탕에 흰 종이 카드,
 * 진한 글자(Hyundai Blue). 운전 수첩처럼 회전 한 번이 도장 한 칸이다.
 * 금색은 "깜빡이를 놓친 회전"(빈 도장 칸) 한 곳에만 쓴다.
 *
 * 밤에는 큰 흰 화면이 노안 운전자에게 눈부시다. [night]이면 짙은 남색 바탕에 밝기를 낮춘 크림색 종이로 바꾼다.
 * 글자는 그대로 남색이라 수첩 문법은 같고, 대비는 본문 8:1, 보조 글자 5:1 이상을 지킨다.
 * [night]은 스냅숏 상태라 바뀌면 이 색을 읽은 화면과 그림이 다시 그려진다.
 */
object C {
    /** 시스템 야간 모드이거나 `--ez night true`로 시작했을 때 */
    var night: Boolean by mutableStateOf(false)

    val Ground: Color get() = if (night) Color(0xFF0B1B30) else Color(0xFFE6EEF6)
    val Ink: Color get() = Color(0xFF002C5F)
    val Sub: Color get() = if (night) Color(0xFF3B4A5C) else Color(0xFF4A5D73)
    val Line: Color get() = if (night) Color(0xFFB3A78D) else Color(0xFFD6E1EC)
    /** 수첩 종이. 밤에는 흰색 대신 밝기를 낮춘 크림색 */
    val Card: Color get() = if (night) Color(0xFFD4C9B1) else Color(0xFFFFFFFF)
    /** 종이 위에 붙인 쪽지 (조건·알림) */
    val Note: Color get() = if (night) Color(0xFFC6BA9F) else Color(0xFFEFF4F9)
    /** 놓친 회전 점선 동그라미. 옅으면 안 보여서 진하게 (흰 종이 대비 5:1 넘게) */
    val Gold: Color get() = if (night) Color(0xFF7A4528) else Color(0xFF94593B)
    /** 금색 글자용: 바탕 대비 4.5:1 이상 */
    val GoldText: Color get() = if (night) Color(0xFF66381F) else Color(0xFF7E4E37)
    val SkyBlue: Color get() = if (night) Color(0xFF7D9AB6) else Color(0xFFAACAE6)
    /** 다른 분이 운전한 회전 (빼고 센 칸) */
    val Other: Color get() = if (night) Color(0xFFA9A18E) else Color(0xFFC9D4E0)
    /** 남색으로 채운 버튼·칸 위의 글자 */
    val OnInk: Color get() = if (night) Color(0xFFE3D9C3) else Color(0xFFE6EEF6)
    /** 종이 밖 바탕 위에 바로 쓰는 글자 (가족 화면 머리) */
    val OnGround: Color get() = if (night) Color(0xFFD4C9B1) else Color(0xFF002C5F)
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

/**
 * 설계 기준 창(세로 1칸 613×887dp, 가로 2칸 1247×887dp — 920×1330·1870×1330px, 240dpi)을 실제 앱 창 크기에 맞춰 통째로 비례 확대한다.
 * Pleos Connect v2.1.0 에뮬레이터(2560×1440, 160dpi)의 세로 1칸 창은 약 841×1210dp라, 이대로 두면 화면이 작게 그려지고 아래가 빈다.
 */
@Composable
fun FitWindow(content: @Composable () -> Unit) {
    val c = LocalConfiguration.current
    val w = c.screenWidthDp.toFloat()
    val h = c.screenHeightDp.toFloat()
    val k = (if (w < h) minOf(w / 613f, h / 887f) else minOf(w / 1247f, h / 887f)).coerceIn(0.5f, 3f)
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density * k, d.fontScale)) { content() }
}
