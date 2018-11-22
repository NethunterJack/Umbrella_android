package org.secfirst.umbrella.whitelabel.feature.account.view

import Extensions.Companion.PERMISSION_REQUEST_EXTERNAL_STORAGE
import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.RouterTransaction
import com.codekidlabs.storagechooser.StorageChooser
import doRestartApplication
import kotlinx.android.synthetic.main.account_settings_view.*
import kotlinx.android.synthetic.main.account_settings_view.view.*
import kotlinx.android.synthetic.main.settings_export_dialog.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import org.secfirst.umbrella.whitelabel.BuildConfig
import org.secfirst.umbrella.whitelabel.R
import org.secfirst.umbrella.whitelabel.UmbrellaApplication
import org.secfirst.umbrella.whitelabel.component.DialogManager
import org.secfirst.umbrella.whitelabel.component.FeedLocationDialog
import org.secfirst.umbrella.whitelabel.data.database.reader.FeedLocation
import org.secfirst.umbrella.whitelabel.feature.account.DaggerAccountComponent
import org.secfirst.umbrella.whitelabel.feature.account.interactor.AccountBaseInteractor
import org.secfirst.umbrella.whitelabel.feature.account.presenter.AccountBasePresenter
import org.secfirst.umbrella.whitelabel.feature.base.view.BaseController
import org.secfirst.umbrella.whitelabel.feature.tour.view.TourController
import requestExternalStoragePermission
import java.io.File
import javax.inject.Inject

class SettingsController : BaseController(), AccountView, FeedLocationDialog.FeedLocationListener {

    @Inject
    internal lateinit var presenter: AccountBasePresenter<AccountView, AccountBaseInteractor>
    private lateinit var exportAlertDialog: AlertDialog
    private lateinit var exportView: View
    private lateinit var feedLocationView: View
    private lateinit var destinationPath: String
    private var isWipeData: Boolean = false
    private lateinit var mainView: View
    private lateinit var feedLocationDialog: FeedLocationDialog

    override fun onInject() {
        DaggerAccountComponent.builder()
                .application(UmbrellaApplication.instance)
                .build()
                .inject(this)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        setUpToolbar()
        disableNavigation()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        mainView = inflater.inflate(R.layout.account_settings_view, container, false)
        exportView = inflater.inflate(R.layout.settings_export_dialog, container, false)
        feedLocationView = inflater.inflate(R.layout.feed_location_dialog, container, false)
        presenter.onAttach(this)

        exportAlertDialog = AlertDialog
                .Builder(activity)
                .setView(exportView)
                .create()

        exportView.exportDialogWipeData.setOnClickListener { wipeDataClick() }
        exportView.exportDialogOk.onClick { exportDataOk() }
        exportView.exportDialogCancel.onClick { exportDataClose() }

        mainView.settingsImportData.onClick { importDataClick() }
        mainView.settingsExportData.setOnClickListener { exportDataClick() }
        mainView.settingsLocation.setOnClickListener { setLocationClick() }

        presenter.prepareFeedLocation()
        initExportGroup()
        feedLocationDialog = FeedLocationDialog(feedLocationView, this, this)
        return mainView
    }

    private fun setLocationClick() {
        feedLocationDialog.startLocationView()
    }

    private fun importDataClick() {
        val chooser = StorageChooser.Builder()
                .withActivity(mainActivity)
                .withFragmentManager(mainActivity.fragmentManager)
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.FILE_PICKER)
                .build()
        chooser.show()
        chooser.setOnSelectListener { path -> presenter.validateBackupPath(path) }
    }

    private fun exportDataClick() {
        val dialogManager = DialogManager(this)
        dialogManager.showDialog(object : DialogManager.DialogFactory {
            override fun createDialog(context: Context?): Dialog {
                return exportAlertDialog
            }
        })
    }

    private fun initExportGroup() {
        exportView.exportDialogGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.exportDialogTypeExport -> showFileChooserPreview()
                R.id.ExportDialogShareType -> presenter.prepareShareContent(getFilename())
            }
        }
    }

    override fun onShareContent(backupFile: File) {
        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, backupFile)
        val shareIntent = ShareCompat.IntentBuilder.from(mainActivity)
                .setType(context.contentResolver.getType(uri))
                .setStream(uri)
                .intent

        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val pm = context.packageManager
        if (shareIntent.resolveActivity(pm) != null)
            startActivity(Intent.createChooser(shareIntent, context.getString(R.string.settings_umbrella_share_title)))
    }

    private fun showFileChooserPreview() {
        if (ContextCompat.checkSelfPermission(mainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            chooseFolderDialog()
        } else {
            mainActivity.requestExternalStoragePermission()
        }
    }

    private fun chooseFolderDialog() {
        val chooser = StorageChooser.Builder()
                .withActivity(mainActivity)
                .withFragmentManager(mainActivity.fragmentManager)
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .build()
        chooser.show()
        chooser.setOnSelectListener { path ->
            destinationPath = path
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showFileChooserPreview()
            } else {
                // Permission request was denied.
            }
        }
    }


    override fun loadDefaultValue(feedLocation: FeedLocation) {
        mainView.settingsLabelLocation.text = feedLocation.location
    }

    override fun onLocationSuccess(feedLocation: FeedLocation) {
        mainView.settingsLabelLocation.text = feedLocation.location
        presenter.submitFeedLocation(feedLocation)
    }

    private fun setUpToolbar() {
        settingsToolbar?.let {
            it.title = context.getString(R.string.settings_title)
            mainActivity.setSupportActionBar(it)
            mainActivity.supportActionBar?.setDisplayShowHomeEnabled(true)
            mainActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun exportDatabaseSuccessfully() {
        context.toast(context.getString(R.string.export_database_success))
        exportAlertDialog.dismiss()
        if (isWipeData) router.pushController(RouterTransaction.with(TourController()))
    }

    override fun onImportBackupFail() {
        context.toast(context.getString(R.string.import_dialog_fail_message))
    }

    override fun onImportBackupSuccess() {
        doRestartApplication(context)
    }

    private fun exportDataOk() {
        presenter.submitExportDatabase(destinationPath, getFilename(), isWipeData)
    }

    private fun exportDataClose() = exportAlertDialog.dismiss()

    private fun wipeDataClick() {
        isWipeData = true
    }

    private fun getFilename(): String {
        val fileName = exportView.ExportDialogFileName.text.toString()
        return if (fileName.isBlank()) context.getString(R.string.export_dialog_default_message) else fileName
    }
}