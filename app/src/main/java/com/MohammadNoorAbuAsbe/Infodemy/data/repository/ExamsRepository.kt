package com.MohammadNoorAbuAsbe.Infodemy.data.repository

import com.MohammadNoorAbuAsbe.Infodemy.data.models.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ExamsRepository(private val client: OkHttpClient) {

    /**
     * Fetches exams data from the server
     */
    suspend fun fetchExamsData(token: String): List<Exam> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/StudentExams/Data")
                .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Unexpected response code: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            parseExamsData(responseBody)
        }
    }

    /**
     * Parses the JSON response into a list of Exam objects
     */
    private suspend fun parseExamsData(responseBody: String): List<Exam> {
        return withContext(Dispatchers.Default) {
            val jsonObject = JSONObject(responseBody)
            val collapsedExams = jsonObject.getJSONObject("collapsedExams")
            val clientData = collapsedExams.getJSONArray("clientData")
            val examsList = mutableListOf<Exam>()

            for (i in 0 until clientData.length()) {
                val examJson = clientData.getJSONObject(i)

                // Map semester code to number (א=1, ב=2, etc.)
                val semesterHebrew = examJson.getString("krs_bhn_moed_sms")
                val semesterNumber = when (semesterHebrew) {
                    "א" -> 1
                    "ב" -> 2
                    "ג" -> 3
                    "ד" -> 4
                    else -> 0
                }

                examsList.add(
                    Exam(
                        rowkey = examJson.getString("rowkey"),
                        courseName = examJson.getString("krs_shm"),
                        examType = examJson.getString("krs_bhn_shm"),
                        date = examJson.getString("date"),
                        hebrewDate = examJson.getString("hebrewdate"),
                        formattedDateTime = examJson.getString("krs_bhn_moed_yom_dt"),
                        time = examJson.getString("krs_bhn_moed_mishaa"),
                        location = examJson.optString("hdr_shm", "Location not specified"),
                        eligibility = examJson.getString("mzv_zakaut_short_info"),
                        eligibilityDetails = examJson.optString("mzv_zakaut", ""),
                        semester = semesterHebrew,
                        semesterNumber = semesterNumber,
                        examMoed = examJson.getInt("krs_bhn_moed_mis"),
                        courseNumber = examJson.getString("krs_mis_kvuza"),
                        lecturer = examJson.optString("pm_shm", "Lecturer not specified"),
                        moedOrder = examJson.getString("moedorder"),
                        krsSnl = examJson.getString("krs_snl")
                    )
                )
            }

            examsList
        }
    }
}