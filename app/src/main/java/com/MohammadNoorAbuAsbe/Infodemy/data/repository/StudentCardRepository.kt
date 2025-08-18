package com.MohammadNoorAbuAsbe.Infodemy.data.repository

import com.MohammadNoorAbuAsbe.Infodemy.data.models.ServerDateTimeResponse
import com.MohammadNoorAbuAsbe.Infodemy.data.models.SnlData
import com.MohammadNoorAbuAsbe.Infodemy.data.models.StudentCard
import com.MohammadNoorAbuAsbe.Infodemy.data.models.StudentCardDataResponse
import com.MohammadNoorAbuAsbe.Infodemy.data.models.StudentCardResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class StudentCardRepository(private val client: OkHttpClient) {

    /**
     * Fetches the initial student card data (SNL data)
     */
    suspend fun fetchStudentCardData(token: String): StudentCardDataResponse? {
        return withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {
                put("urlParameters", JSONObject())
            }

            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/StudentCard/Data")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                parseStudentCardDataResponse(responseBody)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Fetches the actual student card using SNL data
     */
    suspend fun fetchStudentCard(token: String, snlData: SnlData): StudentCard? {
        return withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {
                put("snl", snlData.snl)
                put("selected", snlData.selected)
                put("__hash", snlData.__hash)
            }

            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/StudentCard/GetStudentCard")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                parseStudentCardResponse(responseBody)?.studentCard
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Fetches the server date and time
     */
    suspend fun fetchServerDateTime(token: String): String? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Service/ServerDateTime")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                parseServerDateTimeResponse(responseBody)?.dateTime
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Fetches the institution logo image from URL
     */
    suspend fun fetchInstituteLogo(logoUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(logoUrl)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                response.body?.bytes()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseStudentCardDataResponse(json: String): StudentCardDataResponse? {
        return try {
            val jsonObject = JSONObject(json)
            val snlsDataArray = jsonObject.getJSONArray("snlsData")
            val snlsData = mutableListOf<SnlData>()

            for (i in 0 until snlsDataArray.length()) {
                val snlObject = snlsDataArray.getJSONObject(i)
                snlsData.add(
                    SnlData(
                        snl = snlObject.getString("snl"),
                        selected = snlObject.getBoolean("selected"),
                        __hash = snlObject.getString("__hash")
                    )
                )
            }

            StudentCardDataResponse(
                snlsData = snlsData,
                zht = jsonObject.getString("zht"),
                title = jsonObject.optString("title"),
                gtm = jsonObject.optString("gtm"),
                toolbar = jsonObject.optString("toolbar"),
                headerText = jsonObject.optString("headerText")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseStudentCardResponse(json: String): StudentCardResponse? {
        return try {
            val jsonObject = JSONObject(json)
            val studentCardObject = jsonObject.getJSONObject("studentCard")

            val studentCard = StudentCard(
                displayStudentCard = studentCardObject.getBoolean("displayStudentCard"),
                snl = studentCardObject.optString("snl"),
                institute = studentCardObject.getString("institute"),
                studentsAssociationLogo = studentCardObject.optString("studentsAssociationLogo"),
                instituteLogo = studentCardObject.getString("instituteLogo"),
                studentName = studentCardObject.getString("studentName"),
                cardText = studentCardObject.getString("cardText"),
                barcodeBase64 = studentCardObject.getString("barcodeBase64"),
                qrcodeBase64 = studentCardObject.optString("qrcodeBase64"),
                studentImage = studentCardObject.getString("studentImage"),
                greenPass = studentCardObject.getBoolean("greenPass"),
                __hash = studentCardObject.getString("__hash")
            )

            StudentCardResponse(studentCard = studentCard)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseServerDateTimeResponse(json: String): ServerDateTimeResponse? {
        return try {
            val jsonObject = JSONObject(json)
            ServerDateTimeResponse(
                dateTime = jsonObject.getString("dateTime")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
