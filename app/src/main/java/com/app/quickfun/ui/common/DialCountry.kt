package com.app.quickfun.ui.common

/**
 * Код страны для ввода телефона: только цифры после «+», ожидаемая длина национальной части (без кода).
 */
data class DialCountry(
    val iso: String,
    val nameRu: String,
    /** Цифры кода без «+», например 7 для РФ/КЗ */
    val dialDigits: String,
    val nationalDigits: Int
)

val quickFunDialCountries: List<DialCountry> = listOf(
    DialCountry("RU", "Россия", "7", 10),
    DialCountry("KZ", "Казахстан", "7", 10),
    DialCountry("UA", "Украина", "380", 9),
    DialCountry("BY", "Беларусь", "375", 9),
    DialCountry("UZ", "Узбекистан", "998", 9),
    DialCountry("AM", "Армения", "374", 8),
    DialCountry("AZ", "Азербайджан", "994", 9),
    DialCountry("GE", "Грузия", "995", 9),
    DialCountry("DE", "Германия", "49", 10),
    DialCountry("FR", "Франция", "33", 9),
    DialCountry("ES", "Испания", "34", 9),
    DialCountry("IT", "Италия", "39", 10),
    DialCountry("PL", "Польша", "48", 9),
    DialCountry("US", "США / Канада", "1", 10),
    DialCountry("GB", "Великобритания", "44", 10),
    DialCountry("TR", "Турция", "90", 10),
    DialCountry("IN", "Индия", "91", 10),
    DialCountry("CN", "Китай", "86", 11),
    DialCountry("JP", "Япония", "81", 10),
    DialCountry("KR", "Корея", "82", 9),
    DialCountry("IL", "Израиль", "972", 9),
    DialCountry("AE", "ОАЭ", "971", 9),
)

fun dialCountryByIso(iso: String): DialCountry =
    quickFunDialCountries.firstOrNull { it.iso.equals(iso, ignoreCase = true) }
        ?: quickFunDialCountries.first()

/**
 * Разбор сохранённого E.164 в страну из списка и национальную часть (длина может не совпасть с ожидаемой — пользователь допишет).
 */
fun parseStoredE164ToDraft(e164: String?): Pair<DialCountry, String> {
    val raw = e164?.trim().orEmpty()
    if (raw.isEmpty()) return dialCountryByIso("RU") to ""
    val withPlus = if (raw.startsWith('+')) raw else "+$raw"
    val sorted = quickFunDialCountries.sortedByDescending { it.dialDigits.length }
    for (c in sorted) {
        val prefix = "+${c.dialDigits}"
        if (withPlus.startsWith(prefix)) {
            val national = withPlus.removePrefix(prefix).filter { it.isDigit() }
            return c to national
        }
    }
    return dialCountryByIso("RU") to ""
}
