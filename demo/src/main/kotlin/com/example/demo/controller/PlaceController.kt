package com.example.demo.controller

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
        val isCorkageAvailable = request.placeInfo.contains("콜키지 가능")
        val isFreeCorkage = request.placeInfo.contains("무료")

        val cleanTitle = Jsoup.parse(request.place.title).text()

        val placeEntity = PlaceEntity(
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
            placeInfo = request.placeInfo.joinToString(",") // 리스트를 문자열로 변환
        )

        val existingPlace = placeRepository.findByPlaceUrl(request.placeUrl)
        return if (existingPlace != null) {
            // 기존 데이터가 있는 경우 업데이트
            //placeEntity.placeUrl = existingPlace.placeUrl
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
            // 🔀 완전히 랜덤한 User-Agent 생성
            val randomUserAgent = faker.internet().userAgent()
            println("🕵️ 사용된 User-Agent: $randomUserAgent")

            // 🕒 요청 간격 조절 (랜덤 지연: 1 ~ 3초)
            sleep((1000..3000).random().toLong())

            // 🌐 Jsoup으로 페이지 요청 (랜덤 User-Agent)
            val document = Jsoup.connect(searchUrl)
                .userAgent(randomUserAgent)
                .referrer("http://www.naver.com")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Connection", "keep-alive")
                .timeout(10000)
                .get()

            // 🔗 검색 결과에서 링크 추출
            val placeLinkElement = document.selectFirst("div#_title a")
            val placeLink = placeLinkElement?.attr("href") ?: ""

            if (placeLink.isNotEmpty()) {
                val placeId = Regex("/restaurant/(\\d+)").find(placeLink)?.groupValues?.get(1)

                if (placeId != null) {
                    val finalUrl = "https://m.place.naver.com/restaurant/$placeId/information"
                    val c_randomUserAgent = faker.internet().userAgent()
                    println("🕵️ 사용된 User-Agent: $c_randomUserAgent")

                    // 🕒 요청 간격 조절 (랜덤 지연: 1 ~ 3초)
                    sleep((1000..3000).random().toLong())

                    val infoDocument = Jsoup.connect(finalUrl)
                        .userAgent(c_randomUserAgent)
                        .referrer("http://www.naver.com")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Connection", "keep-alive")
                        .timeout(10000)
                        .get()

                    // 🧾 가게 정보 추출
                    val placeInfo = infoDocument.select("div.woHEA ul.JU0iX li.c7TR6 div, div.woHEA ul.JU0iX li.c7TR6 span")
                        .map { it.text().trim() }
                        .filter { it.isNotEmpty() }

                    println("✅ 가게 정보: $placeInfo")

                    return mapOf("placeUrl" to finalUrl, "placeInfo" to placeInfo)
                } else {
                    return mapOf("error" to "❌ placeId를 찾을 수 없습니다..")
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
    val placeUrl: String
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
