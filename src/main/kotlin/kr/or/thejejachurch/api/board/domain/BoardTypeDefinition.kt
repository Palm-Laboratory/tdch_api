package kr.or.thejejachurch.api.board.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "board_type")
class BoardTypeDefinition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true, length = 32)
    var key: String,
    @Column(nullable = false, length = 100)
    var label: String,
    @Column
    var description: String? = null,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
)
