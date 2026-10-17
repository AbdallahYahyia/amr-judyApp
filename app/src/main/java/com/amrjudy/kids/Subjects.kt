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


/* ====================== Math (in English, like Judy's school) ====================== */

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
    return s.shuffled().map { it.toString() }
}

private val NUM_WORDS = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"
)

private fun addQ(): MQ {
    val a = (0..10).random(); val b = (0..10).random(); val ans = a + b
    val glyph = if (ans in 1..10) "🍎".repeat(a) + "  ➕  " + "🍏".repeat(b) else ""
    return MQ(glyph, "$a + $b = ?", "$a plus $b equals what?", numChoices(ans, 0, 20), ans.toString())
}

private fun subQ(): MQ {
    val a = (1..20).random(); val b = (0..a).random(); val ans = a - b
    val glyph = if (a <= 10) "🍎".repeat(a) else ""
    return MQ(glyph, "$a − $b = ?", "$a minus $b equals what?", numChoices(ans, 0, 20), ans.toString())
}

private const val BIGGER = "greater than  >"
private const val SMALLER = "less than  <"
private const val EQUAL = "equal to  ="

private fun compareQ(): MQ {
    val a = (0..99).random()
    val b = if ((0..6).random() == 0) a else (0..99).random()
    val ans = when { a > b -> BIGGER; a < b -> SMALLER; else -> EQUAL }
    return MQ(
        "", "$a   ?   $b", "$a and $b. Is $a greater than, less than, or equal to $b?",
        listOf(BIGGER, SMALLER, EQUAL), ans
    )
}

private fun missingQ(): MQ {
    val step = listOf(1, 1, 2, 5, 10).random()
    val start = (0..40).random()
    val seq = List(4) { start + step * it }
    val hide = (1..3).random()
    val ans = seq[hide]
    val shown = seq.mapIndexed { i, v -> if (i == hide) "?" else v.toString() }.joinToString(",  ")
    return MQ("", shown, "What is the missing number?", numChoices(ans, 0, 120), ans.toString())
}

private fun beforeAfterQ(): MQ = when ((0..2).random()) {
    0 -> {
        val n = (0..98).random()
        MQ("", "What comes after $n?", "What number comes after $n?", numChoices(n + 1, 0, 100), (n + 1).toString())
    }
    1 -> {
        val n = (2..100).random()
        MQ("", "What comes before $n?", "What number comes before $n?", numChoices(n - 1, 0, 100), (n - 1).toString())
    }
    else -> {
        val n = (0..98).random()
        MQ("", "$n,  ?,  ${n + 2}", "What number comes between $n and ${n + 2}?", numChoices(n + 1, 0, 100), (n + 1).toString())
    }
}

private fun placeQ(): MQ {
    val n = (11..99).random()
    return if ((0..1).random() == 0) {
        MQ("🔟", "How many tens are in $n?", "How many tens are in $n?", numChoices(n / 10, 1, 9), (n / 10).toString())
    } else {
        MQ("1️⃣", "How many ones are in $n?", "How many ones are in $n?", numChoices(n % 10, 0, 9), (n % 10).toString())
    }
}

private fun make10Q(): MQ {
    val a = (0..10).random()
    return MQ(
        "🔟", "$a + ? = 10", "$a plus what equals ten?",
        numChoices(10 - a, 0, 10), (10 - a).toString()
    )
}

private val THINGS = listOf("balloons" to "🎈", "apples" to "🍎", "balls" to "⚽", "roses" to "🌹", "cookies" to "🍪")

private fun wordQ(): MQ {
    val (word, emoji) = THINGS.random()
    return if ((0..1).random() == 0) {
        val a = (1..10).random(); val b = (1..10).random()
        val t = "Judy has $a $word and gets $b more. How many does she have now?"
        MQ(emoji, t, t, numChoices(a + b, 0, 20), (a + b).toString())
    } else {
        val a = (2..15).random(); val b = (1..a).random()
        val t = "Judy has $a $word and gives $b away. How many are left?"
        MQ(emoji, t, t, numChoices(a - b, 0, 20), (a - b).toString())
    }
}

private val CLOCKS = listOf("🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛")
private val HALF_CLOCKS = listOf("🕜", "🕝", "🕞", "🕟", "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦", "🕧")

private fun clockLabel(h: Int, half: Boolean) = if (half) "half past $h" else "$h o'clock"

private fun clockQ(): MQ {
    val half = (0..1).random() == 1
    val h = (1..12).random()
    val ans = clockLabel(h, half)
    val others = (1..12).filter { it != h }.shuffled().take(3).map { clockLabel(it, half) }
    return MQ(if (half) HALF_CLOCKS[h - 1] else CLOCKS[h - 1], "What time is it?", "What time is it?", (others + ans).shuffled(), ans)
}

private fun numberWordsQ(): MQ {
    val n = (0..20).random()
    return if ((0..1).random() == 0) {
        MQ("", "Which number is \"${NUM_WORDS[n]}\"?", "Which number is ${NUM_WORDS[n]}?", numChoices(n, 0, 20), n.toString())
    } else {
        MQ(
            "", "$n", "How do we write this number in words?",
            numChoices(n, 0, 20).map { NUM_WORDS[it.toInt()] }, NUM_WORDS[n]
        )
    }
}

private val COUNT_THINGS = listOf("🍎", "⭐", "🐟", "🎈", "🚗", "🌸", "🐥")

private fun countQ(): MQ {
    val n = (1..20).random()
    return MQ(COUNT_THINGS.random().repeat(n), "How many?", "How many are there?", numChoices(n, 1, 20), n.toString())
}

private val SHAPES = listOf(
    "🔵" to "circle", "🟩" to "square", "🔺" to "triangle", "⭐" to "star", "❤️" to "heart", "🔶" to "diamond"
)

private fun shapeQ(): MQ {
    val s = SHAPES.random()
    return MQ(
        s.first, "What shape is this?", "What shape is this?",
        (listOf(s.second) + SHAPES.filter { it !== s }.shuffled().take(3).map { it.second }).shuffled(), s.second
    )
}

@Composable
fun MathHub(vm: AppViewModel) {
    Hub(
        vm, "Math",
        listOf(
            HubCard("➕", "Addition", Sky) { QuizRunner(vm, en = true, glyphSize = 28) { addQ() } },
            HubCard("➖", "Subtraction", Peach) { QuizRunner(vm, en = true, glyphSize = 28) { subQ() } },
            HubCard("⚖️", "Greater / Less", Mint) { QuizRunner(vm, en = true) { compareQ() } },
            HubCard("🔢", "Missing Number", Sun) { QuizRunner(vm, en = true) { missingQ() } },
            HubCard("↔️", "Before & After", Pink) { QuizRunner(vm, en = true) { beforeAfterQ() } },
            HubCard("🔟", "Tens & Ones", Lilac) { QuizRunner(vm, en = true, glyphSize = 60) { placeQ() } },
            HubCard("🎯", "Make 10", Sky) { QuizRunner(vm, en = true, glyphSize = 60) { make10Q() } },
            HubCard("📖", "Word Problems", Pink) { QuizRunner(vm, en = true, glyphSize = 60) { wordQ() } },
            HubCard("🕒", "Clock", Mint) { QuizRunner(vm, en = true, glyphSize = 100) { clockQ() } },
            HubCard("🔺", "Shapes", Peach) { QuizRunner(vm, en = true, glyphSize = 100) { shapeQ() } },
            HubCard("🔤", "Number Words", Sun) { QuizRunner(vm, en = true) { numberWordsQ() } },
            HubCard("🧮", "Counting", Lilac) { QuizRunner(vm, en = true, glyphSize = 30) { countQ() } }
        )
    )
}

/* ====================== Science (in English, like Judy's school) ====================== */

@Composable
fun ScienceHub(vm: AppViewModel) {
    Hub(
        vm, "Science",
        listOf(
            HubCard("👀", "Five Senses", Sky) {
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("📖", "What do we see with?", "eyes 👁️"),
                        MapItem("🎵", "What do we hear with?", "ears 👂"),
                        MapItem("🌹", "What do we smell with?", "nose 👃"),
                        MapItem("🍭", "What do we taste with?", "tongue 👅"),
                        MapItem("🧸", "What do we touch with?", "hands ✋")
                    ),
                    listOf("eyes 👁️", "ears 👂", "nose 👃", "tongue 👅", "hands ✋"), en = true
                )
            },
            HubCard("🧒", "My Body", Pink) {
                val head = "head"; val arms = "arms"; val legs = "legs"; val hands = "hands"; val feet = "feet"; val eyes = "eyes"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🧒", "Which part do we think with?", head),
                        MapItem("🏃", "Which part helps us to run and jump?", legs),
                        MapItem("✏️", "Which part do we write with?", hands),
                        MapItem("🤗", "Which part do we use to hug?", arms),
                        MapItem("👟", "Which part do we wear shoes on?", feet),
                        MapItem("📖", "Which part do we read with?", eyes)
                    ),
                    listOf(head, arms, legs, hands, feet, eyes), en = true
                )
            },
            HubCard("🧠", "Body Organs", Lilac) {
                val brain = "brain"; val heart = "heart"; val lungs = "lungs"; val stomach = "stomach"; val bones = "bones"; val muscles = "muscles"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🧠", "Which organ helps us think?", brain),
                        MapItem("❤️", "Which organ pumps blood?", heart),
                        MapItem("🌬️", "Which organs help us breathe?", lungs),
                        MapItem("🍽️", "Where does our food go to be digested?", stomach),
                        MapItem("🦴", "What holds our body up?", bones),
                        MapItem("💪", "What helps us move?", muscles)
                    ),
                    listOf(brain, heart, lungs, stomach, bones, muscles), en = true
                )
            },
            HubCard("❤️", "Heart & Blood", Pink) {
                val heart = "heart"; val vessels = "blood vessels"; val blood = "blood"; val lungs = "lungs"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("❤️", "What pumps blood around the body?", heart),
                        MapItem("🩸", "What tubes carry blood around the body?", vessels),
                        MapItem("🩸", "What is the red liquid that flows inside our body?", blood),
                        MapItem("🌬️", "Which organs give the blood fresh air (oxygen)?", lungs),
                        MapItem("🔄", "What goes around and around in our circulation?", blood)
                    ),
                    listOf(heart, vessels, blood, lungs), en = true
                )
            },
            HubCard("💓", "Heart Facts", Sun) {
                val yes = "yes ✅"; val no = "no ❌"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🏃", "Does your heart beat faster when you run?", yes),
                        MapItem("😴", "Does your heart stop when you sleep?", no),
                        MapItem("💪", "Is the heart a strong muscle?", yes),
                        MapItem("✋", "Can you feel your pulse in your wrist?", yes),
                        MapItem("🏠", "Is your heart as big as a house?", no),
                        MapItem("🩸", "Does blood travel all around your body?", yes),
                        MapItem("🛌", "Does your heart beat slower when you rest?", yes)
                    ),
                    listOf(yes, no), en = true
                )
            },
            HubCard("🪥", "Healthy Habits", Mint) {
                val yes = "yes ✅"; val no = "no ❌"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🪥", "Should we brush our teeth every day?", yes),
                        MapItem("🧼", "Should we wash our hands before eating?", yes),
                        MapItem("🍬", "Is it good to eat sweets all day?", no),
                        MapItem("🏃", "Is exercise good for our heart?", yes),
                        MapItem("😴", "Do we need sleep to grow strong?", yes),
                        MapItem("📺", "Is it good to watch TV all day?", no)
                    ),
                    listOf(yes, no), en = true
                )
            },
            HubCard("🌱", "Living or Not", Mint) {
                val alive = "living 🌱"; val notAlive = "non-living 🪨"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🐱", "Is a cat living or non-living?", alive), MapItem("🌳", "Is a tree living or non-living?", alive),
                        MapItem("🪨", "Is a rock living or non-living?", notAlive), MapItem("🚗", "Is a car living or non-living?", notAlive),
                        MapItem("🐟", "Is a fish living or non-living?", alive), MapItem("⚽", "Is a ball living or non-living?", notAlive),
                        MapItem("🌻", "Is a flower living or non-living?", alive), MapItem("📚", "Is a book living or non-living?", notAlive)
                    ),
                    listOf(alive, notAlive), en = true
                )
            },
            HubCard("🌊", "Where Do They Live?", Peach) {
                val sea = "sea 🌊"; val forest = "forest 🌳"; val farm = "farm 🏡"; val desert = "desert 🏜️"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🐟", "Where does a fish live?", sea), MapItem("🐙", "Where does an octopus live?", sea),
                        MapItem("🐘", "Where does an elephant live?", forest), MapItem("🦁", "Where does a lion live?", forest),
                        MapItem("🐮", "Where does a cow live?", farm), MapItem("🐔", "Where does a hen live?", farm),
                        MapItem("🐪", "Where does a camel live?", desert)
                    ),
                    listOf(sea, forest, farm, desert), en = true
                )
            },
            HubCard("🌿", "Parts of a Plant", Lilac) {
                val root = "roots"; val stem = "stem"; val leaf = "leaves"; val flower = "flower"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🌱", "Which part takes water from the soil?", root),
                        MapItem("🌿", "Which part holds the plant up?", stem),
                        MapItem("🍃", "Which part makes food from sunlight?", leaf),
                        MapItem("🌸", "Which part makes the seeds and fruit?", flower)
                    ),
                    listOf(root, stem, leaf, flower), en = true
                )
            },
            HubCard("💧", "Plants Need", Pink) {
                val yes = "yes ✅"; val no = "no ❌"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("💧", "Do plants need water?", yes), MapItem("☀️", "Do plants need sunlight?", yes),
                        MapItem("🌬️", "Do plants need air?", yes), MapItem("🍫", "Do plants need chocolate?", no),
                        MapItem("🪨", "Do plants eat rocks?", no)
                    ),
                    listOf(yes, no), en = true
                )
            },
            HubCard("🌦️", "Weather", Sky) {
                val sunny = "sunny"; val rainy = "rainy"; val cloudy = "cloudy"; val windy = "windy"; val snowy = "snowy"; val stormy = "stormy"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("☀️", "What is the weather?", sunny), MapItem("🌧️", "What is the weather?", rainy),
                        MapItem("☁️", "What is the weather?", cloudy), MapItem("🌬️", "What is the weather?", windy),
                        MapItem("❄️", "What is the weather?", snowy), MapItem("⛈️", "What is the weather?", stormy)
                    ),
                    listOf(sunny, rainy, cloudy, windy, snowy, stormy), en = true
                )
            },
            HubCard("🍂", "Seasons", Peach) {
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🏖️", "Which season is this?", "summer"), MapItem("⛄", "Which season is this?", "winter"),
                        MapItem("🌸", "Which season is this?", "spring"), MapItem("🍂", "Which season is this?", "autumn")
                    ),
                    listOf("summer", "winter", "spring", "autumn"), en = true
                )
            },
            HubCard("🥦", "Healthy Food", Mint) {
                val good = "healthy 💪"; val bad = "unhealthy 🍬"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🍎", "Is an apple healthy or unhealthy?", good), MapItem("🥕", "Is a carrot healthy or unhealthy?", good),
                        MapItem("🍭", "Is a lollipop healthy or unhealthy?", bad), MapItem("🥛", "Is milk healthy or unhealthy?", good),
                        MapItem("🍟", "Are chips healthy or unhealthy?", bad), MapItem("🥦", "Is broccoli healthy or unhealthy?", good),
                        MapItem("🥤", "Is soda healthy or unhealthy?", bad)
                    ),
                    listOf(good, bad), en = true
                )
            },
            HubCard("🌗", "Day & Night", Lilac) {
                val day = "day ☀️"; val night = "night 🌙"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("☀️", "Does the sun shine in the day or at night?", day),
                        MapItem("🌙", "Do we see the moon in the day or at night?", night),
                        MapItem("⭐", "Do we see the stars in the day or at night?", night),
                        MapItem("🛏️", "Do we sleep in the day or at night?", night),
                        MapItem("🏫", "Do we go to school in the day or at night?", day)
                    ),
                    listOf(day, night), en = true
                )
            },
            HubCard("🐣", "Animal Babies", Pink) {
                val calf = "calf"; val chick = "chick"; val puppy = "puppy"; val kitten = "kitten"; val lamb = "lamb"; val duckling = "duckling"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🐮", "What is a baby cow called?", calf), MapItem("🐔", "What is a baby hen called?", chick),
                        MapItem("🐶", "What is a baby dog called?", puppy), MapItem("🐱", "What is a baby cat called?", kitten),
                        MapItem("🐑", "What is a baby sheep called?", lamb), MapItem("🦆", "What is a baby duck called?", duckling)
                    ),
                    listOf(calf, chick, puppy, kitten, lamb, duckling), en = true
                )
            },
            HubCard("🧱", "Hard or Soft", Sun) {
                val hard = "hard 🪨"; val soft = "soft ☁️"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🛏️", "Is a pillow hard or soft?", soft), MapItem("🪨", "Is a rock hard or soft?", hard),
                        MapItem("🧸", "Is a teddy bear hard or soft?", soft), MapItem("🪵", "Is wood hard or soft?", hard),
                        MapItem("🧶", "Is wool hard or soft?", soft), MapItem("🪟", "Is glass hard or soft?", hard)
                    ),
                    listOf(hard, soft), en = true
                )
            },
            HubCard("🦅", "How Animals Move", Mint) {
                val fly = "fly"; val swim = "swim"; val walk = "walk"; val crawl = "crawl"
                MapQuiz(
                    vm,
                    listOf(
                        MapItem("🦅", "How does a bird move?", fly), MapItem("🦋", "How does a butterfly move?", fly),
                        MapItem("🐟", "How does a fish move?", swim), MapItem("🐬", "How does a dolphin move?", swim),
                        MapItem("🐶", "How does a dog move?", walk), MapItem("🐘", "How does an elephant move?", walk),
                        MapItem("🐍", "How does a snake move?", crawl), MapItem("🐛", "How does a caterpillar move?", crawl)
                    ),
                    listOf(fly, swim, walk, crawl), en = true
                )
            }
        )
    )
}
