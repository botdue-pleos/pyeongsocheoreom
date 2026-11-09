package com.botdue.orae.engine

import kotlin.math.abs

/**
 * 누가 운전하는지 운전석 위치로 가린다.
 *
 * 부모님 차를 자녀가 몰기도 한다. 그 운전까지 기준선에 섞이면 평소 대비 변화가 흐려지거나 엉뚱하게 잡힌다.
 * 처음 [learnTrips]번 출발의 운전석 위치 가운데값을 "본인 자리"로 삼고,
 * 그보다 [tolerance] 넘게 다르면 다른 사람이 운전한 것으로 본다.
 * 위치 값의 단위는 차종마다 달라 [tolerance]는 실차에서 맞춘다.
 */
class DriverMatcher(
    private val ownerPosition: Int?,
    private val tolerance: Int = 6,
) {
    fun isOwner(position: Int): Boolean = ownerPosition == null || abs(position - ownerPosition) <= tolerance

    /** 본인 자리와 다른 출발 */
    fun otherTrips(signals: List<DriveSignal>): List<DriverSeat> =
        signals.filterIsInstance<DriverSeat>().filterNot { isOwner(it.foreAftPosition) }

    companion object {
        fun learn(signals: List<DriveSignal>, learnTrips: Int = 20, tolerance: Int = 6): DriverMatcher {
            val first = signals.filterIsInstance<DriverSeat>().sortedBy { it.timeMs }.take(learnTrips)
            val median = first.map { it.foreAftPosition }.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }
            return DriverMatcher(median, tolerance)
        }
    }
}
