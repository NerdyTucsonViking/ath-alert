package com.nerdyviking.athalert

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nerdyviking.athalert.data.PrefsRepository
import com.nerdyviking.athalert.work.NotificationHelper
import com.nerdyviking.athalert.work.WorkScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
private fun AppScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefsRepository = remember { PrefsRepository(context) }
    val prefs by prefsRepository.prefsFlow.collectAsState(initial = com.nerdyviking.athalert.data.AppPrefs())
    val scope = rememberCoroutineScope()

    var coinId by remember(prefs.coinId) { mutableStateOf(prefs.coinId) }
    var coinLabel by remember(prefs.coinLabel) { mutableStateOf(prefs.coinLabel) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canNotify(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("ATH Alert", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tracks one CoinGecko coin id and notifies you when CoinGecko records a new all-time high.")

        OutlinedTextField(
            value = coinId,
            onValueChange = { coinId = it },
            label = { Text("CoinGecko coin id") },
            supportingText = { Text("Examples: bitcoin, ethereum, solana") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = coinLabel,
            onValueChange = { coinLabel = it },
            label = { Text("Display label") },
            supportingText = { Text("Optional friendly name shown in the app") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Notifications enabled")
            Switch(
                checked = prefs.notificationsEnabled,
                onCheckedChange = {
                    scope.launch { prefsRepository.setNotificationsEnabled(it) }
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                scope.launch {
                    prefsRepository.saveCoin(coinId, coinLabel)
                }
            }) { Text("Save") }

            Button(onClick = {
                WorkScheduler.start(context)
                WorkScheduler.checkNow(context)
            }) { Text("Start Tracking") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { WorkScheduler.checkNow(context) }) { Text("Check Now") }
            Button(onClick = { WorkScheduler.stop(context) }) { Text("Stop") }
        }

        Button(onClick = {
            NotificationHelper.createChannel(context)
            NotificationHelper.showAthNotification(
                context,
                title = "Test notification",
                body = "ATH Alert notifications are working."
            )
        }) { Text("Send Test Notification") }

        Text("Current coin: ${prefs.coinLabel} (${prefs.coinId})")
        Text("Last seen price: $${"%.2f".format(prefs.lastSeenPrice)}")
        Text("Last recorded ATH used by app: $${"%.2f".format(prefs.lastNotifiedAth)}")
        Text("Status: ${prefs.lastStatus}")
        Text(
            "Checks are scheduled with WorkManager, so timing is approximate rather than exact.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
