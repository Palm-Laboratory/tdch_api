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
@Table(name = "media_collection")
class MediaCollection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "collection_key", nullable = false, unique = true, length = 64)
    var collectionKey: String,
    @Column(nullable = false, length = 120)
    var title: String,
    @Column(columnDefinition = "text")
    var description: String? = null,
    @Column(name = "default_path", nullable = false, unique = true, length = 255)
    var defaultPath: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "content_kind", nullable = false, length = 20)
    var contentKind: ContentKind,
    @Column(nullable = false)
    var active: Boolean = true,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
