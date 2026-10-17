package com.amrjudy.kids

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            LaunchedEffect(Unit) { permission.launch(Manifest.permission.RECORD_AUDIO) }
            BackHandler { vm.back() }
            AppRoot(vm)
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    Box(Modifier.fillMaxSize().background(Cream).systemBarsPadding()) {
        when (vm.screen) {
            "home" -> HomeScreen(vm)
            "menu" -> MenuScreen(vm)
            "speech" -> SpeechArena(vm)
            "feed" -> FeedAnimal(vm)
            "balloons" -> BalloonPop(vm)
            "echo" -> VoiceEchoScreen(vm)
            "colors" -> ColorsShapes(vm)
            "body" -> BodyParts(vm)
            "count" -> Counting(vm)
            "needs" -> NeedsCards(vm)
            "animals" -> AnimalSounds(vm)
            "chess" -> ChessMenu(vm)
            "buddy" -> Buddy(vm)
            "stars" -> StarTree(vm)
            "family" -> FamilyAlbum(vm)
            "parent" -> ParentStudio(vm)
            "english" -> EnglishHub(vm)
            "math" -> MathHub(vm)
            "science" -> ScienceHub(vm)
            "talk" -> TalkHub(vm)
            else -> HomeScreen(vm)
        }
        if (vm.paused) PauseOverlay(vm)
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("مين هيلعب؟", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        for (p in Profile.values()) {
            BigCard(
                p.emoji, "${p.nameAr} - ${p.age} سنين",
                if (p == Profile.AMR) Sky else Pink,
                Modifier.fillMaxWidth().weight(1f), 110.sp
            ) {
                vm.selectProfile(p)
                vm.speech.speak("أهلاً يا ${p.nameAr}", pitch = 1.4f)
            }
        }
        Text(
            "👨‍👩‍👧 الأهل", fontSize = 18.sp, color = Ink,
            modifier = Modifier.clickable { vm.go("parent") }.padding(12.dp)
        )
    }
}

private class MenuItem(val emoji: String, val label: String, val color: Color, val screen: String)

@Composable
fun MenuScreen(vm: AppViewModel) {
    val amr = vm.profile == Profile.AMR
    val entries = buildList<MenuItem> {
        if (amr) {
            add(MenuItem("🗣️", "ألعاب الكلام", Pink, "talk"))
            add(MenuItem("🔊", "نطق الكلمات", Sun, "speech"))
            add(MenuItem("🐰", "أطعم الحيوان", Mint, "feed"))
            add(MenuItem("🎈", "بالونات", Sky, "balloons"))
            add(MenuItem("🎤", "صدى الصوت", Lilac, "echo"))
            add(MenuItem("🐮", "أصوات الحيوانات", Peach, "animals"))
            add(MenuItem("🎨", "ألوان وأشكال", Pink, "colors"))
            add(MenuItem("🧒", "جسمي", Mint, "body"))
            add(MenuItem("🔢", "عدّ", Sun, "count"))
            add(MenuItem("💬", "أنا عايز", Sky, "needs"))
            add(MenuItem("♞", "أبطال الشطرنج", Lilac, "chess"))
            add(MenuItem("🐻", "دبدوب", Peach, "buddy"))
        } else {
            add(MenuItem("🔤", "English", Sky, "english"))
            add(MenuItem("➕", "حساب", Mint, "math"))
            add(MenuItem("🔬", "علوم", Peach, "science"))
            add(MenuItem("♞", "أكاديمية الشطرنج", Lilac, "chess"))
            add(MenuItem("🐻", "دبدوب", Pink, "buddy"))
            add(MenuItem("🎨", "ألوان وأشكال", Pink, "colors"))
            add(MenuItem("🎈", "بالونات", Sky, "balloons"))
            add(MenuItem("🎤", "صدى الصوت", Mint, "echo"))
        }
        if (vm.photoUri != null) add(MenuItem("👨‍👩‍👧‍👦", "عيلتي", Pink, "family"))
        add(MenuItem("🌳", "شجرة النجوم", Mint, "stars"))
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("${vm.profile?.nameAr ?: ""} 👋", vm.stars) { vm.back() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { e ->
                BigCard(e.emoji, e.label, e.color, Modifier.fillMaxWidth().height(150.dp), 56.sp) { vm.go(e.screen) }
            }
        }
    }
}

@Composable
fun PauseOverlay(vm: AppViewModel) {
    Box(
        Modifier.fillMaxSize().background(Color(0xEEFFF8E1)).clickable(enabled = true) { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Bouncy { Text("😴", fontSize = 120.sp) }
            Text("وقت الراحة", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
            Text("اشرب مية وارتاح شوية 💧", fontSize = 22.sp, color = Ink)
            BigCard("▶️", "كمّل لعب", Mint, Modifier.size(width = 220.dp, height = 140.dp), 48.sp) { vm.resume() }
        }
    }
}
