package com.MohammadNoorAbuAsbe.Infodemy.data.repository

import com.MohammadNoorAbuAsbe.Infodemy.data.models.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate

class MessagesRepository(private val client: OkHttpClient) {

    suspend fun fetchMessages(token: String): List<Message> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Home/MessagesData")
                .post("""{}""".toRequestBody("application/json".toMediaType())) // Add empty JSON body
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Unexpected response code: ${response.code}, body: ${response.body?.string()}")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            parseMessages(responseBody)
        }
    }

    private fun parseMessages(responseBody: String): List<Message> {
        try {
            val jsonResponse = JSONObject(responseBody)
            if (!jsonResponse.has("messages")) {
                throw JSONException("Key 'messages' not found in response")
            }
            val messagesArray = jsonResponse.getJSONArray("messages")
            val messages = mutableListOf<Message>()

            for (i in 0 until messagesArray.length()) {
                val messageObject = messagesArray.getJSONObject(i)
                messages.add(
                    Message(
                        id = messageObject.getString("id"),
                        title = messageObject.getString("title"),
                        text = messageObject.getString("text"),
                        date = messageObject.getString("date")
                    )
                )
            }

            // Sort messages by date in descending order
            return messages.sortedByDescending { LocalDate.parse(it.date.split("T")[0]) }
        } catch (e: JSONException) {
            throw JSONException("Error parsing messages: ${e.message}")
        }
    }
}
