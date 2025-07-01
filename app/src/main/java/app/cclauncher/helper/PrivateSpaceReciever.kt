package app.cclauncher.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Receives broadcasts related to Private Space state changes
 */
class PrivateSpaceReceiver : BroadcastReceiver() {
    private val TAG = "PrivateSpaceReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Received broadcast: ${intent?.action}")

        when (intent?.action) {
            Intent.ACTION_PROFILE_AVAILABLE -> {
                // Private Space was unlocked
                Toast.makeText(context, "Private Space unlocked", Toast.LENGTH_LONG).show()

                // Notify the launcher to refresh app list
                val refreshIntent = Intent("app.cclauncher.ACTION_REFRESH_APPS")
                context.sendBroadcast(refreshIntent)
            }

            Intent.ACTION_PROFILE_UNAVAILABLE -> {
                // Private Space was locked
                Toast.makeText(context, "Private Space locked", Toast.LENGTH_LONG).show()

                // Notify the launcher to refresh app list
                val refreshIntent = Intent("app.cclauncher.ACTION_REFRESH_APPS")
                context.sendBroadcast(refreshIntent)
            }
        }
    }
}