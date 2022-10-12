package app.simple.inure.viewmodels.dialogs

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.BuildConfig
import app.simple.inure.constants.Misc
import app.simple.inure.extensions.viewmodels.RootViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UninstallerViewModel(application: Application, val packageInfo: PackageInfo) : RootViewModel(application) {

    val error = MutableLiveData<String>()

    private val success: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            initShell()
        }
    }

    fun getSuccessStatus(): LiveData<String> {
        return success
    }

    private fun runCommand() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(Misc.delay)

            kotlin.runCatching {
                Shell.enableVerboseLogging = BuildConfig.DEBUG
                Shell.setDefaultBuilder(Shell.Builder.create()
                                            .setFlags(Shell.FLAG_MOUNT_MASTER)
                                            .setTimeout(10))

                Shell.cmd(formUninstallCommand()).submit { shellResult ->
                    if (shellResult.isSuccess) {
                        success.postValue("Done")
                    } else {
                        success.postValue("Failed")
                        val sb = StringBuilder()
                        for (s in shellResult.out) {
                            sb.append(s)
                            sb.append("\t")
                        }
                        warning.postValue(sb.toString())
                    }
                }
            }.onFailure {
                success.postValue("Failed")
                error.postValue(it)
            }.getOrElse {
                success.postValue("Failed")
                error.postValue(it)
            }
        }
    }

    private fun formUninstallCommand(): String {
        return if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            "pm uninstall -k --user 0 ${packageInfo.packageName}"
        } else {
            "pm uninstall ${packageInfo.packageName}"
        }
    }

    override fun onShellCreated(shell: Shell?) {
        runCommand()
    }
}