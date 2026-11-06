package com.botdue.orae.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextStepTest {
    private val advisor = NextStepAdvisor()
    private val change = ChangeReport(TurnDirection.RIGHT, 0.05, emptyList())
    private val centers = listOf(
        CareCenter("시 치매안심센터", 15, "25분"),
        CareCenter("읍 치매안심센터", 7, "12분"),
    )

    @Test
    fun `변화가 있고 만 60세 이상이면 가장 가까운 센터를 제안한다`() {
        assertEquals("읍 치매안심센터", advisor.freeCheck(change, age = 74, centers)?.name)
        assertEquals("읍 치매안심센터", advisor.freeCheck(change, age = 60, centers)?.name)
    }

    @Test
    fun `변화가 없으면 제안하지 않는다`() {
        assertNull(advisor.freeCheck(null, age = 74, centers))
    }

    @Test
    fun `만 60세 미만은 무료 대상이 아니라 제안하지 않는다`() {
        assertNull(advisor.freeCheck(change, age = 59, centers))
    }

    @Test
    fun `주변에 센터 정보가 없으면 제안하지 않는다`() {
        assertNull(advisor.freeCheck(change, age = 74, emptyList()))
    }
}
