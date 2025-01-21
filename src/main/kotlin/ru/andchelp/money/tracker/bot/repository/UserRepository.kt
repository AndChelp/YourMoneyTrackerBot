package ru.andchelp.money.tracker.bot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.andchelp.money.tracker.bot.model.User

@Repository
interface UserRepository : JpaRepository<User, Long>