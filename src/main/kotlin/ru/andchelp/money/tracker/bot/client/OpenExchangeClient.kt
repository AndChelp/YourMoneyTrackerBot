package ru.andchelp.money.tracker.bot.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "rateClient", url = "https://openexchangerates.org/api/latest.json")
interface OpenExchangeClient {
    @GetMapping
    fun getExchangeRate(@RequestParam("app_id") appId: String, @RequestParam symbols: String): Rates
}

data class Rates(
    val rates: Map<String, Double>? = null
)