package `in`.raahi.app.ui.screens.auth

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneInputHandlerTest {

    @Test
    fun testContinuousTypingSequential() {
        var state = TextFieldValue("")
        val digits = "7009750326"
        
        for (i in digits.indices) {
            val nextChar = digits[i]
            val newText = state.text + nextChar
            val newTfv = TextFieldValue(newText, selection = TextRange(newText.length))
            state = PhoneInputHandler.processInput(state, newTfv)
            
            assertEquals("Substring up to step $i should match", digits.substring(0, i + 1), state.text)
            assertEquals("Cursor should be at end", i + 1, state.selection.start)
        }
        
        assertEquals("7009750326", state.text)
    }

    @Test
    fun testEleventhDigitRejected() {
        var state = TextFieldValue("7009750326", selection = TextRange(10))
        
        // Attempting to type 11th digit '8'
        val eleventhInput = TextFieldValue("70097503268", selection = TextRange(11))
        val result = PhoneInputHandler.processInput(state, eleventhInput)
        
        assertEquals("11th digit must be rejected", "7009750326", result.text)
        assertEquals("Cursor must remain at 10", 10, result.selection.start)
    }

    @Test
    fun testBackspaceHandling() {
        var state = TextFieldValue("7009750326", selection = TextRange(10))
        
        // Backspace removes '6'
        val backspaced = TextFieldValue("700975032", selection = TextRange(9))
        state = PhoneInputHandler.processInput(state, backspaced)
        
        assertEquals("700975032", state.text)
        assertEquals(9, state.selection.start)
    }

    @Test
    fun testPasteWithCountryCodeAndSpaces() {
        val current = TextFieldValue("")
        
        // Pasting "+91 70097 50326"
        val pasted = TextFieldValue("+91 70097 50326", selection = TextRange(15))
        val result = PhoneInputHandler.processInput(current, pasted)
        
        assertEquals("7009750326", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun testPasteWithLeadingZero() {
        val current = TextFieldValue("")
        
        // Pasting "07009750326"
        val pasted = TextFieldValue("07009750326", selection = TextRange(11))
        val result = PhoneInputHandler.processInput(current, pasted)
        
        assertEquals("7009750326", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun testPasteWithDashes() {
        val current = TextFieldValue("")
        
        // Pasting "70097-50326"
        val pasted = TextFieldValue("70097-50326", selection = TextRange(11))
        val result = PhoneInputHandler.processInput(current, pasted)
        
        assertEquals("7009750326", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun testPasteExceedingTenDigitsClampsToTen() {
        val current = TextFieldValue("")
        
        // Pasting 12 random digits
        val pasted = TextFieldValue("700975032699", selection = TextRange(12))
        val result = PhoneInputHandler.processInput(current, pasted)
        
        assertEquals("7009750326", result.text)
        assertEquals(10, result.selection.start)
    }
}
