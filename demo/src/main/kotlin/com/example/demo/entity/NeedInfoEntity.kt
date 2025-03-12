package com.example.demo.entity

import jakarta.persistence.*

@Entity
@Table(name = "needinfo")
data class NeedInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val pid: Long? = null,
    val searchUrl: String?,
    val finalUrl: String?,

    val process: Int
)
{
    // 기본 생성자 추가
    constructor() : this(null, null, "", "", 0)
}