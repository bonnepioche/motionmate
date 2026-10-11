/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.motionmate.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.tiefensuche.motionmate.stats.ProgressIconOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.util.Database
import com.tiefensuche.motionmate.util.Util
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val packageInfo = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0)

            findPreference<Preference>("app_version")?.summary =
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"

            findPreference<SeekBarPreference>("step_width")?.setOnPreferenceChangeListener { _, newValue ->
                Util.stepWidth = newValue as Int
                true
            }
            findPreference<ListPreference>("theme")?.setOnPreferenceChangeListener { _, newValue ->
                Util.applyTheme(newValue.toString())
                true
            }
            findPreference<ListPreference>("progress_icon_style")?.let {
                updateProgressIconPreview(it)
            }
            findPreference<Preference>("import")?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/*"
                }
                startActivityForResult(intent, 1)
                true
            }
            findPreference<Preference>("export")?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/*"
                    putExtra(Intent.EXTRA_TITLE, "step_data.csv")
                }
                startActivityForResult(intent, 2)
                true
            }
        }

        /** The visual chooser uses the exact ten GoalProgressView drawings from Home. */
        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (preference !is ListPreference || preference.key != "progress_icon_style") {
                super.onDisplayPreferenceDialog(preference)
                return
            }
            val values = preference.entryValues
            val adapter = ProgressIconPreviewAdapter(
                requireContext(),
                preference.entries,
                values,
                resources.getStringArray(R.array.progress_icon_descriptions),
                preference.value
            )
            val selected = values.indexOfFirst {
                it.toString() == ProgressIconOptions.normalize(preference.value)
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.progress_icon_title)
                .setSingleChoiceItems(adapter, selected) { dialog, position ->
                    val selectedValue = values[position].toString()
                    if (preference.callChangeListener(selectedValue)) {
                        preference.value = selectedValue
                        updateProgressIconPreview(preference)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        /** Display the currently selected symbol directly in the Settings row. */
        private fun updateProgressIconPreview(preference: ListPreference) {
            val size = (48f * resources.displayMetrics.density + 0.5f).toInt()
            val preview = GoalProgressView(requireContext()).apply {
                setIconStyle(preference.value)
                setProgress(0.75f, 75)
            }
            val exactly = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
            preview.measure(exactly, exactly)
            preview.layout(0, 0, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            preview.draw(Canvas(bitmap))
            preference.icon = BitmapDrawable(resources, bitmap)
            preference.isIconSpaceReserved = true
        }

        override fun onActivityResult(
            requestCode: Int, resultCode: Int, resultData: Intent?) {
            super.onActivityResult(requestCode, resultCode, resultData)
            if (resultCode != Activity.RESULT_OK)
                return
            resultData?.data?.also { uri ->
                when (requestCode) {
                    1 -> import(uri)
                    2 -> export(uri)
                }
            }
        }

        private fun import(uri: Uri) {
            val db = Database.getInstance(requireContext())
            try {
                requireContext().contentResolver.openFileDescriptor(uri, "r")?.use {
                    FileInputStream(it.fileDescriptor).bufferedReader().use {
                        var entries = 0
                        var failed = 0
                        for (line in it.readLines()) {
                            entries += 1
                            try {
                                val split = line.split(",")
                                db.addEntry(split[0].toLong(), split[1].toInt())
                            } catch (ex: Exception) {
                                println("Can not import entry, ${ex.message}")
                                failed += 1
                            }
                        }
                        Toast.makeText(context, "Imported ${entries - failed} entries ($failed failed).", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (ex: Exception) {
                println("Can not open file, ${ex.message}")
                Toast.makeText(context, "Can not open file", Toast.LENGTH_LONG).show()
            }
        }

        private fun export(uri: Uri) {
            val db = Database.getInstance(requireContext())
            try {
                requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).bufferedWriter().use {
                        var entries = 0
                        for (entry in db.getEntries(db.firstEntry, db.lastEntry)) {
                            it.write("${entry.timestamp},${entry.steps}\r\n")
                            entries += 1
                        }
                        Toast.makeText(context, "Exported $entries entries.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (ex: Exception) {
                println("Can not open file, ${ex.message}")
                Toast.makeText(context, "Can not open file", Toast.LENGTH_LONG).show()
            }
        }
    }
}
