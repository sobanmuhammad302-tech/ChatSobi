package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.OfflineKnowledgeEngine
import com.example.model.KnowledgeDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ChatSobi", appName)
    }

    @Test
    fun `offline knowledge engine answers science query`() {
        val result = OfflineKnowledgeEngine.answerQuery("Explain Quantum Entanglement", KnowledgeDomain.SCIENCE)
        assertNotNull(result)
        assertTrue(result.text.contains("entanglement") || result.text.contains("Quantum"))
        assertTrue(result.isOfflineEngine)
    }

    @Test
    fun `offline knowledge engine solves math expression accurately`() {
        val result = OfflineKnowledgeEngine.answerQuery("calculate 25 * 4", KnowledgeDomain.MATHEMATICS)
        assertNotNull(result)
        assertTrue(result.text.contains("100"))

        val mulResult = OfflineKnowledgeEngine.answerQuery("50 * 30")
        assertNotNull(mulResult)
        assertTrue(mulResult.text.contains("1500"))
    }

    @Test
    fun `offline knowledge engine answers direct facts accurately`() {
        val speedResult = OfflineKnowledgeEngine.answerQuery("what is the speed of light")
        assertNotNull(speedResult)
        assertTrue(speedResult.text.contains("299,792,458"))

        val capitalResult = OfflineKnowledgeEngine.answerQuery("what is the capital of france")
        assertNotNull(capitalResult)
        assertTrue(capitalResult.text.contains("Paris"))
    }

    @Test
    fun `formula formatter converts raw symbols to clean unicode`() {
        val raw = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"
        val clean = com.example.ui.components.FormulaFormatter.cleanFormula(raw)
        assertTrue(clean.contains("±"))
        assertTrue(clean.contains("√"))
        assertTrue(!clean.contains("\\frac"))
    }
}
