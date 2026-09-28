package com.yavin.androidhandbookaudio.domain.model

data class TimedTranscript(
    val segments: List<TranscriptSegment>,
)

data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)
