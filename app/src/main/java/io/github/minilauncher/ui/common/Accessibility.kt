package io.github.minilauncher.ui.common

import android.view.View
import android.widget.Button
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * The launcher's buttons are plain text views, styled to look like text.
 * This makes TalkBack announce them as buttons all the same.
 */
fun View.announceAsButton() {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Button::class.java.name
            }
        },
    )
}

/** [announceAsButton] for each of [ids] in [root]. */
fun announceAsButtons(root: View, vararg ids: Int) {
    ids.forEach { id -> root.findViewById<View>(id)?.announceAsButton() }
}
