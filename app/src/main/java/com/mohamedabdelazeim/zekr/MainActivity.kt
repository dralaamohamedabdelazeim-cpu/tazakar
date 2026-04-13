package com.mohamedabdelazeim.zekr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohamedabdelazeim.zekr.data.ZekrPrefs
import com.mohamedabdelazeim.zekr.ui.screens.AdhkarScreen
import com.mohamedabdelazeim.zekr.ui.screens.HomeScreen
import com.mohamedabdelazeim.zekr.ui.theme.ZekrTheme
import com.mohamedabdelazeim.zekr.worker.ZekrScheduler

class MainActivity : ComponentActivity() {

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScheduler()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // طلب إذن الإشعارات وتشغيل الجدولة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startScheduler()
            }
        } else {
            startScheduler()
        }

        setContent {
            ZekrTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(onNavigateToAdhkar = { navController.navigate("adhkar") })
                        }
                        composable("adhkar") {
                            AdhkarScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    private fun startScheduler() {
        ZekrPrefs.setEnabled(this, true)
        val minutes = ZekrPrefs.getIntervalnMinutes(this).toLong()
        ZekrScheduler.schedule(this, minutes)
    }
}
