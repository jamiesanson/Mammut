package io.github.koss.mammut.feature.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.koss.mammut.BuildConfig
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.extension.comingSoon
import io.github.koss.mammut.extension.startActivity
import io.github.koss.mammut.feature.contributors.ContributorsController
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_about.*
import org.jetbrains.anko.sdk27.coroutines.onClick

@ContainerOptions(CacheImplementation.NO_CACHE)
class AboutController: BaseController() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_about, container, false)

    override fun onAttach(view: View) {
        super.onAttach(view)
        ossCell.onClick {
            startActivity<OssLicensesMenuActivity>()
        }

        contributorsCell.onClick {
            router.pushController(RouterTransaction.with(ContributorsController())
                    .popChangeHandler(VerticalChangeHandler())
                    .pushChangeHandler(VerticalChangeHandler()))
        }

        closeButton.onClick {
            activity?.onBackPressed()
        }

        versionTextView.text = "Mammut build ${BuildConfig.VERSION_NAME}/${BuildConfig.BUILD_TYPE}"
    }
}