package com.app.quickfun.domain.phone

/**
 * Сборка и проверка номера в стиле E.164 (+ и только цифры после +).
 */
object PhoneE164 {
    fun digitsOnly(s: String): String = s.filter { it.isDigit() }

    /** [dialCodeDigits] без +, например "7"; [nationalDigits] — национальная часть без кода страны. */
    fun build(dialCodeDigits: String, nationalDigits: String): String? {
        val d = digitsOnly(dialCodeDigits)
        val n = digitsOnly(nationalDigits)
        if (d.isEmpty() || n.isEmpty()) return null
        return "+$d$n"
    }

    fun isPlausible(e164: String): Boolean {
        val s = e164.trim()
        if (!s.startsWith('+')) return false
        val digits = s.drop(1).filter { it.isDigit() }
        return digits.length in 8..15
    }

    /** Полный E.164, если национальная часть набрана полностью и номер правдоподобен. */
    fun validE164IfComplete(dialCodeDigits: String, nationalDigits: String, expectedNationalLen: Int): String? {
        val n = digitsOnly(nationalDigits).take(expectedNationalLen)
        if (n.length != expectedNationalLen) return null
        val built = build(dialCodeDigits, n) ?: return null
        return if (isPlausible(built)) built else null
    }
}
