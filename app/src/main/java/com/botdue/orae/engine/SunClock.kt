package com.botdue.orae.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * 해 뜨고 지는 시각. 위치(FusedLocation)와 날짜만으로 계산해 인터넷이 필요 없다.
 * 미국 해양대기청(NOAA)의 일출·일몰 근사식으로, 오차는 몇 분 안이다.
 */
class SunClock(
    private val latitude: Double,
    private val longitude: Double,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {
    /** 그날 해 뜨는 시각과 지는 시각 (epoch ms) */
    fun riseAndSet(date: LocalDate): Pair<Long, Long> = moment(date, rising = true) to moment(date, rising = false)

    fun isDark(timeMs: Long): Boolean {
        val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
        val (rise, set) = riseAndSet(date)
        return timeMs < rise || timeMs >= set
    }

    private fun moment(date: LocalDate, rising: Boolean): Long {
        val rad = Math.PI / 180
        val lngHour = longitude / 15
        val t = date.dayOfYear + ((if (rising) 6.0 else 18.0) - lngHour) / 24
        val m = 0.9856 * t - 3.289
        val l = norm(m + 1.916 * sin(m * rad) + 0.020 * sin(2 * m * rad) + 282.634, 360.0)
        var ra = norm(atan(0.91764 * tan(l * rad)) / rad, 360.0)
        ra = (ra + floor(l / 90) * 90 - floor(ra / 90) * 90) / 15
        val sinDec = 0.39782 * sin(l * rad)
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(ZENITH * rad) - sinDec * sin(latitude * rad)) / (cosDec * cos(latitude * rad))
        val h = (if (rising) 360 - acos(cosH) / rad else acos(cosH) / rad) / 15
        val ut = norm(h + ra - 0.06571 * t - 6.622 - lngHour, 24.0)
        // 세계시를 그 지역 시각으로 바꿔 같은 날짜에 붙인다 (한국 일출은 세계시로 전날 밤이다)
        val start = date.atStartOfDay(zone)
        val offsetHours = start.offset.totalSeconds / 3600.0
        return start.toInstant().toEpochMilli() + (norm(ut + offsetHours, 24.0) * 3_600_000).toLong()
    }

    private fun norm(v: Double, range: Double) = ((v % range) + range) % range

    private companion object {
        /** 대기 굴절을 넣은 일출·일몰 천정각 */
        const val ZENITH = 90.833
    }
}
