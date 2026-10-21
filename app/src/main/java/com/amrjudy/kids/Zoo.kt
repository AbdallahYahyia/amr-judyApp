package com.amrjudy.kids

import androidx.compose.foundation.background
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

/* Animals and "where is it?" (position words) for Amr. */

private class Critter(val emoji: String, val name: String, val home: String)

private val CRITTERS = listOf(
    Critter("🐱", "قطة", "🏠"), Critter("🐶", "كلب", "🏠"),
    Critter("🐮", "بقرة", "🏡"), Critter("🐑", "خروف", "🏡"), Critter("🐔", "فرخة", "🏡"),
    Critter("🐴", "حصان", "🏡"), Critter("🦆", "بطة", "🏡"),
    Critter("🦁", "أسد", "🌳"), Critter("🐯", "نمر", "🌳"), Critter("🐻", "دب", "🌳"),
    Critter("🐵", "قرد", "🌳"), Critter("🐘", "فيل", "🌳"), Critter("🦒", "زرافة", "🌳"),
    Critter("🐟", "سمكة", "🌊"), Critter("🐬", "دولفين", "🌊"), Critter("🐙", "أخطبوط", "🌊"),
    Critter("🐢", "سلحفاة", "🌊"), Critter("🐪", "جمل", "🏜️")
)
private val HOMES = listOf("🏠", "🏡", "🌳", "🌊", "🏜️")

@Composable
fun ZooHub(vm: AppViewModel) {
    Hub(
        vm, "حيوانات وأماكن",
        listOf(
            HubCard("🐾", "فين الحيوان؟", Mint) {
                QuizRunner(vm) {
                    val c = CRITTERS.random()
                    val ch = (listOf(c.emoji) + CRITTERS.filter { it !== c }.shuffled().take(3).map { it.emoji }).shuffled()
                    MQ("", "فين ال${c.name}؟", "فين ال${c.name}؟", ch, c.emoji)
                }
            },
            HubCard("🗣️", "ده مين؟", Pink) {
                SayItScreen(vm, CRITTERS.shuffled().map { SayItItem(it.emoji, it.name) }, manual = true, prompt = "ده مين؟")
            },
            HubCard("🏡", "بيعيش فين؟", Peach) {
                QuizRunner(vm, glyphSize = 100) {
                    val c = CRITTERS.random()
                    val ch = (listOf(c.home) + HOMES.filter { it != c.home }.shuffled().take(3)).shuffled()
                    MQ(c.emoji, "بيعيش فين؟", "ده ${c.name}. بيعيش فين؟", ch, c.home)
                }
            },
            HubCard("⚽", "فين الكورة؟", Sun) { WhereGame(vm) }
        )
    )
}

/* ---------------- Where is it? (فوق / تحت / جنب / جوه / قدام / ورا) ---------------- */

private class Prep(val word: String, val key: String)

private val PREPS = listOf(
    Prep("فوق", "on"), Prep("تحت", "under"), Prep("جنب", "beside"),
    Prep("جوه", "in"), Prep("قدام", "front"), Prep("ورا", "behind")
)
private val WHERE_ITEMS = listOf("⚽" to "الكورة", "🐱" to "القطة", "🧸" to "الدبدوب", "🍎" to "التفاحة")
private val WHERE_OBJS = listOf(
    "🪑" to "الكرسي", "🛏️" to "السرير", "🌳" to "الشجرة", "🏠" to "البيت", "📦" to "الصندوق", "🧺" to "السلة"
)

private fun allowed(key: String, obj: String) = when (key) {
    "in" -> obj in listOf("📦", "🏠", "🧺")
    "under" -> obj in listOf("🪑", "🛏️", "🌳")
    else -> true
}

@Composable
private fun Scene(item: String, obj: String, key: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "on" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item, fontSize = 34.sp, modifier = Modifier.offset(y = 6.dp)); Text(obj, fontSize = 54.sp)
            }
            "under" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(obj, fontSize = 54.sp); Text(item, fontSize = 34.sp, modifier = Modifier.offset(y = (-6).dp))
            }
            "beside" -> Row(verticalAlignment = Alignment.Bottom) {
                Text(obj, fontSize = 54.sp); Text(item, fontSize = 34.sp)
            }
            "in" -> Box(contentAlignment = Alignment.Center) {
                Text(obj, fontSize = 66.sp); Text(item, fontSize = 24.sp, modifier = Modifier.offset(y = 8.dp))
            }
            "front" -> Box {
                Text(obj, fontSize = 62.sp)
                Text(item, fontSize = 38.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 4.dp))
            }
            else -> Box {
                Text(item, fontSize = 38.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 4.dp))
                Text(obj, fontSize = 62.sp)
            }
        }
    }
}

@Composable
fun WhereGame(vm: AppViewModel) {
    var round by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val level = (1 + correct / 4).coerceAtMost(2)

    val q = remember(round) {
        val p = PREPS.random()
        val obj = WHERE_OBJS.filter { allowed(p.key, it.first) }.random()
        Triple(p, WHERE_ITEMS.random(), obj)
    }
    val (prep, item, obj) = q
    val sentence = "${item.second} ${prep.word} ${obj.second}"
    val options = remember(round) {
        (listOf(prep) + PREPS.filter { it !== prep && allowed(it.key, obj.first) }.shuffled().take(3)).shuffled()
    }
    fun ask() = vm.speech.speak("$sentence. فين الصورة دي؟", pitch = 1.3f)
    LaunchedEffect(round) { ask() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "المستوى $level  " + "⭐".repeat(level), fontSize = 16.sp,
                fontWeight = FontWeight.Bold, color = Ink
            )
            if (level == 1) {
                Text(
                    sentence, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp)
                )
            } else {
                BigCard("🔊", "اسمع تاني", Sky, Modifier.padding(8.dp).size(width = 150.dp, height = 90.dp), 28.sp) { ask() }
            }
            for (row in options.chunked(2)) {
                Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (o in row) {
                        Scene(item.first, obj.first, o.key, Modifier.weight(1f).fillMaxHeight()) {
                            if (!done) {
                                if (o === prep) {
                                    done = true; win = level; vm.addStars(level)
                                    vm.speech.speak("برافو! $sentence", pitch = 1.4f)
                                    scope.launch { delay(2200); done = false; correct++; round++ }
                                } else vm.speech.speak("لأ، دوّر تاني. $sentence", pitch = 1.3f)
                            }
                        }
                    }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}
