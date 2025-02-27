package com.example.demo.controller

import com.example.demo.util.TextUtils

import com.example.demo.entity.PlaceEntity
import com.example.demo.entity.AddressTEntity
import com.example.demo.entity.CategoryEntity

import com.example.demo.repository.PlaceRepository
import com.example.demo.repository.AddressTRepository
import com.example.demo.repository.CategoryRepository

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
import java.lang.Thread.sleep
import net.datafaker.Faker

import io.github.cdimascio.dotenv.dotenv
val dotenv = dotenv()
val naverClientId = dotenv["NAVER_CLIENT_ID"]
val naverClientSecret = dotenv["NAVER_CLIENT_SECRET"]

@RestController
@RequestMapping("/api")
class PlaceController (
    private val placeRepository: PlaceRepository,
    private val addressTRepository: AddressTRepository,
    private val categoryRepository: CategoryRepository
) {
    private val faker = Faker()

    // ✅ 저장된 장소 목록 조회 API 추가
    @GetMapping("/places")
    fun getAllPlaces(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int
    ): Page<PlaceEntity> {
        val pageable = PageRequest.of(page, size)
        return placeRepository.findAll(pageable)
    }

    @PostMapping("/savePlace")
    fun savePlace(@RequestBody request: PlaceRequest): ResponseEntity<String> {
        println("🔍 Received request: $request")
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
        val existingPlace = placeRepository.findByPlaceUrl(request.placeUrl)

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
            corkageInfolist = placeDesc
        )

        return if (existingPlace != null) {
            // 기존 데이터가 있는 경우 업데이트
            placeRepository.save(placeEntity)
            ResponseEntity.ok("해당 가게 정보를 업데이트 했습니다.")
        } else {
            // 새로운 데이터 추가
            placeRepository.save(placeEntity)
    
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
    
            ResponseEntity.ok("데이터가 성공적으로 저장되었습니다!")
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

    @GetMapping("/getplaceurl")
    fun getPlaceUrl(@RequestParam query: String): Map<String, Any> {
        val searchUrl = "https://m.search.naver.com/search.naver?query=$query"
    
        try {
            val randomUserAgent = faker.internet().userAgent()
            println("🕵️ 사용된 User-Agent: $randomUserAgent")
    
            sleep((1000..3000).random().toLong()) // 초기 대기 시간
    
            // 🔥 검색 결과 페이지를 한 번만 요청
            val document = Jsoup.connect(searchUrl)
                .userAgent(randomUserAgent)
                .referrer("http://www.naver.com")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Connection", "keep-alive")
                .timeout(10000)
                .get()
    
            // ✅ 검색 결과 로딩 대기 (최대 10초)
            val maxWaitTimeMs = 10000L
            val checkIntervalMs = 500L
            var elapsedTime = 0L
            var placeLinkElement: org.jsoup.nodes.Element? = null
    
            while (elapsedTime < maxWaitTimeMs) {
                placeLinkElement = document.selectFirst("div#_title a")
                if (placeLinkElement != null) {
                    println("✅ 검색 결과 페이지 로딩 완료!")
                    break
                }
                sleep(checkIntervalMs)
                elapsedTime += checkIntervalMs
                println("⏳ 검색 결과 페이지 로딩 대기 중... ($elapsedTime ms)")
            }
    
            if (placeLinkElement == null) {
                return mapOf("error" to "❌ 검색 결과 페이지 로딩에 실패했습니다.")
            }
    
            val placeLink = placeLinkElement.attr("href") ?: ""
            if (placeLink.isNotEmpty()) {
                val placeId = Regex("/restaurant/(\\d+)").find(placeLink)?.groupValues?.get(1)
    
                if (placeId != null) {
                    val finalUrl = "https://m.place.naver.com/restaurant/$placeId/information"
                    val c_randomUserAgent = faker.internet().userAgent()
                    println("🕵️ 사용된 User-Agent: $c_randomUserAgent")
    
                    sleep((1000..3000).random().toLong())
    
                    // 🔥 상세 페이지를 한 번만 요청
                    val infoDocument = Jsoup.connect(finalUrl)
                        .userAgent(c_randomUserAgent)
                        .referrer("http://www.naver.com")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .timeout(10000)
                        .get()
    
                    // ✅ 상세 페이지 로딩 대기 (최대 10초)
                    elapsedTime = 0L
                    var placeInfoLoaded = false
                    var placeDescLoaded = false
    
                    while (elapsedTime < maxWaitTimeMs) {
                        placeInfoLoaded = infoDocument.selectFirst("div.woHEA ul.JU0iX li.c7TR6 div") != null
                        placeDescLoaded = infoDocument.selectFirst("div.T8RFa.CEyr5") != null
                        
                        if (placeInfoLoaded && placeDescLoaded) {
                            println("✅ 상세 페이지 로딩 완료!")
                            break
                        }
    
                        sleep(checkIntervalMs)
                        elapsedTime += checkIntervalMs
                        println("⏳ 상세 페이지 로딩 대기 중... ($elapsedTime ms)")
                    }
    
                    if (!placeInfoLoaded || !placeDescLoaded) {
                        return mapOf("error" to "❌ 상세 페이지 로딩에 실패했습니다.")
                    }
    
                    val placeInfo = infoDocument.select("div.woHEA ul.JU0iX li.c7TR6 div, div.woHEA ul.JU0iX li.c7TR6 span")
                        .map { it.text().trim() }
                        .filter { it.isNotEmpty() }
    
                    val placeDesc = infoDocument.select("div.T8RFa.CEyr5")
                        .map { it.wholeText().trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString("\n")
    
                    println("✅ 가게 정보: $placeInfo")
                    println("📜 가게 설명: $placeDesc")
    
                    val corkageInfoList = TextUtils.extractCorkageInfo(placeDesc.trimIndent())
                    println("📜 콜키지 추가정보: $corkageInfoList")
    
                    return mapOf(
                        "placeUrl" to finalUrl,
                        "placeInfo" to placeInfo,
                        "placeDesc" to corkageInfoList
                    )
                } else {
                    return mapOf("error" to "❌ placeId를 찾을 수 없습니다.")
                }
            } else {
                return mapOf("error" to "❌ 가게 링크를 찾을 수 없습니다.")
            }
    
        } catch (e: Exception) {
            e.printStackTrace()
            return mapOf("error" to "JSoup 요청 실패: ${e.message}")
        }
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
