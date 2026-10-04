package com.yavin.androidhandbookaudio.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRevealTransitionTest {
    @Test
    fun `radius reaches the farthest corner from reveal origin`() {
        val size = Size(width = 300f, height = 400f)
        val origin = Offset(x = 300f, y = 0f)

        assertEquals(500f, calculateRevealRadius(origin, size), 0.001f)
    }

    @Test
    fun `center origin uses diagonal half size`() {
        val size = Size(width = 300f, height = 400f)
        val origin = Offset(x = 150f, y = 200f)

        assertEquals(250f, calculateRevealRadius(origin, size), 0.001f)
    }

    @Test
    fun `unavailable origin falls back near top right within root bounds`() {
        val size = Size(width = 1_000f, height = 2_000f)

        assertEquals(
            Offset(x = 900f, y = 200f),
            resolveRevealOrigin(Offset.Unspecified, size),
        )
    }

    @Test
    fun `measured origin is clamped into current root bounds`() {
        val size = Size(width = 300f, height = 400f)

        assertEquals(
            Offset(x = 300f, y = 0f),
            resolveRevealOrigin(Offset(x = 450f, y = -20f), size),
        )
    }
}
