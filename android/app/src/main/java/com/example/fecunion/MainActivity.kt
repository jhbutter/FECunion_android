package com.example.fecunion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fecunion.ui.FileSelectScreen
import com.example.fecunion.ui.MainViewModel
import com.example.fecunion.ui.theme.FECunionTheme

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
