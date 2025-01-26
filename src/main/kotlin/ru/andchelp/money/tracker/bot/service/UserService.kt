package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import ru.andchelp.money.tracker.bot.model.User
import ru.andchelp.money.tracker.bot.repository.UserRepository

@Service
class UserService(private val userRepository: UserRepository, private val currencyService: CurrencyService) {

    fun save(userId: Long, globalCurrencyCode: String): User {
        return userRepository.save(
            User(
                id = userId,
                globalCurrency = currencyService.findByCode(globalCurrencyCode)
            )
        )
    }

    fun findById(id: Long): User {
        return userRepository.findById(id).orElseThrow()
    }

    fun userExists(id: Long): Boolean {
        return userRepository.existsById(id)
    }

}