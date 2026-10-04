package com.yavin.androidhandbookaudio.data.transcript

import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
import com.yavin.androidhandbookaudio.domain.model.TranscriptStyleRange
import com.yavin.androidhandbookaudio.domain.model.TranscriptTextStyle
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
        require(segments.zipWithNext().all { (previous, current) ->
            previous.startMs <= current.startMs
        }) {
            "SRT cues must be ordered by nondecreasing start time"
        }
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

        val markup = lines
            .drop(timingIndex + 1)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .trim()
        if (markup.isEmpty()) return null

        val parsedText = parseInlineFormatting(markup)
        if (parsedText.text.isEmpty()) return null

        return TranscriptSegment(
            startMs = startMs,
            endMs = endMs,
            text = parsedText.text,
            styleRanges = parsedText.styleRanges,
        )
    }

    private fun parseInlineFormatting(markup: String): ParsedTranscriptText {
        val output = StringBuilder()
        val styleRanges = mutableListOf<TranscriptStyleRange>()
        val openRanges = TranscriptTextStyle.entries.associateWith { ArrayDeque<Int>() }
        var sourceIndex = 0

        INLINE_TAG_PATTERN.findAll(markup).forEach { match ->
            output.append(markup, sourceIndex, match.range.first)
            sourceIndex = match.range.last + 1

            val style = match.groupValues[2].toTranscriptTextStyle() ?: return@forEach
            if (match.value.dropLast(1).trimEnd().endsWith('/')) return@forEach

            val starts = openRanges.getValue(style)
            if (match.groupValues[1].isEmpty()) {
                starts.addLast(output.length)
            } else if (starts.isNotEmpty()) {
                styleRanges.addStyleRange(starts.removeLast(), output.length, style)
            }
        }
        output.append(markup, sourceIndex, markup.length)

        openRanges.forEach { (style, starts) ->
            while (starts.isNotEmpty()) {
                styleRanges.addStyleRange(starts.removeLast(), output.length, style)
            }
        }

        return ParsedTranscriptText(
            text = output.toString(),
            styleRanges = styleRanges.sortedWith(
                compareBy<TranscriptStyleRange>(TranscriptStyleRange::start)
                    .thenBy(TranscriptStyleRange::endExclusive)
                    .thenBy { it.style.ordinal },
            ),
        )
    }

    private fun MutableList<TranscriptStyleRange>.addStyleRange(
        start: Int,
        endExclusive: Int,
        style: TranscriptTextStyle,
    ) {
        if (start < endExclusive) {
            add(TranscriptStyleRange(start, endExclusive, style))
        }
    }

    private fun String.toTranscriptTextStyle(): TranscriptTextStyle? = when (lowercase()) {
        "i" -> TranscriptTextStyle.ITALIC
        "b" -> TranscriptTextStyle.BOLD
        "u" -> TranscriptTextStyle.UNDERLINE
        else -> null
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
        val INLINE_TAG_PATTERN = Regex(
            pattern = """<\s*(/?)\s*([A-Za-z][A-Za-z0-9]*)(?:\s+[^>]*)?\s*/?>""",
            option = RegexOption.IGNORE_CASE,
        )
        val TIMING_PATTERN = Regex(
            """(\d+):([0-5]\d):([0-5]\d)[,.](\d{3})\s*-->\s*(\d+):([0-5]\d):([0-5]\d)[,.](\d{3})(?:\s+.*)?""",
        )
    }

    private data class ParsedTranscriptText(
        val text: String,
        val styleRanges: List<TranscriptStyleRange>,
    )
}
