package com.amrjudy.kids

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView

private fun videoId(u: String): String? =
    Regex("(?:v=|youtu\\.be/|embed/|shorts/)([A-Za-z0-9_-]{6,})").find(u)?.groupValues?.get(1)

/** A web page locked to the chosen video: tapping "related videos" etc. does nothing. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebVideo(rawUrl: String) {
    val url = rawUrl.replace("://www.youtube.com", "://m.youtube.com")
    val vid = videoId(url)
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val req = request ?: return true
                        val u = req.url.toString()
                        val host = req.url.host ?: ""
                        val allowed =
                            u.startsWith(url) ||
                                host.endsWith("consent.youtube.com") ||
                                (vid != null && videoId(u) == vid) ||
                                (vid == null && host == (Uri.parse(url).host ?: ""))
                        return !allowed
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun LocalVideo(uriText: String) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val mc = MediaController(ctx)
                mc.setAnchorView(this)
                setMediaController(mc)
                setVideoURI(Uri.parse(uriText))
                setOnPreparedListener { start() }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/** The children's video corner: only videos the parent added (in the parent area) are shown. */
@Composable
fun VideosScreen(vm: AppViewModel) {
    var sel by remember { mutableStateOf(-1) }
    val list = remember(vm.videoVersion) { vm.videos() }
    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar("فيديوهات", vm.stars) { if (sel >= 0) sel = -1 else vm.back() }
        if (sel in list.indices) {
            val v = list[sel]
            Text(
                v.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(6.dp)
            )
            if (v.url.startsWith("content:") || v.url.startsWith("file:")) LocalVideo(v.url) else WebVideo(v.url)
        } else if (list.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("🎬", fontSize = 90.sp)
                Text(
                    "لسه مفيش فيديوهات. الأهل يقدروا يضيفوا فيديوهات من لوحة الأب.",
                    fontSize = 20.sp, color = Ink, textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                list.forEachIndexed { i, v ->
                    BigCard("🎬", v.title, if (i % 2 == 0) Sky else Mint, Modifier.fillMaxWidth().height(120.dp), 40.sp) {
                        sel = i
                    }
                }
            }
        }
    }
}
