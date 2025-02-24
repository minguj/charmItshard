package com.example.demo.controller

import com.example.demo.entity.PlaceEntity
import com.example.demo.repository.PlaceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder

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
    private val placeRepository: PlaceRepository
) {
    private val faker = Faker()

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

        placeRepository.save(placeEntity)
        return ResponseEntity.ok("데이터가 성공적으로 저장되었습니다!")
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

            // 🌐 Jsoup으로 페이지 요청 (랜덤 User-Agent)
            val document = Jsoup.connect(searchUrl)
                .userAgent(randomUserAgent)
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
                        .timeout(10000)
                        .get()

                    // 🧾 가게 정보 추출
                    val placeInfo = infoDocument.select("div.woHEA ul.JU0iX li.c7TR6 div, div.woHEA ul.JU0iX li.c7TR6 span")
                        .map { it.text().trim() }
                        .filter { it.isNotEmpty() }

                    println("✅ 가게 정보: $placeInfo")

                    return mapOf("placeUrl" to finalUrl, "placeInfo" to placeInfo)
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
