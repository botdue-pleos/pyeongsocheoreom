package com.botdue.orae.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 기준선을 더 정확하게: 누가 운전했는지, 어떤 여건이었는지, 깜빡이를 언제 켰는지 */
class BaselineAccuracyTest {
    private val start = LocalDate.of(2026, 9, 21)
    private val zone = ZoneId.of("Asia/Seoul")
    private val detector = MissedTurnSignalDetector()

    private fun turn(t: Long, dir: TurnDirection, leadMs: Long?) = buildList {
        add(TurnGuidance(t - 25_000, dir, 300))
        if (leadMs != null) {
            add(TurnSignalChanged(t - leadMs, if (dir == TurnDirection.LEFT) TurnSignalState.LEFT else TurnSignalState.RIGHT))
            add(TurnSignalChanged(t + 3_000, TurnSignalState.OFF))
        }
        add(TurnExecuted(t, dir))
    }

    /** [week]주째 회전 [turns]번, 그중 앞의 [missed]번 놓침 */
    private fun week(
        week: Int, turns: Int, missed: Int,
        dir: TurnDirection = TurnDirection.RIGHT,
        context: DriveContext = DriveContext.CLEAR,
        leadMs: Long? = 6_000,
        owner: Boolean = true,
        hour: Int = 10,
    ): List<TurnResult> {
        val t0 = start.plusDays(week * 7L).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return (0 until turns).map { i ->
            val signalled = i >= missed
            TurnResult(t0 + i * 60_000L, dir, signalled, if (signalled) leadMs else null, owner, context)
        }
    }

    @Test
    fun `깜빡이를 회전 몇 초 전에 켰는지 남긴다`() {
        val r = detector.detect(turn(100_000, TurnDirection.RIGHT, 7_000) + turn(300_000, TurnDirection.LEFT, null))
        assertEquals(7_000L, r[0].leadMs)
        assertNull(r[1].leadMs)
    }

    @Test
    fun `운전석 위치가 본인 자리와 다르면 다른 사람 운전으로 남긴다`() {
        val matcher = DriverMatcher(ownerPosition = 42)
        val signals = listOf(DriverSeat(0, 43)) + turn(100_000, TurnDirection.RIGHT, 6_000) +
            listOf(DriverSeat(200_000, 58)) + turn(300_000, TurnDirection.RIGHT, 6_000)
        val r = detector.detect(signals, matcher::isOwner)
        assertEquals(listOf(true, false), r.map { it.owner })
        assertEquals(1, matcher.otherTrips(signals).size)
    }

    @Test
    fun `본인 자리는 처음 출발들의 운전석 위치 가운데값이다`() {
        val seats = listOf(41, 42, 43, 58, 42).mapIndexed { i, p -> DriverSeat(i * 1000L, p) }
        val m = DriverMatcher.learn(seats)
        assertTrue(m.isOwner(44))
        assertFalse(m.isOwner(58))
    }

    @Test
    fun `와이퍼가 돌면 비, 해가 졌으면 해 진 뒤로 여건을 남긴다`() {
        val signals = listOf(WiperChanged(0, true)) + turn(100_000, TurnDirection.RIGHT, 6_000) +
            listOf(WiperChanged(200_000, false)) + turn(300_000, TurnDirection.RIGHT, 6_000) + turn(500_000, TurnDirection.RIGHT, 6_000)
        val r = detector.detect(signals, isDark = { it >= 500_000 })
        assertEquals(listOf(DriveContext.RAIN, DriveContext.CLEAR, DriveContext.DARK), r.map { it.context })
    }

    @Test
    fun `서울 11월 9일 해는 7시쯤 뜨고 5시 반쯤 진다`() {
        val sun = SunClock(37.57, 126.98, zone)
        val (rise, set) = sun.riseAndSet(LocalDate.of(2026, 11, 9))
        val riseAt = Instant.ofEpochMilli(rise).atZone(zone).toLocalTime()
        val setAt = Instant.ofEpochMilli(set).atZone(zone).toLocalTime()
        assertTrue("일출 $riseAt", riseAt.hour == 7 && riseAt.minute < 20)
        assertTrue("일몰 $setAt", setAt.hour == 17 && setAt.minute in 15..35)
        fun at(h: Int) = LocalDate.of(2026, 11, 9).atTime(h, 0).atZone(zone).toInstant().toEpochMilli()
        assertTrue(sun.isDark(at(5)))
        assertFalse(sun.isDark(at(12)))
        assertTrue(sun.isDark(at(20)))
    }

    @Test
    fun `다른 사람이 운전한 회전은 기준선에서 빼서 변화로 잡지 않는다`() {
        val own = (0..6).flatMap { week(it, 20, 1) }
        val child = (4..6).flatMap { week(it, 6, 4, owner = false, hour = 12) }
        val b = Baseline(own + child, start)
        assertNull(b.detectChange())
        assertEquals(18, b.excludedTurns)
    }

    @Test
    fun `비 오는 날에만 늘었으면 맑은 낮은 그대로라고 알린다`() {
        val clear = (0..6).flatMap { week(it, 16, 1) }
        val rain = (0..3).flatMap { week(it, 4, 0, context = DriveContext.RAIN, hour = 15) } +
            (4..6).flatMap { week(it, 6, 4, context = DriveContext.RAIN, hour = 15) }
        val c = Baseline(clear + rain, start).detectChange()?.clear
        assertNotNull(c)
        assertFalse(c!!.changed)
        assertEquals(listOf(DriveContext.RAIN), c.risesIn)
    }

    @Test
    fun `맑은 낮에도 늘었으면 날씨 탓이 아니라고 본다`() {
        val results = (0..3).flatMap { week(it, 20, 1) } + week(4, 20, 3) + week(5, 20, 5) + week(6, 20, 6)
        assertTrue(Baseline(results, start).detectChange()!!.clear!!.changed)
    }

    @Test
    fun `깜빡이를 켜는 때가 3주 내내 늦어지면 알린다`() {
        val results = (0..3).flatMap { week(it, 20, 0, leadMs = 6_500) } +
            week(4, 20, 0, leadMs = 5_500) + week(5, 20, 0, leadMs = 4_500) + week(6, 20, 0, leadMs = 4_000)
        val lead = Baseline(results, start).lead()!!
        assertTrue(lead.later)
        assertEquals(6_500L, lead.usualMs)
        assertEquals(4_000L, lead.recentMs)
    }

    @Test
    fun `한 주만 늦게 켜면 늦어졌다고 보지 않는다`() {
        val results = (0..5).flatMap { week(it, 20, 0, leadMs = 6_500) } + week(6, 20, 0, leadMs = 3_000)
        assertFalse(Baseline(results, start).lead()!!.later)
    }

    @Test
    fun `좌회전이 3주 내내 줄면 줄었다고 본다`() {
        val right = (0..6).flatMap { week(it, 10, 0) }
        val left = (0..3).flatMap { week(it, 10, 0, dir = TurnDirection.LEFT, hour = 15) } +
            listOf(7, 5, 4).mapIndexed { i, n -> week(4 + i, n, 0, dir = TurnDirection.LEFT, hour = 15) }.flatten()
        val share = Baseline(right + left, start).leftShare()!!
        assertTrue(share.fewer)
        assertEquals(0.5, share.usual, 1e-9)
    }

    @Test
    fun `데모 데이터 7주째는 자녀 운전을 빼고도 맑은 낮 우회전 변화와 늦어진 깜빡이가 잡힌다`() {
        val source = DemoSignalSource(DemoScenario.CHANGED)
        val signals = source.signals()
        val matcher = DriverMatcher.learn(signals)
        val b = Baseline(detector.detect(signals, matcher::isOwner, source.sun::isDark), source.startDate)
        // 운전석·비·자녀 운전을 넣기 전과 본인 기록이 같아야 화면 숫자가 그대로다
        assertEquals(listOf(2, 1, 2, 0, 3, 5, 5), b.weekly().map { it.missed })
        assertTrue(matcher.otherTrips(signals).isNotEmpty())
        val change = b.detectChange()!!
        assertEquals(TurnDirection.RIGHT, change.direction)
        assertTrue(change.clear!!.changed)
        assertTrue(b.lead(TurnDirection.RIGHT)!!.later)
        assertFalse(b.leftShare()!!.fewer)
    }

    @Test
    fun `여건별로 평소와 최근 놓침 비율을 나눠 낸다`() {
        val results = (0..3).flatMap { week(it, 10, 1) + week(it, 10, 1, context = DriveContext.RAIN) } +
            (4..6).flatMap { week(it, 10, 1) + week(it, 10, 5, context = DriveContext.RAIN) }
        val rates = Baseline(results, start).contexts().associateBy { it.context }
        assertFalse(rates.getValue(DriveContext.CLEAR).rose)
        assertTrue(rates.getValue(DriveContext.RAIN).rose)
        assertEquals(0.5, rates.getValue(DriveContext.RAIN).recentRate, 1e-9)
        assertEquals(0, rates.getValue(DriveContext.DARK).recentTurns)
    }

    @Test
    fun `주마다 다른 사람이 운전해서 뺀 회전 수를 센다`() {
        val results = (0..4).flatMap { week(it, 10, 1) } + week(1, 3, 0, owner = false) + week(4, 2, 0, owner = false)
        assertEquals(listOf(0, 3, 0, 0, 2), Baseline(results, start).weeklyOthers())
    }
}
