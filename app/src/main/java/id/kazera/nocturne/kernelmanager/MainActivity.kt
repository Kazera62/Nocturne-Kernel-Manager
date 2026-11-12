package id.kazera.nocturne.kernelmanager

import android.app.Activity
import android.os.Build
import android.os.Bundle
import id.kazera.nocturne.kernelmanager.ui.home.HomeScreen

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(HomeScreen.create(this))
    }
}
