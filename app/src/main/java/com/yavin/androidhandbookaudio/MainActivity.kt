package com.yavin.androidhandbookaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidHandbookAudioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AndroidHandbookAudioApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AndroidHandbookAudioApp(modifier: Modifier = Modifier) {
    Text(
        text = "Android Handbook Audio",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AndroidHandbookAudioAppPreview() {
    AndroidHandbookAudioTheme {
        AndroidHandbookAudioApp()
    }
}
