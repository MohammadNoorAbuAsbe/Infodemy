package com.MohammadNoorAbuAsbe.Infodemy.data.repository

import com.MohammadNoorAbuAsbe.Infodemy.data.models.Krs
import com.MohammadNoorAbuAsbe.Infodemy.data.models.MaazanData
import com.MohammadNoorAbuAsbe.Infodemy.data.models.MasHit
import com.MohammadNoorAbuAsbe.Infodemy.data.models.Msl
import com.MohammadNoorAbuAsbe.Infodemy.data.models.Tchum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MaazanRepository(private val client: OkHttpClient) {

    /**
     * Fetches Maazan data from the server
     */
    suspend fun fetchMaazanData(token: String): MaazanData {
        val msl = fetchMaazanConfig(token)

        return withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("msl", JSONObject().apply {
                    put("nmrtr", msl.nmrtr)
                    put("ptMsl", msl.ptMsl)
                    put("isTofesTiulim", msl.isTofesTiulim)
                    put("name", msl.name)
                    put("pdg", msl.pdg)
                    put("__hash", msl.__hash)
                })
            }.toString()

            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/StudentMaazanCommon/GetMaazan")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Unexpected response code: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            parseMaazanData(responseBody)
        }
    }

    private suspend fun fetchMaazanConfig(token: String): Msl {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Maazan/Data")
                .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Config request failed: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty config response")
            parseConfigResponse(responseBody)
        }
    }

    private fun parseConfigResponse(responseBody: String): Msl {
        val json = JSONObject(responseBody)
        val mslsArray = json.getJSONArray("msls")

        if (mslsArray.length() == 0) {
            throw Exception("No msl configuration found")
        }

        // Take the first msl object
        val mslJson = mslsArray.getJSONObject(0)
        return Msl(
            nmrtr = mslJson.getInt("nmrtr"),
            ptMsl = mslJson.getInt("ptMsl"),
            isTofesTiulim = mslJson.getBoolean("isTofesTiulim"),
            name = mslJson.getString("name"),
            pdg = mslJson.getString("pdg"),
            __hash = mslJson.getString("__hash")
        )
    }

    /**
     * Parses the JSON response into MaazanData object
     */
    private fun parseMaazanData(responseBody: String): MaazanData {
        val jsonObject = JSONObject(responseBody)
        val maazanObject = jsonObject.getJSONObject("maazan")
        val masHitsArray = maazanObject.getJSONArray("masHits")

        val masHits = mutableListOf<MasHit>()

        for (i in 0 until masHitsArray.length()) {
            val masHitJson = masHitsArray.getJSONObject(i)

            // Parse tchums if they exist
            val tchums = mutableListOf<Tchum>()
            if (masHitJson.has("tchums") && !masHitJson.isNull("tchums")) {
                val tchumsArray = masHitJson.getJSONArray("tchums")
                for (j in 0 until tchumsArray.length()) {
                    val tchumJson = tchumsArray.getJSONObject(j)

                    // Parse krss if they exist
                    val krss = mutableListOf<Krs>()
                    if (tchumJson.has("krss") && !tchumJson.isNull("krss")) {
                        val krssArray = tchumJson.getJSONArray("krss")
                        for (k in 0 until krssArray.length()) {
                            val krsJson = krssArray.getJSONObject(k)
                            krss.add(
                                Krs(
                                    style = krsJson.getString("style"),
                                    name = krsJson.getString("name"),
                                    zin = krsJson.getString("zin"),
                                    nidrash = krsJson.getString("nidrash"),
                                    nirsham = krsJson.getString("nirsham"),
                                    nilmad = krsJson.getString("nilmad"),
                                    ptor = krsJson.getString("ptor"),
                                    notar = krsJson.getString("notar"),
                                    ahuz = krsJson.getString("ahuz"),
                                    description = krsJson.optString("description"),
                                    isNotComplete = krsJson.getBoolean("isNotComplete")
                                )
                            )
                        }
                    }

                    tchums.add(
                        Tchum(
                            secondariesTchums = tchumJson.optJSONArray("secondariesTchums"),
                            krss = krss,
                            name = tchumJson.getString("name"),
                            zin = tchumJson.getString("zin"),
                            nidrash = tchumJson.getString("nidrash"),
                            nirsham = tchumJson.getString("nirsham"),
                            nilmad = tchumJson.getString("nilmad"),
                            ptor = tchumJson.getString("ptor"),
                            notar = tchumJson.getString("notar"),
                            ahuz = tchumJson.getString("ahuz"),
                            description = tchumJson.optString("description"),
                            isNotComplete = tchumJson.getBoolean("isNotComplete")
                        )
                    )
                }
            }

            masHits.add(
                MasHit(
                    tchums = tchums,
                    isSumUpRecord = masHitJson.getBoolean("isSumUpRecord"),
                    name = masHitJson.getString("name"),
                    zin = masHitJson.getString("zin"),
                    nidrash = masHitJson.getString("nidrash"),
                    nirsham = masHitJson.getString("nirsham"),
                    nilmad = masHitJson.getString("nilmad"),
                    ptor = masHitJson.getString("ptor"),
                    notar = masHitJson.getString("notar"),
                    ahuz = masHitJson.getString("ahuz"),
                    description = masHitJson.optString("description"),
                    isNotComplete = masHitJson.getBoolean("isNotComplete")
                )
            )
        }
        println(MaazanData(masHits = masHits))
        return MaazanData(masHits = masHits)
    }
}