package com.botdue.orae.ui

import com.botdue.orae.engine.Baseline
import com.botdue.orae.engine.ChangeReport
import com.botdue.orae.engine.DemoScenario
import com.botdue.orae.engine.DemoSignalSource
import com.botdue.orae.engine.DriverMatcher
import com.botdue.orae.engine.LeadReport
import com.botdue.orae.engine.LeftShareReport
import com.botdue.orae.engine.MissedTurnSignalDetector
import com.botdue.orae.engine.TurnDirection
import com.botdue.orae.engine.WeekStat
import java.time.LocalDate
import kotlin.math.roundToInt

/** 화면에 보여줄 운전 기록 요약. 엔진 결과를 화면 문구에 맞게 묶는다 */
class Summary(val scenario: DemoScenario) {
    private val source = DemoSignalSource(scenario)
    private val signals = source.signals()
    private val driver = DriverMatcher.learn(signals)
    private val baseline = Baseline(
        MissedTurnSignalDetector().detect(signals, driver::isOwner, source.sun::isDark),
        source.startDate,
    )

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

    /** 깜빡이 켜는 때. 변화가 잡힌 방향이 있으면 그 방향으로 본다 */
    val lead: LeadReport? = baseline.lead(change?.direction)
    val leftShare: LeftShareReport? = baseline.leftShare()

    /** 최근 4주 동안 다른 사람이 운전해서 기록에서 뺀 출발 횟수 */
    val otherDrives: Int = driver.otherTrips(signals).count {
        val date = java.time.Instant.ofEpochMilli(it.timeMs).atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
        !date.isBefore(recentRange.first) && !date.isAfter(recentRange.second)
    }

    /** 우회전 / 좌회전 / 회전 */
    val directionNoun: String = when (change?.direction) {
        TurnDirection.RIGHT -> "우회전"
        TurnDirection.LEFT -> "좌회전"
        else -> "회전"
    }

    /** 늘어난 놓침이 날씨·밤 때문인지 한 줄로. 맑은 낮 회전이 적으면 말하지 않는다 */
    val contextLine: String? = change?.clear?.let { c ->
        when {
            c.changed -> "맑은 낮 운전에서도 늘었어요. 날씨나 밤 때문만은 아니에요."
            c.risesIn.isNotEmpty() -> "맑은 낮은 평소와 같아요. ${c.risesIn.joinToString("·") { it.label }}에만 늘었어요."
            else -> null
        }
    }

    /** 깜빡이를 켜는 때가 늦어졌으면 한 줄로 */
    val leadLine: String? = lead?.takeIf { it.later }?.let {
        "깜빡이를 켜는 때도 회전 ${seconds(it.usualMs)} 전에서 ${seconds(it.recentMs)} 전으로 늦어졌어요."
    }

    fun seconds(ms: Long): String = "${(ms / 1000.0).roundToInt()}초"

    fun percent(share: Double): String = "${(share * 100).roundToInt()}%"

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
