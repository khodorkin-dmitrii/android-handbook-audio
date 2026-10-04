package com.yavin.androidhandbookaudio.data.transcript

import com.yavin.androidhandbookaudio.domain.model.TranscriptStyleRange
import com.yavin.androidhandbookaudio.domain.model.TranscriptTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class SrtParserTest {
    private val parser = SrtParser()

    @Test
    fun `parses LF cues milliseconds and multiline text in source order`() {
        val transcript = parser.parse(
            """
            1
            00:00:00,125 --> 00:00:00,978
            First line
            continues here

            2
            00:00:01,100 --> 00:00:03,500
            Next text
            """.trimIndent(),
        )

        assertEquals(2, transcript.segments.size)
        assertEquals(125L, transcript.segments[0].startMs)
        assertEquals(978L, transcript.segments[0].endMs)
        assertEquals("First line continues here", transcript.segments[0].text)
        assertTrue(transcript.segments[0].styleRanges.isEmpty())
        assertEquals(1_100L, transcript.segments[1].startMs)
        assertEquals(3_500L, transcript.segments[1].endMs)
    }

    @Test
    fun `parses italic cue and removes markup`() {
        val segment = parseCue("<i>What is Gradle?</i>")

        assertEquals("What is Gradle?", segment.text)
        assertEquals(
            listOf(TranscriptStyleRange(0, 15, TranscriptTextStyle.ITALIC)),
            segment.styleRanges,
        )
    }

    @Test
    fun `parses bold cue`() {
        val segment = parseCue("Use <b>stable APIs</b>.")

        assertEquals("Use stable APIs.", segment.text)
        assertEquals(
            listOf(TranscriptStyleRange(4, 15, TranscriptTextStyle.BOLD)),
            segment.styleRanges,
        )
    }

    @Test
    fun `parses underlined cue`() {
        val segment = parseCue("Read the <u>documentation</u>.")

        assertEquals("Read the documentation.", segment.text)
        assertEquals(
            listOf(TranscriptStyleRange(9, 22, TranscriptTextStyle.UNDERLINE)),
            segment.styleRanges,
        )
    }

    @Test
    fun `supports nested formatting ranges`() {
        val segment = parseCue("<b><i>text</i></b>")

        assertEquals("text", segment.text)
        assertEquals(
            setOf(
                TranscriptStyleRange(0, 4, TranscriptTextStyle.BOLD),
                TranscriptStyleRange(0, 4, TranscriptTextStyle.ITALIC),
            ),
            segment.styleRanges.toSet(),
        )
    }

    @Test
    fun `strips unknown tags while preserving inner text`() {
        val segment = parseCue("Keep <font color=\"red\">this text</font> visible")

        assertEquals("Keep this text visible", segment.text)
        assertTrue(segment.styleRanges.isEmpty())
    }

    @Test
    fun `malformed supported markup does not crash`() {
        val segment = parseCue("<i>Still italic <b>and bold</i>")

        assertEquals("Still italic and bold", segment.text)
        assertEquals(
            setOf(
                TranscriptStyleRange(0, 21, TranscriptTextStyle.ITALIC),
                TranscriptStyleRange(13, 21, TranscriptTextStyle.BOLD),
            ),
            segment.styleRanges.toSet(),
        )
    }

    @Test
    fun `parses CRLF input`() {
        val transcript = parser.parse(
            "1\r\n01:02:03,004 --> 01:02:04,005\r\nCRLF text\r\n",
        )

        assertEquals(3_723_004L, transcript.segments.single().startMs)
        assertEquals(3_724_005L, transcript.segments.single().endMs)
        assertEquals("CRLF text", transcript.segments.single().text)
    }

    @Test
    fun `skips malformed cues and keeps valid cues`() {
        val transcript = parser.parse(
            """
            1
            not a timestamp
            Broken

            2
            00:00:04,000 --> 00:00:03,000
            Invalid range

            3
            00:00:05,000 --> 00:00:06,000
            Valid
            """.trimIndent(),
        )

        assertEquals(1, transcript.segments.size)
        assertEquals("Valid", transcript.segments.single().text)
    }

    @Test
    fun `rejects cues whose start times are out of order`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(
                """
                1
                00:00:05,000 --> 00:00:06,000
                Later cue first

                2
                00:00:01,000 --> 00:00:02,000
                Earlier cue second
                """.trimIndent(),
            )
        }

        assertEquals(
            "SRT cues must be ordered by nondecreasing start time",
            error.message,
        )
    }

    private fun parseCue(markup: String) = parser.parse(
        """
        1
        00:00:00,125 --> 00:00:01,500
        $markup
        """.trimIndent(),
    ).segments.single()
}
