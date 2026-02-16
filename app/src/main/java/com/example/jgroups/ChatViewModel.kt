package com.example.jgroups

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    var isProbeMode by mutableStateOf(false)
    var probeQuery by mutableStateOf("")
    var probeResults by mutableStateOf("Ready for probe...")
    var messageText by mutableStateOf("")
    var chatHistory by mutableStateOf("Waiting for Ethernet...")

    private val chatManager = JGroupsChatManager(application.applicationContext)

    fun onNetworkEvent() {
        viewModelScope.launch(Dispatchers.IO) {
            chatManager.connectOrRefresh(
                memberName = "ANDROID",
                onStatusUpdate = { status ->
                    viewModelScope.launch(Dispatchers.Main) { chatHistory += "\n[System] $status" }
                },
                onMessageReceived = { msg ->
                    viewModelScope.launch(Dispatchers.Main) { chatHistory += "\n$msg" }
                }
            )
        }
    }

    fun sendMessage() {
        if (messageText.isBlank()) return
        val currentMsg = messageText
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatManager.send(currentMsg)
                withContext(Dispatchers.Main) {
                    chatHistory += "\nMe: $currentMsg"
                    messageText = ""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatHistory += "\n[Error] Send failed: ${e.message}"
                }
            }
        }
    }

    fun executeProbe() {
        probeResults = "Executing probe..."
        viewModelScope.launch(Dispatchers.IO) {
            val oldOut = System.out
            val bos = ByteArrayOutputStream()
            val ps = PrintStream(bos)

            try {
                System.setOut(ps)
                // Split args and run JGroups Probe tool
                val args = probeQuery.split(" ").filter { it.isNotBlank() }.toTypedArray()
                org.jgroups.tests.Probe.main(args)
                ps.flush()
                val result = bos.toString()
                withContext(Dispatchers.Main) {
                    probeResults = result.ifBlank { "No responses received." }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    probeResults = "Probe Error: ${e.message}"
                }
            } finally {
                System.setOut(oldOut)
                ps.close()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatManager.close()
    }
}