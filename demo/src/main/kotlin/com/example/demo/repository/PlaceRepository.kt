package com.example.demo.repository

import com.example.demo.entity.PlaceEntity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface PlaceRepository : JpaRepository<PlaceEntity, Long> {
    fun findByTitle(title: String): PlaceEntity?
    override fun findAll(pageable: Pageable): Page<PlaceEntity>
    fun findByTitleAndAddress(title: String, address: String?): PlaceEntity?

    // ✅ 필터링 메서드 추가
    @Query("""
        SELECT p FROM PlaceEntity p
        WHERE p.placeUrl IS NOT NULL AND TRIM(p.placeUrl) <> ''
        AND (:category IS NULL OR p.category LIKE CONCAT('%', :category, '%'))
        AND (:city IS NULL OR p.address LIKE CONCAT('%', :city, '%'))
        AND (:district IS NULL OR p.address LIKE CONCAT('%', :district, '%'))
        AND (:searchTerm IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    fun findByFilters(
        @Param("category") category: String?,
        @Param("city") city: String?,
        @Param("district") district: String?,
        @Param("searchTerm") searchTerm: String?,
        pageable: Pageable
    ): Page<PlaceEntity>
}