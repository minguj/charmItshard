package com.example.demo.entity

import jakarta.persistence.*

@Entity
@Table(name = "places")
data class PlaceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val title: String,
    val link: String?,
    val category: String?,
    val description: String?,
    val telephone: String?,
    val address: String?,
    val roadAddress: String?,
    val mapx: String?,
    val mapy: String?,
    
    val placeUrl: String?,
    
    val corkageAvailable: Boolean = false,
    val freeCorkage: Boolean = false,

    @Lob
    val placeInfo: String?,

    @Lob
    val corkageInfolist: String?, // 🔥 corkageInfolist 추가

    // ✅ 추가: 지하철 정보 저장
    val nearbySubways: String? // `(종각,1호선,337m),(종로3가,1호선,494m)` 형식
) {
    // JPA를 위한 기본 생성자
    constructor() : this(null, "", null, null, null, null, null, null, null, null, null, false, false, null, null, null)
}