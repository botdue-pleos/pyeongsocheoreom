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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.botdue.orae.engine.CareCenter
import com.botdue.orae.engine.ChangeReport

/**
 * 변화 안내: 본인에게 먼저, 부드럽게 알린다.
 * 같은 방향 회전 20번씩 모아 평소와 최근 3주를 견주고, 다음 회전 전 알림을 켤지 묻는다.
 * [center]가 있으면 가까운 치매안심센터의 무료 검사를 알리고 길안내를 열어 둔다.
 */
@Composable
fun ChangeScreen(
    summary: Summary,
    report: ChangeReport,
    center: CareCenter?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onGuide: (CareCenter) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px)
    ) {
        Header("오래운전", "주차 중")
        Spacer(Modifier.height(48.px))
        T("요즘 ${summary.directionNoun}할 때\n깜빡이를 자주 놓치셨어요", 60.tx, weight = FontWeight.Bold, lineHeight = 1.25f)
        Spacer(Modifier.height(22.px))
        T("${summary.directionNoun} 20번씩 모아 평소와 견줘 봤어요.", 30.tx, color = C.Sub)

        Spacer(Modifier.height(44.px))
        val rows = listOf("평소" to summary.per20(report.baselineRate)) +
            listOf("3주 전", "2주 전", "지난주").zip(report.recent.map { summary.per20(it.missRate) })
        Column(verticalArrangement = Arrangement.spacedBy(22.px)) {
            rows.forEachIndexed { i, (label, missed) ->
                val latest = i == rows.lastIndex
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(label, 28.tx, Modifier.width(130.px), if (i == 0) FontWeight.Bold else FontWeight.Normal, if (i == 0) C.Ink else C.Sub)
                    TurnDots(
                        turnCells(20, missed), columns = 10, gap = 8.px,
                        description = "$label 20번 중 $missed 번 놓침", modifier = Modifier.weight(1f),
                        ring = 3.px, radius = 4.px,
                    )
                    T(
                        "${missed}번", 28.tx, Modifier.padding(start = 20.px).width(100.px),
                        if (latest) FontWeight.Bold else FontWeight.Normal, if (latest) C.GoldText else C.Ink,
                    )
                }
            }
        }

        Spacer(Modifier.height(44.px))
        Column(
            Modifier
                .fillMaxWidth()
                .background(C.Card, RoundedCornerShape(18.px))
                .padding(40.px),
            verticalArrangement = Arrangement.spacedBy(28.px),
        ) {
            T("다음 회전 300m 전에\n한 번 더 알려드릴까요?", 40.tx, weight = FontWeight.Bold, lineHeight = 1.3f)
            Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
                OutlineButton("괜찮아요", Modifier.weight(1f), onDecline)
                PrimaryButton("네, 알려 주세요", Modifier.weight(1.5f), onAccept)
            }
        }
        if (center != null) {
            Spacer(Modifier.height(44.px))
            Divider()
            Spacer(Modifier.height(40.px))
            T("가까운 곳에서 기억력을\n무료로 확인해 보실 수 있어요", 36.tx, weight = FontWeight.Bold, lineHeight = 1.3f)
            Spacer(Modifier.height(16.px))
            T("만 60세 이상은 치매안심센터에서 15분 정도면 받으실 수 있어요. 비용은 들지 않아요.", 28.tx, color = C.Sub, lineHeight = 1.45f)
            Spacer(Modifier.height(28.px))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(center.name, 30.tx, weight = FontWeight.Bold)
                    Spacer(Modifier.height(6.px))
                    T("차로 ${center.minutes}, ${center.distanceKm}km", 26.tx, color = C.Sub)
                }
                OutlineButton("길 안내", Modifier.width(240.px)) { onGuide(center) }
            }
        }
        Spacer(Modifier.height(24.px))
        T("이 내용은 운전하시는 분만 봐요. 가족 공유는 꺼져 있어요.", 26.tx, color = C.Sub, lineHeight = 1.45f)
    }
}

