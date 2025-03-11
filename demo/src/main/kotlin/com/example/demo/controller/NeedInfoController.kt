package com.example.demo.controller

import com.example.demo.entity.NeedInfoEntity
import org.springframework.http.ResponseEntity
import com.example.demo.repository.NeedInfoRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@RestController
@RequestMapping("/api")
class NeedINfoController(
    private val needInfoRepository: NeedInfoRepository
) {
    @PostMapping("/saveFailedUrl")
    fun saveFailedUrl(@RequestBody request: FailedUrlRequest): ResponseEntity<String> {

        val existingEntity = needInfoRepository.findBySearchUrlAndFinalUrl(request.searchUrl ?: "", request.finalUrl ?: "")
        if (existingEntity != null) {
            return ResponseEntity.ok("이미 동일한 URL이 정보수집 요청 되어 있습니다.")
        }

        // pid로 기존 엔티티 조회
        var existingPidEntity = needInfoRepository.findByPid(request.pid)

        return if (existingPidEntity != null) {
            // 기존 엔티티가 존재하면 새로 업데이트된 엔티티를 만들어서 저장
            val updatedEntity = NeedInfoEntity(
                id = existingPidEntity.id,
                searchUrl = request.searchUrl ?: existingPidEntity.searchUrl,
                finalUrl = request.finalUrl ?: existingPidEntity.finalUrl,
                pid = request.pid,
                process = existingPidEntity.process // 기존 값 유지
            )
            // 새로운 엔티티 저장
            needInfoRepository.save(updatedEntity)

            ResponseEntity.ok("정보 수집에 필요한 URL이 업데이트되었습니다.")
        } else {
            // 새로 저장하는 경우
            needInfoRepository.save(
                NeedInfoEntity(
                    searchUrl = request.searchUrl,
                    finalUrl = request.finalUrl,
                    pid = request.pid,
                    process = false
                )
            )
            ResponseEntity.ok("정보 수집이 필요한 상호가 등록 되었습니다. 곧 서버에서 작업을 진행합니다.")
        }       

    }
}

data class FailedUrlRequest(
    val searchUrl: String?,
    val finalUrl: String?,
    val pid: Long,
)