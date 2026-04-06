package kr.or.thejejachurch.api.navigation.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "site_navigation_set")
class SiteNavigationSet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "set_key", nullable = false, unique = true, length = 50)
    val setKey: String,
    @Column(nullable = false, length = 100)
    val label: String,
    @Column(length = 255)
    val description: String? = null,
    @Column(nullable = false)
    val active: Boolean = true,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
