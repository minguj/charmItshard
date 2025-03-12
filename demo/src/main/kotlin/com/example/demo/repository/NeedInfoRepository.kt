package com.example.demo.repository

import com.example.demo.entity.NeedInfoEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NeedInfoRepository : JpaRepository<NeedInfoEntity, Long> {
    fun findByPid(pid: Long): NeedInfoEntity?
    fun findBySearchUrlAndFinalUrlAndProcess(searchUrl: String, finalUrl: String, process: Int): NeedInfoEntity?
}
