package app.simple.inure.viewmodels.panels

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.constants.DebloatSortConstants
import app.simple.inure.constants.SortConstant
import app.simple.inure.enums.Removal
import app.simple.inure.extensions.viewmodels.PackageUtilsViewModel
import app.simple.inure.models.Bloat
import app.simple.inure.preferences.DebloatPreferences
import app.simple.inure.sort.DebloatSort.getSortedList
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.FlagUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class DebloatViewModel(application: Application) : PackageUtilsViewModel(application) {

    private val bloatList: MutableLiveData<ArrayList<Bloat>> by lazy {
        MutableLiveData<ArrayList<Bloat>>()
    }

    fun getBloatList(): LiveData<ArrayList<Bloat>> {
        return bloatList
    }

    private fun parseUADList() {
        viewModelScope.launch(Dispatchers.IO) {
            val uadList = getUADList()
            val apps = getInstalledApps() + getUninstalledApps()
            var bloats = ArrayList<Bloat>()

            uadList.forEach { (id, bloat) ->
                apps.forEach { app ->
                    if (app.packageName == id) {
                        bloat.packageInfo = app
                        bloats.add(bloat)
                    }
                }
            }

            // Filter system or user apps
            when (DebloatPreferences.getApplicationType()) {
                SortConstant.SYSTEM -> {
                    bloats = bloats.parallelStream().filter { b ->
                        b.packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    }.collect(Collectors.toList()) as ArrayList<Bloat>
                }
                SortConstant.USER -> {
                    bloats = bloats.parallelStream().filter { b ->
                        b.packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                    }.collect(Collectors.toList()) as ArrayList<Bloat>
                }
            }

            // Sort the bloat list
            bloats.getSortedList()

            // Apply filters
            bloats = bloats.applyListFilter()
            bloats = bloats.applyMethodsFilter()
            bloats = bloats.applyStateFilter()

            // Remove duplicates
            bloats = bloats.distinctBy { it.id } as ArrayList<Bloat>

            bloatList.postValue(bloats)
        }
    }

    override fun onAppsLoaded(apps: ArrayList<PackageInfo>) {
        super.onAppsLoaded(apps)
        parseUADList()
    }

    /**
     * {
     *     "id": "com.android.package",
     *     "list": "Oem",
     *     "description": "desc \n",
     *     "dependencies": [],
     *     "neededBy": [],
     *     "labels": [],
     *     "removal": "Recommended"
     *   },
     */
    private fun getUADList(): HashMap<String, Bloat> {
        val bufferedReader = BufferedReader(InputStreamReader(DebloatViewModel::class.java.getResourceAsStream(UAD_FILE_NAME)))
        val stringBuilder = StringBuilder()
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
        bufferedReader.close()

        val json = stringBuilder.toString()
        val jsonArray = org.json.JSONArray(json)
        val bloats = hashMapOf<String, Bloat>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getString("id")
            val list = jsonObject.getString("list")
            val description = jsonObject.getString("description")
            val removal = jsonObject.getString("removal")
            val dependencies = jsonObject.getJSONArray("dependencies")
            val neededBy = jsonObject.getJSONArray("neededBy")
            val labels = jsonObject.getJSONArray("labels")

            val bloat = Bloat()
            bloat.id = id
            bloat.list = list
            bloat.description = description
            bloat.removal = Removal.valueOf(removal.uppercase())
            bloat.dependencies = ArrayList()
            bloat.neededBy = ArrayList()
            bloat.labels = ArrayList()

            for (j in 0 until dependencies.length()) {
                bloat.dependencies.add(dependencies.getString(j))
            }

            for (j in 0 until neededBy.length()) {
                bloat.neededBy.add(neededBy.getString(j))
            }

            for (j in 0 until labels.length()) {
                bloat.labels.add(labels.getString(j))
            }

            bloats[id] = bloat
        }

        return bloats
    }

    fun refreshBloatList() {
        refreshPackageData()
    }

    private fun ArrayList<Bloat>.applyListFilter(): ArrayList<Bloat> {
        val listType = DebloatPreferences.getListType()
        val filteredList = ArrayList<Bloat>()

        parallelStream().forEach {
            if (FlagUtils.isFlagSet(listType, DebloatSortConstants.AOSP)) {
                if (it.list.lowercase() == "aosp") {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(listType, DebloatSortConstants.CARRIER)) {
                if (it.list.lowercase() == "carrier") {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(listType, DebloatSortConstants.GOOGLE)) {
                if (it.list.lowercase() == "google") {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(listType, DebloatSortConstants.MISC)) {
                if (it.list.lowercase() == "misc") {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(listType, DebloatSortConstants.OEM)) {
                if (it.list.lowercase() == "oem") {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(listType, DebloatSortConstants.PENDING)) {
                if (it.list.lowercase() == "pending") {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(listType, DebloatSortConstants.UNLISTED_LIST)) {
                if (it.list.lowercase() == "unlisted") {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }
        }

        return filteredList
    }

    private fun ArrayList<Bloat>.applyMethodsFilter(): ArrayList<Bloat> {
        val removalType = DebloatPreferences.getRemovalType()
        val filteredList = ArrayList<Bloat>()

        parallelStream().forEach {
            if (FlagUtils.isFlagSet(removalType, DebloatSortConstants.RECOMMENDED)) {
                if (it.removal == Removal.RECOMMENDED) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(removalType, DebloatSortConstants.ADVANCED)) {
                if (it.removal == Removal.ADVANCED) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(removalType, DebloatSortConstants.EXPERT)) {
                if (it.removal == Removal.EXPERT) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(removalType, DebloatSortConstants.UNSAFE)) {
                if (it.removal == Removal.UNSAFE) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(removalType, DebloatSortConstants.UNLISTED)) {
                if (it.removal == Removal.UNLISTED) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }
        }

        return filteredList
    }

    private fun ArrayList<Bloat>.applyStateFilter(): ArrayList<Bloat> {
        val state = DebloatPreferences.getRemovalType()
        val filteredList = ArrayList<Bloat>()

        parallelStream().forEach {
            if (FlagUtils.isFlagSet(state, DebloatSortConstants.DISABLED)) {
                if (it.packageInfo.applicationInfo.enabled.not()) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(state, DebloatSortConstants.ENABLED)) {
                if (it.packageInfo.applicationInfo.enabled) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }

            if (FlagUtils.isFlagSet(state, DebloatSortConstants.UNINSTALLED)) {
                if (it.packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED == 0) {
                    synchronized(filteredList) {
                        if (filteredList.contains(it).invert()) {
                            filteredList.add(it)
                        }
                    }
                }
            }
        }

        return filteredList
    }

    companion object {
        private const val UAD_FILE_NAME = "/uad_lists.json"
    }
}