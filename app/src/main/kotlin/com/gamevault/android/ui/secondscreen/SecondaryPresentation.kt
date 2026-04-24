package com.gamevault.android.ui.secondscreen

import android.app.Presentation
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gamevault.android.ui.theme.GameVaultTheme

class SecondaryPresentation(
    private val activity: ComponentActivity,
    display: Display,
) : Presentation(activity, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        val cv = ComposeView(activity).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            // Extension functions (lifecycle 2.8+) — must be set before window attach
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
        }
        cv.setContent {
            GameVaultTheme {
                SecondScreenUI()
            }
        }
        setContentView(cv)
    }
}
