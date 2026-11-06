package com.botdue.orae.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionAndWanderingTest {
    private val advisor = ConditionAdvisor()
    private val farTrip = Trip("큰딸 집", distanceKm = 82, usesHighway = true, hour = 19, restStop = "여주휴게소")

    @Test
    fun `해 진 뒤 먼 곳은 낮에 가기를 권한다`() {
        val advice = advisor.advise(farTrip, setOf(Condition.NEAR_AT_NIGHT))
        assertEquals(listOf(Advice.SuggestDaytime(Condition.NEAR_AT_NIGHT)), advice)
    }

    @Test
    fun `낮이거나 가까운 곳이면 아무 말도 하지 않는다`() {
        assertTrue(advisor.advise(farTrip.copy(hour = 10), setOf(Condition.NEAR_AT_NIGHT)).isEmpty())
        assertTrue(advisor.advise(farTrip.copy(distanceKm = 5, usesHighway = false), setOf(Condition.NEAR_AT_NIGHT)).isEmpty())
    }

    @Test
    fun `조건을 정하지 않았으면 제안하지 않는다`() {
        assertTrue(advisor.advise(farTrip, emptySet()).isEmpty())
    }

    @Test
    fun `고속도로 경로에는 휴게소를 넣는다`() {
        val advice = advisor.advise(farTrip.copy(hour = 10), setOf(Condition.REST_ON_HIGHWAY))
        assertEquals(listOf(Advice.AddRestStop("여주휴게소")), advice)
    }

    private val detector = WanderingDetector()

    @Test
    fun `자주 가던 길에서 5분 안에 재탐색이 두 번이면 헤맴`() {
        assertTrue(detector.isWandering(12, listOf(0L, 3 * 60_000L)))
    }

    @Test
    fun `재탐색 한 번이나 멀리 떨어진 두 번은 헤맴이 아니다`() {
        assertFalse(detector.isWandering(12, listOf(0L)))
        assertFalse(detector.isWandering(12, listOf(0L, 20 * 60_000L)))
    }

    @Test
    fun `처음 가는 길에서는 재탐색이 잦아도 헤맴으로 보지 않는다`() {
        assertFalse(detector.isWandering(1, listOf(0L, 60_000L, 120_000L)))
    }

    @Test
    fun `조건마다 제안 횟수와 지킨 횟수를 센다`() {
        val kept = ConditionRecord(
            listOf(
                ConditionEvent(Condition.REST_ON_HIGHWAY, followed = true),
                ConditionEvent(Condition.NEAR_AT_NIGHT, followed = true),
                ConditionEvent(Condition.NEAR_AT_NIGHT, followed = false),
                ConditionEvent(Condition.NEAR_AT_NIGHT, followed = true),
            )
        ).kept()
        assertEquals(
            listOf(Kept(Condition.NEAR_AT_NIGHT, 3, 2), Kept(Condition.REST_ON_HIGHWAY, 1, 1)),
            kept,
        )
    }

    @Test
    fun `조건이 작동한 적이 없으면 기록도 없다`() {
        assertTrue(ConditionRecord(emptyList()).kept().isEmpty())
    }
}
