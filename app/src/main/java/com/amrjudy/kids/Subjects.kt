package com.amrjudy.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/* ======================= shared building blocks ======================= */

class HubCard(val emoji: String, val label: String, val color: Color, val content: @Composable () -> Unit)

/** A screen with a grid of activity cards; tapping one opens it, the top-bar button goes back. */
@Composable
fun Hub(vm: AppViewModel, title: String, cards: List<HubCard>) {
    var sel by remember { mutableStateOf(-1) }
    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar(title, vm.stars) { if (sel < 0) vm.back() else { vm.speech.stopSpeaking(); sel = -1 } }
        if (sel < 0) {
            val scroll = cards.size > 8
            Column(
                Modifier.fillMaxSize()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (row in cards.chunked(2)) {
                    val rowMod = if (scroll) Modifier.fillMaxWidth().height(130.dp)
                    else Modifier.fillMaxWidth().weight(1f)
                    Row(rowMod, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (c in row) {
                            BigCard(c.emoji, c.label, c.color, Modifier.weight(1f).fillMaxHeight(), 48.sp) {
                                sel = cards.indexOf(c)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            key(sel) { cards[sel].content() }
        }
    }
}

/** One multiple-choice question. */
class MQ(
    val glyph: String,
    val question: String,
    val spoken: String,
    val choices: List<String>,
    val answer: String
)

@Composable
fun OptTile(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(24.dp)).background(color).clickable(onClick = onClick).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = if (text.length <= 3) 52.sp else 24.sp,
            fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center
        )
    }
}

/** Endless quiz: asks make() for a question, speaks it, rewards a star for the right answer. */
@Composable
fun QuizRunner(vm: AppViewModel, en: Boolean = false, glyphSize: Int = 70, make: () -> MQ) {
    var q by remember { mutableStateOf(make()) }
    var win by remember { mutableStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun say(t: String) { if (en) vm.speech.speakEn(t) else vm.speech.speak(t, pitch = 1.15f) }
    LaunchedEffect(q) { say(q.spoken) }
    val colors = listOf(Pink, Sky, Mint, Sun)
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (q.glyph.isNotEmpty()) Text(q.glyph, fontSize = glyphSize.sp, textAlign = TextAlign.Center)
            Text(
                q.question, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { say(q.spoken) }
            )
            for (row in q.choices.chunked(2)) {
                val rowMod = if (q.choices.size <= 2) Modifier.fillMaxWidth().height(170.dp)
                else Modifier.fillMaxWidth().weight(1f)
                Row(rowMod, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in row) {
                        OptTile(c, colors[q.choices.indexOf(c) % 4], Modifier.weight(1f).fillMaxHeight()) {
                            if (!locked) {
                                if (c == q.answer) {
                                    locked = true; vm.addStars(1); win = 1
                                    say(if (en) "Great job!" else "برافو! إجابة صح")
                                    scope.launch { delay(1500); win = 0; locked = false; q = make() }
                                } else say(if (en) "Try again" else "حاول تاني")
                            }
                        }
                    }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

class MapItem(val glyph: String, val question: String, val answer: String)

/** Question per item; the answer plus up to 3 other options are shown. */
@Composable
fun MapQuiz(vm: AppViewModel, items: List<MapItem>, options: List<String>, en: Boolean = false, glyphSize: Int = 90) {
    QuizRunner(vm, en, glyphSize) {
        val m = items.random()
        MQ(
            m.glyph, m.question, m.question,
            (listOf(m.answer) + options.filter { o -> o != m.answer }.shuffled().take(3)).shuffled(),
            m.answer
        )
    }
}

/* ============================== English ============================== */

private class Letter(val letter: String, val word: String, val emoji: String)

private val LETTERS = listOf(
    Letter("A", "apple", "🍎"), Letter("B", "ball", "⚽"), Letter("C", "cat", "🐱"),
    Letter("D", "dog", "🐶"), Letter("E", "elephant", "🐘"), Letter("F", "fish", "🐟"),
    Letter("G", "grapes", "🍇"), Letter("H", "house", "🏠"), Letter("I", "ice cream", "🍦"),
    Letter("J", "jet", "✈️"), Letter("K", "koala", "🐨"), Letter("L", "lion", "🦁"),
    Letter("M", "moon", "🌙"), Letter("N", "nose", "👃"), Letter("O", "orange", "🍊"),
    Letter("P", "pizza", "🍕"), Letter("Q", "queen", "👑"), Letter("R", "rabbit", "🐰"),
    Letter("S", "sun", "☀️"), Letter("T", "tree", "🌳"), Letter("U", "umbrella", "☂️"),
    Letter("V", "van", "🚐"), Letter("W", "watermelon", "🍉"), Letter("X", "xylophone", "🎶"),
    Letter("Y", "yarn", "🧶"), Letter("Z", "zebra", "🦓")
)

private val EN_COLORS = listOf(
    "🍎" to "red", "🍌" to "yellow", "🐸" to "green", "🍊" to "orange",
    "🐳" to "blue", "🐷" to "pink", "🍆" to "purple", "🐻" to "brown", "⚫" to "black", "⚪" to "white"
)
private val EN_NUMBERS = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
private val FRUIT = listOf("🍎", "🍌", "🍓", "🍊", "⚽", "🐟")

@Composable
fun EnglishHub(vm: AppViewModel) {
    Hub(
        vm, "English",
        listOf(
            HubCard("🔤", "ABC", Sky) { AlphabetExplorer(vm) },
            HubCard("❓", "What is this?", Mint) {
                QuizRunner(vm, en = true, glyphSize = 100) {
                    val l = LETTERS.random()
                    MQ(l.emoji, "What is this?", "What is this?",
                        (listOf(l.word) + LETTERS.filter { it !== l }.shuffled().take(3).map { it.word }).shuffled(), l.word)
                }
            },
            HubCard("👆", "Find it", Peach) {
                QuizRunner(vm, en = true) {
                    val l = LETTERS.random()
                    MQ("", "Find: ${l.word}", "Where is the ${l.word}?",
                        (listOf(l.emoji) + LETTERS.filter { it !== l }.shuffled().take(3).map { it.emoji }).shuffled(), l.emoji)
                }
            },
            HubCard("🎨", "Colors", Pink) {
                QuizRunner(vm, en = true, glyphSize = 100) {
                    val c = EN_COLORS.random()
                    MQ(c.first, "What color?", "What color is it?",
                        (listOf(c.second) + EN_COLORS.filter { it !== c }.shuffled().take(3).map { it.second }).shuffled(), c.second)
                }
            },
            HubCard("🔢", "Numbers", Sun) {
                QuizRunner(vm, en = true, glyphSize = 34) {
                    val n = (1..10).random()
                    val f = FRUIT.random()
                    MQ(f.repeat(n), "How many?", "How many?",
                        (listOf(EN_NUMBERS[n - 1]) + EN_NUMBERS.filter { it != EN_NUMBERS[n - 1] }.shuffled().take(3)).shuffled(),
                        EN_NUMBERS[n - 1])
                }
            },
            HubCard("🗣️", "Say it!", Lilac) {
                SayItScreen(
                    vm,
                    LETTERS.map { SayItItem(it.emoji, it.word, it.word) },
                    lang = "en-US", english = true
                )
            }
        )
    )
}

@Composable
private fun AlphabetExplorer(vm: AppViewModel) {
    var sel by remember { mutableStateOf(0) }
    val l = LETTERS[sel]
    LaunchedEffect(sel) { vm.speech.speakEn("${l.letter}. ${l.letter} for ${l.word}") }
    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Sun)
                .clickable { vm.speech.speakEn("${l.letter}. ${l.letter} for ${l.word}") },
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(l.letter + l.letter.lowercase(), fontSize = 76.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Bouncy { Text(l.emoji, fontSize = 76.sp) }
                Text(l.word, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BigCard("⬅️", "", Peach, Modifier.size(72.dp), 28.sp) { sel = (sel + 25) % 26 }
            MicButton(vm, l.word, lang = "en-US") { s ->
                if (s > 0) { vm.addStars(s); vm.speech.speakEn("Great job!") }
                else vm.speech.speakEn("Try again")
            }
            BigCard("➡️", "", Peach, Modifier.size(72.dp), 28.sp) { sel = (sel + 1) % 26 }
        }
        Spacer(Modifier.height(8.dp))
        for (row in LETTERS.chunked(7)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (x in row) {
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (x === l) Pink else Color.White)
                            .clickable { sel = LETTERS.indexOf(x) },
                        contentAlignment = Alignment.Center
                    ) { Text(x.letter, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink) }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/* ================================ Math ================================ */

fun ar(n: Int): String = n.toString().map { '٠' + (it - '0') }.joinToString("")

private fun numChoices(ans: Int, lo: Int, hi: Int): List<String> {
    val s = linkedSetOf(ans)
    var guard = 0
    while (s.size < 4 && guard++ < 60) {
        val d = (1..3).random() * (if ((0..1).random() == 0) 1 else -1)
        val v = ans + d
        if (v in lo..hi) s.add(v)
    }
    var v = lo
    while (s.size < 4 && v <= hi) { s.add(v); v++ }
    return s.shuffled().map { ar(it) }
}

private fun addQ(): MQ {
    val a = (0..10).random(); val b = (0..10).random(); val ans = a + b
    val glyph = if (ans in 1..10) "🍎".repeat(a) + "  ➕  " + "🍏".repeat(b) else ""
    return MQ(glyph, "${ar(a)} + ${ar(b)} = ؟", "$a زائد $b يساوي كام؟", numChoices(ans, 0, 20), ar(ans))
}

private fun subQ(): MQ {
    val a = (1..20).random(); val b = (0..a).random(); val ans = a - b
    val glyph = if (a <= 10) "🍎".repeat(a) else ""
    return MQ(glyph, "${ar(a)} − ${ar(b)} = ؟", "$a ناقص $b يساوي كام؟", numChoices(ans, 0, 20), ar(ans))
}

private const val BIGGER = "أكبر من  >"
private const val SMALLER = "أصغر من  <"
private const val EQUAL = "يساوي  ="

private fun compareQ(): MQ {
    val a = (0..99).random()
    val b = if ((0..6).random() == 0) a else (0..99).random()
    val ans = when { a > b -> BIGGER; a < b -> SMALLER; else -> EQUAL }
    return MQ("", "${ar(a)}   ؟   ${ar(b)}", "$a و $b. أكبر ولا أصغر ولا يساوي؟", listOf(BIGGER, SMALLER, EQUAL), ans)
}

private fun missingQ(): MQ {
    val step = listOf(1, 1, 2, 5, 10).random()
    val start = (0..40).random()
    val seq = List(4) { start + step * it }
    val hide = (1..3).random()
    val ans = seq[hide]
    val shown = seq.mapIndexed { i, v -> if (i == hide) "؟" else ar(v) }.joinToString("،  ")
    return MQ("", shown, "إيه الرقم الناقص؟", numChoices(ans, 0, 120), ar(ans))
}

private fun placeQ(): MQ {
    val n = (11..99).random()
    return if ((0..1).random() == 0) {
        MQ("🔟", "العدد ${ar(n)} فيه كام عشرة؟", "العدد $n فيه كام عشرة؟", numChoices(n / 10, 1, 9), ar(n / 10))
    } else {
        MQ("1️⃣", "العدد ${ar(n)} فيه كام آحاد؟", "العدد $n فيه كام آحاد؟", numChoices(n % 10, 0, 9), ar(n % 10))
    }
}

private val THINGS = listOf("بالونات" to "🎈", "تفاحات" to "🍎", "كور" to "⚽", "ورود" to "🌹", "بسكوتات" to "🍪")

private fun wordQ(): MQ {
    val (word, emoji) = THINGS.random()
    return if ((0..1).random() == 0) {
        val a = (1..10).random(); val b = (1..10).random()
        MQ(emoji, "مع جودي ${ar(a)} $word، وجابت ${ar(b)} كمان. بقى معاها كام؟",
            "مع جودي $a $word، وجابت $b كمان. بقى معاها كام؟", numChoices(a + b, 0, 20), ar(a + b))
    } else {
        val a = (2..15).random(); val b = (1..a).random()
        MQ(emoji, "مع جودي ${ar(a)} $word، وادّت صاحبتها ${ar(b)}. فضل معاها كام؟",
            "مع جودي $a $word، وادّت صاحبتها $b. فضل معاها كام؟", numChoices(a - b, 0, 20), ar(a - b))
    }
}

private val CLOCKS = listOf("🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛")

private fun clockQ(): MQ {
    val h = (1..12).random()
    return MQ(CLOCKS[h - 1], "الساعة كام؟", "الساعة كام؟", numChoices(h, 1, 12), ar(h))
}

@Composable
fun MathHub(vm: AppViewModel) {
    Hub(
        vm, "حساب",
        listOf(
            HubCard("➕", "جمع", Sky) { QuizRunner(vm, glyphSize = 28) { addQ() } },
            HubCard("➖", "طرح", Peach) { QuizRunner(vm, glyphSize = 28) { subQ() } },
            HubCard("⚖️", "أكبر وأصغر", Mint) { QuizRunner(vm) { compareQ() } },
            HubCard("🔢", "الرقم الناقص", Sun) { QuizRunner(vm) { missingQ() } },
            HubCard("🔟", "عشرات وآحاد", Lilac) { QuizRunner(vm, glyphSize = 60) { placeQ() } },
            HubCard("📖", "مسائل", Pink) { QuizRunner(vm, glyphSize = 60) { wordQ() } },
            HubCard("🕒", "الساعة", Sky) { QuizRunner(vm, glyphSize = 100) { clockQ() } }
        )
    )
}

/* ============================== Science ============================== */

@Composable
fun ScienceHub(vm: AppViewModel) {
    Hub(
        vm, "علوم",
        listOf(
            HubCard("👀", "الحواس", Sky) {
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("📖", "بنشوف بإيه؟", "العين 👁️"),
                        MapItem("🎵", "بنسمع بإيه؟", "الودن 👂"),
                        MapItem("🌹", "بنشم بإيه؟", "الأنف 👃"),
                        MapItem("🍭", "بنحس بالطعم بإيه؟", "اللسان 👅"),
                        MapItem("🧸", "بنلمس بإيه؟", "الإيد ✋")
                    ),
                    listOf("العين 👁️", "الودن 👂", "الأنف 👃", "اللسان 👅", "الإيد ✋")
                )
            },
            HubCard("🌱", "حي ولا لأ؟", Mint) {
                val alive = "حي 🌱"; val notAlive = "مش حي 🪨"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🐱", "القطة حي ولا مش حي؟", alive), MapItem("🌳", "الشجرة حي ولا مش حي؟", alive),
                        MapItem("🪨", "الحجر حي ولا مش حي؟", notAlive), MapItem("🚗", "العربية حي ولا مش حي؟", notAlive),
                        MapItem("🐟", "السمكة حي ولا مش حي؟", alive), MapItem("⚽", "الكورة حي ولا مش حي؟", notAlive),
                        MapItem("🌻", "الوردة حي ولا مش حي؟", alive), MapItem("📚", "الكتاب حي ولا مش حي؟", notAlive)
                    ),
                    listOf(alive, notAlive)
                )
            },
            HubCard("🌊", "بيعيش فين؟", Peach) {
                val sea = "البحر 🌊"; val forest = "الغابة 🌳"; val farm = "المزرعة 🏡"; val desert = "الصحراء 🏜️"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🐟", "السمكة بتعيش فين؟", sea), MapItem("🐙", "الأخطبوط بيعيش فين؟", sea),
                        MapItem("🐘", "الفيل بيعيش فين؟", forest), MapItem("🦁", "الأسد بيعيش فين؟", forest),
                        MapItem("🐮", "البقرة بتعيش فين؟", farm), MapItem("🐔", "الفرخة بتعيش فين؟", farm),
                        MapItem("🐪", "الجمل بيعيش فين؟", desert)
                    ),
                    listOf(sea, forest, farm, desert)
                )
            },
            HubCard("💧", "النبات محتاج؟", Lilac) {
                val yes = "آه ✅"; val no = "لأ ❌"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("💧", "النبات محتاج مية؟", yes), MapItem("☀️", "النبات محتاج شمس؟", yes),
                        MapItem("🌬️", "النبات محتاج هوا؟", yes), MapItem("🍫", "النبات محتاج شوكولاتة؟", no),
                        MapItem("🪨", "النبات بياكل حجر؟", no)
                    ),
                    listOf(yes, no)
                )
            },
            HubCard("🍂", "الفصول", Pink) {
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🏖️", "ده فصل إيه؟", "الصيف"), MapItem("⛄", "ده فصل إيه؟", "الشتا"),
                        MapItem("🌸", "ده فصل إيه؟", "الربيع"), MapItem("🍂", "ده فصل إيه؟", "الخريف")
                    ),
                    listOf("الصيف", "الشتا", "الربيع", "الخريف")
                )
            },
            HubCard("🥦", "أكل صحي", Mint) {
                val good = "صحي 💪"; val bad = "مش صحي 🍬"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🍎", "التفاحة صحي ولا مش صحي؟", good), MapItem("🥕", "الجزر صحي ولا مش صحي؟", good),
                        MapItem("🍭", "اللوليبوب صحي ولا مش صحي؟", bad), MapItem("🥛", "اللبن صحي ولا مش صحي؟", good),
                        MapItem("🍟", "الشيبسي صحي ولا مش صحي؟", bad), MapItem("🥦", "البروكلي صحي ولا مش صحي؟", good),
                        MapItem("🥤", "المشروب الغازي صحي ولا مش صحي؟", bad)
                    ),
                    listOf(good, bad)
                )
            },
            HubCard("🌗", "نهار وليل", Sky) {
                val day = "نهار ☀️"; val night = "ليل 🌙"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("☀️", "الشمس بتطلع في النهار ولا الليل؟", day), MapItem("🌙", "القمر بيظهر في النهار ولا الليل؟", night),
                        MapItem("⭐", "النجوم بتظهر في النهار ولا الليل؟", night), MapItem("🛏️", "بننام في النهار ولا الليل؟", night),
                        MapItem("🏫", "بنروح المدرسة في النهار ولا الليل؟", day)
                    ),
                    listOf(day, night)
                )
            }
        )
    )
}
