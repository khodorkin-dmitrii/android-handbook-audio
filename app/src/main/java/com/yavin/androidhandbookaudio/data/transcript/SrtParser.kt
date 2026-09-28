package com.yavin.androidhandbookaudio.data.transcript

import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
import javax.inject.Inject

class SrtParser @Inject constructor() {
    fun parse(input: String): TimedTranscript {
        val normalizedInput = input
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
        if (normalizedInput.isEmpty()) return TimedTranscript(emptyList())

        val segments = normalizedInput
            .split(BLOCK_SEPARATOR)
            .mapNotNull(::parseBlock)
        return TimedTranscript(segments)
    }

    private fun parseBlock(block: String): TranscriptSegment? {
        val lines = block.lines().map(String::trim)
        val timingIndex = lines.indexOfFirst { "-->" in it }
        if (timingIndex == -1) return null

        val timing = TIMING_PATTERN.matchEntire(lines[timingIndex]) ?: return null
        val startMs = timing.groupValues.timestampAt(1) ?: return null
        val endMs = timing.groupValues.timestampAt(5) ?: return null
        if (endMs <= startMs) return null

        val text = lines
            .drop(timingIndex + 1)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .trim()
        if (text.isEmpty()) return null

        return TranscriptSegment(
            startMs = startMs,
            endMs = endMs,
            text = text,
        )
    }

    private fun List<String>.timestampAt(offset: Int): Long? {
        val hours = getOrNull(offset)?.toLongOrNull() ?: return null
        val minutes = getOrNull(offset + 1)?.toLongOrNull()?.takeIf { it in 0..59 } ?: return null
        val seconds = getOrNull(offset + 2)?.toLongOrNull()?.takeIf { it in 0..59 } ?: return null
        val millis = getOrNull(offset + 3)?.toLongOrNull() ?: return null
        return hours * 3_600_000 + minutes * 60_000 + seconds * 1_000 + millis
    }

    private companion object {
        val BLOCK_SEPARATOR = Regex("\\n\\s*\\n+")
        val TIMING_PATTERN = Regex(
            """(\d+):([0-5]\d):([0-5]\d)[,.](\d{3})\s*-->\s*(\d+):([0-5]\d):([0-5]\d)[,.](\d{3})(?:\s+.*)?""",
        )
    }
}
