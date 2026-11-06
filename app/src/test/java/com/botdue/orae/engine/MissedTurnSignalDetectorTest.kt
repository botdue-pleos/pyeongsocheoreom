package com.botdue.orae.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedTurnSignalDetectorTest {
    private val detector = MissedTurnSignalDetector(signalWindowMs = 10_000)

    @Test
    fun `안내 후 같은 방향 깜빡이를 켜고 돌면 켬으로 판정`() {
        val r = detector.detect(
            listOf(
                TurnGuidance(0, TurnDirection.RIGHT, 300),
                TurnSignalChanged(20_000, TurnSignalState.RIGHT),
                TurnExecuted(25_000, TurnDirection.RIGHT),
            )
        )
        assertEquals(1, r.size)
        assertTrue(r[0].signalled)
    }

    @Test
    fun `깜빡이 없이 돌면 놓침`() {
        val r = detector.detect(
            listOf(
                TurnGuidance(0, TurnDirection.LEFT, 300),
                TurnExecuted(25_000, TurnDirection.LEFT),
            )
        )
        assertFalse(r.single().signalled)
    }

    @Test
    fun `반대 방향 깜빡이는 놓침`() {
        val r = detector.detect(
            listOf(
                TurnGuidance(0, TurnDirection.LEFT, 300),
                TurnSignalChanged(20_000, TurnSignalState.RIGHT),
                TurnExecuted(25_000, TurnDirection.LEFT),
            )
        )
        assertFalse(r.single().signalled)
    }

    @Test
    fun `회전 10초보다 훨씬 전에 켰다 끈 깜빡이는 인정하지 않음`() {
        val r = detector.detect(
            listOf(
                TurnGuidance(0, TurnDirection.RIGHT, 300),
                TurnSignalChanged(1_000, TurnSignalState.RIGHT),
                TurnSignalChanged(3_000, TurnSignalState.OFF),
                TurnExecuted(40_000, TurnDirection.RIGHT),
            )
        )
        assertFalse(r.single().signalled)
    }

    @Test
    fun `안내 없는 회전은 판정하지 않음`() {
        val r = detector.detect(listOf(TurnExecuted(25_000, TurnDirection.RIGHT)))
        assertTrue(r.isEmpty())
    }

}
