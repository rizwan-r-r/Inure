package app.simple.inure.viewmodels.factory

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.viewmodels.viewers.TextViewerData
import app.simple.inure.viewmodels.viewers.XMLViewerData

class TextDataFactory(private val applicationInfo: ApplicationInfo, private val path: String, val application: Application)
    : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") // Cast is checked
        when {
            modelClass.isAssignableFrom(TextViewerData::class.java) -> {
                return TextViewerData(applicationInfo, path, application) as T
            }
            else -> {
                throw IllegalArgumentException("Nope, Wrong Viewmodel!!")
            }
        }
    }
}