package com.botdue.orae.engine

/**
 * 회전 안내 대비 깜빡이 누락 판정.
 *
 * 규칙: 내비가 안내한 방향으로 실제 회전했을 때, 회전 직전 [signalWindowMs] 안에
 * 같은 방향 깜빡이가 한 번이라도 켜져 있었으면 "켬", 아니면 "놓침"으로 본다.
 * 안내 없이 한 회전(골목 진입 등)은 판정하지 않는다.
 *
 * 판정마다 함께 남기는 것
 * - 회전 몇 초 전에 켰는지 ([TurnResult.leadMs]): 놓치기 전에 먼저 늦어지는지 보려고
 * - 누가 운전했는지 ([isOwner]): 출발할 때 운전석 위치([DriverSeat])로 가린다
 * - 운전 여건 ([TurnResult.context]): 와이퍼가 돌면 비, [isDark]이면 해 진 뒤
 */
class MissedTurnSignalDetector(
    private val signalWindowMs: Long = 10_000,
    private val guidanceWindowMs: Long = 90_000,
) {
    fun detect(
        signals: List<DriveSignal>,
        isOwner: (seatPosition: Int) -> Boolean = { true },
        isDark: (timeMs: Long) -> Boolean = { false },
    ): List<TurnResult> {
        val sorted = signals.sortedBy { it.timeMs }
        val results = mutableListOf<TurnResult>()
        var signalState = TurnSignalState.OFF
        // 지금 켜진 깜빡이가 켜진 시각
        var signalOnAt: Long? = null
        val signalHistory = mutableListOf<TurnSignalChanged>()
        var lastGuidance: TurnGuidance? = null
        var owner = true
        var wiperOn = false

        for (signal in sorted) {
            when (signal) {
                is TurnGuidance -> lastGuidance = signal
                is DriverSeat -> owner = isOwner(signal.foreAftPosition)
                is WiperChanged -> wiperOn = signal.on
                is TurnSignalChanged -> {
                    if (signal.state != signalState) signalOnAt = if (signal.state == TurnSignalState.OFF) null else signal.timeMs
                    signalState = signal.state
                    signalHistory += signal
                }
                is TurnExecuted -> {
                    val guidance = lastGuidance
                    val guided = guidance != null &&
                        guidance.direction == signal.direction &&
                        signal.timeMs - guidance.timeMs <= guidanceWindowMs
                    if (guided) {
                        val expected = signal.direction.toSignalState()
                        val windowStart = signal.timeMs - signalWindowMs
                        val onInWindow = signalHistory.lastOrNull {
                            it.state == expected && it.timeMs in windowStart..signal.timeMs
                        }
                        val alreadyOn = signalState == expected
                        val lead = when {
                            alreadyOn -> signalOnAt?.let { signal.timeMs - it }
                            onInWindow != null -> signal.timeMs - onInWindow.timeMs
                            else -> null
                        }
                        val context = when {
                            wiperOn -> DriveContext.RAIN
                            isDark(signal.timeMs) -> DriveContext.DARK
                            else -> DriveContext.CLEAR
                        }
                        results += TurnResult(signal.timeMs, signal.direction, onInWindow != null || alreadyOn, lead, owner, context)
                        lastGuidance = null
                    }
                }
            }
        }
        return results
    }

    private fun TurnDirection.toSignalState() = when (this) {
        TurnDirection.LEFT -> TurnSignalState.LEFT
        TurnDirection.RIGHT -> TurnSignalState.RIGHT
    }
}
