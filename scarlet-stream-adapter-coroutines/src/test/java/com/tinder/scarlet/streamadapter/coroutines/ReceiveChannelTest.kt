/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.streamadapter.coroutines

import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.v2.OkHttpWebSocketConnection
import com.tinder.scarlet.testutils.v2.containingBytes
import com.tinder.scarlet.testutils.v2.containingText
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

class ReceiveChannelTest {

    @get:Rule
    private val connection = OkHttpWebSocketConnection.create<Service>(
        OkHttpWebSocketConnection.Configuration(
            streamAdapterFactories = listOf(CoroutinesStreamAdapterFactory())
        )
    ) { observeEvents() }

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() {
        // Given
        connection.givenConnectionIsEstablished()
        val textMessage1 = "Hello"
        val textMessage2 = "Hi"
        val bytesMessage1 = "Yo".toByteArray()
        val bytesMessage2 = "Sup".toByteArray()
        val testTextChannel = connection.server.observeText()
        val testBytesChannel = connection.server.observeBytes()

        // When
        connection.client.sendText(textMessage1)
        val isSendTextSuccessful = connection.client.sendTextAndConfirm(textMessage2)
        connection.client.sendBytes(bytesMessage1)
        val isSendBytesSuccessful = connection.client.sendBytesAndConfirm(bytesMessage2)

        // Then
        assertThat(isSendTextSuccessful).isTrue()
        assertThat(isSendBytesSuccessful).isTrue()

        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(textMessage1),
            any<WebSocketEvent.OnMessageReceived>().containingText(textMessage2),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage1),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage2)
        )

        runBlocking {
            assertThat(testTextChannel.receiveOrNull()).isEqualTo(textMessage1)
            assertThat(testTextChannel.receiveOrNull()).isEqualTo(textMessage2)

            assertThat(testBytesChannel.receiveOrNull()).isEqualTo(bytesMessage1)
            assertThat(testBytesChannel.receiveOrNull()).isEqualTo(bytesMessage2)
        }
    }

    private interface Service {
        @Receive
        fun observeEvents(): Stream<WebSocketEvent>

        @Receive
        fun observeText(): ReceiveChannel<String>

        @Receive
        fun observeBytes(): ReceiveChannel<ByteArray>

        @Send
        fun sendText(message: String)

        @Send
        fun sendTextAndConfirm(message: String): Boolean

        @Send
        fun sendBytes(message: ByteArray)

        @Send
        fun sendBytesAndConfirm(message: ByteArray): Boolean
    }
}