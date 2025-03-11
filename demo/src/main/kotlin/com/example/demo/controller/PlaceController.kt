package com.example.demo.controller

import com.example.demo.util.TextUtils
import com.example.demo.util.TextUtils.convertTMToWGS84
import com.example.demo.util.TextUtils.calculateDistance

import com.example.demo.entity.PlaceEntity
import com.example.demo.entity.AddressTEntity
import com.example.demo.entity.CategoryEntity
import com.example.demo.entity.NeedInfoEntity

import com.example.demo.repository.PlaceRepository
import com.example.demo.repository.AddressTRepository
import com.example.demo.repository.CategoryRepository
import com.example.demo.repository.NeedInfoRepository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import net.andreinc.mockneat.unit.user.Users

import org.jsoup.Jsoup
import org.jsoup.Connection
import kotlin.random.Random
import kotlin.text.toDouble
import kotlin.text.toDoubleOrNull
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.regex.Pattern
import java.lang.Thread.sleep
import net.datafaker.Faker

import io.github.cdimascio.dotenv.dotenv
val dotenv = dotenv()
val naverClientId = dotenv["NAVER_CLIENT_ID"]
val naverClientSecret = dotenv["NAVER_CLIENT_SECRET"]
val openApiSubwayUrl = dotenv["OPENAPI_SUBWAY_URL"]

@RestController
@RequestMapping("/api")
class PlaceController (
    private val placeRepository: PlaceRepository,
    private val addressTRepository: AddressTRepository,
    private val categoryRepository: CategoryRepository,
    private val needInfoRepository: NeedInfoRepository
) {
    private val faker = Faker()

    // ✅ 저장된 장소 목록 조회 API (필터링 기능 추가)
    @GetMapping("/places")
    fun getAllPlaces(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) district: String?,
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) subway: String?,
    ): Page<PlaceEntity> {
        val pageable = PageRequest.of(page, size)
        return placeRepository.findByFilters(category, city, district, searchTerm, subway, pageable)
    }

    @PostMapping("/savePlace")
    fun savePlace(@RequestBody request: PlaceRequest): ResponseEntity<String> {
        val placeDesc = request.placeDesc?.joinToString(",") ?: ""

        // ✅ 콜키지 가능 여부 확인 (placeDesc + request.placeInfo 모두 체크)
        val isCorkageAvailable = listOf("콜키지", "corkage", "병입료", "주류반입")
            .any { keyword -> 
                placeDesc.contains(keyword) && 
                !listOf("주류반입 금지", "주류반입금지").any { noKeyword -> 
                    placeDesc.contains(noKeyword) 
                }
            } || request.placeInfo.contains("콜키지 가능")

        // ✅ 무료 콜키지 여부 확인 (placeDesc + request.placeInfo 모두 체크)
        val isFreeCorkage = listOf("콜키지 무료", "콜키지무료", "콜키지프리", "콜키지 프리", "무료", "프리")
            .any { keyword -> 
                placeDesc.contains(keyword) 
            } || request.placeInfo.contains("무료")

        val cleanTitle = Jsoup.parse(request.place.title).text()   

        val (placemapx, placemapy) = convertTMToWGS84(request.place.mapx?.toDouble() ?: 0.0, request.place.mapy?.toDouble() ?: 0.0)
        val existingPlace = placeRepository.findByTitleAndAddress(cleanTitle, request.place.address ?: "")
        
        // ✅ 외부 지하철 API 호출
        val subwayInfo = getNearestSubwayInfo(placemapy, placemapx) ?: emptyList()

        val formattedSubList = subwayInfo.map { station ->  
            val stationName = station["station_name"]?.toString() ?: "Unknown"
            val lineName = station["line_name"]?.toString() ?: "Unknown"
            val distance = (station["distance_m"] as? Double)?.toInt() ?: 0
            "[$stationName,$lineName,${distance}m]"
        }.joinToString(",")
        println("🔍 변환된 리스트: $formattedSubList")

        val placeEntity = PlaceEntity(
            id = existingPlace?.id,
            title = cleanTitle,
            link = request.place.link,
            category = request.place.category,
            description = request.place.description,
            telephone = request.place.telephone,
            address = request.place.address,
            roadAddress = request.place.roadAddress,
            mapx = request.place.mapx,
            mapy = request.place.mapy,
            placeUrl = request.placeUrl, // 🔥 placeUrl 저장
            corkageAvailable = isCorkageAvailable,
            freeCorkage = isFreeCorkage,
            placeInfo = request.placeInfo.joinToString(","), // 리스트를 문자열로 변환
            corkageInfolist = placeDesc,
            nearbySubways = formattedSubList
        )

        return if (existingPlace != null) {
            // 기존 데이터가 있는 경우 업데이트
            val savedEntity = placeRepository.save(placeEntity)
            ResponseEntity.ok(savedEntity.id.toString())

        } else {
            // 새로운 데이터 추가
            val savedEntity = placeRepository.save(placeEntity)
    
            // addressT 테이블에 지역/구 정보 저장
            val addressTokens = request.place.address?.split(" ") ?: emptyList()
            if (addressTokens.size >= 2) {
                val region = addressTokens[0]
                val subregion = addressTokens[1]
                if (!addressTRepository.existsByRegionAndSubregion(region, subregion)) {
                    addressTRepository.save(AddressTEntity(region = region, subregion = subregion))
                }
            }
    
            // category 테이블에 중복 없이 저장
            val categoryTokens = request.place.category?.split(">") ?: emptyList()
            if (categoryTokens.size > 1) {
                val mainCategory = categoryTokens[1].trim()
                if (!categoryRepository.existsByName(mainCategory)) {
                    categoryRepository.save(CategoryEntity(name = mainCategory))
                }
            }
    
            ResponseEntity.ok(savedEntity.id.toString())
        }
    }
    
    @GetMapping("/search")
    fun searchPlaces(@RequestParam query: String, @RequestParam page: Int?): ResponseEntity<Any> {
        val display = 5
        val start = ((page ?: 1) - 1) * display + 1

        val uri = "https://openapi.naver.com/v1/search/local.json?query=$query&display=$display"
        val headers = org.springframework.http.HttpHeaders().apply {
            set("X-Naver-Client-Id", naverClientId)
            set("X-Naver-Client-Secret", naverClientSecret)
        }

        val requestEntity = org.springframework.http.HttpEntity<String>(headers)
        val response = RestTemplate().exchange(uri, org.springframework.http.HttpMethod.GET, requestEntity, Map::class.java)

        println("🔍 헤더: $headers")
        println("🔍 최종 요청 URI: $uri")
        println("🔍 응답 데이터: ${response.body}")

        val isLastPage = (start + display > (response.body?.get("total") as Int))
        return ResponseEntity.ok(mapOf("results" to response.body?.get("items"), "isLastPage" to isLastPage))
    }   

    private fun getNearestSubwayInfo(mapx: Double, mapy: Double): List<Map<String, Any>> {
        val response = RestTemplate().getForObject(openApiSubwayUrl, Map::class.java) as Map<String, Any>
    
        val subwayData = (response["subwayStationMaster"] as? Map<*, *>)?.get("row") as? List<Map<String, Any>> ?: emptyList()
    
        // 500m 이내 역 필터링
        val nearbyStations = subwayData.mapNotNull { subway ->
            val yPoint = (subway["LAT"] as? String)?.toDoubleOrNull() ?: return@mapNotNull null
            val xPoint = (subway["LOT"] as? String)?.toDoubleOrNull() ?: return@mapNotNull null
            val distance = calculateDistance(mapy, mapx, yPoint, xPoint)
    
            if (distance <= 800) {
                mapOf(
                    "station_name" to (subway["BLDN_NM"]?.toString() ?: "Unknown Station"),
                    "line_name" to (subway["ROUTE"]?.toString() ?: "Unknown Line"),
                    "distance_m" to distance
                )
            } else {
                null
            }
        }
        .distinctBy { it["station_name"]}
        .sortedBy { it["distance_m"] as Double }

        return nearbyStations
    }
}

data class PlaceRequest(
    val place: PlaceData,
    val placeInfo: List<String>,
    val placeUrl: String,
    val placeDesc: List<String>?
)

data class PlaceData(
    val title: String,
    val link: String?,
    val category: String?,
    val description: String?,
    val telephone: String?,
    val address: String?,
    val roadAddress: String?,
    val mapx: String?,
    val mapy: String?
)
