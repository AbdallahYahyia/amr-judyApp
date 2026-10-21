package com.amrjudy.kids

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Colourful water-blaster: tap anywhere and the cartoon cannon squirts a drop at the floating balloons
 * and animal bubbles. Pops say the colour / animal name. Every 10 pops = a celebration.
 * (No weapons or violence: it is a water-and-balloons game.)
 */

private val BL_COLORS = listOf(
    "أحمر" to Color(0xFFFF5C5C), "أزرق" to Color(0xFF4DA3FF), "أصفر" to Color(0xFFFFD43B),
    "أخضر" to Color(0xFF3FC96B), "برتقالي" to Color(0xFFFF922B), "بنفسجي" to Color(0xFFA77BFF),
    "وردي" to Color(0xFFFF8AC4)
)
private val BL_ANIMALS = listOf(
    "🐱" to "قطة", "🐶" to "كلب", "🐮" to "بقرة", "🐑" to "خروف", "🐔" to "فرخة", "🐵" to "قرد",
    "🐘" to "فيل", "🦁" to "أسد", "🐰" to "أرنب", "🐟" to "سمكة", "🦆" to "بطة", "🐴" to "حصان"
)

private class Tgt(
    val balloon: Boolean, val color: Color, val name: String, val emoji: String,
    val x0: Float, var y: Float, val vy: Float, val r: Float, val phase: Float
) { var x = x0 }

private class Shot(var x: Float, var y: Float, val vx: Float, val vy: Float, val color: Color)
private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Color)
private class Pop(val x: Float, val y: Float, var age: Float)

private fun lighten(c: Color, f: Float = 0.45f) = Color(
    c.red + (1f - c.red) * f, c.green + (1f - c.green) * f, c.blue + (1f - c.blue) * f, 1f
)

@Composable
fun BubbleBlaster(vm: AppViewModel) {
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    val tick = remember { mutableIntStateOf(0) }
    val targets = remember { ArrayList<Tgt>() }
    val shots = remember { ArrayList<Shot>() }
    val parts = remember { ArrayList<Particle>() }
    val pops = remember { ArrayList<Pop>() }
    val clock = remember { floatArrayOf(0f, 0f) }          // [time, spawn timer]
    var angle by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(0) }
    var mission by remember { mutableStateOf(BL_COLORS.random()) }
    var win by remember { mutableStateOf(0) }
    val tone = remember { try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (e: Exception) { null } }
    DisposableEffect(Unit) { onDispose { try { tone?.release() } catch (e: Exception) { } } }

    val pivotPx = 62f * density
    val barrelPx = 120f * density

    fun beep(t: Int, ms: Int) { try { tone?.startTone(t, ms) } catch (e: Exception) { } }

    LaunchedEffect(Unit) { vm.speech.speak("اضغط وفرقع البالونات!", pitch = 1.3f) }

    fun fire(p: Offset) {
        if (size.width == 0 || shots.size >= 6) return
        val cx = size.width / 2f
        val cy = size.height - pivotPx
        val dx = p.x - cx
        val dy = minOf(p.y - cy, -40f * density)               // always shoot upwards
        val a = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat().coerceIn(-80f, 80f)
        angle = a
        val rad = Math.toRadians(a.toDouble())
        val ux = sin(rad).toFloat(); val uy = -cos(rad).toFloat()
        val sp = 1100f * density
        shots.add(Shot(cx + ux * barrelPx, cy + uy * barrelPx, ux * sp, uy * sp, BL_COLORS.random().second))
        beep(ToneGenerator.TONE_PROP_BEEP, 40)
    }

    fun hit(t: Tgt) {
        score++
        beep(ToneGenerator.TONE_PROP_BEEP2, 90)
        pops.add(Pop(t.x, t.y, 0f))
        repeat(12) {
            parts.add(
                Particle(
                    t.x, t.y,
                    (Math.random().toFloat() * 2f - 1f) * 320f * density,
                    (Math.random().toFloat() * 1.6f - 1.3f) * 320f * density,
                    0.8f, if (t.balloon) t.color else BL_COLORS.random().second
                )
            )
        }
        if (score % 10 == 0) {
            vm.addStars(3); win = 3
            vm.speech.speak("برافو! عشرة!", pitch = 1.4f)
        } else {
            vm.speech.speak(t.name, pitch = 1.4f)
            if (score % 3 == 0) vm.addStars(1)
        }
        if (t.balloon && t.name == mission.first) {
            vm.addStars(1)
            var m = mission
            while (m.first == mission.first) m = BL_COLORS.random()
            mission = m
        }
    }

    fun step(dt: Float) {
        clock[0] += dt; clock[1] += dt
        val w = size.width.toFloat(); val h = size.height.toFloat()
        if (clock[1] > 0.85f && targets.size < 7) {
            clock[1] = 0f
            val r = (34f + Math.random().toFloat() * 14f) * density
            val balloon = Math.random() < 0.65
            val col = BL_COLORS.random()
            val an = BL_ANIMALS.random()
            targets.add(
                Tgt(
                    balloon, if (balloon) col.second else Color.White, if (balloon) col.first else an.second,
                    if (balloon) "" else an.first,
                    r + Math.random().toFloat() * (w - 2 * r), h + r * 1.5f,
                    -(70f + Math.random().toFloat() * 70f) * density, r, Math.random().toFloat() * 6f
                )
            )
        }
        val ti = targets.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            t.y += t.vy * dt
            t.x = t.x0 + sin(clock[0] * 1.6f + t.phase) * 16f * density
            if (t.y < -t.r * 2.5f) ti.remove()
        }
        val si = shots.iterator()
        while (si.hasNext()) {
            val s = si.next()
            s.x += s.vx * dt; s.y += s.vy * dt
            if (s.y < -40f || s.x < -40f || s.x > w + 40f) { si.remove(); continue }
            val tg = targets.firstOrNull { hypot(it.x - s.x, it.y - s.y) < it.r + 10f * density }
            if (tg != null) { targets.remove(tg); si.remove(); hit(tg) }
        }
        val pi = parts.iterator()
        while (pi.hasNext()) {
            val p = pi.next()
            p.life -= dt
            p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 900f * density * dt
            if (p.life <= 0f) pi.remove()
        }
        val qi = pops.iterator()
        while (qi.hasNext()) { val q = qi.next(); q.age += dt; if (q.age > 0.45f) qi.remove() }
    }

    LaunchedEffect(size) {
        if (size.width == 0) return@LaunchedEffect
        var last = withFrameNanosCompat()
        while (true) {
            val now = withFrameNanosCompat()
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            step(dt)
            tick.intValue++
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("مدفع البالونات", vm.stars) { vm.back() }
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF7FD3FF), Color(0xFFCDEFFF), Color(0xFFFFF1C2))))
                .onSizeChanged { size = it }
                .pointerInput(Unit) { detectTapGestures { fire(it) } }
        ) {
            val frame = tick.intValue                       // re-read every frame
            if (frame >= 0) {
                Text("☀️", fontSize = 60.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 46.dp, end = 14.dp))
                Text("☁️", fontSize = 56.sp, modifier = Modifier.align(Alignment.TopStart).padding(top = 70.dp, start = 24.dp).alpha(0.9f))
                Text("☁️", fontSize = 44.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 30.dp).alpha(0.8f))

                // targets
                for (t in targets) {
                    val rDp = (t.r / density).dp
                    if (t.balloon) {
                        val h = t.r * 2.2f
                        Box(Modifier.offset { IntOffset((t.x - t.r).roundToInt(), (t.y - h / 2f).roundToInt()) }) {
                            Box(
                                Modifier.size(rDp * 2, (h / density).dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Brush.radialGradient(listOf(lighten(t.color), t.color)))
                            )
                            Box(
                                Modifier.padding(start = rDp * 0.55f, top = rDp * 0.35f)
                                    .size(rDp * 0.4f, rDp * 0.6f).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.55f))
                            )
                            Box(
                                Modifier.align(Alignment.BottomCenter).offset(y = 22.dp)
                                    .size(2.dp, 24.dp).background(Color(0x88555555))
                            )
                        }
                    } else {
                        Box(
                            Modifier.offset { IntOffset((t.x - t.r).roundToInt(), (t.y - t.r).roundToInt()) }
                                .size(rDp * 2).clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xCCFFFFFF), Color(0x66BFE9FF))))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(t.emoji, fontSize = (t.r / density * 1.1f).sp) }
                    }
                }

                // water drops
                for (s in shots) {
                    Box(
                        Modifier.offset { IntOffset((s.x - 11f * density).roundToInt(), (s.y - 11f * density).roundToInt()) }
                            .size(22.dp).clip(CircleShape).background(s.color)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
                // splash particles
                for (p in parts) {
                    Box(
                        Modifier.offset { IntOffset((p.x - 6f * density).roundToInt(), (p.y - 6f * density).roundToInt()) }
                            .size(12.dp).clip(CircleShape).background(p.color).alpha((p.life / 0.8f).coerceIn(0f, 1f))
                    )
                }
                for (q in pops) {
                    Text(
                        "💥", fontSize = (46 + q.age * 80).sp,
                        modifier = Modifier.offset { IntOffset((q.x - 30f * density).roundToInt(), (q.y - 34f * density).roundToInt()) }
                            .alpha((1f - q.age / 0.45f).coerceIn(0f, 1f))
                    )
                }

                // ground + cannon
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(54.dp)
                        .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF7BDC7B), Color(0xFF4FB86A))))
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 62.dp)
                        .size(width = 34.dp, height = 120.dp)
                        .graphicsLayer { rotationZ = angle; transformOrigin = TransformOrigin(0.5f, 1f) }
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF4C6EF5), Color(0xFF91A7FF), Color(0xFF4C6EF5))))
                        .border(2.dp, Color(0xFF3B5BDB), RoundedCornerShape(14.dp))
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(92.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFE066), Color(0xFFFFB400))))
                        .border(3.dp, Color(0xFFE67700), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("😄", fontSize = 46.sp) }

                // scoreboard + mission
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🎯 $score", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.85f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    Text(
                        "فرقع ال${mission.first}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(mission.second)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                if (score == 0) {
                    Text(
                        "👆 اضغط عالبالونة", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Celebrate(win) { win = 0 }
        }
    }
}

private suspend fun withFrameNanosCompat(): Long = androidx.compose.runtime.withFrameNanos { it }
