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
    /** 맑은 낮 회전만 따로 견준 결과. 날씨나 밤 탓인지 가린다 */
    val clear: ClearCheck? = null,
)

/**
 * 맑은 낮 회전만 평소와 최근 3주를 견준 것.
 * [changed]가 false면 늘어난 놓침이 [risesIn] 여건(비 오는 날, 해 진 뒤)에 몰려 있다는 뜻이다.
 */
data class ClearCheck(
    val baseRate: Double,
    val recentRate: Double,
    val changed: Boolean,
    val risesIn: List<DriveContext>,
)

/** 회전 몇 초 전에 깜빡이를 켜는지. 평소와 지난주 가운데값 */
data class LeadReport(val usualMs: Long, val recentMs: Long, val later: Boolean)

/** 회전 가운데 좌회전 비율. 평소와 최근 3주 */
data class LeftShareReport(val usual: Double, val recent: Double, val fewer: Boolean)

/** 운전 여건 하나(맑은 낮·비 오는 날·해 진 뒤)에서 평소와 최근 3주 놓침 비율 */
data class ContextRate(
    val context: DriveContext,
    val baseRate: Double,
    val recentRate: Double,
    val recentTurns: Int,
    /** 최근 회전이 판단할 만큼 있고, 평소보다 늘었다 */
    val rose: Boolean,
)

/**
 * 나만의 기준선.
 *
 * 처음 [baselineWeeks]주 기록을 본인의 "평소"로 삼고, 그 뒤로는 평소 대비 변화만 본다.
 * 다른 사람과 비교하거나 점수로 판정하지 않는다 (진단 아님).
 * 변화 판정: 최근 3주가 모두 평소보다 높고, 가장 최근 주가 평소보다 [minIncrease] 이상 높을 때.
 */
class Baseline(
    results: List<TurnResult>,
    private val startDate: LocalDate,
    private val baselineWeeks: Int = 4,
    private val minIncrease: Double = 0.1,
    /** 깜빡이를 이만큼 늦게 켜게 되면 늦어졌다고 본다 */
    private val minLeadDropMs: Long = 1_500,
    /** 좌회전 비율이 이만큼 줄면 줄었다고 본다 */
    private val minLeftDrop: Double = 0.1,
    /** 여건별로 견줄 때 최근 3주에 이만큼은 있어야 판단한다 */
    private val minContextTurns: Int = 8,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {
    /** 본인이 운전한 회전만 본다. 다른 가족이 몬 회전은 평소에도 최근에도 넣지 않는다 */
    private val results = results.filter { it.owner }
    private val others = results.filterNot { it.owner }

    /** 다른 사람이 운전해서 뺀 회전 수 */
    val excludedTurns: Int = results.size - this.results.size

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
            if (allAbove && enough) return ChangeReport(dir, base, recent, clearCheck(dir))
        }
        return null
    }

    private fun inBaseline(r: TurnResult) = weekOf(r) < baselineWeeks

    private fun inRecent(r: TurnResult): Boolean {
        val last = weekly().size - 1
        return weekOf(r) in (last - 2)..last && !inBaseline(r)
    }

    private fun rate(turns: List<TurnResult>) =
        if (turns.isEmpty()) 0.0 else turns.count { !it.signalled }.toDouble() / turns.size

    /** 맑은 낮 회전만 따로 견준다. 맑은 낮에도 늘었으면 날씨·밤 탓이 아니다 */
    private fun clearCheck(direction: TurnDirection?): ClearCheck? {
        val turns = results.filter { direction == null || it.direction == direction }
        fun of(c: DriveContext) = turns.filter { it.context == c }
        val clearRecent = of(DriveContext.CLEAR).filter(::inRecent)
        if (clearRecent.size < minContextTurns) return null
        val base = rate(of(DriveContext.CLEAR).filter(::inBaseline))
        val recent = rate(clearRecent)
        val risesIn = listOf(DriveContext.RAIN, DriveContext.DARK).filter { c ->
            val r = of(c).filter(::inRecent)
            r.size >= minContextTurns && rate(r) - rate(of(c).filter(::inBaseline)) >= minIncrease
        }
        return ClearCheck(base, recent, recent - base >= minIncrease, risesIn)
    }

    /** 여건별 평소·최근 놓침 비율. 화면에서 날씨·밤 탓인지 나눠 보여 줄 때 쓴다 */
    fun contexts(direction: TurnDirection? = null): List<ContextRate> {
        if (isLearning()) return emptyList()
        val turns = results.filter { direction == null || it.direction == direction }
        return DriveContext.entries.map { c ->
            val of = turns.filter { it.context == c }
            val recent = of.filter(::inRecent)
            val base = rate(of.filter(::inBaseline))
            val now = rate(recent)
            ContextRate(c, base, now, recent.size, recent.size >= minContextTurns && now - base >= minIncrease)
        }
    }

    /** 주마다 다른 사람이 운전해서 뺀 회전 수. [weekly]와 같은 길이 */
    fun weeklyOthers(): List<Int> {
        val byWeek = others.groupBy(::weekOf)
        return weekly().indices.map { byWeek[it]?.size ?: 0 }
    }

    /** 처음 4주가 지나야 본다. 켠 회전이 없는 주는 가운데값을 낼 수 없어 null */
    fun lead(direction: TurnDirection? = null): LeadReport? {
        if (isLearning()) return null
        val turns = results.filter { (direction == null || it.direction == direction) && it.leadMs != null }
        val usual = median(turns.filter(::inBaseline).mapNotNull { it.leadMs }) ?: return null
        val byWeek = turns.groupBy(::weekOf)
        val last = weekly().size - 1
        val recent = (maxOf(baselineWeeks, last - 2)..last).map { w -> median(byWeek[w].orEmpty().mapNotNull { it.leadMs }) ?: return null }
        val later = recent.all { it < usual } && usual - recent.last() >= minLeadDropMs
        return LeadReport(usual, recent.last(), later)
    }

    fun leftShare(): LeftShareReport? {
        if (isLearning()) return null
        fun share(turns: List<TurnResult>) =
            if (turns.isEmpty()) 0.0 else turns.count { it.direction == TurnDirection.LEFT }.toDouble() / turns.size
        val byWeek = results.groupBy(::weekOf)
        val last = weekly().size - 1
        val usual = share(results.filter(::inBaseline))
        val weeks = (maxOf(baselineWeeks, last - 2)..last).map { share(byWeek[it].orEmpty()) }
        val fewer = weeks.all { it < usual } && usual - weeks.last() >= minLeftDrop
        return LeftShareReport(usual, share(results.filter(::inRecent)), fewer)
    }

    private fun median(values: List<Long>): Long? = values.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }
}
