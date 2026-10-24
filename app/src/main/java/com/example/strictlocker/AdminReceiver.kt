package com.example.strictlocker

import android.app.admin.DeviceAdminReceiver

/**
 * Device administrator receiver. The user must explicitly enable this in Android settings.
 * This component does not enable administrator access silently.
 */
class AdminReceiver : DeviceAdminReceiver()
