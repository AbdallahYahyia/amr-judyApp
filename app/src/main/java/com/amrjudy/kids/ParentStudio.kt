package com.amrjudy.kids

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val VOICE_KEYS = listOf(
    "باب", "ماما", "بابا", "قطة", "عربية", "مية", "تفاحة",
    "عطشان", "جائع", "زعلان", "مبسوط", "عاوز أنام", "الحمام"
)

@Composable
fun ParentStudio(vm: AppViewModel) {
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar("لوحة الأب", vm.stars) { vm.back() }
        if (!unlocked) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("🔒", fontSize = 64.sp)
                Text("اكتب الرقم السري (الافتراضي 1234)", fontSize = 18.sp, color = Ink)
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it.take(8); wrong = false },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                if (wrong) Text("الرقم غلط", color = Color(0xFFE03131))
                Spacer(Modifier.height(12.dp))
                Button(onClick = { if (vm.checkPin(pin)) unlocked = true else wrong = true }) { Text("دخول") }
            }
        } else ParentContent(vm)
    }
}

@Composable
private fun ParentContent(vm: AppViewModel) {
    val ctx = LocalContext.current
    val echo = remember { VoiceEcho() }
    var recording by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var newPin by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val out = File(ctx.filesDir, "family.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                vm.setPhoto(out.path)
                info = "تم حفظ الصورة ✅"
            } catch (e: Exception) { info = "ما قدرتش أحفظ الصورة" }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("📷 صورة العيلة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Button(onClick = { picker.launch("image/*") }) { Text("اختيار صورة") }

        Text("🎙️ سجّل صوتك للكلمات (٣ ثواني لكل كلمة)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        key(refresh) {
            for (k in VOICE_KEYS) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$k  ${if (vm.hasCustomVoice(k)) "✅" else ""}", fontSize = 20.sp, color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = recording == null, onClick = {
                            recording = k
                            Thread {
                                try { echo.record(3); vm.saveCustomVoice(k, echo) } catch (e: Exception) { }
                                recording = null; refresh++
                            }.start()
                        }) { Text(if (recording == k) "⏺ ..." else "سجّل") }
                        Button(onClick = { vm.sayWord(k) }) { Text("🔊") }
                    }
                }
            }
        }

        Text("🔑 تغيير الرقم السري", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        OutlinedTextField(
            value = newPin, onValueChange = { newPin = it.filter(Char::isDigit).take(8) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
        )
        Button(enabled = newPin.length >= 4, onClick = { vm.setPin(newPin); newPin = ""; info = "تم تغيير الرقم السري ✅" }) { Text("حفظ") }
        if (info.isNotEmpty()) Text(info, color = Ink)
        Text("⏱️ التطبيق يعمل استراحة تلقائية بعد ٨ دقائق", fontSize = 16.sp, color = Ink)
    }
}
