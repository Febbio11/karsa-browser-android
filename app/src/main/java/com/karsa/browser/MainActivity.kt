package com.karsa.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.karsa.browser.browser.RenderSettings
import com.karsa.browser.ui.BrowserScreen
import com.karsa.browser.ui.SettingsStore
import com.karsa.browser.ui.theme.KarsaBrowserTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SettingsStore(applicationContext)
        setContent {
            val dark by store.darkTheme.collectAsState(initial = false)
            val settings by store.settings.collectAsState(initial = RenderSettings())
            KarsaBrowserTheme(darkTheme = dark) {
                BrowserScreen(
                    settings = settings,
                    darkTheme = dark,
                    onToggleDarkTheme = {
                        lifecycleScope.launch { store.setDarkTheme(!dark) }
                    },
                    onUpdateSettings = { transform ->
                        lifecycleScope.launch { store.update(transform, settings) }
                    }
                )
            }
        }
    }
}
