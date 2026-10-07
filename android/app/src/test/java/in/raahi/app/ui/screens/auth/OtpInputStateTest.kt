package `in`.raahi.app.ui.screens.auth

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class OtpInputStateTest {

    @Test
    fun testSequentialTyping_1to6() {
        var completedOtp: String? = null
        val state = OtpInputState { completedOtp = it }

        // Type 1
        state.onValueChanged(TextFieldValue("1", TextRange(1)))
        assertEquals(listOf("1", "", "", "", "", ""), state.digits)
        assertEquals(1, state.selectedIndex)
        assertNull(completedOtp)

        // Type 2
        state.onValueChanged(TextFieldValue("12", TextRange(2)))
        assertEquals(listOf("1", "2", "", "", "", ""), state.digits)
        assertEquals(2, state.selectedIndex)
        assertNull(completedOtp)

        // Type 3
        state.onValueChanged(TextFieldValue("123", TextRange(3)))
        assertEquals(listOf("1", "2", "3", "", "", ""), state.digits)
        assertEquals(3, state.selectedIndex)
        assertNull(completedOtp)

        // Type 4
        state.onValueChanged(TextFieldValue("1234", TextRange(4)))
        assertEquals(listOf("1", "2", "3", "4", "", ""), state.digits)
        assertEquals(4, state.selectedIndex)
        assertNull(completedOtp)

        // Type 5
        state.onValueChanged(TextFieldValue("12345", TextRange(5)))
        assertEquals(listOf("1", "2", "3", "4", "5", ""), state.digits)
        assertEquals(5, state.selectedIndex)
        assertNull(completedOtp)

        // Type 6
        state.onValueChanged(TextFieldValue("123456", TextRange(6)))
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), state.digits)
        assertEquals("123456", state.code)
        assertEquals("123456", completedOtp)
        assertTrue(state.isComplete)
    }

    @Test
    fun testBackspaceHandling() {
        val state = OtpInputState("1234")
        assertEquals(listOf("1", "2", "3", "4", "", ""), state.digits)

        // Backspace removes 4
        state.onValueChanged(TextFieldValue("123", TextRange(3)))
        assertEquals(listOf("1", "2", "3", "", "", ""), state.digits)
        assertEquals(3, state.selectedIndex)

        // Backspace removes 3
        state.onValueChanged(TextFieldValue("12", TextRange(2)))
        assertEquals(listOf("1", "2", "", "", "", ""), state.digits)
        assertEquals(2, state.selectedIndex)
    }

    @Test
    fun testInPlaceReplacement() {
        val state = OtpInputState("123456")
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), state.digits)

        // User taps cell 2 (index 1 which has '2')
        state.selectCell(1)
        assertEquals(1, state.selectedIndex)

        // User types '9' to replace '2'
        state.onValueChanged(TextFieldValue("193456", TextRange(2)))
        assertEquals(listOf("1", "9", "3", "4", "5", "6"), state.digits)
        assertEquals("193456", state.code)
        assertEquals(2, state.selectedIndex)
    }

    @Test
    fun testPastingSixDigitOtp() {
        var completedOtp: String? = null
        val state = OtpInputState { completedOtp = it }

        // Paste "852963"
        state.onValueChanged(TextFieldValue("852963", TextRange(6)))
        assertEquals(listOf("8", "5", "2", "9", "6", "3"), state.digits)
        assertEquals("852963", state.code)
        assertEquals("852963", completedOtp)
        assertTrue(state.isComplete)
    }

    @Test
    fun testNonDigitRejection() {
        val state = OtpInputState("12")
        // User types letter 'a'
        state.onValueChanged(TextFieldValue("12a", TextRange(3)))
        assertEquals(listOf("1", "2", "", "", "", ""), state.digits)
        assertEquals("12", state.code)
    }
}
