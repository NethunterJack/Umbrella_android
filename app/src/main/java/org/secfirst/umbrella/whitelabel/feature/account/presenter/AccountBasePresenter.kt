package org.secfirst.umbrella.whitelabel.feature.account.presenter

import org.secfirst.umbrella.whitelabel.data.database.reader.FeedLocation
import org.secfirst.umbrella.whitelabel.data.database.reader.FeedSource
import org.secfirst.umbrella.whitelabel.feature.account.interactor.AccountBaseInteractor
import org.secfirst.umbrella.whitelabel.feature.account.view.AccountView
import org.secfirst.umbrella.whitelabel.feature.base.presenter.BasePresenter

interface AccountBasePresenter<V : AccountView, I : AccountBaseInteractor> : BasePresenter<V, I> {

    fun submitChangeDatabaseAccess(userToken: String)

    fun submitExportDatabase(destinationDir: String, fileName: String, isWipeData: Boolean)

    fun setUserLogIn()

    fun prepareShareContent(fileName: String)

    fun validateBackupPath(backupPath: String)

    fun submitInsertFeedSource(feedSources: List<FeedSource>)

    fun submitFeedLocation(feedLocation: FeedLocation)

    fun prepareView()

    fun submitPutRefreshInterval(position: Int, refreshIntervalTime: String)

}