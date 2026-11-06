package com.botdue.orae.engine

/**
 * 치매안심센터. 실제로는 공공데이터포털 "전국치매센터표준데이터"의 위도·경도로
 * 지금 위치에서 가까운 곳을 고른다.
 */
data class CareCenter(val name: String, val distanceKm: Int, val minutes: String)

/**
 * 변화를 알린 다음 본인이 할 수 있는 일을 잇는다.
 *
 * 만 60세 이상은 소득과 상관없이 치매안심센터에서 인지선별검사를 무료로 받을 수 있다.
 * 변화가 보였을 때만, 가장 가까운 센터 한 곳을 제안한다. 가라고 하지 않고 길만 열어 둔다.
 */
class NextStepAdvisor(private val freeCheckAge: Int = 60) {
    fun freeCheck(report: ChangeReport?, age: Int, centers: List<CareCenter>): CareCenter? {
        if (report == null || age < freeCheckAge) return null
        return centers.minByOrNull { it.distanceKm }
    }
}
