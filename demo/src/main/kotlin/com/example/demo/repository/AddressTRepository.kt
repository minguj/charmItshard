package com.example.demo.repository

import com.example.demo.entity.AddressTEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// AddressTRepository
@Repository
interface AddressTRepository : JpaRepository<AddressTEntity, Long> {
    fun existsByRegionAndSubregion(region: String, subregion: String): Boolean
}