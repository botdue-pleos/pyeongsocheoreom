package com.botdue.orae.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * 주차 후 이번 주 카드: 이번 주 회전을 칸으로 보여주고 평소와 견준다.
 * 출발 화면에서 옮겨 온 이번 주 도장 줄과 "자주 놓치셨어요"는 여기서만 보여준다 (출발 전에는 말하지 않는다).
 * 아래 버튼은 스크롤 밖에 고정해 Pleos 세로 1칸(920×1330)에서 잘리지 않게 한다.
 */
@Composable
fun ParkedScreen(summary: Summary, onDone: () -> Unit, onDetail: () -> Unit, onFamily: () -> Unit) {
    val week = summary.thisWeek
    val changed = summary.change != null

    Column(
        Modifier
            .fillMaxSize()
            .page()
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 48.px)
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Header("평소처럼", "도착했어요")
            Spacer(Modifier.height(48.px))
            T(
                if (changed) "이번 주는 깜빡이를\n평소보다 자주 놓치셨어요" else "이번 주 운전은\n평소와 비슷해요",
                if (changed) 60.tx else 68.tx, weight = FontWeight.Bold, lineHeight = 1.22f,
            )
            Spacer(Modifier.height(20.px))
            val usual = if (summary.learning) {
                "처음 4주는 평소 운전을 익히는 중이에요."
            } else {
                "평소에는 한 주에 ${summary.usualMissedPerWeek}번쯤 놓치세요."
            }
            T("회전 ${week.turns}번 중 깜빡이를 ${week.missed}번 놓치셨어요.\n$usual", 32.tx, color = C.Sub, lineHeight = 1.5f)

            Spacer(Modifier.height(40.px))
            TurnDots(
                turnCells(week.turns, week.missed), columns = 12, gap = 14.px,
                description = "이번 주 회전 ${week.turns}번 중 놓친 회전 ${week.missed}번",
                ring = 6.px, radius = 10.px,
            )
            Spacer(Modifier.height(20.px))
            Legend()

            Spacer(Modifier.height(32.px))
            Divider()
            // 시연용 값: 실제로는 Door·기어·NaviHelper 재탐색 이벤트로 센다
            InfoRow("다니던 길 다시 찾기", if (changed) "1번" else "없었어요"); Divider()
            InfoRow("해 진 뒤 운전", "1번"); Divider()
        }

        Spacer(Modifier.height(28.px))
        Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
            OutlineButton("가족과 같이 보기", Modifier.weight(1f), onFamily)
            if (changed) {
                PrimaryButton("자세히 보기", Modifier.weight(1f), onDetail)
            } else {
                PrimaryButton("확인했어요", Modifier.weight(1f), onDone)
            }
        }
    }
}
