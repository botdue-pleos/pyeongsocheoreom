package com.botdue.orae.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BaselineTest {
    private val start = LocalDate.of(2026, 9, 21)
    private val zone = ZoneId.of("Asia/Seoul")

    /** [week]주째 하루에 우회전 [turns]번, 그중 [missed]번 놓침 */
    private fun week(week: Int, turns: Int, missed: Int): List<TurnResult> {
        val t0 = start.plusDays(week * 7L).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        return (0 until turns).map { i -> TurnResult(t0 + i * 60_000L, TurnDirection.RIGHT, signalled = i >= missed) }
    }

    @Test
    fun `처음 4주는 기준선을 만드는 중이라 변화를 판정하지 않는다`() {
        val results = (0..3).flatMap { week(it, 20, 1) }
        val b = Baseline(results, start)
        assertTrue(b.isLearning())
        assertNull(b.detectChange())
    }

    @Test
    fun `최근 3주가 평소보다 계속 높으면 변화로 본다`() {
        val results = (0..3).flatMap { week(it, 20, 1) } + week(4, 20, 3) + week(5, 20, 5) + week(6, 20, 6)
        val report = Baseline(results, start).detectChange()
        assertNotNull(report)
        assertEquals(TurnDirection.RIGHT, report!!.direction)
        assertEquals(listOf(3, 5, 6), report.recent.map { it.missed })
    }

    @Test
    fun `한 주만 튀면 변화로 보지 않는다`() {
        val results = (0..3).flatMap { week(it, 20, 1) } + week(4, 20, 1) + week(5, 20, 1) + week(6, 20, 6)
        assertNull(Baseline(results, start).detectChange())
    }

    @Test
    fun `원래 자주 놓치던 사람은 그만큼을 평소로 본다`() {
        val results = (0..6).flatMap { week(it, 20, 5) }
        assertNull(Baseline(results, start).detectChange())
    }

    @Test
    fun `데모 데이터 7주째에는 우회전 변화가 잡히고 4주째에는 안 잡힌다`() {
        val detector = MissedTurnSignalDetector()
        val changed = DemoSignalSource(DemoScenario.CHANGED)
        val stable = DemoSignalSource(DemoScenario.STABLE)
        val c = Baseline(detector.detect(changed.signals()), changed.startDate).detectChange()
        val s = Baseline(detector.detect(stable.signals()), stable.startDate).detectChange()
        assertEquals(TurnDirection.RIGHT, c?.direction)
        assertNull(s)
    }
}
