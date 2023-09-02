package app.simple.inure.extensions.livedata

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import app.simple.inure.crash.Utils
import app.simple.inure.database.instances.StackTraceDatabase
import app.simple.inure.models.StackTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

open class ErrorLiveData : MutableLiveData<Throwable>() {

    override fun postValue(value: Throwable?) {
        super.postValue(value)
    }

    fun postError(value: Throwable, application: Application) {
        postValue(value)
        saveTraceToDatabase(value, application.applicationContext)
    }

    private fun saveTraceToDatabase(throwable: Throwable, applicationContext: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stacktrace: Writer = StringWriter()
                val printWriter = PrintWriter(stacktrace)
                throwable.printStackTrace(printWriter)
                printWriter.close()

                throwable.printStackTrace()
                StackTraceDatabase.getInstance(applicationContext)
                    ?.stackTraceDao()?.insertTrace(
                            StackTrace(stacktrace.toString(),
                                       throwable.localizedMessage ?: throwable.toString(),
                                       Utils.getCause(throwable).toString(),
                                       System.currentTimeMillis()))
            } catch (e: SQLiteDatabaseLockedException) {
                Log.e("ErrorLiveData", "Database was found locked when " +
                        "trying to save stacktrace, skipping...")
            }
        }
    }
}