package com.example.jgroups

import android.content.Context
import android.util.Log
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.Receiver
import org.jgroups.protocols.UDP2
import java.io.InputStream


class JGroupsChatManager(private val context: Context) {
    var channel: JChannel? = null
        private set

    fun connectOrRefresh(
        memberName: String,
        onStatusUpdate: (String) -> Unit,
        onMessageReceived: (String) -> Unit
    ) {
        try {
            if (channel != null) {
                Log.d("JGroupsManager", "Refresh channel")
                refreshTransport()
                onStatusUpdate("Transport Hot-swapped: ${channel?.viewAsString}")
            } else {
                Log.d("JGroupsManager", "New channel")
                setupNewChannel(memberName, onMessageReceived)
                onStatusUpdate("Connected: ${channel?.viewAsString}")
            }
        } catch (e: Exception) {
            Log.e("JGroupsManager", "Network transition failed", e)
            onStatusUpdate("Error: ${e.message}")
        }
    }

    private fun setupNewChannel(memberName: String, onMessageReceived: (String) -> Unit) {
        val configStream: InputStream = context.resources.openRawResource(R.raw.chat)

        channel = JChannel(configStream).apply {
            name = memberName

            setDiscardOwnMessages(true)

            receiver = object : Receiver {
                override fun receive(msg: Message) {
                    val sender = msg.src()
                    val payload = msg.getObject<Any>()
                    if (payload is String) {
                        onMessageReceived("$sender: $payload")
                    }
                }
            }
            connect("chat")
        }
    }

    private fun refreshTransport() {

        try {

            val udp: UDP2? = channel?.protocolStack?.findProtocol(UDP2::class.java)

            udp?.reconnect();

/*
            val stopThreads = UDP::class.java.getDeclaredMethod("stopThreads")
            stopThreads.isAccessible = true
            stopThreads.invoke(udp)
            val destroySockets = UDP::class.java.getDeclaredMethod("destroySockets")
            destroySockets.isAccessible = true
            destroySockets.invoke(udp)

            //Refresh network
            Util.resetCachedAddresses(true, true)

            val createSockets = UDP::class.java.getDeclaredMethod("createSockets")
            createSockets.isAccessible = true
            createSockets.invoke(udp)
            val startThreads = UDP::class.java.getDeclaredMethod("startThreads")
            startThreads.isAccessible = true
            startThreads.invoke(udp)
            */

            //socketinfo = method.invoke(udp) as String?
            //Log.d("JGroupsManager", "socketinfo$socketinfo")
        } catch (e: Exception) {
            Log.e("JGroupsManager", "Failed to refresh transport", e)
        }
    }

    fun send(text: String) {
        channel?.send(null, text)
    }

    fun close() {
        channel?.close()
        channel = null
    }
}