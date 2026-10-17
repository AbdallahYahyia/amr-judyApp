package com.amrjudy.kids

import kotlin.random.Random

/**
 * Small kid-friendly chess engine. Board index = row * 8 + col, row 0 is the top (Black's back rank).
 * Uppercase = White, lowercase = Black. Castling and en-passant are left out on purpose (simpler for kids);
 * pawns that reach the last row become queens.
 */
class Chess(val b: CharArray = START.toCharArray(), val whiteTurn: Boolean = true) {

    companion object {
        const val START =
            "rnbqkbnr" +
            "pppppppp" +
            "........" +
            "........" +
            "........" +
            "........" +
            "PPPPPPPP" +
            "RNBQKBNR"

        val KNIGHT = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        val KING = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
        val DIAG = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        val ORTHO = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

        fun isWhite(c: Char) = c in 'A'..'Z'
        fun isBlack(c: Char) = c in 'a'..'z'

        fun symbol(c: Char): String = when (c) {
            'K' -> "♔"; 'Q' -> "♕"; 'R' -> "♖"; 'B' -> "♗"; 'N' -> "♘"; 'P' -> "♙"
            'k' -> "♚"; 'q' -> "♛"; 'r' -> "♜"; 'b' -> "♝"; 'n' -> "♞"; 'p' -> "♟"
            else -> ""
        }

        fun value(c: Char): Int = when (c.lowercaseChar()) {
            'p' -> 100; 'n' -> 320; 'b' -> 330; 'r' -> 500; 'q' -> 900; 'k' -> 20000; else -> 0
        }
    }

    private fun inside(r: Int, c: Int) = r in 0..7 && c in 0..7

    /** Squares the piece on [from] attacks (pawns: diagonals only). */
    fun attacksFrom(from: Int): List<Int> {
        val p = b[from]
        if (p == '.') return emptyList()
        val r = from / 8; val c = from % 8
        val white = isWhite(p)
        val out = ArrayList<Int>()
        when (p.lowercaseChar()) {
            'p' -> {
                val dr = if (white) -1 else 1
                for (dc in intArrayOf(-1, 1)) if (inside(r + dr, c + dc)) out.add((r + dr) * 8 + c + dc)
            }
            'n' -> for ((dr, dc) in KNIGHT) if (inside(r + dr, c + dc)) out.add((r + dr) * 8 + c + dc)
            'k' -> for ((dr, dc) in KING) if (inside(r + dr, c + dc)) out.add((r + dr) * 8 + c + dc)
            else -> {
                val dirs = when (p.lowercaseChar()) {
                    'b' -> DIAG; 'r' -> ORTHO; else -> DIAG + ORTHO
                }
                for ((dr, dc) in dirs) {
                    var rr = r + dr; var cc = c + dc
                    while (inside(rr, cc)) {
                        out.add(rr * 8 + cc)
                        if (b[rr * 8 + cc] != '.') break
                        rr += dr; cc += dc
                    }
                }
            }
        }
        return out
    }

    private fun pseudoMoves(from: Int): List<Int> {
        val p = b[from]
        if (p == '.') return emptyList()
        val white = isWhite(p)
        val r = from / 8; val c = from % 8
        val out = ArrayList<Int>()
        if (p.lowercaseChar() == 'p') {
            val dr = if (white) -1 else 1
            val startRow = if (white) 6 else 1
            if (inside(r + dr, c) && b[(r + dr) * 8 + c] == '.') {
                out.add((r + dr) * 8 + c)
                if (r == startRow && b[(r + 2 * dr) * 8 + c] == '.') out.add((r + 2 * dr) * 8 + c)
            }
            for (sq in attacksFrom(from)) {
                val t = b[sq]
                if (t != '.' && isWhite(t) != white) out.add(sq)
            }
        } else {
            for (sq in attacksFrom(from)) {
                val t = b[sq]
                if (t == '.' || isWhite(t) != white) out.add(sq)
            }
        }
        return out
    }

    fun kingSquare(white: Boolean): Int = b.indexOf(if (white) 'K' else 'k')

    fun isAttacked(sq: Int, byWhite: Boolean): Boolean {
        for (i in 0 until 64) {
            val p = b[i]
            if (p != '.' && isWhite(p) == byWhite && attacksFrom(i).contains(sq)) return true
        }
        return false
    }

    fun inCheck(white: Boolean): Boolean {
        val k = kingSquare(white)
        return k >= 0 && isAttacked(k, !white)
    }

    fun legalMoves(from: Int): List<Int> {
        val p = b[from]
        if (p == '.') return emptyList()
        val white = isWhite(p)
        return pseudoMoves(from).filter { to -> !make(from, to).inCheck(white) }
    }

    fun allLegal(white: Boolean): List<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>()
        for (i in 0 until 64) {
            val p = b[i]
            if (p != '.' && isWhite(p) == white) for (t in legalMoves(i)) list.add(i to t)
        }
        return list
    }

    /** Returns a new position after moving; turn passes to the other side. */
    fun make(from: Int, to: Int): Chess {
        val nb = b.copyOf()
        var p = nb[from]
        nb[from] = '.'
        if (p == 'P' && to / 8 == 0) p = 'Q'
        if (p == 'p' && to / 8 == 7) p = 'q'
        nb[to] = p
        return Chess(nb, !whiteTurn)
    }

    fun isCheckmate(): Boolean = inCheck(whiteTurn) && allLegal(whiteTurn).isEmpty()
    fun isStalemate(): Boolean = !inCheck(whiteTurn) && allLegal(whiteTurn).isEmpty()

    private fun material(forWhite: Boolean): Int {
        var s = 0
        for (c in b) if (c != '.') s += if (isWhite(c) == forWhite) value(c) else -value(c)
        return s
    }

    /**
     * Easy AI: level 0 = mostly random (picks captures sometimes),
     * level 1 = looks one move ahead (avoids hanging pieces, takes free pieces, plays mate-in-1).
     */
    fun aiMove(level: Int = 0): Pair<Int, Int>? {
        val me = whiteTurn
        val moves = allLegal(me)
        if (moves.isEmpty()) return null
        // always take a mate in one
        for (m in moves) if (make(m.first, m.second).isCheckmate()) return m
        if (level == 0) {
            val caps = moves.filter { b[it.second] != '.' }
            return if (caps.isNotEmpty() && Random.nextInt(100) < 40) caps.random() else moves.random()
        }
        var best = Int.MIN_VALUE
        val bestList = ArrayList<Pair<Int, Int>>()
        for (m in moves) {
            val after = make(m.first, m.second)
            var worst = Int.MAX_VALUE
            val replies = after.allLegal(!me)
            if (replies.isEmpty()) {
                worst = if (after.inCheck(!me)) 100000 else 0
            } else {
                for (r in replies) {
                    val s = after.make(r.first, r.second).material(me)
                    if (s < worst) worst = s
                }
            }
            if (worst > best) { best = worst; bestList.clear(); bestList.add(m) }
            else if (worst == best) bestList.add(m)
        }
        return bestList.random()
    }
}

/** A mini puzzle: 64-char board string (row 0 on top). White to move. */
data class Puzzle(
    val title: String,
    val hint: String,
    val board: String,
    val star: Int? = null   // if set, the goal is to capture the piece on this square
)

val PUZZLES = listOf(
    Puzzle(
        "كش مات في نقلة", "حرّك الطابية وادي كش مات!",
        "......k." + ".....ppp" + "........" + "........" + "........" + "........" + "........" + "R.....K."
    ),
    Puzzle(
        "كش مات بالوزير", "الوزير يروح جنب الملك!",
        ".......k" + "Q......." + "......K." + "........" + "........" + "........" + "........" + "........"
    ),
    Puzzle(
        "هات النجمة ⭐", "الطابية تاكل النجمة!",
        "........" + "........" + "........" + "...p...." + "........" + "........" + "........" + "...R...K",
        star = 3 * 8 + 3
    ),
    Puzzle(
        "هات النجمة ⭐ بالحصان", "الحصان يقفز على النجمة!",
        "........" + "........" + "....p..." + "........" + "...N...." + "........" + "........" + "K.......",
        star = 2 * 8 + 4
    )
)
