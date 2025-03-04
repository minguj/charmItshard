package com.example.demo.repository

import com.example.demo.entity.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// CategoryRepository
@Repository
interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun existsByName(name: String): Boolean
}