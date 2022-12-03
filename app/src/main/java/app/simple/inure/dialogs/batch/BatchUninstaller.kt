package app.simple.inure.dialogs.batch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.constants.BundleConstants
import app.simple.inure.constants.IntentConstants
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.decorations.views.CustomProgressBar
import app.simple.inure.extensions.fragments.ScopedBottomSheetFragment
import app.simple.inure.factories.panels.BatchViewModelFactory
import app.simple.inure.models.BatchPackageInfo
import app.simple.inure.models.BatchUninstallerProgressStateModel
import app.simple.inure.preferences.ConfigurationPreferences
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.NullSafety.isNull
import app.simple.inure.util.ParcelUtils.parcelable
import app.simple.inure.util.ParcelUtils.parcelableArrayList
import app.simple.inure.viewmodels.panels.BatchUninstallerViewModel
import app.simple.inure.viewmodels.panels.BatchViewModel

class BatchUninstaller : ScopedBottomSheetFragment() {

    private lateinit var count: TypeFaceTextView
    private lateinit var name: TypeFaceTextView
    private lateinit var state: TypeFaceTextView
    private lateinit var progress: CustomProgressBar
    private lateinit var cancel: DynamicRippleTextView

    private var appList = arrayListOf<BatchPackageInfo>()

    private var batchUninstallerViewModel: BatchUninstallerViewModel? = null
    private var batchViewModel: BatchViewModel? = null
    private var batchUninstallerProgressStateModel = BatchUninstallerProgressStateModel()

    private var appUninstallObserver = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                batchUninstallerProgressStateModel.decrementQueued()
                batchUninstallerProgressStateModel.incrementDone()
                setState(batchUninstallerProgressStateModel)
                kotlin.runCatching {
                    val packageName = result.data?.getStringExtra(IntentConstants.EXTRA_PACKAGE_NAME)!!
                    setName(packageName, done = true)
                    Log.d("BatchUninstaller", "Uninstalled -> $packageName")
                }.getOrElse {
                    Log.d("BatchUninstaller", "Failed to uninstall -> ${it.message}")
                }
            }
            Activity.RESULT_CANCELED -> {
                batchUninstallerProgressStateModel.decrementQueued()
                batchUninstallerProgressStateModel.incrementFailed()
                setState(batchUninstallerProgressStateModel)
                kotlin.runCatching {
                    val packageName = result.data?.getStringExtra(IntentConstants.EXTRA_PACKAGE_NAME)!!
                    setName(packageName, done = false)
                    Log.d("BatchUninstaller", "Failed to uninstall -> $packageName")
                }.getOrElse {
                    Log.d("BatchUninstaller", "Failed to uninstall -> ${it.message}")
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_batch_uninstall, container, false)

        count = view.findViewById(R.id.progress_count)
        name = view.findViewById(R.id.name)
        state = view.findViewById(R.id.progress_state)
        progress = view.findViewById(R.id.progress)
        cancel = view.findViewById(R.id.cancel)

        appList = requireArguments().parcelableArrayList(BundleConstants.selectedBatchApps)!!
        batchViewModel = ViewModelProvider(requireActivity())[BatchViewModel::class.java]

        if (ConfigurationPreferences.isUsingRoot().invert()) {
            batchUninstallerProgressStateModel.count = appList.size
            batchUninstallerProgressStateModel.queued = appList.size
            setState(batchUninstallerProgressStateModel)
        }

        if (ConfigurationPreferences.isUsingRoot()) {
            val batchViewModelFactory = BatchViewModelFactory(appList)
            batchUninstallerViewModel = ViewModelProvider(this, batchViewModelFactory)[BatchUninstallerViewModel::class.java]
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.max = appList.size

        batchUninstallerViewModel?.getDone()?.observe(viewLifecycleOwner) {
            // progress.animateProgress(it, animate = true)
        }

        batchUninstallerViewModel?.getState()?.observe(viewLifecycleOwner) {
            setState(it)
        }

        batchUninstallerViewModel?.getDone()?.observe(viewLifecycleOwner) {
            cancel.setText(R.string.close)
        }

        if (ConfigurationPreferences.isUsingRoot().invert()) {
            if (savedInstanceState.isNull()) {
                for (app in appList) {
                    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    intent.putExtra(IntentConstants.EXTRA_PACKAGE_NAME, app.packageInfo.packageName)
                    intent.data = Uri.parse("package:${app.packageInfo.packageName}")
                    appUninstallObserver.launch(intent)
                }
            }
        }

        cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setState(state: BatchUninstallerProgressStateModel) {
        with(StringBuilder()) {
            append(getString(R.string.progress, ((state.count - state.queued) / appList.size * 100F).toInt()))
            append(" | ")
            append(getString(R.string.count_done, state.done))
            append(" | ")
            append(getString(R.string.count_failed, state.failed))
            append(" | ")
            append(getString(R.string.count_queued, state.queued))
            this@BatchUninstaller.state.text = toString()
            progress.animateProgress(((state.count - state.queued) / appList.size * 100F).toInt(), animate = true)
            batchViewModel?.refresh()
        }
    }

    private fun setName(packageName: String, done: Boolean) {
        if (name.text.isNotEmpty()) {
            name.append(System.lineSeparator())
        }

        name.append(packageName)
        name.append(" -> ")
        name.append(getString(R.string.done))

        if (done) {
            name.append(getString(R.string.done))
        } else {
            name.setText(R.string.failed)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(BundleConstants.batchUninstallerProgressStateModel, batchUninstallerProgressStateModel)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (ConfigurationPreferences.isUsingRoot().invert()) {
            if (savedInstanceState != null) {
                batchUninstallerProgressStateModel = savedInstanceState.parcelable(BundleConstants.batchUninstallerProgressStateModel)!!
                setState(batchUninstallerProgressStateModel)
            }
        }
        super.onViewStateRestored(savedInstanceState)
    }

    companion object {
        fun newInstance(list: ArrayList<BatchPackageInfo>): BatchUninstaller {
            val args = Bundle()
            args.putParcelableArrayList(BundleConstants.selectedBatchApps, list)
            val fragment = BatchUninstaller()
            fragment.arguments = args
            return fragment
        }
    }
}