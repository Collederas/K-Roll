package com.collederas.kroll.support.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * A dummy controller acting as a "probe" for the Client Security Chain.
 * It is used to verify that /client/ endpoints are correctly protected by API Keys.
*/

@RestController
@RequestMapping("/client")
class ClientRouteTestController {
    @GetMapping("/ping")
    fun ping() = "pong"
}
