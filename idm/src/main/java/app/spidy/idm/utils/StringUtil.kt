package app.spidy.idm.utils

import java.text.Normalizer
import java.util.*

object StringUtil {
    private val seeds = arrayListOf(
        "a", "b", "c", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
        "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    )

    fun slugify(word: String, replacement: String = "-") = Normalizer
        .normalize(word, Normalizer.Form.NFD)
        .replace("[^\\p{ASCII}]".toRegex(), "")
        .replace("[^a-zA-Z0-9\\s]+".toRegex(), "").trim()
        .replace("\\s+".toRegex(), replacement)
        .toLowerCase(Locale.ROOT)

    fun randomUUID(): String {
        var s = ""
        for (i in 0..4) {
            s += seeds.random()
        }
        return s
    }
}