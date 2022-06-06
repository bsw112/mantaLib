package com.manta.common

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

internal fun Context.isAllPermissionGranted(permissions: List<String>): Boolean {
    return permissions.none { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
}

internal fun Context.checkPermissionGranted(permissions: List<String>): Map<String, Boolean> {
    return permissions.associateWith {
        (ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED)
    }
}

internal fun ComponentActivity.registerPermissionRequestLauncher(
    onPermissionGranted: () -> Unit,
    withDeniedPermissions: (Array<String>) -> Unit = {}
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResult ->
        permissionResult.filter { it.value == false }.map { it.key }.toTypedArray()
            .also { deniedPermissions: Array<String> ->
                if (deniedPermissions.isEmpty()) {
                    onPermissionGranted()
                } else {
                    withDeniedPermissions(deniedPermissions)
                }
            }
    }
}

