package com.yavin.androidhandbookaudio.domain.model

data class TimedTranscript(
    val segments: List<TranscriptSegment>,
)

data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val styleRanges: List<TranscriptStyleRange> = emptyList(),
)

data class TranscriptStyleRange(
    val start: Int,
    val endExclusive: Int,
    val style: TranscriptTextStyle,
)

enum class TranscriptTextStyle {
    ITALIC,
    BOLD,
    UNDERLINE,
}
