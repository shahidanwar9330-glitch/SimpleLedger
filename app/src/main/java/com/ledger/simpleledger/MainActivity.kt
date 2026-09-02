package com.ledger.simpleledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.ledger.simpleledger.ui.navigation.SimpleLedgerNavGraph
import com.ledger.simpleledger.ui.theme.SimpleLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleLedgerRoot()
        }
    }
}

@Composable
private fun SimpleLedgerRoot() {
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as SimpleLedgerApp)
    // darkModeOverride: "system" | "light" | "dark" — read once at launch; changing it in
    // Settings takes effect the next time the app is opened, keeping this simple and predictable.
    val override = remember { app.settingsPrefs.darkModeOverride }
    val systemDark = isSystemInDarkTheme()
    val useDark = when (override) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    SimpleLedgerTheme(darkTheme = useDark) {
        Surface(
            modifier = Modifier,
            color = MaterialTheme.colorScheme.background
        ) {
            SimpleLedgerNavGraph()
        }
    }
}
