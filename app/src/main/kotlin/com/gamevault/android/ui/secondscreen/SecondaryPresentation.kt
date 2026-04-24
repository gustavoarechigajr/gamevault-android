package com.gamevault.android.ui.secondscreen

import android.app.Presentation
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.LocalLifecycleOwner
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
        setContentView(ComposeView(activity).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setContent {
                // Share the Activity's lifecycle so collectAsState() works inside the Presentation
                CompositionLocalProvider(LocalLifecycleOwner provides activity) {
                    GameVaultTheme {
                        SecondScreenUI()
                    }
                }
            }
        })
    }
}
