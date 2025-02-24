package com.example.demo.repository

import com.example.demo.entity.PlaceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface PlaceRepository : JpaRepository<PlaceEntity, Long> {
    fun findByTitle(title: String): PlaceEntity?
}
