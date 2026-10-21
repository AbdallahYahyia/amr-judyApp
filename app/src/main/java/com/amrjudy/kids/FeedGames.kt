package com.amrjudy.kids

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * "Feed / help" games for Amr. Same idea in three flavours: someone asks, the child gives the right thing.
 * They get harder as the child succeeds: level 1 = shows the word, level 2 = listen only,
 * level 3 = two things at once (or a bigger number).
 */

private fun levelOf(correct: Int) = (1 + correct / 4).coerceAtMost(3)

@Composable
private fun LevelBadge(level: Int) {
    Text(
        "المستوى $level  " + "⭐".repeat(level),
        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun OptionsGrid(options: List<Pair<String, String>>, onPick: (Pair<String, String>) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in options.chunked(3)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (o in row) BigCard(o.first, o.second, Peach, Modifier.weight(1f).height(110.dp), 40.sp) { onPick(o) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun FeedHub(vm: AppViewModel) {
    Hub(
        vm, "أطعم وساعد",
        listOf(
            HubCard("🐰", "أطعم الحيوان", Mint) { FeedAnimal(vm) },
            HubCard("👾", "الوحش الجعان", Pink) { FeedMonster(vm) },
            HubCard("🤝", "مين محتاج إيه؟", Sky) { HelpWho(vm) }
        )
    )
}

/* ---------------- 1) Feed the animal ---------------- */

private class Pet(val emoji: String, val happy: String, val foods: List<Pair<String, String>>)

private val PETS = listOf(
    Pet("🐰", "🥰", listOf("🥕" to "جزرة", "🥬" to "خس")),
    Pet("🐱", "😻", listOf("🐟" to "سمكة", "🥛" to "لبن")),
    Pet("🐶", "😍", listOf("🦴" to "عضمة", "🍖" to "لحمة")),
    Pet("🐵", "🤩", listOf("🍌" to "موزة", "🍎" to "تفاحة")),
    Pet("🐮", "😋", listOf("🌿" to "برسيم", "🌽" to "درة")),
    Pet("🐑", "😊", listOf("🌱" to "حشيش", "🍞" to "عيش")),
    Pet("🐔", "😄", listOf("🌽" to "درة", "🌾" to "حب")),
    Pet("🐴", "😁", listOf("🍎" to "تفاحة", "🥕" to "جزرة")),
    Pet("🐘", "🥳", listOf("🍉" to "بطيخ", "🍌" to "موزة")),
    Pet("🐼", "😆", listOf("🎋" to "بامبو", "🥕" to "جزرة")),
    Pet("🐻", "😍", listOf("🍯" to "عسل", "🐟" to "سمكة")),
    Pet("🐭", "🤭", listOf("🧀" to "جبنة", "🍞" to "عيش")),
    Pet("🐷", "😋", listOf("🥔" to "بطاطس", "🍎" to "تفاحة")),
    Pet("🦆", "😄", listOf("🍞" to "عيش", "🌾" to "حب"))
)
private val ALL_FOODS: List<Pair<String, String>> = PETS.flatMap { it.foods }.distinct()

@Composable
fun FeedAnimal(vm: AppViewModel) {
    var petIdx by remember { mutableStateOf(PETS.indices.random()) }
    var round by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }
    var fed by remember { mutableStateOf(setOf<String>()) }
    var eating by remember { mutableStateOf(false) }
    var oops by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val level = levelOf(correct)
    val pet = PETS[petIdx]
    val wants = remember(petIdx, round, level) { pet.foods.shuffled().take(if (level >= 3) 2 else 1) }
    val options = remember(petIdx, round, level) {
        val others = ALL_FOODS.filter { f -> pet.foods.none { it.second == f.second } }.shuffled()
        (wants + others.take((if (level == 1) 3 else if (level == 2) 4 else 5) - wants.size)).shuffled()
    }
    val wantText = wants.joinToString(" و") { it.second }
    fun ask() = vm.speech.speak("أنا جعان! عايز $wantText", pitch = 1.4f)

    LaunchedEffect(petIdx, round, level) { ask() }

    fun complete(stars: Int) {
        if (eating) return
        eating = true; win = stars; vm.addStars(stars)
        vm.speech.speak("نام نام! شكراً!", pitch = 1.5f)
        scope.launch {
            delay(1900)
            eating = false; fed = emptySet(); correct++
            var n = petIdx
            while (n == petIdx) n = PETS.indices.random()
            petIdx = n; round++
        }
    }

    fun pick(o: Pair<String, String>) {
        if (eating || o.second in fed) return
        if (wants.any { it.second == o.second }) {
            fed = fed + o.second
            if (fed.size >= wants.size) complete(level)
            else vm.speech.speak("حلو! وكمان ${wants.first { it.second !in fed }.second}", pitch = 1.4f)
        } else {
            oops = true
            vm.speech.speak("لأ، أنا عايز $wantText", pitch = 1.4f)
            scope.launch { delay(900); oops = false }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            LevelBadge(level)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Bouncy {
                    Text(
                        when { eating -> pet.happy; oops -> "😕"; else -> pet.emoji },
                        fontSize = 110.sp,
                        modifier = Modifier.then(Modifier)
                    )
                }
            }
            if (level == 1) {
                Text(
                    wants.joinToString("  ") { it.first } + "  عايز $wantText",
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center
                )
            } else {
                BigCard("🔊", "اسمع تاني", Sky, Modifier.size(width = 150.dp, height = 90.dp), 28.sp) { ask() }
            }
            OptionsGrid(options) { pick(it) }
            MicButton(vm, wantText, Modifier.padding(bottom = 8.dp)) { s ->
                if (s > 0) complete(maxOf(s, level))
                else if (s == 0) vm.speech.speak("قول $wantText", pitch = 1.3f)
                else vm.speech.speak("ما سمعتكش، قول تاني", pitch = 1.3f)
            }
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- 2) The hungry monster: give the right number ---------------- */

private val NUMS = listOf("", "واحد", "اتنين", "تلاتة", "أربعة", "خمسة", "ستة", "سبعة", "تمانية", "تسعة", "عشرة")
private val MONSTER_FOOD = listOf(
    "🍪" to "بسكوت", "🍎" to "تفاح", "🍌" to "موز", "⭐" to "نجوم", "🎈" to "بالونات", "🍓" to "فراولة"
)

@Composable
fun FeedMonster(vm: AppViewModel) {
    var round by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }
    var win by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val level = levelOf(correct)
    val max = when (level) { 1 -> 3; 2 -> 5; else -> 8 }
    val target = remember(round, level) { (1..max).random() }
    val food = remember(round) { MONSTER_FOOD.random() }
    var given by remember(round) { mutableStateOf(0) }
    var bonusDone by remember(round) { mutableStateOf(false) }
    val ask = "عايز ${NUMS[target]} ${food.second}"

    LaunchedEffect(round, level) { vm.speech.speak("أنا جعان! $ask", pitch = 0.9f) }

    fun tap() {
        if (done) return
        given++
        vm.speech.speak(NUMS[given], pitch = 1.2f)
        if (given == target) {
            done = true; win = level; vm.addStars(level)
            scope.launch {
                delay(900)
                vm.speech.speak("نام نام! ${NUMS[target]}! شكراً", pitch = 0.9f)
                delay(1900)
                done = false; correct++; round++
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LevelBadge(level)
            Bouncy { Text(if (done) "😋" else "👾", fontSize = 100.sp) }
            if (level < 3) {
                Text(
                    "$ask   ($target)", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    color = Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                BigCard("🔊", "اسمع تاني", Sky, Modifier.size(width = 150.dp, height = 90.dp), 28.sp) {
                    vm.speech.speak("أنا جعان! $ask", pitch = 0.9f)
                }
            }
            Text(
                if (given == 0) "ادّيله بالضغط على الأكل 👇" else food.first.repeat(given),
                fontSize = 30.sp, color = Ink, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                BigCard(food.first, "ادّي", Peach, Modifier.size(width = 150.dp, height = 130.dp), 56.sp) { tap() }
                BigCard("↩️", "", Mint, Modifier.size(64.dp), 26.sp) { if (!done && given > 0) given-- }
            }
            Spacer(Modifier.height(10.dp))
            MicButton(vm, NUMS[target], Modifier.padding(bottom = 12.dp)) { s ->
                if (s > 0 && !bonusDone) { bonusDone = true; vm.addStars(s); win = s; vm.speech.speak("برافو! قلت ${NUMS[target]}", pitch = 1.4f) }
                else if (s == 0) vm.speech.speak("قول ${NUMS[target]}", pitch = 1.3f)
                else if (s < 0) vm.speech.speak("ما سمعتكش، قول تاني", pitch = 1.3f)
            }
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- 3) Who needs what? ---------------- */

private class Need(val emoji: String, val feel: String, val item: Pair<String, String>)

private val NEEDS = listOf(
    Need("😋", "جعان", "🍞" to "عيش"),
    Need("🥵", "حران", "🧊" to "ثلج"),
    Need("🥶", "بردان", "🧥" to "جاكت"),
    Need("😴", "نعسان", "🛏️" to "سرير"),
    Need("🤤", "عطشان", "💧" to "مية"),
    Need("🙌", "وسخان", "🧼" to "صابون"),
    Need("😨", "خايف", "🧸" to "دبدوب"),
    Need("🤩", "عايز ألعب", "⚽" to "كورة"),
    Need("🤓", "عايز أقرا", "📚" to "كتاب")
)

@Composable
fun HelpWho(vm: AppViewModel) {
    var round by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }
    var got by remember { mutableStateOf(setOf<String>()) }
    var done by remember { mutableStateOf(false) }
    var oops by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val level = levelOf(correct)
    val needs = remember(round, level) { NEEDS.shuffled().take(if (level >= 3) 2 else 1) }
    val options = remember(round, level) {
        val others = NEEDS.filter { n -> needs.none { it === n } }.shuffled().map { it.item }
        (needs.map { it.item } + others.take((if (level == 1) 3 else if (level == 2) 4 else 5) - needs.size)).shuffled()
    }
    val sentence = "أنا " + needs.joinToString(" و") { it.feel }
    fun ask() = vm.speech.speak(sentence, pitch = 1.3f)

    LaunchedEffect(round, level) { ask() }

    fun complete(stars: Int) {
        if (done) return
        done = true; win = stars; vm.addStars(stars)
        vm.speech.speak("شكراً! ساعدتني", pitch = 1.4f)
        scope.launch { delay(1900); done = false; got = emptySet(); correct++; round++ }
    }

    fun pick(o: Pair<String, String>) {
        if (done || o.second in got) return
        if (needs.any { it.item.second == o.second }) {
            got = got + o.second
            if (got.size >= needs.size) complete(level)
            else vm.speech.speak("حلو! وكمان؟", pitch = 1.4f)
        } else {
            oops = true
            vm.speech.speak("لأ، $sentence", pitch = 1.3f)
            scope.launch { delay(900); oops = false }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            LevelBadge(level)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Bouncy {
                    Text(
                        if (done) "🥰" else if (oops) "😕" else needs.joinToString(" ") { it.emoji },
                        fontSize = if (needs.size > 1) 80.sp else 110.sp
                    )
                }
            }
            if (level == 1) {
                Text(sentence, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
            } else {
                BigCard("🔊", "اسمع تاني", Sky, Modifier.size(width = 150.dp, height = 90.dp), 28.sp) { ask() }
            }
            OptionsGrid(options) { pick(it) }
            Spacer(Modifier.height(8.dp))
        }
        Celebrate(win) { win = 0 }
    }
}
