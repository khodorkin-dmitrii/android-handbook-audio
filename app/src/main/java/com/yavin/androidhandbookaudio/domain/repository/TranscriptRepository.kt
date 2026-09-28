package com.yavin.androidhandbookaudio.domain.repository

import com.yavin.androidhandbookaudio.domain.model.TimedTranscript

interface TranscriptRepository {
    suspend fun getTimedTranscript(url: String, format: String): TimedTranscript
}
