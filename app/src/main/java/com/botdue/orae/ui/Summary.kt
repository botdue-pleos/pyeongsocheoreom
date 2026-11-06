package com.botdue.orae.ui

import com.botdue.orae.engine.Baseline
import com.botdue.orae.engine.ChangeReport
import com.botdue.orae.engine.DemoScenario
import com.botdue.orae.engine.DemoSignalSource
import com.botdue.orae.engine.MissedTurnSignalDetector
import com.botdue.orae.engine.TurnDirection
import com.botdue.orae.engine.WeekStat
import java.time.LocalDate
import kotlin.math.roundToInt

/** 화면에 보여줄 운전 기록 요약. 엔진 결과를 화면 문구에 맞게 묶는다 */
class Summary(val scenario: DemoScenario) {
    private val source = DemoSignalSource(scenario)
    private val baseline = Baseline(MissedTurnSignalDetector().detect(source.signals()), source.startDate)

    val weeks: List<WeekStat> = baseline.weekly()
    val thisWeek: WeekStat = weeks.last()
    val learning: Boolean = baseline.isLearning()
    val change: ChangeReport? = baseline.detectChange()
    val usualMissedPerWeek: Int = baseline.baselineMissedPerWeek().roundToInt()

    /** 마지막 주의 시작일 */
    val thisWeekStart: LocalDate = source.startDate.plusWeeks((weeks.size - 1).toLong())

    /** 최근 4주 기간 */
    val recentRange: Pair<LocalDate, LocalDate> =
        source.startDate.plusWeeks(maxOf(0, weeks.size - 4).toLong()) to thisWeekStart.plusDays(6)

    /** 우회전 / 좌회전 / 회전 */
    val directionNoun: String = when (change?.direction) {
        TurnDirection.RIGHT -> "우회전"
        TurnDirection.LEFT -> "좌회전"
        else -> "회전"
    }

    /** 20번 기준으로 몇 번 놓쳤는지 (변화 안내 화면에서 주별로 견주기 위해) */
    fun per20(rate: Double): Int = (rate * 20).roundToInt()
}

fun LocalDate.korean(): String = "${monthValue}월 ${dayOfMonth}일"

/** 받침에 따라 "로"/"으로" */
fun String.toward(): String {
    val last = lastOrNull() ?: return this
    if (last !in '가'..'힣') return "${this}로"
    val jong = (last.code - 0xAC00) % 28
    return if (jong == 0 || jong == 8) "${this}로" else "${this}으로"
}

/** 받침에 따라 "를"/"을" */
fun String.obj(): String {
    val last = lastOrNull() ?: return this
    if (last !in '가'..'힣') return "${this}를"
    return if ((last.code - 0xAC00) % 28 == 0) "${this}를" else "${this}을"
}
