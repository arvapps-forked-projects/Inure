package app.simple.inure.extensions.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.DeadObjectException
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import app.simple.inure.R
import app.simple.inure.apk.utils.PackageUtils
import app.simple.inure.services.DataLoaderService
import app.simple.inure.util.ArrayUtils
import app.simple.inure.util.NullSafety.isNotNull

abstract class PackageUtilsViewModel(application: Application) : WrappedViewModel(application) {

    private var apps: ArrayList<PackageInfo> = arrayListOf()
    private var uninstalledApps: ArrayList<PackageInfo> = arrayListOf()

    private var serviceConnection: ServiceConnection? = null

    @SuppressLint("StaticFieldLeak") // This is an application context
    private var dataLoaderService: DataLoaderService? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var intentFilter: IntentFilter = IntentFilter()

    init {
        intentFilter.addAction(DataLoaderService.APPS_LOADED)
        intentFilter.addAction(DataLoaderService.UNINSTALLED_APPS_LOADED)
        intentFilter.addAction(DataLoaderService.INSTALLED_APPS_LOADED)

        @Suppress("UNCHECKED_CAST")
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                dataLoaderService = (service as DataLoaderService.LoaderBinder).getService()
                if (dataLoaderService!!.hasDataLoaded()) {
                    apps = dataLoaderService!!.getInstalledApps().clone() as ArrayList<PackageInfo>
                    uninstalledApps = dataLoaderService!!.getUninstalledApps().clone() as ArrayList<PackageInfo>

                    onAppsLoaded(apps)
                    onUninstalledAppsLoaded(uninstalledApps)
                } else {
                    dataLoaderService!!.startLoading()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                dataLoaderService = null
            }
        }

        @Suppress("UNCHECKED_CAST")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    DataLoaderService.APPS_LOADED -> {
                        apps = dataLoaderService!!.getInstalledApps().clone() as ArrayList<PackageInfo>
                        uninstalledApps = dataLoaderService!!.getUninstalledApps().clone() as ArrayList<PackageInfo>

                        onAppsLoaded(apps)
                        onUninstalledAppsLoaded(uninstalledApps)
                    }
                    DataLoaderService.UNINSTALLED_APPS_LOADED -> {
                        uninstalledApps = dataLoaderService!!.getUninstalledApps().clone() as ArrayList<PackageInfo>
                        onUninstalledAppsLoaded(uninstalledApps)
                    }
                    DataLoaderService.INSTALLED_APPS_LOADED -> {
                        apps = dataLoaderService!!.getInstalledApps().clone() as ArrayList<PackageInfo>
                        onAppsLoaded(apps)
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(applicationContext()).registerReceiver(broadcastReceiver!!, intentFilter)
        applicationContext().bindService(Intent(applicationContext(), DataLoaderService::class.java), serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    fun getInstalledApps(): ArrayList<PackageInfo> {
        if (apps.isNotNull() && apps.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return dataLoaderService!!.getInstalledApps().clone() as ArrayList<PackageInfo>
        } else {
            return arrayListOf()
        }
    }

    fun getUninstalledApps(): ArrayList<PackageInfo> {
        if (uninstalledApps.isNotNull() && uninstalledApps.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return dataLoaderService!!.getUninstalledApps().clone() as ArrayList<PackageInfo>
        } else {
            return arrayListOf()
        }
    }

    fun refreshPackageData() {
        dataLoaderService!!.refresh()
    }

    fun refreshUninstalledPackageData() {
        dataLoaderService!!.refreshUninstalled()
    }

    fun refreshInstalledPackageData() {
        dataLoaderService!!.refreshInstalled()
    }

    protected fun PackageManager.isPackageInstalled(packageName: String): Boolean {
        while (true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageUtils.flags))
                } else {
                    @Suppress("DEPRECATION")
                    getPackageInfo(packageName, PackageUtils.flags.toInt())
                }
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            } catch (e: DeadObjectException) {
                Log.e("PackageUtilsViewModel", "isPackageInstalled: DeadObjectException")
            }
        }
    }

    protected fun PackageManager.isPackageEnabled(packageName: String): Boolean {
        return try {
            getPackageInfo(packageName)!!.applicationInfo.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: NullPointerException) {
            false
        }
    }

    protected fun PackageManager.isPackageInstalledAndEnabled(packageName: String): Boolean {
        return isPackageInstalled(packageName) && isPackageEnabled(packageName)
    }

    protected fun PackageManager.getInstalledPackages(flags: Long = PackageUtils.flags): ArrayList<PackageInfo> {
        val packageInfoList = ArrayList<PackageInfo>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageInfoList.addAll(getInstalledPackages(PackageManager.PackageInfoFlags.of(flags)))
        } else {
            @Suppress("DEPRECATION")
            packageInfoList.addAll(getInstalledPackages(flags.toInt()))
        }
        return ArrayUtils.deepCopy(packageInfoList)
    }

    protected fun PackageManager.getPackageInfo(packageName: String): PackageInfo? {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageUtils.flags))
            } else {
                try {
                    @Suppress("DEPRECATION")
                    getPackageInfo(packageName, PackageUtils.flags.toInt())
                } catch (e: RuntimeException) {
                    @Suppress("DEPRECATION")
                    getPackageInfo(packageName, PackageManager.GET_META_DATA)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return null
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun PackageManager.getApplicationInfo(packageName: String): ApplicationInfo? {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(PackageUtils.flags))
            } else {
                @Suppress("DEPRECATION")
                getApplicationInfo(packageName, PackageUtils.flags.toInt())
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Fetches the app's name from the package id of the same application
     * @param context of the given environment
     * @param applicationInfo is [ApplicationInfo] object containing app's
     *        information
     * @return app's name as [String]
     */
    protected fun getApplicationName(context: Context, applicationInfo: ApplicationInfo): String {
        while (true) {
            try {
                return context.packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                return context.getString(R.string.unknown)
            } catch (e: DeadObjectException) {
                Log.e("PackageUtilsViewModel", "getApplicationName: DeadObjectException")
            }
        }
    }

    open fun onUninstalledAppsLoaded(uninstalledApps: ArrayList<PackageInfo>) {
        // Log.d("PackageUtilsViewModel", "onUninstalledAppsLoaded: ${uninstalledApps.size}")
    }

    open fun onAppsLoaded(apps: ArrayList<PackageInfo>) {
        // Log.d("PackageUtilsViewModel", "onAppsLoaded: ${apps.size}")
    }

    override fun onCleared() {
        super.onCleared()
        try {
            serviceConnection?.let {
                application.applicationContext.unbindService(it)
            }

            LocalBroadcastManager.getInstance(applicationContext()).unregisterReceiver(broadcastReceiver!!)
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
        }
    }
}