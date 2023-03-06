package app.simple.inure.factories.actions

import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import app.simple.inure.viewmodels.dialogs.UninstallerShizukuViewModel

class UninstallerViewModelFactory(private val packageInfo: PackageInfo) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!

        when {
            modelClass.isAssignableFrom(UninstallerShizukuViewModel::class.java) -> {
                return UninstallerShizukuViewModel(application, packageInfo) as T
            }
            else -> {
                throw IllegalArgumentException("Wrong View Model")
            }
        }
    }
}