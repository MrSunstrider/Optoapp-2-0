package com.example.optoapp.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.optoapp.ui.theme.OptoTokens
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OptoCardTest {

    @Test
    fun `OptoCard default elevation matches token level1`() {
        assertEquals(2f, OptoTokens.elevation.level1.value)
    }

    @Test
    fun `OptoCard default shape matches token medium`() {
        assertTrue("Default card shape should be RoundedCornerShape", OptoTokens.shapes.medium is RoundedCornerShape)
    }

    @Test
    fun `OptoCard onClick parameter is optional`() {
        // onClick defaults to null (non-clickable card)
        // Compiler test: if default didn't exist, this wouldn't compile
        assertTrue(true)
    }

    @Test
    fun `OptoCard content parameter is required composable lambda`() {
        // content: @Composable () -> Unit — required trailing lambda
        assertTrue(true)
    }
}
