package org.secfirst.umbrella.whitelabel.feature.account.presenter

import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.raizlabs.android.dbflow.config.FlowManager
import org.apache.commons.io.FileUtils
import org.secfirst.umbrella.whitelabel.UmbrellaApplication
import org.secfirst.umbrella.whitelabel.data.database.AppDatabase
import org.secfirst.umbrella.whitelabel.data.database.AppDatabase.EXTENSION
import org.secfirst.umbrella.whitelabel.data.database.AppDatabase.NAME
import org.secfirst.umbrella.whitelabel.data.database.reader.FeedLocation
import org.secfirst.umbrella.whitelabel.data.database.reader.FeedSource
import org.secfirst.umbrella.whitelabel.feature.account.interactor.AccountBaseInteractor
import org.secfirst.umbrella.whitelabel.feature.account.view.AccountView
import org.secfirst.umbrella.whitelabel.feature.base.presenter.BasePresenterImp
import org.secfirst.umbrella.whitelabel.misc.AppExecutors.Companion.uiContext
import org.secfirst.umbrella.whitelabel.misc.getDigitsFrom
import org.secfirst.umbrella.whitelabel.misc.launchSilent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AccountPresenterImp<V : AccountView, I : AccountBaseInteractor> @Inject constructor(
        interactor: I) : BasePresenterImp<V, I>(
        interactor = interactor), AccountBasePresenter<V, I> {


    override fun setUserLogIn() {
        interactor?.setLoggedIn()
    }

    override fun submitChangeDatabaseAccess(userToken: String) {
        launchSilent(uiContext) {
            interactor?.let {
                val res = it.changeDatabaseAccess(userToken)
                getView()?.isLogged(res)
            }
        }
    }

    override fun submitExportDatabase(destinationDir: String, fileName: String, isWipeData: Boolean) {

        val dstDatabase = File("$destinationDir/$fileName.$EXTENSION")
        val databaseFile = FlowManager.getContext().getDatabasePath("$NAME.$EXTENSION")
        databaseFile.copyTo(dstDatabase, true)
        if (isWipeData)
            cleanDatabase()

        getView()?.exportDatabaseSuccessfully()
    }

    private fun cleanDatabase() {
        val cacheDir = UmbrellaApplication.instance.cacheDir
        FileUtils.deleteQuietly(cacheDir)
        FlowManager.getDatabase(AppDatabase.NAME).reset()
    }


    override fun prepareShareContent(fileName: String) {
        val databaseFile = FlowManager.getContext().getDatabasePath("$NAME.$EXTENSION")
        val backupFile = File("${FlowManager.getContext().cacheDir}/$fileName.$EXTENSION")
        databaseFile.copyTo(backupFile, true)
        getView()?.onShareContent(backupFile)
    }

    override fun validateBackupPath(backupPath: String) {
        val backupDatabase = File(backupPath)
        val databaseFile = FlowManager.getContext().getDatabasePath("$NAME.$EXTENSION")
        backupDatabase.copyTo(databaseFile, true)
        if (backupDatabase.extension == EXTENSION) {
            getView()?.onImportBackupSuccess()
        } else
            getView()?.onImportBackupFail()
    }

    override fun submitInsertFeedSource(feedSources: List<FeedSource>) {
        launchSilent(uiContext) {
            interactor?.insertAllFeedSources(feedSources)
        }
    }

    override fun submitFeedLocation(feedLocation: FeedLocation) {
        launchSilent(uiContext) {
            interactor?.insertFeedLocation(feedLocation)
        }
    }

    override fun prepareView() {
        launchSilent(uiContext) {
            interactor?.let {
                val feedLocation = it.fetchFeedLocation()
                val refreshFeedInterval = it.fetchRefreshInterval()
                val feedSource = it.fetchFeedSources()
                getView()?.loadDefaultValue(feedLocation, refreshFeedInterval, feedSource)
            }
        }
    }

    override fun submitPutRefreshInterval(position: Int, refreshIntervalTime: String) {
        launchSilent(uiContext) {
            val workManager = WorkManager.getInstance()
            val time = refreshIntervalTime.getDigitsFrom()
            val workBuilder = PeriodicWorkRequest
                    .Builder(NotificationWorker::class.java, 20, defineTime(time))

            workManager.enqueue(workBuilder
                    .setInputData(makeDataFromWorker())
                    .build())
            interactor?.putRefreshInterval(position)
        }
    }

    private suspend fun makeDataFromWorker(): Data {
        val feedSources = interactor?.fetchFeedSources()?.filter { it.lastChecked }
        val feedsCode = mutableListOf<Int>()
        feedSources?.forEach { feedsCode.add(it.code) }
        val location = interactor?.fetchFeedLocation()?.iso2
        return Data.Builder()
                .putString(NotificationWorker.EXTRA_COUNTRY_CODE, location)
                .putString(NotificationWorker.EXTRA_SOURCES, feedsCode.joinToString(","))
                .build()
    }

    private fun defineTime(time: Long): TimeUnit {
        return if (time > 60)
            TimeUnit.HOURS
        else
            TimeUnit.MINUTES
    }
}