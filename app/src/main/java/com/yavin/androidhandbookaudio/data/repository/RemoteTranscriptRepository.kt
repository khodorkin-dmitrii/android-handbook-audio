package com.yavin.androidhandbookaudio.data.repository

import com.yavin.androidhandbookaudio.data.transcript.SrtParser
import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.repository.TranscriptRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RemoteTranscriptRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val srtParser: SrtParser,
) : TranscriptRepository {
    override suspend fun getTimedTranscript(url: String, format: String): TimedTranscript {
        require(format.equals(SUPPORTED_FORMAT, ignoreCase = true)) {
            "Unsupported timed transcript format: $format"
        }
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Transcript request failed with HTTP ${response.code}")
                }
                srtParser.parse(response.body.string())
            }
        }
    }

    private companion object {
        const val SUPPORTED_FORMAT = "srt"
    }
}
