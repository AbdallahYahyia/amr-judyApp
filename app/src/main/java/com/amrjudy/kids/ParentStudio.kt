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

// Texts the app speaks. Whatever you record under the exact same text replaces the robot voice everywhere.
private val VOICE_KEYS = listOf(
    "باب", "ماما", "بابا", "قطة", "عربية", "مية", "تفاحة",
    "عطشان", "جائع", "زعلان", "مبسوط", "عاوز أنام", "الحمام",
    "ممتاز! برافو!", "حلو أوي! برافو", "شاطر! كمان مرة", "جرّب تاني يا بطل",
    "برافو! إجابة صح", "حاول تاني", "برافو يا عمرو!", "ده إيه؟", "بيعمل إيه؟", "نام نام! شكراً!", "بوم!",
    "عايز مية", "عايز عيش", "ماما تعالي", "افتح الباب", "عربية تجري", "عايز أنام", "لبن حلو",
    "باي باي", "لو سمحت", "شكراً", "العب كورة", "عايز تفاحة",
    "كورة", "كوباية", "موزة", "بيت", "شمس", "قمر", "نجمة", "جزمة", "عيش", "كلب", "فرخة", "سمكة", "كرسي", "تليفون"
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

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            vm.addVideo("فيديو ${vm.videos().size + 1}", uri.toString())
            info = "تمت إضافة الفيديو ✅"
        }
    }

    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) {
            info = try {
                ctx.contentResolver.openOutputStream(uri)?.use { Backup.write(ctx, it) }
                "تم حفظ النسخة الاحتياطية ✅"
            } catch (e: Exception) { "ما قدرتش أحفظ النسخة الاحتياطية" }
        }
    }
    val backupLoader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            info = try {
                val n = ctx.contentResolver.openInputStream(uri)?.use { Backup.read(ctx, it) } ?: -1
                if (n < 0) "الملف ده مش نسخة احتياطية صحيحة"
                else { vm.reloadFromPrefs(); refresh++; "تم الاسترجاع ✅ (الإعدادات و$n تسجيل صوت)" }
            } catch (e: Exception) { "ما قدرتش أسترجع النسخة" }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("💾 نسخة احتياطية", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(
            "بتحفظ الإعدادات، الرقم السري، النجوم، تسجيلات صوتك، صورة العيلة وقايمة الفيديوهات في ملف واحد. احتفظ بيه (مثلاً في جوجل درايف) عشان ترجّعه لو غيرت الموبايل.",
            fontSize = 15.sp, color = Ink
        )
        Button(onClick = { backupSaver.launch("amr-judy-backup.zip") }) { Text("💾 احفظ نسخة احتياطية") }
        Button(onClick = { backupLoader.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) {
            Text("♻️ استرجع من نسخة احتياطية")
        }
        Text("(فيديوهات الموبايل نفسها مش بتتحفظ، الروابط بس. اضفها تاني لو غيرت الموبايل.)", fontSize = 13.sp, color = Ink)

        Text("📷 صورة العيلة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Button(onClick = { picker.launch("image/*") }) { Text("اختيار صورة") }

        Text("🛠️ حل مشكلة الصوت (هواوي وموبايلات من غير جوجل)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(
            if (vm.speech.arabicOk) "حالة الصوت العربي: شغال ✅" else "حالة الصوت العربي: مش متاح ❌",
            fontSize = 17.sp, color = Ink
        )
        Text(
            "موبايلات هواوي الجديدة مفيهاش خدمات جوجل، فمفيش صوت عربي ولا تعرّف على الكلام. فيه حلّين:\n" +
                "١) سجّل صوتك أو صوت ماما (الأحسن ويطلع بلهجة مصرية): استخدم «سجّل الكل بالترتيب» تحت.\n" +
                "٢) ركّب محرك صوت عربي من برّه جوجل بلاي: افتح الصفحة تحت، نزّل ملف عربي (ara) واحد مناسب لموبايلك (arm64-v8a غالباً)، ثبّته، وبعدين اختاره من القايمة هنا.\n" +
                "التعرف على كلام الطفل مش هيشتغل من غير جوجل، فالتطبيق بيكتفي بإنه يسمع صوت الطفل ويديله نجوم على المحاولة.",
            fontSize = 15.sp, color = Ink
        )
        Button(onClick = {
            try {
                val i = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    Uri.parse("https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html")
                )
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
            } catch (e: Exception) { info = "ما قدرتش أفتح المتصفح" }
        }) { Text("🌐 صفحة تحميل محركات الصوت العربية", fontSize = 13.sp) }
        var engines by remember { mutableStateOf(listOf<Pair<String, String>>()) }
        Button(onClick = { engines = vm.speech.engines() }) { Text("عرض محركات الصوت اللي على الموبايل", fontSize = 13.sp) }
        for ((pkg, label) in engines) {
            Button(onClick = {
                vm.chooseEngine(pkg)
                info = "تم اختيار: $label"
            }) { Text((if (vm.speech.currentEngine() == pkg) "✅ " else "") + label, fontSize = 13.sp) }
        }
        if (engines.isNotEmpty()) {
            Button(onClick = { vm.chooseEngine(null); info = "رجعنا لمحرك الموبايل الافتراضي" }) {
                Text("رجوع للمحرك الافتراضي", fontSize = 13.sp)
            }
            Button(onClick = { vm.speech.recheck(); info = "اتعمل فحص تاني للصوت" }) { Text("فحص الصوت تاني", fontSize = 13.sp) }
        }

        Text("🎙️ سجّل الكل بالترتيب", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        var guide by remember { mutableStateOf(listOf<String>()) }
        var gi by remember { mutableStateOf(0) }
        if (guide.isEmpty() || gi >= guide.size) {
            if (guide.isNotEmpty()) Text("خلصت التسجيل ✅", color = Ink)
            Button(enabled = recording == null, onClick = {
                guide = (VOICE_KEYS + arabicLetterNames()).distinct().filter { !vm.hasCustomVoice(it) }
                gi = 0
            }) { Text("ابدأ (الكلمات اللي لسه ما اتسجلتش)", fontSize = 13.sp) }
        } else {
            val cur = guide[gi]
            Text("قول بوضوح: $cur  (${gi + 1}/${guide.size})", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = recording == null, onClick = {
                    recording = cur
                    Thread {
                        try { echo.record(3); vm.saveCustomVoice(cur, echo) } catch (e: Exception) { }
                        recording = null; refresh++; gi++
                    }.start()
                }) { Text(if (recording == cur) "⏺ بيسجّل..." else "⏺ سجّل") }
                Button(enabled = recording == null, onClick = { gi++ }) { Text("تخطي") }
                Button(enabled = recording == null, onClick = { guide = emptyList() }) { Text("إيقاف") }
            }
        }

        Text("🔊 صوت الكلام (لهجة مصرية)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        var voices by remember { mutableStateOf(listOf<android.speech.tts.Voice>()) }
        Button(onClick = { voices = vm.speech.arabicVoices() }) { Text("عرض الأصوات العربية في الموبايل") }
        if (voices.isNotEmpty()) {
            Text(
                if (vm.speech.isEgyptianVoiceAvailable()) "في صوت مصري ✅ (اختار واحد)"
                else "مفيش صوت مصري. نزّل صوت عربي (مصر) من إعدادات الموبايل ↓",
                fontSize = 16.sp, color = Ink
            )
            for (v in voices.take(10)) {
                val label = (if (v.locale.country == "EG") "🇪🇬 " else "") + v.locale.displayName + " – " + v.name
                Button(onClick = {
                    vm.chooseVoice(v.name)
                    vm.speech.speak("إزّيك يا عمرو؟ أنا صاحبك")
                }) { Text(label, fontSize = 13.sp) }
            }
        }
        Button(onClick = { vm.speech.openInstallVoiceData() }) { Text("تحميل بيانات صوت (مصر) من النظام") }
        Button(onClick = { vm.speech.openTtsSettings() }) { Text("إعدادات الكلام في الموبايل") }
        Text(
            "ملحوظة: صوت الموبايل العربي ساعات بيبقى فصحى مش مصري. أحسن طريقة للصوت المصري: سجّل صوتك أنت أو ماما للكلمات تحت، والتطبيق هيشغّل تسجيلك بدل الصوت الآلي.",
            fontSize = 15.sp, color = Ink
        )

        Text("🎙️ سجّل صوتك (٣ ثواني لكل كلمة)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        var newWord by remember { mutableStateOf("") }
        OutlinedTextField(
            value = newWord, onValueChange = { newWord = it.take(40) },
            label = { Text("اكتب أي كلمة أو جملة عايز تسجّلها") }, singleLine = true
        )
        Button(enabled = newWord.isNotBlank() && recording == null, onClick = {
            val k = newWord.trim()
            recording = k
            Thread {
                try { echo.record(3); vm.saveCustomVoice(k, echo) } catch (e: Exception) { }
                recording = null; refresh++
            }.start()
            newWord = ""
        }) { Text("سجّل الجملة دي") }
        key(refresh) {
            for (k in (VOICE_KEYS + vm.recordedKeys()).distinct()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$k  ${if (vm.hasCustomVoice(k)) "✅" else ""}", fontSize = 18.sp, color = Ink,
                        modifier = Modifier.weight(1f))
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

        Text("🎬 فيديوهات تعليمية للأطفال", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(
            "الأطفال بيشوفوا بس الفيديوهات اللي تضيفوها هنا. تقدر تضيف رابط (زي يوتيوب) أو فيديو من الموبايل.",
            fontSize = 15.sp, color = Ink
        )
        var vTitle by remember { mutableStateOf("") }
        var vUrl by remember { mutableStateOf("") }
        OutlinedTextField(
            value = vTitle, onValueChange = { vTitle = it.take(40) },
            label = { Text("اسم الفيديو") }, singleLine = true
        )
        OutlinedTextField(
            value = vUrl, onValueChange = { vUrl = it.take(300) },
            label = { Text("رابط الفيديو") }, singleLine = true
        )
        Button(enabled = vTitle.isNotBlank() && vUrl.startsWith("http"), onClick = {
            vm.addVideo(vTitle, vUrl); vTitle = ""; vUrl = ""; info = "تمت إضافة الفيديو ✅"
        }) { Text("أضف الرابط") }
        Button(onClick = { videoPicker.launch(arrayOf("video/*")) }) { Text("اختيار فيديو من الموبايل") }
        val savedVideos = remember(vm.videoVersion) { vm.videos() }
        savedVideos.forEachIndexed { i, v ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🎬 ${v.title}", fontSize = 17.sp, color = Ink, modifier = Modifier.weight(1f))
                Button(onClick = { vm.removeVideo(i) }) { Text("🗑") }
            }
        }
        Text("ابحث عن فيديوهات (بتفتح يوتيوب، انسخ الرابط وضيفه فوق):", fontSize = 15.sp, color = Ink)
        for ((label, query) in listOf(
            "حروف الهجاء للأطفال" to "حروف الهجاء للأطفال",
            "تعليم النطق للأطفال" to "تعليم النطق للأطفال تأخر الكلام",
            "أصوات الحيوانات للأطفال" to "أصوات الحيوانات للأطفال",
            "الأرقام والعد للأطفال" to "تعليم الأرقام للأطفال"
        )) {
            Button(onClick = {
                try {
                    val i = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                    )
                    i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(i)
                } catch (e: Exception) { info = "ما قدرتش أفتح يوتيوب" }
            }) { Text("🔎 $label", fontSize = 13.sp) }
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
