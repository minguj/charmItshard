package com.example.demo.controller

import com.example.demo.entity.AddressTEntity
import com.example.demo.repository.AddressTRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AddressController(
    private val addressTRepository: AddressTRepository
) {

    @GetMapping("/getaddress")
    fun getAllAddresses(): List<AddressTEntity> {
        return addressTRepository.findAll()
    }
}