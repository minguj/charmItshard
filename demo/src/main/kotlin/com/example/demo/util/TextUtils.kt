package com.example.demo.util

import kotlin.math.*

object TextUtils {
    fun extractCorkageInfo(inputText: String): List<String> {
        val corkagePattern = Regex(".*(콜키지.?프리|콜키지.?차지|콜키지|corkage|병입료|주류반입|와인|메그넘|위스키|사케).*", RegexOption.IGNORE_CASE)
        return inputText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { corkagePattern.matches(it) }
    }

    fun convertTMToWGS84(tmX: Double, tmY: Double): Pair<Double, Double> {
        // 1270000000 -> 127.0000000 (경도)
        val lon = tmX / 10000000.0
        
        // 375000000 -> 37.5000000 (위도)
        val lat = tmY / 10000000.0
        
        return Pair(lat, lon)
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반경 (미터 단위)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }    

}
