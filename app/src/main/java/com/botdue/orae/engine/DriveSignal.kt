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

/**
 * Vehicle Seat getCurrentSeatForeAftPosition: 출발할 때 운전석 앞뒤 위치.
 * 같은 차를 가족이 번갈아 몰 때 누가 운전하는지 가리는 데 쓴다 ([DriverMatcher]).
 */
data class DriverSeat(
    override val timeMs: Long,
    val foreAftPosition: Int,
) : DriveSignal

/** Vehicle Wiper registerWiperState: 앞유리 와이퍼가 켜지거나 꺼짐 */
data class WiperChanged(
    override val timeMs: Long,
    val on: Boolean,
) : DriveSignal

/** 회전한 때의 운전 여건. 와이퍼가 돌면 비, 해가 진 뒤면 어두울 때 */
enum class DriveContext(val label: String) {
    CLEAR("맑은 낮"),
    RAIN("비 오는 날"),
    DARK("해 진 뒤"),
}

/** 회전 한 번의 판정 결과 */
data class TurnResult(
    val timeMs: Long,
    val direction: TurnDirection,
    val signalled: Boolean,
    /** 회전 몇 ms 전에 깜빡이를 켰는지. 놓쳤으면 null */
    val leadMs: Long? = null,
    /** 기준선의 주인(본인)이 운전했는지. 다른 가족이 몰았으면 false */
    val owner: Boolean = true,
    val context: DriveContext = DriveContext.CLEAR,
)
