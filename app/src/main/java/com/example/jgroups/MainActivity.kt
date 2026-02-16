package com.example.jgroups

import android.content.Context
import android.net.*
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jgroups.ui.theme.JGroupsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.Inet4Address

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) {
            if (isNotIpipa(lp)) {
                lifecycleScope.launch {
                    delay(1000)

                    val currentLp = connectivityManager.getLinkProperties(network)
                    if (isNotIpipa(currentLp)) {
                        viewModel.onNetworkEvent()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")

        setContent {
            JGroupsTheme {
                JGroupsChatApp(viewModel)
            }
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    /**
     * Filters out 169.254.x.x (Auto-IP) and IPv6 addresses.
     * Only returns true if a valid routable IPv4 address is found.
     */
    private fun isNotIpipa(linkProperties: LinkProperties?): Boolean {
        linkProperties?.linkAddresses?.forEach { linkAddress ->
            val addr = linkAddress.address
            if (addr is Inet4Address && !addr.isLoopbackAddress) {
                val host = addr.hostAddress
                if (host != null && !host.startsWith("169.254.")) {
                    Log.d("MainActivity", "Valid IPv4 detected: $host")
                    return true
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

@Composable
fun JGroupsChatApp(viewModel: ChatViewModel = viewModel()) {
    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.05f)
                .background(Color.DarkGray),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { viewModel.isProbeMode = false }) { Text("Chat") }
            Button(onClick = { viewModel.isProbeMode = true }) { Text("Probe") }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.95f)
                .padding(16.dp)
        ) {
            if (viewModel.isProbeMode) {
                Text("Command: java org.jgroups.tests.Probe", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = viewModel.probeQuery,
                        onValueChange = { viewModel.probeQuery = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Arguments") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.executeProbe() }) {
                        Text("Execute")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E)) // Terminal Dark
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = viewModel.probeResults,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = viewModel.messageText,
                        onValueChange = { viewModel.messageText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Message") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.sendMessage() }) {
                        Text("Send")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = viewModel.chatHistory)
                }
            }
        }
    }
}