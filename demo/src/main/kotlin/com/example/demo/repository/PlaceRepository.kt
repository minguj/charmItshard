package com.example.demo.repository

import com.example.demo.entity.PlaceEntity
import com.example.demo.entity.AddressTEntity
import com.example.demo.entity.CategoryEntity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface PlaceRepository : JpaRepository<PlaceEntity, Long> {
    fun findByTitle(title: String): PlaceEntity?
    override fun findAll(pageable: Pageable): Page<PlaceEntity>
    fun findByPlaceUrl(placeUrl: String): PlaceEntity?
}

// AddressTRepository
@Repository
interface AddressTRepository : JpaRepository<AddressTEntity, Long> {
    fun existsByRegionAndSubregion(region: String, subregion: String): Boolean
}

// CategoryRepository
@Repository
interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun existsByName(name: String): Boolean
}