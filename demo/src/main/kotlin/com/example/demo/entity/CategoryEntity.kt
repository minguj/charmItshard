package com.example.demo.entity

import jakarta.persistence.*

@Entity
@Table(name = "category")
data class CategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val name: String
)
