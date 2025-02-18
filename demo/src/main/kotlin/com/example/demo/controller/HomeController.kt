package com.example.demo.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")  // 기본 URL 지정
class HomeController {

    @GetMapping
    fun home(): String {
        return "Hello, Spring Boot!"
    }
}
