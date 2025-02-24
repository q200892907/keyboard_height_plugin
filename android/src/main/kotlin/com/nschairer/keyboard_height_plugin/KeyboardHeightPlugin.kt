package com.nschairer.keyboard_height_plugin

import android.graphics.Rect
import android.os.Build
import androidx.annotation.NonNull
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

class KeyboardHeightPlugin : FlutterPlugin, EventChannel.StreamHandler, ActivityAware {
    private val keyboardHeightEventChannelName = "keyboardHeightEventChannel"
    private var eventSink: EventChannel.EventSink? = null
    private var eventChannel: EventChannel? = null
    private var activityPluginBinding: ActivityPluginBinding? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, keyboardHeightEventChannelName)
        eventChannel?.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        eventChannel?.setStreamHandler(null)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        val rootView = activityPluginBinding?.activity?.window?.decorView?.rootView
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)

            val screenHeight = rootView.height
            val navigationBarHeight = getNavigationBarHeight()
            var keypadHeight = screenHeight - r.bottom
            if (isNavigationBarVisible()) {
                keypadHeight -= navigationBarHeight
            }
            val displayMetrics = activityPluginBinding?.activity?.resources?.displayMetrics
            val logicalKeypadHeight = keypadHeight / (displayMetrics?.density ?: 1f)

            if (keypadHeight > screenHeight * 0.15) {
                events?.success(logicalKeypadHeight.toDouble())
            } else {
                events?.success(0.0)
            }
        }
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    private fun getNavigationBarHeight(): Int {
        // Ensure activityPluginBinding and decorView are non-null
        val decorView = activityPluginBinding?.activity?.window?.decorView
        val rootWindowInsets = decorView?.rootWindowInsets

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API level 30 (Android 11) and above, use WindowInsets for navigation bar height
            val insets = rootWindowInsets?.getInsets(android.view.WindowInsets.Type.systemBars())
            insets?.bottom ?: 0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rootWindowInsets?.systemGestureInsets?.bottom ?: 0
        } else {
            val systemWindowInsetBottom = rootWindowInsets?.systemWindowInsetBottom
            systemWindowInsetBottom ?: 0
        }
    }

    private fun isNavigationBarVisible(): Boolean {
        val decorView = activityPluginBinding?.activity?.window?.decorView
        val rootWindowInsets = decorView?.rootWindowInsets ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rootWindowInsets.isVisible(android.view.WindowInsets.Type.navigationBars())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rootWindowInsets.systemGestureInsets.bottom > 0
        } else {
            val systemWindowInsetBottom = rootWindowInsets.systemWindowInsetBottom
            systemWindowInsetBottom > 0
        }
    }

    // Implement ActivityAware methods
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityPluginBinding = binding
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activityPluginBinding = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activityPluginBinding = binding
    }

    override fun onDetachedFromActivity() {
        activityPluginBinding = null
    }
}
