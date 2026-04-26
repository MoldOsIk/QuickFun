package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.VenueSeatLayoutConfig
import com.app.quickfun.domain.repository.BookingRepository

class GenerateVenueSeatLayoutUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String, config: VenueSeatLayoutConfig) {
        val existing = bookingRepository.listSeats(placeId)
        if (existing.isNotEmpty()) {
            throw IllegalStateException(
                "У заведения уже есть места в базе. Удалите их в Supabase или очистите таблицу seats " +
                    "для этого place_id, затем сгенерируйте схему заново."
            )
        }
        val batch: List<Triple<Int, Int, String?>> = when (config) {
            is VenueSeatLayoutConfig.BowlingLanes -> {
                require(config.count in 1..40) { "Число дорожек: от 1 до 40." }
                (1..config.count).map { i ->
                    Triple(0, i, "Дорожка $i")
                }
            }
            is VenueSeatLayoutConfig.BilliardTables -> {
                require(config.count in 1..40) { "Число столов: от 1 до 40." }
                (1..config.count).map { i ->
                    Triple(0, i, "Стол $i")
                }
            }
            is VenueSeatLayoutConfig.CinemaGrid -> {
                require(config.rows in 1..40 && config.seatsPerRow in 1..80) {
                    "Зал: от 1 до 40 рядов, от 1 до 80 мест в ряду."
                }
                buildList {
                    for (r in 1..config.rows) {
                        for (s in 1..config.seatsPerRow) {
                            add(Triple(r, s, "Ряд $r · место $s"))
                        }
                    }
                }
            }
            is VenueSeatLayoutConfig.KaraokeRooms -> {
                require(config.rooms in 1..20) { "Число комнат: от 1 до 20." }
                if (config.rooms == 1) {
                    listOf(Triple(0, 1, "Караоке"))
                } else {
                    (1..config.rooms).map { i ->
                        Triple(0, i, "Комната $i")
                    }
                }
            }
        }
        bookingRepository.insertSeatsBatch(placeId, batch)
    }
}
