package com.botdue.orae.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** 한 주 동안의 회전 기록 */
data class WeekStat(val weekIndex: Int, val turns: Int, val missed: Int) {
    val missRate: Double get() = if (turns == 0) 0.0 else missed.toDouble() / turns
}

/** 평소보다 달라졌다고 판단한 결과 */
data class ChangeReport(
    val direction: TurnDirection?,
    val baselineRate: Double,
    /** 최근 3주, 오래된 주부터 */
    val recent: List<WeekStat>,
)

/**
 * 나만의 기준선.
 *
 * 처음 [baselineWeeks]주 기록을 본인의 "평소"로 삼고, 그 뒤로는 평소 대비 변화만 본다.
 * 다른 사람과 비교하거나 점수로 판정하지 않는다 (진단 아님).
 * 변화 판정: 최근 3주가 모두 평소보다 높고, 가장 최근 주가 평소보다 [minIncrease] 이상 높을 때.
 */
class Baseline(
    private val results: List<TurnResult>,
    private val startDate: LocalDate,
    private val baselineWeeks: Int = 4,
    private val minIncrease: Double = 0.1,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {
    private fun weekOf(r: TurnResult): Int {
        val date = Instant.ofEpochMilli(r.timeMs).atZone(zone).toLocalDate()
        return (ChronoUnit.DAYS.between(startDate, date) / 7).toInt()
    }

    /** 주별 기록. [direction]이 null이면 모든 회전 */
    fun weekly(direction: TurnDirection? = null): List<WeekStat> {
        val filtered = results.filter { direction == null || it.direction == direction }
        val weeks = filtered.groupBy(::weekOf)
        val last = weeks.keys.maxOrNull() ?: return emptyList()
        return (0..last).map { w ->
            val inWeek = weeks[w].orEmpty()
            WeekStat(w, inWeek.size, inWeek.count { !it.signalled })
        }
    }

    fun baselineRate(direction: TurnDirection? = null): Double {
        val base = weekly(direction).take(baselineWeeks)
        val turns = base.sumOf { it.turns }
        return if (turns == 0) 0.0 else base.sumOf { it.missed }.toDouble() / turns
    }

    /** 평소 한 주에 놓치는 횟수 */
    fun baselineMissedPerWeek(): Double {
        val base = weekly().take(baselineWeeks)
        return if (base.isEmpty()) 0.0 else base.sumOf { it.missed }.toDouble() / base.size
    }

    /** 기준선을 만드는 중이면 true (처음 4주) */
    fun isLearning(): Boolean = weekly().size <= baselineWeeks

    /** 우회전, 좌회전, 전체 순서로 변화가 있는지 본다 */
    fun detectChange(): ChangeReport? {
        if (isLearning()) return null
        for (dir in listOf(TurnDirection.RIGHT, TurnDirection.LEFT, null)) {
            val weeks = weekly(dir).drop(baselineWeeks)
            if (weeks.size < 3) continue
            val recent = weeks.takeLast(3)
            val base = baselineRate(dir)
            val allAbove = recent.all { it.missRate > base }
            val enough = recent.last().missRate - base >= minIncrease
            if (allAbove && enough) return ChangeReport(dir, base, recent)
        }
        return null
    }
}
