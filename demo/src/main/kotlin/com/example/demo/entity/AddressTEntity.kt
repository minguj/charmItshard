package com.example.demo.entity

import jakarta.persistence.*

@Entity
@Table(name = "addressT")
data class AddressTEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val region: String,
    val subregion: String
) {
    // 기본 생성자 추가
    constructor() : this(null, "", "")
}