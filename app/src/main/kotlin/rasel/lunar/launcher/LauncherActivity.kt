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

package rasel.lunar.launcher

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rasel.lunar.launcher.apps.AppDrawer
import rasel.lunar.launcher.databinding.LauncherActivityBinding
import rasel.lunar.launcher.feeds.Feeds
import rasel.lunar.launcher.feeds.WidgetHost
import rasel.lunar.launcher.helpers.Constants.Companion.KEY_BACK_HOME
import rasel.lunar.launcher.helpers.Constants.Companion.KEY_FIRST_LAUNCH
import rasel.lunar.launcher.helpers.Constants.Companion.KEY_STATUS_BAR
import rasel.lunar.launcher.helpers.Constants.Companion.KEY_WINDOW_BACKGROUND
import rasel.lunar.launcher.helpers.Constants.Companion.PREFS_FIRST_LAUNCH
import rasel.lunar.launcher.helpers.Constants.Companion.PREFS_SETTINGS
import rasel.lunar.launcher.helpers.Constants.Companion.widgetHostId
import rasel.lunar.launcher.helpers.UniUtils.Companion.getColorResId
import rasel.lunar.launcher.helpers.ViewPagerAdapter
import rasel.lunar.launcher.home.LauncherHome


internal class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: LauncherActivityBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var settingsPrefs: SharedPreferences

    companion object {
        private var instance: LauncherActivity? = null
        private var widgetManager: AppWidgetManager? = null
        private var widgetHost: WidgetHost? = null

        @JvmStatic val lActivity : LauncherActivity? get() = instance
        @JvmStatic val appWidgetManager: AppWidgetManager? get() = widgetManager
        @JvmStatic val appWidgetHost: WidgetHost? get() = widgetHost
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this
        widgetManager = AppWidgetManager.getInstance(applicationContext)
        widgetHost = WidgetHost(applicationContext, widgetHostId)
        appWidgetHost?.startListening()

        /*  if this is the first launch,
            then remember the event and show the welcome dialog */
        welcomeDialog()

        binding = LauncherActivityBinding.inflate(layoutInflater)
        settingsPrefs = getSharedPreferences(PREFS_SETTINGS, 0)

        /* set up activity's view */
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        setupView()

        /* handle navigation back events */
        handleBackPress()
    }

    override fun onDestroy() {
        super.onDestroy()
        appWidgetHost?.stopListening()
    }

    override fun onResume() {
        super.onResume()
        if (settingsPrefs.getBoolean(KEY_BACK_HOME, false)) viewPager.currentItem = 1
        statusBarView()
        setBgColor()
    }

    private fun welcomeDialog() {
        val prefsFirstLaunch = getSharedPreferences(PREFS_FIRST_LAUNCH, 0)
        if (prefsFirstLaunch.getBoolean(KEY_FIRST_LAUNCH, true)) {
            prefsFirstLaunch.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.welcome)
                .setMessage(R.string.welcome_description)
                .setPositiveButton(R.string.got_it) { dialog, _ ->
                    dialog.dismiss()
                    askPermissions()
                }.show()
        }
    }

    /* ask for the permissions */
    private fun askPermissions() {
        /* phone permission */
        if (this.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
        /* modify system settings */
        if (!Settings.System.canWrite(this)) {
            this.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    .setData(Uri.parse("package:" + this.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /* set up viewpager2 */
    private fun setupView() {
        viewPager = binding.viewPager
        val fragments = mutableListOf(Feeds(), LauncherHome(), AppDrawer())
        viewPager.adapter = ViewPagerAdapter(supportFragmentManager, fragments, lifecycle)
        viewPager.offscreenPageLimit = 1
        viewPager.setCurrentItem(1, false)
    }

    private fun setBgColor() {
        binding.root.setBackgroundColor(Color.parseColor("#${
            settingsPrefs.getString(KEY_WINDOW_BACKGROUND, getString(getColorResId(this, android.R.attr.colorBackground))
                .replace("#", ""))}"))
    }

    private fun statusBarView() {
        if (settingsPrefs.getBoolean(KEY_STATUS_BAR, false)) {
            /* hide status bar */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                @Suppress("DEPRECATION")
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            topPadding(false)
        } else {
            /* show status bar */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsets.Type.statusBars())
            } else {
                @Suppress("DEPRECATION")
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            topPadding(true)
        }
    }

    /* alternative of deprecated onBackPressed method */
    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /* while in todo manager, go back to home screen */
                if (supportFragmentManager.backStackEntryCount != 0) supportFragmentManager.popBackStack()

                /* while in feeds or app drawer, go back to home screen */
                if (viewPager.currentItem != 1) viewPager.currentItem = 1
            }
        })
    }

    private fun topPadding(topPadding: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures())
            view.updatePadding(insets.left, if (topPadding) insets.top else 0, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

}
