// SPDX-FileCopyrightText: 2023 yuzu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

package org.yuzu.yuzu_emu.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.yuzu.yuzu_emu.R
import org.yuzu.yuzu_emu.adapters.HomeSettingAdapter
import org.yuzu.yuzu_emu.databinding.FragmentHomeSettingsBinding
import org.yuzu.yuzu_emu.features.DocumentProvider
import org.yuzu.yuzu_emu.features.settings.ui.SettingsActivity
import org.yuzu.yuzu_emu.features.settings.utils.SettingsFile
import org.yuzu.yuzu_emu.model.HomeSetting
import org.yuzu.yuzu_emu.model.HomeViewModel
import org.yuzu.yuzu_emu.ui.main.MainActivity
import org.yuzu.yuzu_emu.utils.GpuDriverHelper

class HomeSettingsFragment : Fragment() {
    private var _binding: FragmentHomeSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivity: MainActivity

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        homeViewModel.setNavigationVisibility(visible = true, animated = false)
        mainActivity = requireActivity() as MainActivity

        val optionsList: List<HomeSetting> = listOf(
            HomeSetting(
                R.string.advanced_settings,
                R.string.settings_description,
                R.drawable.ic_settings
            ) { SettingsActivity.launch(requireContext(), SettingsFile.FILE_NAME_CONFIG, "") },
            HomeSetting(
                R.string.open_user_folder,
                R.string.open_user_folder_description,
                R.drawable.ic_folder
            ) { openFileManager() },
            HomeSetting(
                R.string.install_gpu_driver,
                R.string.install_gpu_driver_description,
                R.drawable.ic_input
            ) { driverInstaller() },
            HomeSetting(
                R.string.install_amiibo_keys,
                R.string.install_amiibo_keys_description,
                R.drawable.ic_nfc
            ) { mainActivity.getAmiiboKey.launch(arrayOf("*/*")) },
            HomeSetting(
                R.string.select_games_folder,
                R.string.select_games_folder_description,
                R.drawable.ic_add
            ) { mainActivity.getGamesDirectory.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).data) },
            HomeSetting(
                R.string.install_prod_keys,
                R.string.install_prod_keys_description,
                R.drawable.ic_unlock
            ) { mainActivity.getProdKey.launch(arrayOf("*/*")) }
        )

        binding.homeSettingsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HomeSettingAdapter(requireActivity() as AppCompatActivity, optionsList)
        }

        setInsets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openFileManager() {
        // First, try to open the user data folder directly
        try {
            startActivity(getFileManagerIntentOnDocumentProvider(Intent.ACTION_VIEW))
            return
        } catch (_: ActivityNotFoundException) {
        }

        try {
            startActivity(getFileManagerIntentOnDocumentProvider("android.provider.action.BROWSE"))
            return
        } catch (_: ActivityNotFoundException) {
        }

        // Just try to open the file manager, try the package name used on "normal" phones
        try {
            startActivity(getFileManagerIntent("com.google.android.documentsui"))
            showNoLinkNotification()
            return
        } catch (_: ActivityNotFoundException) {
        }

        try {
            // Next, try the AOSP package name
            startActivity(getFileManagerIntent("com.android.documentsui"))
            showNoLinkNotification()
            return
        } catch (_: ActivityNotFoundException) {
        }

        Toast.makeText(
            requireContext(),
            resources.getString(R.string.no_file_manager),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun getFileManagerIntent(packageName: String): Intent {
        // Fragile, but some phones don't expose the system file manager in any better way
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(packageName, "com.android.documentsui.files.FilesActivity")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    private fun getFileManagerIntentOnDocumentProvider(action: String): Intent {
        val authority = "${requireContext().packageName}.user"
        val intent = Intent(action)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = DocumentsContract.buildRootUri(authority, DocumentProvider.ROOT_ID)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return intent
    }

    private fun showNoLinkNotification() {
        val builder = NotificationCompat.Builder(
            requireContext(),
            getString(R.string.notice_notification_channel_id)
        )
            .setSmallIcon(R.drawable.ic_stat_notification_logo)
            .setContentTitle(getString(R.string.notification_no_directory_link))
            .setContentText(getString(R.string.notification_no_directory_link_description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        // TODO: Make the click action for this notification lead to a help article

        with(NotificationManagerCompat.from(requireContext())) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.notification_permission_not_granted),
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            notify(0, builder.build())
        }
    }

    private fun driverInstaller() {
        // Get the driver name for the dialog message.
        var driverName = GpuDriverHelper.customDriverName
        if (driverName == null) {
            driverName = getString(R.string.system_gpu_driver)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_gpu_driver_title))
            .setMessage(driverName)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.select_gpu_driver_default) { _: DialogInterface?, _: Int ->
                GpuDriverHelper.installDefaultDriver(requireContext())
                Toast.makeText(
                    requireContext(),
                    R.string.select_gpu_driver_use_default,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setPositiveButton(R.string.select_gpu_driver_install) { _: DialogInterface?, _: Int ->
                mainActivity.getDriver.launch(arrayOf("application/zip"))
            }
            .show()
    }

    private fun setInsets() =
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view: View, windowInsets: WindowInsetsCompat ->
            val barInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val spacingNavigation = resources.getDimensionPixelSize(R.dimen.spacing_navigation)
            val spacingNavigationRail =
                resources.getDimensionPixelSize(R.dimen.spacing_navigation_rail)

            binding.scrollViewSettings.setPadding(
                barInsets.left + cutoutInsets.left,
                barInsets.top,
                barInsets.right + cutoutInsets.right,
                barInsets.bottom
            )

            binding.linearLayoutSettings.updatePadding(bottom = spacingNavigation)

            if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                binding.linearLayoutSettings.updatePadding(left = spacingNavigationRail)
            } else {
                binding.linearLayoutSettings.updatePadding(right = spacingNavigationRail)
            }

            windowInsets
        }
}
