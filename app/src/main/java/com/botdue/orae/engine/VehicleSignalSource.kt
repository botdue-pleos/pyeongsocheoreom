package com.botdue.orae.engine

import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * 운전 신호 공급원.
 * - [DemoSignalSource]: 시연용 가짜 주행 기록 (현재 사용)
 * - PleosSignalSource: Vehicle SDK TurnSignal, NaviHelper onTBTInfo 콜백을 [DriveSignal]로 변환 (2차 개발 예정)
 */
interface VehicleSignalSource {
    fun signals(): List<DriveSignal>
}

/** 시연 시점: 처음 4주(기준선 만드는 중) 또는 7주째(최근 3주 우회전 깜빡이 놓침 증가) */
enum class DemoScenario(val weeks: Int) { STABLE(4), CHANGED(7) }

/**
 * 지방에 사는 70대 운전자를 흉내 낸 데이터. 매일 오전과 오후에 읍내를 오간다.
 * CHANGED 시나리오에서는 5주째부터 우회전할 때 깜빡이를 놓치는 일이 늘어난다.
 */
class DemoSignalSource(
    private val scenario: DemoScenario,
    val startDate: LocalDate = LocalDate.of(2026, 9, 21),
    private val seed: Int = 7,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) : VehicleSignalSource {

    override fun signals(): List<DriveSignal> {
        val random = Random(seed)
        val out = mutableListOf<DriveSignal>()
        for (d in 0 until scenario.weeks * 7) {
            val date = startDate.plusDays(d.toLong())
            val week = d / 7
            // 일요일은 쉬는 날이 많다
            if (date.dayOfWeek.value == 7 && random.nextBoolean()) continue
            for (hour in listOf(random.nextInt(8, 11), random.nextInt(14, 17))) {
                val start = date.atTime(hour, random.nextInt(0, 50)).atZone(zone).toInstant().toEpochMilli()
                repeat(random.nextInt(1, 4)) { i ->
                    val t = start + i * 120_000L + random.nextLong(0, 40_000)
                    val dir = if (random.nextBoolean()) TurnDirection.LEFT else TurnDirection.RIGHT
                    out += TurnGuidance(t - 25_000, dir, 300)
                    if (random.nextDouble() >= missChance(week, dir)) {
                        val state = if (dir == TurnDirection.LEFT) TurnSignalState.LEFT else TurnSignalState.RIGHT
                        out += TurnSignalChanged(t - 6_000, state)
                        out += TurnSignalChanged(t + 3_000, TurnSignalState.OFF)
                    }
                    out += TurnExecuted(t, dir)
                }
            }
        }
        return out
    }

    private fun missChance(week: Int, dir: TurnDirection): Double {
        if (scenario == DemoScenario.STABLE || dir == TurnDirection.LEFT) return 0.06
        return listOf(0.06, 0.06, 0.06, 0.06, 0.16, 0.25, 0.32).getOrElse(week) { 0.32 }
    }
}
