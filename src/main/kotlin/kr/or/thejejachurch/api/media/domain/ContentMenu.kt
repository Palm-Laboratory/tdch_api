package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "content_menu")
class ContentMenu(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "site_key", nullable = false, unique = true, length = 64)
    var siteKey: String,
    @Column(name = "menu_name", nullable = false, length = 100)
    var menuName: String,
    @Column(nullable = false, unique = true, length = 120)
    var slug: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "content_kind", nullable = false, length = 20)
    var contentKind: ContentKind,
    @Column(nullable = false)
    var active: Boolean = true,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
