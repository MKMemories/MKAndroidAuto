package com.mkmemories.copilot

import android.app.Application
import com.mkmemories.copilot.feature.diag.AppLog

/** Point d'entrée du process : installe le journal de diagnostic et la capture des crashs. */
class MKApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.install(this, BuildConfig.BUILD_NUMBER)
    }
}
