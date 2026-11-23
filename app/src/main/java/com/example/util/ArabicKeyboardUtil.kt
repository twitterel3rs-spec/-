package com.example.util

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList

/**
 * Utility to enforce Arabic keyboard hints (hintLocales = "ar") on text input fields
 * across the application, preventing Android IMEs (e.g. Gboard) from unexpectedly switching
 * to English Latin mode after focusing numbers or other inputs.
 */
object ArabicKeyboardUtil {
    val ArabicLocaleList = LocaleList(Locale("ar"))

    fun text(
        imeAction: ImeAction = ImeAction.Default,
        autoCorrect: Boolean = false
    ): KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrect = autoCorrect,
        keyboardType = KeyboardType.Text,
        imeAction = imeAction,
        hintLocales = ArabicLocaleList
    )

    fun search(
        imeAction: ImeAction = ImeAction.Search
    ): KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrect = false,
        keyboardType = KeyboardType.Text,
        imeAction = imeAction,
        hintLocales = ArabicLocaleList
    )

    fun number(
        imeAction: ImeAction = ImeAction.Default
    ): KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = imeAction
    )

    fun decimal(
        imeAction: ImeAction = ImeAction.Default
    ): KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Decimal,
        imeAction = imeAction
    )

    fun phone(
        imeAction: ImeAction = ImeAction.Default
    ): KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Phone,
        imeAction = imeAction
    )
}
