package com.example.demo.controller

import com.example.demo.entity.CategoryEntity
import com.example.demo.repository.CategoryRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class CategoryController(
    private val categoryRepository: CategoryRepository
) {

    @GetMapping("/getcategory")
    fun getAllCategories(): List<CategoryEntity> {
        return categoryRepository.findAll()
    }
}
