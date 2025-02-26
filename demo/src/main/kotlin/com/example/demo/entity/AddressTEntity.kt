package com.example.demo.entity

import jakarta.persistence.*

@Entity
@Table(name = "addressT")
data class AddressTEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val region: String,
    val subregion: String
)