package com.yavin.androidhandbookaudio.data.transcript

import org.junit.Assert.assertEquals
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
        assertEquals(1_100L, transcript.segments[1].startMs)
        assertEquals(3_500L, transcript.segments[1].endMs)
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
}
