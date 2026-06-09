package com.example.fecunion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fecunion.ui.FileSelectScreen
import com.example.fecunion.ui.MainViewModel

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF00897B),
    secondaryContainer = Color(0xFFB2DFDB),
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFF5F5F5),
)

@Composable
fun FECunionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FECunionTheme {
                val vm: MainViewModel = viewModel()
                FileSelectScreen(vm)
            }
        }
    }
}
