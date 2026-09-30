package com.yavin.androidhandbookaudio.ui.player

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
import com.yavin.androidhandbookaudio.domain.model.TranscriptStyleRange
import com.yavin.androidhandbookaudio.domain.model.TranscriptTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptTextFormattingTest {
    @Test
    fun `maps domain formatting ranges to annotated string spans`() {
        val annotatedText = TranscriptSegment(
            startMs = 0,
            endMs = 1_000,
            text = "text",
            styleRanges = listOf(
                TranscriptStyleRange(0, 4, TranscriptTextStyle.ITALIC),
                TranscriptStyleRange(0, 4, TranscriptTextStyle.BOLD),
                TranscriptStyleRange(0, 4, TranscriptTextStyle.UNDERLINE),
            ),
        ).toAnnotatedString()

        assertEquals("text", annotatedText.text)
        assertEquals(3, annotatedText.spanStyles.size)
        assertTrue(annotatedText.spanStyles.any { range ->
            range.start == 0 && range.end == 4 && range.item.fontStyle == FontStyle.Italic
        })
        assertTrue(annotatedText.spanStyles.any { range ->
            range.start == 0 && range.end == 4 && range.item.fontWeight == FontWeight.Bold
        })
        assertTrue(annotatedText.spanStyles.any { range ->
            range.start == 0 &&
                range.end == 4 &&
                range.item.textDecoration == TextDecoration.Underline
        })
    }

    @Test
    fun `clamps invalid formatting ranges without crashing`() {
        val annotatedText = TranscriptSegment(
            startMs = 0,
            endMs = 1_000,
            text = "text",
            styleRanges = listOf(
                TranscriptStyleRange(-5, 50, TranscriptTextStyle.ITALIC),
                TranscriptStyleRange(10, 20, TranscriptTextStyle.BOLD),
            ),
        ).toAnnotatedString()

        assertEquals("text", annotatedText.text)
        assertEquals(1, annotatedText.spanStyles.size)
        assertEquals(0, annotatedText.spanStyles.single().start)
        assertEquals(4, annotatedText.spanStyles.single().end)
    }
}
