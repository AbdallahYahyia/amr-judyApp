package com.amrjudy.kids

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val filled = mapOf(
    'k' to "♚", 'q' to "♛", 'r' to "♜", 'b' to "♝", 'n' to "♞", 'p' to "♟"
)

private class PieceInfo(val code: Char, val nameAr: String, val say: String, val color: Color)

private val PIECES = listOf(
    PieceInfo('p', "البيدق", "أنا البيدق، بمشي خطوة خطوة", Mint),
    PieceInfo('n', "الحصان", "أنا الحصان، بنط بنط", Peach),
    PieceInfo('r', "القلعة", "أنا القلعة، بجري مستقيم", Sky),
    PieceInfo('b', "الفيل", "أنا الفيل، بمشي بالعرض", Lilac),
    PieceInfo('q', "الوزيرة", "أنا الوزيرة، أنا أقوى واحدة", Pink),
    PieceInfo('k', "الملك", "أنا الملك، لازم تحميني", Sun)
)

/** The 8x8 board. Targets glow; the star square shows a star. */
@Composable
fun BoardView(
    chess: Chess,
    selected: Int?,
    targets: Set<Int>,
    starSq: Int?,
    onTap: (Int) -> Unit
) {
    val t = rememberInfiniteTransition(label = "glow")
    val glow by t.animateFloat(0.25f, 0.9f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "g")
    Column(
        Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp)).border(4.dp, Ink, RoundedCornerShape(16.dp))
    ) {
        for (r in 0 until 8) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 8) {
                    val i = r * 8 + c
                    val light = (r + c) % 2 == 0
                    val base = if (light) Color(0xFFFFF3C4) else Color(0xFF9ED8A8)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(if (selected == i) Color(0xFFFFD43B) else base)
                            .clickable { onTap(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (i in targets) {
                            Box(Modifier.fillMaxSize().background(Color(0xFFFF6B6B).copy(alpha = glow * 0.55f)))
                            Text("●", fontSize = 18.sp, color = Color(0xFFE03131).copy(alpha = glow))
                        }
                        if (starSq == i && chess.b[i] != '.') {
                            Text("⭐", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd))
                        }
                        val p = chess.b[i]
                        if (p != '.') {
                            Text(
                                filled[p.lowercaseChar()] ?: "",
                                fontSize = 30.sp,
                                color = if (Chess.isWhite(p)) Color(0xFFFF7043) else Ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChessMenu(vm: AppViewModel) {
    val judy = vm.profile == Profile.JUDY
    var mode by remember { mutableStateOf("menu") }

    // Back inside chess goes to chess menu first
    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar(if (judy) "أكاديمية الشطرنج" else "أبطال الشطرنج", vm.stars) {
            if (mode == "menu") vm.back() else mode = "menu"
        }
        when {
            mode == "menu" && !judy -> ChessAmr(vm)
            mode == "menu" -> Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BigCard("📖", "تعلّم الحركات", Mint, Modifier.fillMaxWidth().weight(1f)) { mode = "learn" }
                BigCard("🧩", "ألغاز الشطرنج", Sky, Modifier.fillMaxWidth().weight(1f)) { mode = "puzzle" }
                BigCard("🤖", "العب ضد الروبوت", Peach, Modifier.fillMaxWidth().weight(1f)) { mode = "ai" }
                BigCard("👧👦", "جودي ضد عمرو", Pink, Modifier.fillMaxWidth().weight(1f)) { mode = "2p" }
            }
            mode == "learn" -> ChessLearn(vm)
            mode == "puzzle" -> ChessPuzzles(vm)
            mode == "ai" -> ChessPlay(vm, vsAi = true)
            else -> ChessPlay(vm, vsAi = false)
        }
    }
}

/* ---------- Mode A: Amr (age 3) ---------- */

@Composable
private fun ChessAmr(vm: AppViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigCard("👋", "", if (tab == 0) Mint else Color(0xFFE9ECEF), Modifier.weight(1f).height(72.dp), 30.sp) { tab = 0 }
            BigCard("❓", "", if (tab == 1) Sky else Color(0xFFE9ECEF), Modifier.weight(1f).height(72.dp), 30.sp) { tab = 1 }
            BigCard("🍎", "", if (tab == 2) Peach else Color(0xFFE9ECEF), Modifier.weight(1f).height(72.dp), 30.sp) { tab = 2 }
        }
        Spacer(Modifier.height(10.dp))
        when (tab) {
            0 -> AmrMeetPieces(vm)
            1 -> AmrFindPiece(vm)
            else -> AmrKnightJump(vm)
        }
    }
}

@Composable
private fun AmrMeetPieces(vm: AppViewModel) {
    var active by remember { mutableStateOf<PieceInfo?>(null) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        active?.let {
            Bouncy(Modifier.fillMaxWidth().weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(filled[it.code] ?: "", fontSize = 120.sp, color = Ink)
                    Text(it.nameAr, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                }
            }
        } ?: Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text("اضغط على البطل 👇", fontSize = 26.sp, color = Ink)
        }
        for (row in PIECES.chunked(3)) {
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in row) {
                    BigCard(filled[p.code] ?: "", p.nameAr, p.color, Modifier.weight(1f).fillMaxHeight(), 40.sp) {
                        active = p
                        vm.speech.speak(p.say, pitch = 1.5f)
                        vm.addStars(1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmrFindPiece(vm: AppViewModel) {
    var target by remember { mutableStateOf(PIECES.random()) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(target) { vm.speech.speak("فين ${target.nameAr}؟", pitch = 1.3f) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "فين ${target.nameAr}؟", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )
            for (row in PIECES.chunked(2)) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (p in row) {
                        BigCard(filled[p.code] ?: "", "", p.color, Modifier.weight(1f).fillMaxHeight(), 70.sp) {
                            if (p === target) {
                                vm.addStars(1); win = 2
                                vm.speech.speak("برافو يا عمرو!", pitch = 1.4f)
                                scope.launch { delay(1500); win = 0; target = PIECES.random() }
                            } else {
                                vm.speech.speak("ده ${p.nameAr}. دور تاني!", pitch = 1.3f)
                            }
                        }
                    }
                }
            }
        }
        Confetti(win)
    }
}

@Composable
private fun AmrKnightJump(vm: AppViewModel) {
    val size = 5
    fun knightTargets(pos: Int): List<Int> {
        val r = pos / size; val c = pos % size
        return Chess.KNIGHT.mapNotNull { (dr, dc) ->
            val rr = r + dr; val cc = c + dc
            if (rr in 0 until size && cc in 0 until size) rr * size + cc else null
        }
    }
    var knight by remember { mutableStateOf(12) }
    var apple by remember { mutableStateOf(knightTargets(12).random()) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.speech.speak("خلّي الحصان يقفز على التفاحة", pitch = 1.3f) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("الحصان يقفز على التفاحة 🍎", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            for (r in 0 until size) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until size) {
                        val i = r * size + c
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                                .background(if ((r + c) % 2 == 0) Color(0xFFFFF3C4) else Color(0xFF9ED8A8))
                                .clickable {
                                    if (i == apple) {
                                        knight = i; vm.addStars(1); win = 3
                                        vm.speech.speak("حصان شاطر!", pitch = 1.4f)
                                        scope.launch {
                                            delay(1300); win = 0
                                            apple = knightTargets(knight).random()
                                        }
                                    } else {
                                        vm.speech.speak("لأ، هنا التفاحة", pitch = 1.3f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (i) {
                                knight -> Text("♞", fontSize = 40.sp, color = Color(0xFFFF7043))
                                apple -> Bouncy { Text("🍎", fontSize = 36.sp) }
                            }
                        }
                    }
                }
            }
        }
        Confetti(win)
    }
}

/* ---------- Mode B: Judy (age 6) ---------- */

private class Lesson(val code: Char, val title: String, val tip: String)

private val LESSONS = listOf(
    Lesson('P', "البيدق", "البيدق يمشي خطوة لقدّام، ويأكل بالقُطر"),
    Lesson('R', "القلعة", "القلعة تجري مستقيم، لقدام ولورا وجنب"),
    Lesson('B', "الفيل", "الفيل يمشي بالعرض على نفس اللون"),
    Lesson('N', "الحصان", "الحصان يقفز على شكل حرف L"),
    Lesson('Q', "الوزيرة", "الوزيرة تمشي مستقيم وبالعرض، هي الأقوى"),
    Lesson('K', "الملك", "الملك يمشي خطوة واحدة في أي اتجاه")
)

@Composable
private fun ChessLearn(vm: AppViewModel) {
    var idx by remember { mutableStateOf(0) }
    val lesson = LESSONS[idx]
    val sq = if (lesson.code == 'P') 6 * 8 + 3 else 4 * 8 + 3
    val arr = CharArray(64) { '.' }
    arr[sq] = lesson.code
    val game = Chess(arr)
    val targets = game.legalMoves(sq).toSet()
    LaunchedEffect(idx) { vm.speech.speak(lesson.tip) }
    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LESSONS.forEachIndexed { i, l ->
                BigCard(filled[l.code.lowercaseChar()] ?: "", "", if (i == idx) Mint else Color(0xFFE9ECEF),
                    Modifier.weight(1f).height(60.dp), 26.sp) { idx = i }
            }
        }
        Text(lesson.title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(8.dp))
        BoardView(game, sq, targets, null) { }
        Text(lesson.tip, fontSize = 20.sp, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
        BigCard("🔊", "", Sun, Modifier.size(72.dp), 32.sp) { vm.speech.speak(lesson.tip) }
    }
}

@Composable
private fun ChessPuzzles(vm: AppViewModel) {
    var n by remember { mutableStateOf(0) }
    val pz = PUZZLES[n % PUZZLES.size]
    var game by remember(n) { mutableStateOf(Chess(pz.board.toCharArray())) }
    var sel by remember(n) { mutableStateOf<Int?>(null) }
    var solved by remember(n) { mutableStateOf(false) }
    var msg by remember(n) { mutableStateOf(pz.hint) }
    LaunchedEffect(n) { vm.speech.speak(pz.hint) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pz.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(msg, fontSize = 20.sp, color = Ink)
            val targets = sel?.let { game.legalMoves(it).toSet() } ?: emptySet()
            BoardView(game, sel, targets, pz.star) { i ->
                if (solved) return@BoardView
                val p = game.b[i]
                val s = sel
                if (s != null && i in targets) {
                    val next = game.make(s, i)
                    val ok = if (pz.star != null) i == pz.star else next.isCheckmate()
                    if (ok) {
                        game = next; solved = true; vm.addStars(2)
                        msg = "برافو يا جودي! 🎉"; vm.speech.speak("برافو يا جودي! أحسنتي")
                    } else {
                        msg = "قريب! جرّبي تاني"; vm.speech.speak("جرّبي تاني")
                        game = Chess(pz.board.toCharArray())
                    }
                    sel = null
                } else if (p != '.' && Chess.isWhite(p)) sel = i
                else sel = null
            }
            if (solved) BigCard("➡️", "اللغز اللي بعده", Mint, Modifier.fillMaxWidth().height(90.dp)) { n++ }
        }
        Confetti(if (solved) 3 else 0)
    }
}

@Composable
private fun ChessPlay(vm: AppViewModel, vsAi: Boolean) {
    var game by remember { mutableStateOf(Chess()) }
    var sel by remember { mutableStateOf<Int?>(null) }
    var over by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf(if (vsAi) "دورك يا جودي" else "دور الأبيض (جودي)") }
    var thinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun afterMove(g: Chess, mover: String) {
        when {
            g.isCheckmate() -> { over = true; msg = "كش مات! فاز $mover 🏆"; vm.speech.speak("كش مات! برافو"); if (mover != "الروبوت") vm.addStars(3) }
            g.isStalemate() -> { over = true; msg = "تعادل 🤝" }
            g.inCheck(g.whiteTurn) -> { msg = "كش! 👑"; vm.speech.speak("كش") }
            else -> msg = if (vsAi) "دورك يا جودي" else if (g.whiteTurn) "دور الأبيض (جودي)" else "دور الأسود (عمرو)"
        }
    }

    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(msg, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(6.dp))
        val targets = sel?.let { game.legalMoves(it).toSet() } ?: emptySet()
        BoardView(game, sel, targets, null) { i ->
            if (over || thinking) return@BoardView
            val s = sel
            val p = game.b[i]
            if (s != null && i in targets) {
                val moverName = if (game.whiteTurn) "جودي" else "عمرو"
                game = game.make(s, i); sel = null
                afterMove(game, moverName)
                if (vsAi && !over) {
                    thinking = true
                    scope.launch {
                        delay(700)
                        val m = game.aiMove(level = 0)
                        if (m != null) { game = game.make(m.first, m.second); afterMove(game, "الروبوت") }
                        thinking = false
                    }
                }
            } else if (p != '.' && Chess.isWhite(p) == game.whiteTurn) sel = i
            else sel = null
        }
        Spacer(Modifier.height(8.dp))
        BigCard("🔄", "لعبة جديدة", Peach, Modifier.fillMaxWidth().height(80.dp), 28.sp) {
            game = Chess(); sel = null; over = false
            msg = if (vsAi) "دورك يا جودي" else "دور الأبيض (جودي)"
        }
    }
}
