package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "media_video_meta")
class MediaVideoMeta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "media_video_id", nullable = false, unique = true)
    var mediaVideoId: Long,
    @Column(length = 120)
    var preacher: String? = null,
    @Column(name = "scripture_ref", columnDefinition = "text")
    var scriptureRef: String? = null,
    @Column(name = "scripture_text", columnDefinition = "text")
    var scriptureText: String? = null,
    @Column(name = "service_type", length = 100)
    var serviceType: String? = null,
    @Column(columnDefinition = "text")
    var summary: String? = null,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    var tags: Array<String> = emptyArray(),
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
