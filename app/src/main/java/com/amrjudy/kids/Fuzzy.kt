package com.amrjudy.kids

object Fuzzy {
    /** Normalise Arabic text so small spelling/diacritic differences don't matter. */
    fun normalize(s: String): String {
        val sb = StringBuilder()
        for (c in s.trim().lowercase()) {
            when (c) {
                in 'ً'..'ْ', 'ـ' -> {}          // diacritics, tatweel
                'أ', 'إ', 'آ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    fun similarity(a: String, b: String): Double {
        val x = normalize(a); val y = normalize(b)
        val m = maxOf(x.length, y.length)
        if (m == 0) return 1.0
        return 1.0 - levenshtein(x, y).toDouble() / m
    }

    /**
     * Very forgiving for early speech: any heard phrase (or any word in it) that is
     * similar enough, or shares the first sound, counts as an attempt.
     * Returns 0 = no attempt, 1 = partial (1 star), 2 = close (2 stars), 3 = great (3 stars).
     */
    fun score(target: String, heard: List<String>): Int {
        var best = 0.0
        val t = normalize(target)
        for (h in heard) {
            val parts = normalize(h).split(" ").filter { it.isNotBlank() } + normalize(h)
            for (p in parts) {
                var s = similarity(t, p)
                if (p.isNotEmpty() && t.isNotEmpty() && p[0] == t[0]) s = maxOf(s, 0.45)
                if (s > best) best = s
            }
        }
        return when {
            best >= 0.85 -> 3
            best >= 0.6 -> 2
            best >= 0.4 -> 1
            else -> 0
        }
    }
}
