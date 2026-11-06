package com.botdue.orae.engine

/**
 * 길 헤맴 감지.
 *
 * 자주 다니던 길(이 목적지로 [familiarTrips]번 이상 다녀옴)에서
 * [windowMs] 안에 경로 재탐색(NaviHelper onReRouteEnded)이 [threshold]번 이상 나오면 헤맴으로 본다.
 * 처음 가거나 드물게 가는 길에서의 재탐색은 자연스러운 일이라 보지 않는다.
 */
class WanderingDetector(
    private val threshold: Int = 2,
    private val windowMs: Long = 5 * 60_000,
    private val familiarTrips: Int = 5,
) {
    fun isWandering(pastTripsToDestination: Int, reRouteTimesMs: List<Long>): Boolean {
        if (pastTripsToDestination < familiarTrips) return false
        val sorted = reRouteTimesMs.sorted()
        return sorted.indices.any { i ->
            val inWindow = sorted.drop(i).takeWhile { it - sorted[i] <= windowMs }
            inWindow.size >= threshold
        }
    }
}
