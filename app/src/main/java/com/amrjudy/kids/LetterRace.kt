package com.amrjudy.kids

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * Letter race: drive a car down a road; gates come towards you, each showing a letter / syllable / word.
 * Listen to the question and steer into the right gate. Arabic or English, letters / vowels / words,
 * three levels. The language, content and level are chosen when entering.
 */

private class RaceCfg(val lang: Int, val content: Int, val level: Int)   // lang 0 = Arabic, 1 = English

private class RaceQ(
    val prompt: String, val spoken: String, val english: Boolean, val hint: String,
    val options: List<String>, val answer: Int, val big: Boolean
)

private class RaceState {
    var z = 5f
    var carU = 0f
    var targetU = 0f
    var roadPhase = 0f
    var pause = 0.6f
    var phase = 0          // 0 = gates approaching, 1 = gates passing, 2 = short wait before the next question
    var retries = 1
    var repeat = false
}

/* ---------- data ---------- */

private val AR by lazy { arabicLetters() }
private val AR_GROUPS = listOf("بتث", "جحخ", "دذ", "رز", "سش", "صض", "طظ", "عغ", "فق", "كل", "من")
private val AR_MARKS = listOf("َ", "ُ", "ِ")          // fatha, damma, kasra
private val AR_VOWELS = listOf("ا", "و", "ي")

private class EnW(val letter: String, val word: String, val emoji: String)

private val EN = listOf(
    EnW("A", "apple", "🍎"), EnW("B", "ball", "⚽"), EnW("C", "cat", "🐱"), EnW("D", "dog", "🐶"),
    EnW("E", "egg", "🥚"), EnW("F", "fish", "🐟"), EnW("G", "grapes", "🍇"), EnW("H", "hat", "🎩"),
    EnW("I", "ice cream", "🍦"), EnW("J", "juice", "🧃"), EnW("K", "kite", "🪁"), EnW("L", "lion", "🦁"),
    EnW("M", "moon", "🌙"), EnW("N", "nose", "👃"), EnW("O", "orange", "🍊"), EnW("P", "pizza", "🍕"),
    EnW("Q", "queen", "👑"), EnW("R", "rabbit", "🐰"), EnW("S", "sun", "☀️"), EnW("T", "tree", "🌳"),
    EnW("U", "umbrella", "☂️"), EnW("V", "van", "🚐"), EnW("W", "watch", "⌚"), EnW("X", "xylophone", "🎹"),
    EnW("Y", "yellow", "💛"), EnW("Z", "zebra", "🦓")
)
private val EN_GROUPS = listOf("bdpq", "mnw", "un", "ijl", "aeo", "gqy", "ft", "cs", "hn", "vy")

private fun arPick(l: LetterInfo, n: Int, confusable: Boolean): List<LetterInfo> {
    val pool = if (confusable) AR.filter { it.ch != l.ch && AR_GROUPS.any { g -> g.contains(l.ch) && g.contains(it.ch) } }
    else emptyList()
    val rest = AR.filter { it.ch != l.ch && it !in pool }.shuffled()
    return (listOf(l) + (pool.shuffled() + rest).take(n - 1)).shuffled()
}

private fun enPick(w: EnW, n: Int, confusable: Boolean): List<EnW> {
    val lc = w.letter.lowercase()
    val pool = if (confusable) EN.filter { it !== w && EN_GROUPS.any { g -> g.contains(lc) && g.contains(it.letter.lowercase()) } }
    else emptyList()
    val rest = EN.filter { it !== w && it !in pool }.shuffled()
    return (listOf(w) + (pool.shuffled() + rest).take(n - 1)).shuffled()
}

private fun buildQ(cfg: RaceCfg, amr: Boolean, prev: String?): RaceQ {
    val n = if (cfg.level == 1) 2 else 3
    val level = cfg.level
    if (cfg.lang == 0) {
        when (cfg.content) {
            0 -> {
                val l = AR.filter { it.ch != prev }.random()
                val o = arPick(l, n, level >= 2)
                val t = "فين حرف ${l.name}؟"
                return RaceQ(t, t, false, if (level == 1) l.emoji else "", o.map { it.ch }, o.indexOf(l), true)
            }
            1 -> {
                val l = AR.filter { it.ch != prev }.random()
                val mi = (0..2).random()
                val syl = l.ch + AR_VOWELS[mi]
                val opts: List<String>
                val ans: String = l.ch + AR_MARKS[mi]
                if (level == 1) {
                    val other = (0..2).filter { it != mi }.random()
                    opts = listOf(ans, l.ch + AR_MARKS[other]).shuffled()
                } else if (level == 2) {
                    opts = (0..2).map { l.ch + AR_MARKS[it] }.shuffled()
                } else {
                    val others = arPick(l, 3, true).filter { it !== l }.take(2)
                    opts = (listOf(ans) + others.map { it.ch + AR_MARKS[(0..2).random()] }).distinct().shuffled()
                }
                val t = "فين $syl؟"
                return RaceQ(t, t, false, "", opts, opts.indexOf(ans), true)
            }
            else -> {
                val w = AR.filter { it.word != prev }.random()
                val o = arPick(w, n, false)
                val t = "فين كلمة ${w.word}؟"
                val labels = if (amr) o.map { it.emoji } else o.map { it.tashkeel }
                return RaceQ(
                    t, t, false, if (!amr && level < 3) w.emoji else "", labels, o.indexOf(w), amr
                )
            }
        }
    } else {
        when (cfg.content) {
            0 -> {
                val w = EN.filter { it.letter != prev }.random()
                val o = enPick(w, n, level >= 3)
                val lower = level >= 2
                val labels = o.map { if (lower) it.letter.lowercase() else it.letter }
                val t = "Find the letter ${w.letter}"
                return RaceQ(t, t, true, if (level == 1) w.emoji else "", labels, o.indexOf(w), true)
            }
            1 -> {
                val w = EN.filter { it.letter != prev }.random()
                val o = enPick(w, n, false)
                val labels = o.map { if (level == 3) it.letter.lowercase() else it.letter }
                val t = "What letter does ${w.word} start with?"
                return RaceQ(t, t, true, if (level < 3) w.emoji else "", labels, o.indexOf(w), true)
            }
            else -> {
                val w = EN.filter { it.word != prev }.random()
                val o = enPick(w, n, false)
                val t = "Find ${w.word}"
                val labels = if (amr) o.map { it.emoji } else o.map { it.word }
                return RaceQ(t, t, true, if (!amr && level < 3) w.emoji else "", labels, o.indexOf(w), amr)
            }
        }
    }
}

private fun keyOf(q: RaceQ) = q.options[q.answer]

/* ---------- entry (language / content / level chosen here) ---------- */

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(58.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) Sun else Color.White)
            .border(if (selected) 3.dp else 1.dp, if (selected) Color(0xFFFF922B) else Color(0xFFDDD6E8), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center) }
}

@Composable
fun LetterRace(vm: AppViewModel) {
    val amr = vm.profile == Profile.AMR
    var cfg by remember { mutableStateOf<RaceCfg?>(null) }
    var lang by remember { mutableStateOf(0) }
    var content by remember { mutableStateOf(if (amr) 0 else 2) }
    var level by remember { mutableStateOf(if (amr) 1 else 2) }
    val c = cfg
    if (c != null) {
        key(c) { RaceGame(vm, c, amr) { cfg = null } }
        return
    }
    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar("سباق الحروف", vm.stars) { vm.back() }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏎️", fontSize = 64.sp)
            Text("اللغة  Language", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("عربي", lang == 0, Modifier.weight(1f)) { lang = 0 }
                Chip("English", lang == 1, Modifier.weight(1f)) { lang = 1 }
            }
            Text("هنتسابق على إيه؟  What to race?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            val names = if (lang == 0) listOf("حروف", "حركات", "كلمات") else listOf("Letters", "First letter", "Words")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, n -> Chip(n, content == i, Modifier.weight(1f)) { content = i } }
            }
            Text("المستوى  Level", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("١ سهل ⭐", level == 1, Modifier.weight(1f)) { level = 1 }
                Chip("٢ متوسط ⭐⭐", level == 2, Modifier.weight(1f)) { level = 2 }
                Chip("٣ صعب ⭐⭐⭐", level == 3, Modifier.weight(1f)) { level = 3 }
            }
            Text(
                when (level) {
                    1 -> if (lang == 0) "بوابتين وبطيء، ومعاه صورة تساعد" else "2 gates, slow, with a picture to help"
                    2 -> if (lang == 0) "تلات بوابات وأسرع" else "3 gates, faster"
                    else -> if (lang == 0) "تلات بوابات، سريع، وبالسمع بس (من غير كلام مكتوب)" else "3 gates, fast, listen only"
                },
                fontSize = 15.sp, color = Ink, textAlign = TextAlign.Center
            )
            if (amr && content == 2) {
                Text(
                    if (lang == 0) "(في الكلمات بيشوف صور على البوابات)" else "(Words show pictures on the gates)",
                    fontSize = 14.sp, color = Ink, textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(6.dp))
            BigCard("🏁", if (lang == 0) "يلا نسابق!" else "Let's race!", Mint, Modifier.fillMaxWidth().height(90.dp), 40.sp) {
                cfg = RaceCfg(lang, content, level)
            }
        }
    }
}

/* ---------- the game ---------- */

private val GATE_COLORS = listOf(Color(0xFFFF9F43), Color(0xFF4D96FF), Color(0xFF9B6BFF))

private fun depth(z: Float) = 1f / (1f + z)
private fun roadHalf(w: Float, d: Float) = w * (0.07f + 0.43f * d)

@Composable
private fun RaceGame(vm: AppViewModel, cfg: RaceCfg, amr: Boolean, onExit: () -> Unit) {
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    val tick = remember { mutableIntStateOf(0) }
    val st = remember { RaceState() }
    var q by remember { mutableStateOf<RaceQ?>(null) }
    var score by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var flash by remember { mutableStateOf("") }
    var win by remember { mutableStateOf(0) }
    val tone = remember { try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (e: Exception) { null } }
    DisposableEffect(Unit) { onDispose { try { tone?.release() } catch (e: Exception) { } } }
    fun beep(t: Int, ms: Int) { try { tone?.startTone(t, ms) } catch (e: Exception) { } }

    val zSpeed = (if (amr) listOf(0.65f, 0.9f, 1.2f) else listOf(0.95f, 1.35f, 1.8f))[cfg.level - 1]
    val n = if (cfg.level == 1) 2 else 3

    fun say(x: RaceQ) { if (x.english) vm.speech.speakEn(x.spoken) else vm.speech.speak(x.spoken, pitch = 1.2f) }

    fun nextQuestion(repeat: Boolean) {
        val old = q
        val nq = if (repeat && old != null) {
            // same question again, gates shuffled
            val ans = old.options[old.answer]
            val sh = old.options.shuffled()
            RaceQ(old.prompt, old.spoken, old.english, old.hint, sh, sh.indexOf(ans), old.big)
        } else buildQ(cfg, amr, old?.let { keyOfLetter(cfg, it) })
        q = nq
        st.z = 5f; st.phase = 0
        say(nq)
    }

    fun judge() {
        val cur = q ?: return
        st.phase = 1
        var best = 0; var bd = 9f
        for (i in 0 until cur.options.size) {
            val u = ((i + 0.5f) / cur.options.size) * 2f - 1f
            val dd = abs(u - st.carU)
            if (dd < bd) { bd = dd; best = i }
        }
        if (best == cur.answer) {
            score++; streak++
            vm.addStars(cfg.level)
            flash = "⭐"
            beep(ToneGenerator.TONE_PROP_ACK, 150)
            if (cfg.lang == 0) vm.speech.speak("برافو!", pitch = 1.4f) else vm.speech.speakEn("Great job!")
            st.retries = 1; st.repeat = false
            if (streak % 6 == 0) win = 3
            st.pause = 1.0f
        } else {
            streak = 0
            flash = "😕"
            beep(ToneGenerator.TONE_PROP_NACK, 200)
            if (cfg.lang == 0) vm.speech.speak("لأ، جرّب تاني", pitch = 1.3f) else vm.speech.speakEn("Try again")
            // ask the same question once more, then move on
            if (st.retries > 0) { st.retries--; st.repeat = true } else { st.retries = 1; st.repeat = false }
            st.pause = 1.3f
        }
    }

    fun setLane(x: Float) {
        if (size.width == 0) return
        val i = ((x / size.width) * n).toInt().coerceIn(0, n - 1)
        st.targetU = ((i + 0.5f) / n) * 2f - 1f
    }

    fun step(dt: Float) {
        st.roadPhase += dt * (if (q == null || st.phase == 2) 1.6f else 1.6f + 1.2f * zSpeed)
        st.carU += (st.targetU - st.carU) * minOf(1f, 10f * dt)
        if (q == null) {
            st.pause -= dt
            if (st.pause <= 0f) nextQuestion(false)
            return
        }
        when (st.phase) {
            0 -> { st.z -= zSpeed * dt; if (depth(st.z) >= 0.9f) judge() }
            1 -> { st.z -= zSpeed * dt; if (st.z < -0.45f) st.phase = 2 }
            else -> {
                st.pause -= dt
                if (st.pause <= 0f) { flash = ""; nextQuestion(st.repeat) }
            }
        }
    }

    LaunchedEffect(size) {
        if (size.width == 0) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            step(dt)
            tick.intValue++
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("سباق الحروف", vm.stars) { onExit() }
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        setLane(down.position.x)
                        do {
                            val ev = awaitPointerEvent()
                            ev.changes.firstOrNull()?.let { setLane(it.position.x) }
                        } while (ev.changes.any { it.pressed })
                    }
                }
        ) {
            val frame = tick.intValue
            Canvas(Modifier.fillMaxSize()) {
                if (frame >= 0) drawWorld(st, size.width, size.height, n)
            }
            val cur = q
            val W = size.width.toFloat(); val H = size.height.toFloat()
            if (cur != null && W > 0f && st.phase < 2) {
                val horizon = H * 0.30f
                val ground = H * 0.95f
                val d = depth(st.z).coerceIn(0.05f, 1.6f)
                val s = 0.14f + 0.86f * d
                val cx = W / 2f
                val gateW = (W / n) * 0.88f                       // width at d = 1
                val gateH = gateW * 0.95f
                val y = horizon + (ground - horizon) * d
                for (i in cur.options.indices) {
                    val u = ((i + 0.5f) / cur.options.size) * 2f - 1f
                    val x = cx + u * roadHalf(W, d)
                    val col = GATE_COLORS[i % 3]
                    val label = cur.options[i]
                    Box(
                        Modifier
                            .offset { IntOffset((x - gateW / 2f).roundToInt(), (y - gateH).roundToInt()) }
                            .size((gateW / density).dp, (gateH / density).dp)
                            .graphicsLayer { scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0.5f, 1f) }
                    ) {
                        // posts
                        Box(Modifier.align(Alignment.BottomStart).padding(start = 6.dp).size(8.dp, (gateH / density * 0.55f).dp).background(col))
                        Box(Modifier.align(Alignment.BottomEnd).padding(end = 6.dp).size(8.dp, (gateH / density * 0.55f).dp).background(col))
                        // board
                        Box(
                            Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.62f)
                                .clip(RoundedCornerShape(16.dp)).background(Color.White)
                                .border(5.dp, col, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val base = gateW / density
                            val fs = if (cur.big) base * (if (label.length <= 2) 0.40f else 0.30f) else base * 0.24f
                            Text(
                                label, fontSize = fs.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                                textAlign = TextAlign.Center, maxLines = 1, softWrap = false
                            )
                        }
                    }
                }
            }

            // question banner
            if (cur != null) {
                Row(
                    Modifier.align(Alignment.TopCenter).padding(10.dp).clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.92f)).clickable { say(cur) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔊", fontSize = 30.sp)
                    if (cur.hint.isNotEmpty()) Text(cur.hint, fontSize = 40.sp)
                    if (cfg.level < 3) {
                        Text(
                            cur.prompt, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink,
                            textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 220.dp)
                        )
                    }
                }
            }
            Text(
                "⭐ $score", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
            if (flash.isNotEmpty()) {
                Text(flash, fontSize = 110.sp, modifier = Modifier.align(Alignment.Center))
            }
            Celebrate(win) { win = 0 }
        }
    }
}

/** Same letter/word identity for "don't repeat the previous question". */
private fun keyOfLetter(cfg: RaceCfg, q: RaceQ): String? {
    // Arabic letters: first char of the answer; English: the letter; words: the answer text.
    val a = q.options[q.answer]
    return if (cfg.lang == 0 && cfg.content != 2) a.take(1)
    else if (cfg.lang == 1 && cfg.content == 0) a.uppercase()
    else if (cfg.lang == 1 && cfg.content == 1) EN.firstOrNull { it.letter == a.uppercase() }?.letter
    else null
}

/* ---------- drawing: pseudo-3D road, hills, car ---------- */

private fun DrawScope.drawWorld(st: RaceState, W: Float, H: Float, lanes: Int) {
    val horizon = H * 0.30f
    val ground = H * 0.95f
    // sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF69C3FF), Color(0xFFBFE8FF), Color(0xFFE9F8FF)), 0f, horizon), size = Size(W, horizon))
    // clouds
    drawCircle(Color(0xCCFFFFFF), 34f, Offset(W * 0.18f, horizon * 0.45f))
    drawCircle(Color(0xCCFFFFFF), 26f, Offset(W * 0.18f + 36f, horizon * 0.50f))
    drawCircle(Color(0xCCFFFFFF), 30f, Offset(W * 0.74f, horizon * 0.30f))
    drawCircle(Color(0xCCFFFFFF), 22f, Offset(W * 0.74f + 32f, horizon * 0.36f))
    // far buildings
    val bcols = listOf(Color(0xFFE8A0B5), Color(0xFFB7C4F0), Color(0xFFF2D49B), Color(0xFFA8D8B9))
    for (i in 0 until 9) {
        val bw = W * 0.06f; val bh = horizon * (0.18f + 0.12f * ((i * 5) % 4))
        val bx = if (i < 5) W * 0.02f + i * bw * 1.1f else W * 0.62f + (i - 5) * bw * 1.1f
        drawRect(bcols[i % 4], Offset(bx, horizon - bh), Size(bw, bh))
    }
    // hills
    drawOval(Color(0xFF7CCB5B), Offset(-W * 0.25f, horizon - 70f), Size(W * 0.75f, 140f))
    drawOval(Color(0xFF8ED66B), Offset(W * 0.55f, horizon - 55f), Size(W * 0.8f, 110f))

    // grass with moving stripes
    drawRect(Color(0xFF6CC14F), Offset(0f, horizon), Size(W, H - horizon))
    val band = 0.55f
    val ph = st.roadPhase
    val startIdx = floor(ph / band).toInt()
    val frac = ph / band - startIdx
    fun yAt(z: Float): Float = horizon + (ground - horizon) * (1f / (1f + z)).coerceIn(0f, 1.3f)
    val cx = W / 2f
    for (j in 0 until 16) {
        val zNear = (j - frac) * band
        val zFar = (j + 1 - frac) * band
        val yN = yAt(maxOf(zNear, -0.4f)); val yF = yAt(zFar)
        val dN = 1f / (1f + maxOf(zNear, -0.4f)); val dF = 1f / (1f + zFar)
        val even = (startIdx + j) % 2 == 0
        if (even) drawRect(Color(0xFF63B946), Offset(0f, yF), Size(W, (yN - yF).coerceAtLeast(0f)))
        // curbs
        val cw = 0.045f
        val hN = roadHalf(W, dN); val hF = roadHalf(W, dF)
        val curb = if (even) Color(0xFFE63946) else Color.White
        val left = Path().apply {
            moveTo(cx - hF - hF * cw, yF); lineTo(cx - hF, yF); lineTo(cx - hN, yN); lineTo(cx - hN - hN * cw, yN); close()
        }
        val right = Path().apply {
            moveTo(cx + hF, yF); lineTo(cx + hF + hF * cw, yF); lineTo(cx + hN + hN * cw, yN); lineTo(cx + hN, yN); close()
        }
        // road
        val road = Path().apply {
            moveTo(cx - hF, yF); lineTo(cx + hF, yF); lineTo(cx + hN, yN); lineTo(cx - hN, yN); close()
        }
        drawPath(road, if (even) Color(0xFF4B4E57) else Color(0xFF53565F))
        drawPath(left, curb); drawPath(right, curb)
        // lane dashes
        if (even) {
            for (k in 1 until lanes) {
                val u = -1f + 2f * k / lanes
                val dw = 0.012f
                val dash = Path().apply {
                    moveTo(cx + u * hF - hF * dw, yF); lineTo(cx + u * hF + hF * dw, yF)
                    lineTo(cx + u * hN + hN * dw, yN); lineTo(cx + u * hN - hN * dw, yN); close()
                }
                drawPath(dash, Color.White)
            }
        }
    }

    // car (rear view)
    val dCar = 0.93f
    val carW = (W / lanes) * 0.62f
    val carX = cx + st.carU * roadHalf(W, dCar)
    val carY = H * 0.93f
    val tilt = ((st.targetU - st.carU) * 14f).coerceIn(-12f, 12f)
    rotate(tilt, Offset(carX, carY)) {
        // shadow
        drawOval(Color(0x55000000), Offset(carX - carW * 0.55f, carY - carW * 0.10f), Size(carW * 1.1f, carW * 0.22f))
        // wheels
        drawRoundRect(Color(0xFF222222), Offset(carX - carW * 0.52f, carY - carW * 0.34f), Size(carW * 0.18f, carW * 0.30f), CornerRadius(8f))
        drawRoundRect(Color(0xFF222222), Offset(carX + carW * 0.34f, carY - carW * 0.34f), Size(carW * 0.18f, carW * 0.30f), CornerRadius(8f))
        // body
        drawRoundRect(Color(0xFFE5383B), Offset(carX - carW * 0.48f, carY - carW * 0.52f), Size(carW * 0.96f, carW * 0.42f), CornerRadius(carW * 0.12f))
        // cabin
        drawRoundRect(Color(0xFFC1121F), Offset(carX - carW * 0.36f, carY - carW * 0.80f), Size(carW * 0.72f, carW * 0.34f), CornerRadius(carW * 0.14f))
        // rear window
        drawRoundRect(Color(0xFFBFE6FF), Offset(carX - carW * 0.28f, carY - carW * 0.74f), Size(carW * 0.56f, carW * 0.20f), CornerRadius(carW * 0.07f))
        // tail lights + bumper
        drawRoundRect(Color(0xFFFFD43B), Offset(carX - carW * 0.44f, carY - carW * 0.44f), Size(carW * 0.16f, carW * 0.08f), CornerRadius(6f))
        drawRoundRect(Color(0xFFFFD43B), Offset(carX + carW * 0.28f, carY - carW * 0.44f), Size(carW * 0.16f, carW * 0.08f), CornerRadius(6f))
        drawRoundRect(Color(0xFF8D0F1A), Offset(carX - carW * 0.40f, carY - carW * 0.20f), Size(carW * 0.80f, carW * 0.08f), CornerRadius(6f))
    }
}
