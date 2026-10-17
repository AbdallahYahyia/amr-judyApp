package com.amrjudy.kids

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Communication & emotions games for Amr.
 * They use common early-intervention ideas as *play*: taking turns, asking for what you want,
 * naming feelings, calming down, and "first this, then that" rewards.
 * They support, but do not replace, a speech-language therapist.
 */

private val EMOTIONS = listOf(
    "😊" to "مبسوط", "😢" to "زعلان", "😠" to "متضايق", "😨" to "خايف", "😴" to "نعسان", "😮" to "متفاجئ"
)

// Built once (not inside the composable) so the quiz keeps its state across recompositions.
private val EMOTION_ITEMS = EMOTIONS.map { QItem(it.first, it.second, Ink, "مين ${it.second}؟") }

@Composable
fun CommHub(vm: AppViewModel) {
    Hub(
        vm, "تواصل ومشاعر",
        listOf(
            HubCard("🔁", "دور دبدوب ودورك", Sky) { TurnTaking(vm) },
            HubCard("🙋", "اطلب اللي عايزه", Peach) { Requesting(vm) },
            HubCard("😊", "فين الشعور؟", Sun) { QuizBody(vm, EMOTION_ITEMS, 80) },
            HubCard("🗣️", "قول الشعور", Pink) {
                SayItScreen(vm, EMOTIONS.map { SayItItem(it.first, it.second) }, manual = true, prompt = "هو حاسس بإيه؟")
            },
            HubCard("🌬️", "هدّي مع دبدوب", Mint) { Breathing(vm) },
            HubCard("🥇", "الأول وبعدين", Lilac) { FirstThen(vm) }
        )
    )
}

/** Turn-taking: the bear makes a sound, then it is the child's turn to make a sound. */
@Composable
private fun TurnTaking(vm: AppViewModel) {
    val sounds = remember { listOf("با", "ما", "دا", "أو", "آه").shuffled() }
    var round by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf("bear") }
    var msg by remember { mutableStateOf("") }
    var win by remember { mutableStateOf(0) }

    LaunchedEffect(round, phase) {
        if (round >= sounds.size) {
            msg = "خلصنا! برافو عليك 🎉"
            vm.speech.speak("خلصنا! برافو عليك")
        } else if (phase == "bear") {
            msg = "دور دبدوب"
            vm.speech.speak(sounds[round], pitch = 1.3f, rate = 0.6f)
            delay(2200)
            phase = "you"
        } else {
            msg = "دورك أنت! 👆"
            vm.speech.speak("دورك")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            if (round >= sounds.size) {
                Bouncy { Text("🎉", fontSize = 120.sp) }
                Text(msg, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(16.dp))
                BigCard("🔄", "من الأول", Mint, Modifier.size(width = 200.dp, height = 110.dp), 36.sp) {
                    round = 0; phase = "bear"
                }
            } else {
                Bouncy { Text(if (phase == "bear") "🐻" else "🧒", fontSize = 120.sp) }
                if (phase == "bear") Text(sounds[round], fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(msg, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                if (phase == "you") {
                    SoundMicButton(vm) { s ->
                        if (s < 0) msg = "الميكروفون مش شغال، اسمح له من إعدادات التطبيق"
                        else if (s > 0) { win = reward(vm, s); round++; phase = "bear" }
                        else { msg = "جرّب تاني بصوت أعلى"; vm.speech.speak("جرّب تاني") }
                    }
                    Spacer(Modifier.height(10.dp))
                    BigCard("⏭️", "", Peach, Modifier.size(64.dp), 26.sp) { round++; phase = "bear" }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

private val REQUESTS = listOf(
    "🥛" to "لبن", "💧" to "مية", "🍞" to "عيش", "🍎" to "تفاحة", "🍌" to "موزة", "⚽" to "كورة", "🎈" to "بالونة"
)

/** Requesting: the child chooses what they want (tap or say it) and gets it – a natural reward. */
@Composable
private fun Requesting(vm: AppViewModel) {
    val kid = vm.profile?.nameAr ?: "حبيبي"
    var pair by remember { mutableStateOf(REQUESTS.shuffled().take(2)) }
    var got by remember { mutableStateOf<Pair<String, String>?>(null) }
    var listening by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pair) { vm.speech.speak("عايز إيه يا $kid؟ ${pair[0].second} ولا ${pair[1].second}؟", pitch = 1.3f) }

    fun give(item: Pair<String, String>, stars: Int) {
        if (got != null) return
        got = item; win = stars; vm.addStars(stars)
        vm.speech.speak("اتفضل يا $kid! ${item.second}. نام نام", pitch = 1.4f)
        scope.launch { delay(2200); win = 0; got = null; pair = REQUESTS.shuffled().take(2) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val g = got
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (g == null) Bouncy { Text("🐻❓", fontSize = 90.sp) }
                else Bouncy { Text("🧒 ${g.first}", fontSize = 90.sp) }
            }
            Text(
                if (g == null) "عايز إيه؟" else "اتفضل! ${g.second}",
                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (item in pair) {
                    BigCard(item.first, item.second, Peach, Modifier.weight(1f).height(150.dp), 60.sp) { give(item, 1) }
                }
            }
            BigCard(
                if (listening) "👂" else "🎤", if (listening) "اتكلم!" else "قول اسمها",
                if (listening) Pink else Mint, Modifier.size(width = 200.dp, height = 110.dp), 36.sp
            ) {
                if (!listening && got == null) {
                    vm.speech.stopSpeaking()
                    listening = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        vm.speech.listen(
                            onResult = { l ->
                                listening = false
                                val sa = Fuzzy.score(pair[0].second, l)
                                val sb = Fuzzy.score(pair[1].second, l)
                                if (maxOf(sa, sb) == 0) vm.speech.speak("قول اسمها", pitch = 1.3f)
                                else give(if (sa >= sb) pair[0] else pair[1], 1 + maxOf(sa, sb))
                            },
                            onFail = { listening = false; vm.speech.speak("ما سمعتكش، قول تاني", pitch = 1.3f) }
                        )
                    }, 500)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Celebrate(win) { win = 0 }
    }
}

/** Calming breathing with the bear: it grows when we breathe in and shrinks when we breathe out. */
@Composable
private fun Breathing(vm: AppViewModel) {
    var inhale by remember { mutableStateOf(false) }
    var cycles by remember { mutableStateOf(0) }
    val s by animateFloatAsState(if (inhale) 1.35f else 0.8f, tween(4000), label = "breath")

    LaunchedEffect(Unit) {
        while (true) {
            inhale = true
            vm.speech.speak("شهيق", pitch = 1.0f, rate = 0.6f)
            delay(4000)
            inhale = false
            vm.speech.speak("زفير", pitch = 1.0f, rate = 0.6f)
            delay(4000)
            cycles++
            if (cycles == 5) {
                vm.addStars(1)
                vm.speech.speak("برافو! اتهدّينا", pitch = 1.2f)
                delay(2500)
            }
        }
    }
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Text("🐻", fontSize = 130.sp, modifier = Modifier.scale(s))
        }
        Text(
            if (inhale) "شهيق... 🌬️" else "زفير... 💨",
            fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink
        )
        Text("خد نفس معايا ببطء", fontSize = 18.sp, color = Ink, modifier = Modifier.padding(top = 8.dp))
    }
}

private val FT_WORDS = listOf(
    "💧" to "مية", "🥛" to "لبن", "🍞" to "عيش", "🚗" to "عربية", "👩" to "ماما", "👨" to "بابا", "⚽" to "كورة"
)
private val FT_REWARDS = listOf("🎈 بالونة", "🎁 هدية", "🎉 حفلة", "⭐ نجمة كبيرة", "🌈 قوس قزح")

/** "First say it, then you get the surprise": a visual first–then board that rewards effort. */
@Composable
private fun FirstThen(vm: AppViewModel) {
    var idx by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val item = FT_WORDS[idx % FT_WORDS.size]
    val reward = FT_REWARDS[idx % FT_REWARDS.size]

    LaunchedEffect(idx) { vm.speech.speak("الأول قول ${item.second}. بعدين مفاجأة!", pitch = 1.3f) }

    fun finish(stars: Int) {
        if (done) return
        done = true; win = stars; vm.addStars(stars)
        vm.speech.speak("برافو! اتفضل المفاجأة", pitch = 1.4f)
        scope.launch { delay(2600); done = false; win = 0; idx++ }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الأول", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    BigCard(item.first, item.second, Sky, Modifier.size(width = 140.dp, height = 170.dp), 64.sp) {
                        vm.sayWord(item.second)
                    }
                }
                Text("➡️", fontSize = 36.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("بعدين", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    BigCard(
                        if (done) reward.substringBefore(" ") else "❔",
                        if (done) reward.substringAfter(" ") else "مفاجأة",
                        if (done) Sun else Color(0xFFE9ECEF), Modifier.size(width = 140.dp, height = 170.dp), 64.sp
                    ) { }
                }
            }
            Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MicButton(vm, item.second) { s ->
                    if (s > 0) finish(s + 1)
                    else if (s == 0) vm.speech.speak("قول ${item.second}", pitch = 1.3f)
                    else vm.speech.speak("ما سمعتكش، قول تاني", pitch = 1.3f)
                }
                BigCard("👏", "", Mint, Modifier.size(96.dp), 40.sp) { finish(1) }
            }
        }
        Celebrate(win) { win = 0 }
    }
}
