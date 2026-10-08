package it.togo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoTypography

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TogoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = togoColors().surfaceBase,
                    contentColor = togoColors().inkPrimary,
                ) {
                    Text(
                        text = "Togo",
                        style = togoTypography().titleScreen,
                        color = togoColors().inkPrimary,
                    )
                }
            }
        }
    }
}
