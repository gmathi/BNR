package com.bnr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.core.ui.theme.BNRTheme
import com.bnr.app.presentation.navigation.BNRNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by appPreferences.appTheme.collectAsState(initial = "system")
            BNRTheme(appTheme = appTheme) {
                BNRNavGraph()
            }
        }
    }
}
