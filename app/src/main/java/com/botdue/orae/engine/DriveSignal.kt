package com.botdue.orae.engine

enum class TurnDirection { LEFT, RIGHT }

enum class TurnSignalState { OFF, LEFT, RIGHT }

/**
 * 차량에서 들어오는 운전 신호. 모든 신호는 발생 시각(epoch ms)을 가진다.
 * 실제 차량에서는 Pleos SDK 콜백을 이 형태로 바꿔 엔진에 넣는다.
 */
sealed interface DriveSignal {
    val timeMs: Long
}

/** NaviHelper onTBTInfo: 내비가 회전을 안내함 */
data class TurnGuidance(
    override val timeMs: Long,
    val direction: TurnDirection,
    val distanceM: Int,
) : DriveSignal

/** Vehicle TurnSignal onTurnSignalStateUpdated: 방향지시등 상태가 바뀜 */
data class TurnSignalChanged(
    override val timeMs: Long,
    val state: TurnSignalState,
) : DriveSignal

/** 안내받은 회전 지점을 실제로 통과함 (TBT 남은 거리 0 또는 경로 진행) */
data class TurnExecuted(
    override val timeMs: Long,
    val direction: TurnDirection,
) : DriveSignal

/** 회전 한 번의 판정 결과 */
data class TurnResult(
    val timeMs: Long,
    val direction: TurnDirection,
    val signalled: Boolean,
)
