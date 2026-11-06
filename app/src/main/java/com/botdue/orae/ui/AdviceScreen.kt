package com.botdue.orae.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.botdue.orae.engine.Condition

/**
 * 함께 정한 조건에 걸리는 출발일 때 보여주는 제안.
 * 운전을 막지 않는다. "그래도 갈게요"를 누르면 그대로 출발한다.
 */
@Composable
fun AdviceScreen(place: Place, onLater: () -> Unit, onGoAnyway: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px)
    ) {
        Header("오래운전", "오후 7:10")
        Spacer(Modifier.height(72.px))
        T("해가 졌어요.\n${place.name}에는\n내일 낮에 가실까요?", 64.tx, weight = FontWeight.Bold, lineHeight = 1.22f)
        Spacer(Modifier.height(28.px))
        T("${place.name}까지 ${place.minutes} 걸려요.", 32.tx, color = C.Sub, lineHeight = 1.5f)

        Spacer(Modifier.height(56.px))
        Column(
            Modifier
                .fillMaxWidth()
                .background(C.Card, RoundedCornerShape(18.px))
                .padding(40.px),
            verticalArrangement = Arrangement.spacedBy(12.px),
        ) {
            T("가족과 함께 정한 조건", 26.tx, weight = FontWeight.Bold, color = C.Sub)
            T(Condition.NEAR_AT_NIGHT.label, 36.tx, weight = FontWeight.Bold)
        }

        Spacer(Modifier.height(56.px))
        Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
            OutlineButton("그래도 갈게요", Modifier.weight(1f), onGoAnyway)
            PrimaryButton("내일 갈게요", Modifier.weight(1f), onLater)
        }
        Spacer(Modifier.height(24.px))
        T("정하는 건 언제나 운전하시는 분이에요.", 26.tx, color = C.Sub)
    }
}
