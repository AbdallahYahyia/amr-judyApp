package com.amrjudy.kids

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Bright pastel palette
val Pink = Color(0xFFFFB3C7)
val Sky = Color(0xFFA5D8FF)
val Mint = Color(0xFFB2F2BB)
val Sun = Color(0xFFFFEC99)
val Peach = Color(0xFFFFD8A8)
val Lilac = Color(0xFFD0BFFF)
val Ink = Color(0xFF3B2F4A)
val Cream = Color(0xFFFFF8E1)

/** Giant tactile button – emoji on top, short label below. */
@Composable
fun BigCard(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 56.sp,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = emojiSize, textAlign = TextAlign.Center)
        if (label.isNotEmpty())
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
    }
}

@Composable
fun TopBar(title: String, stars: Int, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Peach).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text("🏠", fontSize = 32.sp) }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
        Box(
            Modifier.height(64.dp).clip(RoundedCornerShape(20.dp)).background(Sun).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text("⭐ $stars", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink) }
    }
}

/** Gentle bouncing wrapper to make characters feel alive. */
@Composable
fun Bouncy(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "bounce")
    val s by t.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s"
    )
    Box(modifier.scale(s), contentAlignment = Alignment.Center) { content() }
}

/** Simple confetti burst overlay: show it for ~1.5 seconds when something is achieved. */
@Composable
fun Confetti(stars: Int) {
    if (stars <= 0) return
    val items = listOf("🎉", "⭐", "✨", "🎊", "🌟")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Bouncy { Text("⭐".repeat(stars), fontSize = 64.sp) }
            Row { items.forEach { Text(it, fontSize = 40.sp) } }
        }
    }
}

@Composable
fun gap(h: Dp) = Box(Modifier.height(h))
