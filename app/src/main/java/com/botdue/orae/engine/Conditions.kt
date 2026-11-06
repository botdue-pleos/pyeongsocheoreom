package com.botdue.orae.engine

/** 운전자와 가족이 함께 정하는 운전 조건. 조건부 면허 논의(야간, 고속도로)에 맞춘다 */
enum class Condition(val label: String) {
    REMIND("회전할 때마다 깜빡이 한 번 더 알리기"),
    NEAR_AT_NIGHT("해 진 뒤에는 가까운 곳만 가기"),
    REST_ON_HIGHWAY("고속도로는 휴게소 들러 가기"),
}

/** 출발하려는 운전 */
data class Trip(
    val destination: String,
    val distanceKm: Int,
    val usesHighway: Boolean,
    /** 출발 시각 (0~23시) */
    val hour: Int,
    /** 경로상 첫 휴게소. 실제로는 NaviHelper 경로 정보에서 얻는다 */
    val restStop: String? = null,
)

sealed interface Advice {
    /** 해 진 뒤 먼 곳: 낮에 가기를 권한다 (막지는 않는다) */
    data class SuggestDaytime(val condition: Condition) : Advice

    /** 고속도로 경로: 휴게소를 경유지로 넣는다 (NaviHelper 경유지 설정) */
    data class AddRestStop(val restStop: String) : Advice
}

/**
 * 함께 정한 조건을 실제 출발에 적용한다.
 * 운전을 막지 않고 제안만 한다. 고를 수 있는 건 언제나 운전자 본인이다.
 */
class ConditionAdvisor(
    private val nearKm: Int = 10,
    private val nightFrom: Int = 18,
    private val nightUntil: Int = 6,
) {
    fun isNight(hour: Int) = hour >= nightFrom || hour < nightUntil

    fun advise(trip: Trip, conditions: Set<Condition>): List<Advice> = buildList {
        if (Condition.NEAR_AT_NIGHT in conditions && isNight(trip.hour) && trip.distanceKm > nearKm) {
            add(Advice.SuggestDaytime(Condition.NEAR_AT_NIGHT))
        }
        if (Condition.REST_ON_HIGHWAY in conditions && trip.usesHighway && trip.restStop != null) {
            add(Advice.AddRestStop(trip.restStop))
        }
    }
}

/**
 * 조건이 작동한 한 번. [followed]: 제안대로 했는지
 * (낮에 가기를 골랐는지, 휴게소 경유지를 지나 도착했는지)
 */
data class ConditionEvent(val condition: Condition, val followed: Boolean)

/** 조건 하나를 지킨 기록 */
data class Kept(val condition: Condition, val suggested: Int, val followed: Int)

/**
 * 함께 정한 조건을 실제로 얼마나 지켰는지. 가족이 다음에 조건을 다시 정할 때 근거가 된다.
 * 지키지 않은 것을 탓하지 않고 횟수만 보여준다.
 */
class ConditionRecord(private val events: List<ConditionEvent>) {
    fun kept(): List<Kept> = events
        .groupBy { it.condition }
        .map { (c, e) -> Kept(c, e.size, e.count { it.followed }) }
        .sortedBy { it.condition.ordinal }
}
