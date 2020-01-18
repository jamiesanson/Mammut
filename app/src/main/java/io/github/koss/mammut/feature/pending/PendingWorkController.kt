package io.github.koss.mammut.feature.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.util.observe
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_pending_work.*
import kotlinx.android.synthetic.main.controller_pending_work.toolbar
import kotlinx.android.synthetic.main.view_holder_pending_work.view.*
import org.jetbrains.anko.colorAttr

@ContainerOptions(CacheImplementation.NO_CACHE)
class PendingWorkController: BaseController() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.controller_pending_work, container, false)
    }

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)

        toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp)
        toolbar.navigationIcon?.setTint(view!!.colorAttr(R.attr.colorControlNormal))

        toolbar.setNavigationOnClickListener {
            router.popCurrentController()
        }

        pendingWorkRecyclerView.adapter = FlexAdapter<WorkInfo>().apply {
            register(layout = R.layout.view_holder_pending_work) { workInfo: WorkInfo, view: View, i: Int ->
                view.infoTextView.text = workInfo.toString()
            }
        }

        WorkManager.getInstance().getWorkInfosByTagLiveData("mammut").observe(this) {
            (pendingWorkRecyclerView.adapter as FlexAdapter<WorkInfo>).resetItems(it)
        }
    }
}