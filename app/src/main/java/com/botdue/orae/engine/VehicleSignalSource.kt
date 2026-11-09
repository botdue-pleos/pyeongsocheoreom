package com.botdue.orae.engine

import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * 운전 신호 공급원.
 * - [DemoSignalSource]: 시연용 가짜 주행 기록 (현재 사용)
 * - PleosSignalSource: Vehicle SDK TurnSignal·Seat·Wiper, NaviHelper onTBTInfo 콜백을 [DriveSignal]로 변환 (2차 개발 예정)
 */
interface VehicleSignalSource {
    fun signals(): List<DriveSignal>
}

/** 시연 시점: 처음 4주(기준선 만드는 중) 또는 7주째(최근 3주 우회전 깜빡이 놓침 증가) */
enum class DemoScenario(val weeks: Int) { STABLE(4), CHANGED(7) }

/**
 * 지방에 사는 70대 운전자를 흉내 낸 데이터. 매일 오전과 오후에 읍내를 오간다.
 * CHANGED 시나리오에서는 5주째부터 우회전할 때 깜빡이를 놓치는 일이 늘고, 켜는 때도 늦어진다.
 *
 * 회전·깜빡이는 [seed] 하나로 만든다. 운전석 위치·비·깜빡이 켜는 때·자녀 운전은 따로 뽑아,
 * 이것들을 넣기 전과 본인의 주별 회전·놓침 숫자가 같다.
 */
class DemoSignalSource(
    private val scenario: DemoScenario,
    val startDate: LocalDate = LocalDate.of(2026, 9, 21),
    private val seed: Int = 7,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) : VehicleSignalSource {

    /** 충북 괴산읍 근처 */
    val sun = SunClock(36.81, 127.79, zone)

    override fun signals(): List<DriveSignal> {
        val random = Random(seed)
        val extra = Random(seed + 100)
        val out = mutableListOf<DriveSignal>()
        for (d in 0 until scenario.weeks * 7) {
            val date = startDate.plusDays(d.toLong())
            val week = d / 7
            // 일요일은 쉬는 날이 많다
            if (date.dayOfWeek.value == 7 && random.nextBoolean()) continue
            val rainy = extra.nextDouble() < 0.18
            for (hour in listOf(random.nextInt(8, 11), random.nextInt(14, 17))) {
                val start = date.atTime(hour, random.nextInt(0, 50)).atZone(zone).toInstant().toEpochMilli()
                out += DriverSeat(start - 60_000, OWNER_SEAT + extra.nextInt(-2, 3))
                if (rainy) out += WiperChanged(start - 30_000, on = true)
                repeat(random.nextInt(1, 4)) { i ->
                    val t = start + i * 120_000L + random.nextLong(0, 40_000)
                    val dir = if (random.nextBoolean()) TurnDirection.LEFT else TurnDirection.RIGHT
                    out += TurnGuidance(t - 25_000, dir, 300)
                    if (random.nextDouble() >= missChance(week, dir)) {
                        val state = if (dir == TurnDirection.LEFT) TurnSignalState.LEFT else TurnSignalState.RIGHT
                        out += TurnSignalChanged(t - leadMs(week, dir, extra), state)
                        out += TurnSignalChanged(t + 3_000, TurnSignalState.OFF)
                    }
                    out += TurnExecuted(t, dir)
                }
                if (rainy) out += WiperChanged(start + 8 * 60_000, on = false)
            }
            // 토요일 점심에는 가끔 자녀가 차를 몬다. 운전석이 뒤로 가 있고, 깜빡이를 자주 놓친다
            if (date.dayOfWeek.value == 6 && extra.nextBoolean()) {
                val start = date.atTime(12, extra.nextInt(0, 30)).atZone(zone).toInstant().toEpochMilli()
                out += DriverSeat(start - 60_000, CHILD_SEAT + extra.nextInt(-2, 3))
                repeat(3) { i ->
                    val t = start + i * 120_000L
                    val dir = if (extra.nextBoolean()) TurnDirection.LEFT else TurnDirection.RIGHT
                    out += TurnGuidance(t - 25_000, dir, 300)
                    if (extra.nextDouble() >= 0.5) {
                        out += TurnSignalChanged(t - 2_000, if (dir == TurnDirection.LEFT) TurnSignalState.LEFT else TurnSignalState.RIGHT)
                        out += TurnSignalChanged(t + 3_000, TurnSignalState.OFF)
                    }
                    out += TurnExecuted(t, dir)
                }
            }
        }
        return out
    }

    /** 회전 몇 ms 전에 깜빡이를 켜는지. 평소 5~8초 전, 바뀐 뒤 우회전은 점점 늦게 */
    private fun leadMs(week: Int, dir: TurnDirection, random: Random): Long {
        val changed = scenario == DemoScenario.CHANGED && dir == TurnDirection.RIGHT && week >= 4
        val (from, until) = if (!changed) 5_000L to 8_000L else when (week) {
            4 -> 4_500L to 6_500L
            5 -> 3_500L to 5_500L
            else -> 3_000L to 5_000L
        }
        return random.nextLong(from, until)
    }

    private companion object {
        const val OWNER_SEAT = 42
        const val CHILD_SEAT = 58
    }

    private fun missChance(week: Int, dir: TurnDirection): Double {
        if (scenario == DemoScenario.STABLE || dir == TurnDirection.LEFT) return 0.06
        return listOf(0.06, 0.06, 0.06, 0.06, 0.16, 0.25, 0.32).getOrElse(week) { 0.32 }
    }
}
