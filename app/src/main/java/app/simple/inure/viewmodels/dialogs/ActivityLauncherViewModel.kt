package app.simple.inure.viewmodels.dialogs

import android.app.Application
import android.content.pm.PackageInfo
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.constants.Warnings
import app.simple.inure.exceptions.InureShellException
import app.simple.inure.extensions.viewmodels.RootShizukuViewModel
import app.simple.inure.preferences.ConfigurationPreferences
import app.simple.inure.shizuku.ShizukuUtils
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityLauncherViewModel(application: Application, val packageInfo: PackageInfo, val packageId: String) : RootShizukuViewModel(application) {

    private val result: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    private val success: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            initializeCoreFramework()
        }
    }

    private val action: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun getResults(): LiveData<String> {
        return result
    }

    fun getSuccessStatus(): LiveData<String> {
        return success
    }

    fun getActionStatus(): LiveData<String> {
        return action
    }

    private fun runCommand() {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                Shell.cmd(formLaunchCommand(null)).submit { shellResult ->
                    kotlin.runCatching {
                        for (i in shellResult.out) {
                            result.postValue("\n" + i)
                            if (i.contains("Exception") || i.contains("not exist")) {
                                throw InureShellException("Execution Failed...")
                            }
                        }
                    }.onSuccess {
                        if (shellResult.isSuccess) {
                            success.postValue("Done")
                        } else {
                            success.postValue("Failed")
                        }
                    }.getOrElse {
                        result.postValue("\n" + it.message!!)
                        if (shellResult.isSuccess) {
                            success.postValue("Done")
                        } else {
                            success.postValue("Failed")
                        }
                    }
                }
            }.onFailure {
                result.postValue("\n" + it.message!!)
                success.postValue("Failed")
            }.getOrElse {
                result.postValue("\n" + it.message!!)
                success.postValue("Failed")
            }
        }
    }

    fun runActionCommand(action: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                Shell.cmd(formLaunchCommand(action)).submit { shellResult ->
                    kotlin.runCatching {
                        for (s in shellResult.out) {
                            result.postValue("\n" + s)
                            if (s.contains("Exception") || s.contains("not exist")) {
                                throw InureShellException("Execution Failed...")
                            }
                        }
                    }.onSuccess {
                        if (shellResult.isSuccess) {
                            this@ActivityLauncherViewModel.action.postValue("Done")
                        } else {
                            this@ActivityLauncherViewModel.action.postValue("Failed")
                        }
                    }.getOrElse {
                        result.postValue("\n" + it.message!!)
                        if (shellResult.isSuccess) {
                            this@ActivityLauncherViewModel.action.postValue("Done")
                        } else {
                            this@ActivityLauncherViewModel.action.postValue("Failed")
                        }
                    }
                }
            }.onFailure {
                result.postValue("\n" + it.message!!)
                this@ActivityLauncherViewModel.action.postValue("Failed")
            }.getOrElse {
                result.postValue("\n" + it.message!!)
                this@ActivityLauncherViewModel.action.postValue("Failed")
            }
        }
    }

    private fun runShizuku(action: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                ShizukuUtils.execInternal(app.simple.inure.shizuku.Shell.Command(formLaunchCommand(action)), null).let {
                    Log.d("Shizuku", it.toString())
                }
            }.onSuccess {
                success.postValue("Done")
            }.onFailure {
                success.postValue("Failed")
            }.getOrElse {
                success.postValue("Failed")
            }
        }
    }

    private fun formLaunchCommand(action: String?): String {
        return "am start -n ${packageInfo.packageName}/$packageId -a \"${action ?: "android.intent.action.MAIN"}\""
    }

    fun runWithAction(action: String?) {
        if (ConfigurationPreferences.isUsingRoot()) {
            runActionCommand(action)
        } else {
            runShizuku(action)
        }
    }

    override fun onShellCreated(shell: Shell?) {
        runCommand()
    }

    override fun onShellDenied() {
        warning.postValue(Warnings.getInureWarning01())
        success.postValue("Failed")
    }

    override fun onShizukuCreated() {
        runShizuku()
    }
}