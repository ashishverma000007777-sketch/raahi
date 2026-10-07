package `in`.raahi.app.ui.screens.auth

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Robust controller for 6-cell OTP entry that avoids multi-TextField focus conflicts
 * and maintains continuous IME synchronization on Android software keyboards.
 */
class OtpInputState(
    initialCode: String = "",
    val onComplete: (String) -> Unit = {}
) {
    var digits: List<String> = List(6) { i -> initialCode.getOrNull(i)?.toString() ?: "" }
        private set

    var selectedIndex: Int = initialCode.length.coerceIn(0, 6)
        private set

    var textFieldValue: TextFieldValue = TextFieldValue(
        text = initialCode.filter(Char::isDigit).take(6),
        selection = TextRange(initialCode.filter(Char::isDigit).take(6).length)
    )
        private set

    val code: String
        get() = digits.joinToString("")

    val isComplete: Boolean
        get() = code.length == 6 && digits.all { it.isNotEmpty() }

    /**
     * Handles changes from the backing BasicTextField.
     */
    fun onValueChanged(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        val newText = newValue.text

        // Case 1: Pasted multi-character input
        val newDigitsOnly = newText.filter(Char::isDigit)
        if (newDigitsOnly.length - oldText.length > 1 || (oldText.isEmpty() && newDigitsOnly.length > 1)) {
            val pasted = newDigitsOnly.take(6)
            digits = List(6) { i -> pasted.getOrNull(i)?.toString() ?: "" }
            selectedIndex = pasted.length
            textFieldValue = TextFieldValue(text = pasted, selection = TextRange(pasted.length))
            if (pasted.length == 6) {
                onComplete(pasted)
            }
            return
        }

        // Case 2: Backspace pressed (text shortened)
        if (newText.length < oldText.length) {
            val deleteIndex = newValue.selection.start
            val updated = oldText.removeRange(deleteIndex, deleteIndex + (oldText.length - newText.length))
            digits = List(6) { i -> updated.getOrNull(i)?.toString() ?: "" }
            selectedIndex = deleteIndex.coerceIn(0, 5)
            textFieldValue = TextFieldValue(text = updated, selection = TextRange(selectedIndex))
            return
        }

        // Case 3A: Same length, but characters changed (in-place replacement)
        if (newText.length == oldText.length && newText != oldText) {
            val currentList = digits.toMutableList()
            var changedIndex = -1
            for (i in 0 until minOf(newText.length, 6)) {
                if (newText[i] != oldText.getOrNull(i)) {
                    changedIndex = i
                    break
                }
            }
            if (changedIndex != -1 && newText[changedIndex].isDigit()) {
                currentList[changedIndex] = newText[changedIndex].toString()
                digits = currentList
                val nextIndex = (changedIndex + 1).coerceIn(0, 6)
                selectedIndex = nextIndex
                val finalText = currentList.joinToString("").take(6)
                textFieldValue = TextFieldValue(text = finalText, selection = TextRange(nextIndex.coerceAtMost(finalText.length)))
                if (currentList.all { it.isNotEmpty() } && currentList.size == 6) {
                    onComplete(finalText)
                }
            } else {
                textFieldValue = TextFieldValue(text = oldText, selection = TextRange(oldText.length))
            }
            return
        }

        // Case 3B: Single digit entered (length increased)
        if (newText.length > oldText.length) {
            val insertIndex = (newValue.selection.start - 1).coerceAtLeast(0)
            val insertedChar = newText.getOrNull(insertIndex) ?: newText.lastOrNull()
            if (insertedChar != null && insertedChar.isDigit()) {
                val currentList = digits.toMutableList()
                
                // If a cell was selected, replace at selected index; otherwise append
                val targetIndex = if (selectedIndex in 0..5) selectedIndex else oldText.length.coerceIn(0, 5)
                
                currentList[targetIndex] = insertedChar.toString()
                digits = currentList
                
                val nextIndex = (targetIndex + 1).coerceIn(0, 6)
                selectedIndex = nextIndex
                val finalText = currentList.joinToString("").take(6)
                textFieldValue = TextFieldValue(text = finalText, selection = TextRange(nextIndex.coerceAtMost(finalText.length)))
                
                if (currentList.all { it.isNotEmpty() } && currentList.size == 6) {
                    val completeCode = currentList.joinToString("")
                    if (completeCode.length == 6) {
                        onComplete(completeCode)
                    }
                }
            } else {
                // Non-digit input rejected; preserve existing valid state
                textFieldValue = TextFieldValue(text = oldText, selection = TextRange(oldText.length))
            }
            return
        }

        // Case 4: Selection/cursor change only
        val cursor = newValue.selection.start
        selectedIndex = cursor.coerceIn(0, 5)
        textFieldValue = newValue
    }

    /**
     * User tapped a specific cell.
     */
    fun selectCell(index: Int) {
        val target = index.coerceIn(0, 5)
        val currentLen = code.length
        val effectiveIndex = if (target <= currentLen) target else currentLen.coerceAtMost(5)
        selectedIndex = effectiveIndex
        textFieldValue = TextFieldValue(
            text = code,
            selection = TextRange(effectiveIndex.coerceAtMost(code.length))
        )
    }

    /**
     * Reset the input.
     */
    fun clear() {
        digits = List(6) { "" }
        selectedIndex = 0
        textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
    }
}
