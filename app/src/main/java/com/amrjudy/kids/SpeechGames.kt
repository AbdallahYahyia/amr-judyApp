package com.amrjudy.kids

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

/** Big microphone button. Reports 0..3 stars from the fuzzy matcher, or -1 if nothing was heard. */
@Composable
fun MicButton(
    vm: AppViewModel,
    target: String,
    modifier: Modifier = Modifier,
    lang: String = "ar-EG",
    onScore: (Int) -> Unit
) {
    var listening by remember { mutableStateOf(false) }
    var level by remember { mutableStateOf(0f) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val pulse by animateFloatAsState(1f + level * 0.35f, label = "micPulse")
    val isAmr = vm.profile == Profile.AMR

    fun finish(score: Int) { listening = false; level = 0f; onScore(score) }

    // The phone's recogniser is missing / has no Arabic: just detect that the child made a sound.
    fun soundFallback() {
        Thread {
            val ms = try { SoundDetector().listen(5) { l -> handler.post { level = l } } } catch (e: Exception) { -1 }
            handler.post { finish(soundStars(ms)) }
        }.start()
    }

    BigCard(
        if (listening) "👂" else "🎤", "", if (listening) Pink else Mint,
        modifier.size(96.dp).scale(pulse), 44.sp
    ) {
        if (!listening) {
            if (!vm.hasMic()) {
                vm.askMic()
                onScore(-1)
            } else {
                vm.speech.stopSpeaking()          // don't let the recogniser hear our own voice
                listening = true
                handler.postDelayed({
                    vm.speech.listen(
                        onResult = { heard ->
                            val sc = Fuzzy.score(target, heard)
                            // For a 3-year-old with delayed speech every attempt counts.
                            finish(if (isAmr) maxOf(sc, 1) else sc)
                        },
                        onFail = { code ->
                            when (code) {
                                android.speech.SpeechRecognizer.ERROR_NO_MATCH -> finish(if (isAmr) 1 else 0)
                                android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> finish(-1)
                                else -> soundFallback()
                            }
                        },
                        onRms = { db -> level = ((db + 2f) / 12f).coerceIn(0f, 1f) },
                        lang = lang
                    )
                }, 400)
            }
        }
    }
}

/** Shared reward logic: stars + spoken cheer. Returns stars to show in confetti. */
fun reward(vm: AppViewModel, score: Int): Int {
    return when {
        score >= 3 -> { vm.addStars(3); vm.speech.speak("ممتاز! برافو!", pitch = 1.4f); 3 }
        score == 2 -> { vm.addStars(2); vm.speech.speak("حلو أوي! برافو", pitch = 1.4f); 2 }
        score == 1 -> { vm.addStars(1); vm.speech.speak("شاطر! كمان مرة", pitch = 1.4f); 1 }
        else -> { vm.speech.speak("جرّب تاني يا بطل", pitch = 1.3f); 0 }
    }
}

@Composable
fun Celebrate(win: Int, onDone: () -> Unit) {
    LaunchedEffect(win) { if (win > 0) { delay(1600); onDone() } }
    Confetti(win)
}

/* ---------------- Feature 1: Speech Arena ---------------- */

private val WORDS = listOf(
    "🚪" to "باب", "👩" to "ماما", "👨" to "بابا", "🐱" to "قطة",
    "🚗" to "عربية", "💧" to "مية", "🍎" to "تفاحة"
)

@Composable
fun SpeechArena(vm: AppViewModel) {
    var idx by remember { mutableStateOf(0) }
    var win by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("اسمع وقول") }
    val (emoji, word) = WORDS[idx]
    LaunchedEffect(idx) { vm.sayWord(word) }
    Box(Modifier.fillMaxSize().background(Cream)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("استوديو النطق", vm.stars) { vm.back() }
            Spacer(Modifier.height(8.dp))
            BigCard(emoji, word, Sun, Modifier.fillMaxWidth(0.9f).weight(1f), 150.sp) { vm.sayWord(word) }
            Text(msg, fontSize = 22.sp, color = Ink, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center)
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
            ) {
                BigCard("⬅️", "", Peach, Modifier.size(80.dp), 32.sp) { idx = (idx + WORDS.size - 1) % WORDS.size; msg = "اسمع وقول" }
                MicButton(vm, word) { s ->
                    if (s < 0) { msg = "ما سمعتش، جرّب تاني 🎤"; vm.speech.speak("ما سمعتكش، قول تاني") }
                    else { win = reward(vm, s); msg = if (s > 0) "برافو!" else "حاول كمان مرة" }
                }
                BigCard("➡️", "", Peach, Modifier.size(80.dp), 32.sp) { idx = (idx + 1) % WORDS.size; msg = "اسمع وقول" }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- Feature 3: Balloon pop ---------------- */

private val BALLOON_COLORS = listOf(
    "أحمر" to Color(0xFFFF6B6B), "أزرق" to Color(0xFF4DABF7), "أصفر" to Color(0xFFFFD43B),
    "أخضر" to Color(0xFF51CF66), "برتقالي" to Color(0xFFFF922B), "بنفسجي" to Color(0xFFB197FC)
)

@Composable
fun BalloonPop(vm: AppViewModel) {
    var target by remember { mutableStateOf(BALLOON_COLORS.random()) }
    var popped by remember { mutableStateOf(setOf<String>()) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(target) { vm.speech.speak("فرقع البالونة ال${target.first}", pitch = 1.3f) }
    Box(Modifier.fillMaxSize().background(Color(0xFFE7F5FF))) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("فرقعة البالونات", vm.stars) { vm.back() }
            Text("فرقع ال${target.first}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = target.second)
            for (row in BALLOON_COLORS.chunked(2)) {
                Row(Modifier.fillMaxWidth().weight(1f).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for ((name, col) in row) {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (name in popped) Text("💥", fontSize = 70.sp)
                            else Bouncy {
                                Box(
                                    Modifier.size(width = 110.dp, height = 130.dp)
                                        .clip(RoundedCornerShape(percent = 50)).background(col)
                                        .clickable {
                                            if (name == target.first) {
                                                popped = popped + name
                                                vm.addStars(1); win = 1
                                                vm.speech.speak("بوم!", pitch = 1.8f)
                                                scope.launch {
                                                    delay(900)
                                                    popped = emptySet()
                                                    target = BALLOON_COLORS.random()
                                                }
                                            } else vm.speech.speak("ده ال$name. دوّر تاني", pitch = 1.3f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Text("🎈", fontSize = 28.sp, color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- Feature 4: Silly voice echo ---------------- */

@Composable
fun VoiceEchoScreen(vm: AppViewModel) {
    val echo = remember { VoiceEcho() }
    var state by remember { mutableStateOf("idle") } // idle | recording | playing
    var has by remember { mutableStateOf(false) }

    fun rec() {
        if (state != "idle") return
        state = "recording"
        Thread {
            try { echo.record(3) } catch (e: Exception) { /* mic permission missing or busy */ }
            has = echo.hasRecording(); state = "idle"
        }.start()
    }
    fun play(f: Float) {
        if (state != "idle" || !has) return
        state = "playing"
        Thread {
            try { echo.play(f) } catch (e: Exception) { }
            state = "idle"
        }.start()
    }

    Column(Modifier.fillMaxSize().background(Cream), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar("صدى الصوت المضحك", vm.stars) { vm.back() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Bouncy {
                Text(
                    when (state) { "recording" -> "🎙️"; "playing" -> "🔊"; else -> if (has) "😄" else "🎤" },
                    fontSize = 140.sp
                )
            }
        }
        Text(
            when (state) { "recording" -> "بسجّل... اتكلم!"; "playing" -> "اسمع!"; else -> if (has) "اختار صوت مضحك" else "اضغط واتكلم" },
            fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Bold
        )
        BigCard("🔴", "سجّل صوتك", Pink, Modifier.fillMaxWidth(0.9f).height(110.dp).padding(top = 8.dp), 40.sp) { rec(); vm.addStars(1) }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigCard("🐦", "عصفور", Sky, Modifier.weight(1f).height(130.dp), 54.sp) { play(1.7f) }
            BigCard("🙂", "عادي", Mint, Modifier.weight(1f).height(130.dp), 54.sp) { play(1.0f) }
            BigCard("🧌", "عملاق", Lilac, Modifier.weight(1f).height(130.dp), 54.sp) { play(0.62f) }
        }
    }
}
