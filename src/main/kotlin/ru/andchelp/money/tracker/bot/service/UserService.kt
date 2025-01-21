package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import ru.andchelp.money.tracker.bot.model.User
import ru.andchelp.money.tracker.bot.repository.UserRepository

@Service
class UserService(private val userRepository: UserRepository) {

    fun save(user: User) {
        userRepository.save(user)
    }

    fun findById(id: Long): User {
        return userRepository.findById(id).orElseThrow()
    }

}