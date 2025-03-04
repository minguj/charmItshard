package com.example.demo.entity

import jakarta.persistence.*

@Entity
@Table(name = "category")
data class CategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String
) {
    // 기본 생성자 추가
    constructor() : this(null, "")
}
