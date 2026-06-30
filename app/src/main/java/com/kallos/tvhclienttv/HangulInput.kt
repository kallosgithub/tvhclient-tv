package com.kallos.tvhclienttv

private val hangulInitials = listOf(
    "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ",
    "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ",
)

private val hangulVowels = listOf(
    "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ",
    "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ",
)

private val hangulFinals = mapOf(
    "ㄱ" to 1,
    "ㄲ" to 2,
    "ㄳ" to 3,
    "ㄴ" to 4,
    "ㄵ" to 5,
    "ㄶ" to 6,
    "ㄷ" to 7,
    "ㄹ" to 8,
    "ㄺ" to 9,
    "ㄻ" to 10,
    "ㄼ" to 11,
    "ㄽ" to 12,
    "ㄾ" to 13,
    "ㄿ" to 14,
    "ㅀ" to 15,
    "ㅁ" to 16,
    "ㅂ" to 17,
    "ㅄ" to 18,
    "ㅅ" to 19,
    "ㅆ" to 20,
    "ㅇ" to 21,
    "ㅈ" to 22,
    "ㅊ" to 23,
    "ㅋ" to 24,
    "ㅌ" to 25,
    "ㅍ" to 26,
    "ㅎ" to 27,
)

private val finalToInitial = mapOf(
    1 to "ㄱ",
    2 to "ㄲ",
    4 to "ㄴ",
    7 to "ㄷ",
    8 to "ㄹ",
    16 to "ㅁ",
    17 to "ㅂ",
    19 to "ㅅ",
    20 to "ㅆ",
    21 to "ㅇ",
    22 to "ㅈ",
    23 to "ㅊ",
    24 to "ㅋ",
    25 to "ㅌ",
    26 to "ㅍ",
    27 to "ㅎ",
)

fun appendHangulInput(current: String, key: String): String {
    if (key.isEmpty()) return current
    if (current.isEmpty()) return key

    val last = current.last().toString()
    val consonantIndex = hangulInitials.indexOf(key)
    val vowelIndex = hangulVowels.indexOf(key)

    if (vowelIndex >= 0) {
        val lastInitialIndex = hangulInitials.indexOf(last)

        if (lastInitialIndex >= 0) {
            return current.dropLast(1) + composeHangul(lastInitialIndex, vowelIndex, 0)
        }

        val code = current.last().code

        if (code in 0xAC00..0xD7A3) {
            val offset = code - 0xAC00
            val initial = offset / (21 * 28)
            val medial = (offset % (21 * 28)) / 28
            val final = offset % 28

            if (final > 0) {
                val nextInitial = finalToInitial[final]

                if (nextInitial != null) {
                    val nextInitialIndex = hangulInitials.indexOf(nextInitial)

                    return current.dropLast(1) +
                        composeHangul(initial, medial, 0) +
                        composeHangul(nextInitialIndex, vowelIndex, 0)
                }
            }
        }

        return current + key
    }

    if (consonantIndex >= 0) {
        val code = current.last().code

        if (code in 0xAC00..0xD7A3) {
            val offset = code - 0xAC00
            val initial = offset / (21 * 28)
            val medial = (offset % (21 * 28)) / 28
            val final = offset % 28
            val finalIndex = hangulFinals[key]

            if (final == 0 && finalIndex != null) {
                return current.dropLast(1) + composeHangul(initial, medial, finalIndex)
            }
        }
    }

    return current + key
}

private fun composeHangul(
    initial: Int,
    medial: Int,
    final: Int,
): String {
    return (0xAC00 + ((initial * 21) + medial) * 28 + final).toChar().toString()
}
