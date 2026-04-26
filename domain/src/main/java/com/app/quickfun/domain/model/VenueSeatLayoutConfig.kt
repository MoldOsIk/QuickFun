package com.app.quickfun.domain.model

/** Пакетная генерация строк в [public.seats] под тип заведения. */
sealed class VenueSeatLayoutConfig {
    data class BowlingLanes(val count: Int) : VenueSeatLayoutConfig()
    data class BilliardTables(val count: Int) : VenueSeatLayoutConfig()
    data class CinemaGrid(val rows: Int, val seatsPerRow: Int) : VenueSeatLayoutConfig()
    data class KaraokeRooms(val rooms: Int) : VenueSeatLayoutConfig()
}
