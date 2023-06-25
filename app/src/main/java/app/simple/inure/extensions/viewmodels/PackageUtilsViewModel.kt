package app.simple.inure.extensions.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.DeadObjectException
import android.util.Log
import androidx.lifecycle.viewModelScope
import app.simple.inure.R
import app.simple.inure.apk.utils.PackageUtils
import app.simple.inure.util.ArrayUtils
import app.simple.inure.util.ArrayUtils.clone
import app.simple.inure.util.ArrayUtils.toArrayList
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.NullSafety.isNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.stream.Collectors

abstract class PackageUtilsViewModel(application: Application) : WrappedViewModel(application) {

    private var apps: ArrayList<PackageInfo> = arrayListOf()
    private var uninstalledApps: ArrayList<PackageInfo> = arrayListOf()

    private var areAppsLoadingStarted = false

    fun getInstalledApps(): ArrayList<PackageInfo> {
        if (apps.isNotNull() && apps.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return apps.clone() as ArrayList<PackageInfo>
        } else {
            Log.d("PackageUtilsViewModel", "getInstalledApps: apps is null or empty, reloading")
            apps = loadInstalledApps() as ArrayList<PackageInfo>
            return getInstalledApps()
        }
    }

    fun getUninstalledApps(): ArrayList<PackageInfo> {
        if (uninstalledApps.isNotNull() && uninstalledApps.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return uninstalledApps.clone() as ArrayList<PackageInfo>
        } else {
            Log.d("PackageUtilsViewModel", "getUninstalledApps: uninstalledApps is null or empty, reloading")
            @Suppress("CAST_NEVER_SUCCEEDS")
            loadUninstalledApps()
            return getUninstalledApps()
        }
    }

    protected fun loadPackageData() {
        if (areAppsLoadingStarted.invert()) {
            areAppsLoadingStarted = true

            viewModelScope.launch(Dispatchers.IO) {
                if (apps.isEmpty()) {
                    apps = loadInstalledApps().clone()
                }
                onAppsLoaded(apps.toArrayList())
            }
        }
    }

    fun refreshPackageData() {
        viewModelScope.launch(Dispatchers.IO) {
            apps = loadInstalledApps().clone()
            onAppsLoaded(apps.toArrayList())
        }
    }

    fun MutableList<PackageInfo>.loadPackageNames(): MutableList<PackageInfo> {
        forEach {
            it.applicationInfo.name = getApplicationName(application.applicationContext, it.applicationInfo)
        }

        return this
    }

    private fun loadInstalledApps(): MutableList<PackageInfo> {
        while (true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kotlin.runCatching {
                    return packageManager.getInstalledPackages(PackageManager.PackageInfoFlags
                                                                   .of((PackageManager.GET_META_DATA
                                                                           or PackageManager.MATCH_UNINSTALLED_PACKAGES).toLong())).loadPackageNames()
                }.getOrElse {
                    Log.e("PackageUtilsViewModel", "loadInstalledApps: DeadSystemException")
                }
            } else {
                @Suppress("DEPRECATION")
                kotlin.runCatching {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        packageManager.getInstalledPackages(PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES).loadPackageNames()
                    } else {
                        packageManager.getInstalledPackages(PackageManager.GET_META_DATA or PackageManager.GET_UNINSTALLED_PACKAGES).loadPackageNames()
                    }
                }.getOrElse {
                    Log.e("PackageUtilsViewModel", "loadInstalledApps: DeadSystemException")
                }
            }
        }
    }

    protected fun loadUninstalledApps() {
        while (true) {
            kotlin.runCatching {
                uninstalledApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledPackages(PackageManager.PackageInfoFlags
                                                            .of((PackageManager.GET_META_DATA
                                                                    or PackageManager.MATCH_UNINSTALLED_PACKAGES).toLong())).loadPackageNames()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        @Suppress("DEPRECATION")
                        packageManager.getInstalledPackages(PackageManager.GET_META_DATA
                                                                    or PackageManager.MATCH_UNINSTALLED_PACKAGES).loadPackageNames()
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getInstalledPackages(PackageManager.GET_META_DATA
                                                                    or PackageManager.GET_UNINSTALLED_PACKAGES).loadPackageNames()
                    }
                }.stream().filter {
                    it.applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED == 0
                }.collect(Collectors.toList())
                    .toArrayList()

                @Suppress("UNCHECKED_CAST")
                onUninstalledAppsLoaded(apps.clone() as ArrayList<PackageInfo>)
                return
            }.getOrElse {
                Log.e("PackageUtilsViewModel", "loadUninstalledApps: DeadSystemException")
            }
        }
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
        Log.d("PackageUtilsViewModel", "onUninstalledAppsLoaded: ${uninstalledApps.size}")
    }

    open fun onAppsLoaded(apps: ArrayList<PackageInfo>) {
        Log.d("PackageUtilsViewModel", "onAppsLoaded: ${apps.size}")
    }
}