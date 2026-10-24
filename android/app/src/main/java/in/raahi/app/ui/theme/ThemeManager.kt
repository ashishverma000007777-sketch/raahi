package `in`.raahi.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "raahi_theme_preferences"
    private const val KEY_THEME = "selected_app_theme"

    const val THEME_SYSTEM = "System"
    const val THEME_DARK = "Dark"
    const val THEME_LIGHT = "Light"

    private var prefs: SharedPreferences? = null
    private val _themeFlow = MutableStateFlow(THEME_LIGHT)
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedTheme = prefs?.getString(KEY_THEME, THEME_LIGHT) ?: THEME_LIGHT
            _themeFlow.value = savedTheme
        }
    }

    fun setTheme(theme: String) {
        if (theme in listOf(THEME_SYSTEM, THEME_DARK, THEME_LIGHT)) {
            _themeFlow.value = theme
            prefs?.edit()?.putString(KEY_THEME, theme)?.apply()
        }
    }

    val currentTheme: String
        get() = _themeFlow.value
}
