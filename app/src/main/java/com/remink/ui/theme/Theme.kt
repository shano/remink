package com.remink.ui.theme

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD

@Composable
fun ReminkTheme(content: @Composable () -> Unit) {
    ThemeMMD(content = content)
}
