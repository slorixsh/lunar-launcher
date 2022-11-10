/*
 * Lunar Launcher
 * Copyright (C) 2022 Md Rasel Hossain
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rasel.lunar.launcher.settings.childs

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rasel.lunar.launcher.R
import rasel.lunar.launcher.databinding.ColorPickerBinding
import rasel.lunar.launcher.databinding.SettingsAppearancesBinding
import rasel.lunar.launcher.helpers.ColorPicker
import rasel.lunar.launcher.helpers.Constants
import rasel.lunar.launcher.helpers.UniUtils
import rasel.lunar.launcher.settings.PrefsUtil
import java.util.*


internal class Appearances : BottomSheetDialogFragment() {

    private lateinit var binding : SettingsAppearancesBinding
    private lateinit var windowBackground : String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingsAppearancesBinding.inflate(inflater, container, false)

        /* initialize views according to the saved values */
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> binding.followSystemTheme.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.selectDarkTheme.isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> binding.selectLightTheme.isChecked = true
            else -> binding.followSystemTheme.isChecked = true
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireDialog() as BottomSheetDialog).dismissWithAnimation = true

        /* change theme */
        binding.themeGroup.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (isChecked) {
                when (checkedId) {
                    binding.followSystemTheme.id ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    binding.selectDarkTheme.id ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    binding.selectLightTheme.id ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }

        binding.background.setOnClickListener { selectBackground() }
    }

    override fun onResume() {
        super.onResume()
        windowBackground = requireContext().getSharedPreferences(Constants().PREFS_SETTINGS, 0)
            .getString(Constants().KEY_WINDOW_BACKGROUND, requireActivity().getString(colorId())).toString()
        binding.background.iconTint = if (windowBackground.contains("#")) {
            ColorStateList.valueOf(Color.parseColor(windowBackground))
        } else {
            ColorStateList.valueOf(Color.parseColor("#${windowBackground}"))
        }
    }

    private fun selectBackground() {
        val prefsUtil = PrefsUtil()
        val colorPickerBinding = ColorPickerBinding.inflate(requireActivity().layoutInflater)
        val dialogBuilder = MaterialAlertDialogBuilder(requireActivity())
            .setView(colorPickerBinding.root)
            .setNeutralButton(R.string.default_, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefsUtil.windowBackground(requireContext(),
                    Objects.requireNonNull(colorPickerBinding.colorInput.text).toString().trim { it <= ' ' })
                this.onResume()
            }
            .show()

        /* set up color picker section */
        ColorPicker(windowBackground.replace("#", ""), colorPickerBinding.colorInput,
            colorPickerBinding.colorA, colorPickerBinding.colorR, colorPickerBinding.colorG,
            colorPickerBinding.colorB, colorPickerBinding.root).pickColor()

        dialogBuilder.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            colorPickerBinding.colorInput.text =
                SpannableStringBuilder(requireActivity().getString(colorId()).replace("#", ""))
        }
    }

    private fun colorId(): Int = UniUtils().getColorResId(requireContext(), android.R.attr.colorBackground)

}