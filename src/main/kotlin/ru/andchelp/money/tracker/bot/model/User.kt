package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: Long? = null,
    @ManyToOne
    val globalCurrency: Currency? = null,
)

@Repository
interface UserRepository : JpaRepository<User, Long>