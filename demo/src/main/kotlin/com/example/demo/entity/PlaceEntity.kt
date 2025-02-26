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
    val placeInfo: String?
) {
    // JPA를 위한 기본 생성자
    constructor() : this(null, "", null, null, null, null, null, null, null, null, null, false, false, null)
}