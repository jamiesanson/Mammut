package io.github.koss.mammut.feature.instancebrowser

import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.component.widget.scrollintercepting.Direction
import io.github.koss.mammut.component.widget.scrollintercepting.NestedScrollListener
import io.github.koss.mammut.component.widget.scrollintercepting.ScrollInterceptionBehavior
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.instances.response.InstanceDetail
import io.github.koss.mammut.repo.RegistrationRepository
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.extension.observe
import io.github.koss.mammut.feature.instance.InstanceActivity
import io.github.koss.mammut.feature.instancebrowser.about.InstanceAboutFragment
import io.github.koss.mammut.feature.instancebrowser.recyclerview.InstanceBrowserAdapter
import io.github.koss.mammut.feature.joininstance.JoinInstanceActivity
import kotlinx.android.synthetic.main.activity_instance_browser.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.startActivity
import javax.inject.Inject

class InstanceBrowserActivity: BaseActivity(), NestedScrollListener {

    @Inject
    lateinit var viewModelFactory: MammutViewModelFactory

    @Inject
    lateinit var registrationRepository: RegistrationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instance_browser)
        themeEngine.applyFontToCollapsingLayout(collapsingLayout)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setupScrolling()
        setupAboutFragment()

        instanceRecyclerView.layoutManager = LinearLayoutManager(this)
        instanceRecyclerView.adapter = InstanceBrowserAdapter(ViewModelProviders.of(this, viewModelFactory), onInstanceClicked = {
            with (it) {
                InstanceActivity.launch(this@InstanceBrowserActivity,
                        instanceName = instanceName,
                        authCode = accessToken!!.accessToken)

                this@InstanceBrowserActivity.finish()
            }
        }) { detail, id ->
            expandAboutFragment(detail, id)
        }
        instanceRecyclerView.setExpandablePage(instanceAboutPageLayout)

        addInstanceButton.onClick {
            startActivity<JoinInstanceActivity>()
        }

        registrationRepository.getAllCompletedRegistrationsLive().observe(this) {
            (instanceRecyclerView.adapter as? InstanceBrowserAdapter)?.submitList(it)
        }

        instanceAboutPageLayout.pushParentToolbarOnExpand(toolbar)
    }

    override fun onScroll(direction: Direction) {
        when (direction) {
            Direction.DOWN -> addInstanceButton.collapse(duration = 200L)
            Direction.UP -> addInstanceButton.expand(duration = 200L)
        }
    }

    override fun onBackPressed() {
        if (instanceAboutPageLayout.isExpandedOrExpanding) {
            collapseAbout()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupScrolling() {
        val params = scrollInterceptionView.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = ScrollInterceptionBehavior()
        behavior.scrollCallback {
            onScroll(it)
        }
        params.behavior = behavior
        scrollInterceptionView.requestLayout()
    }

    private fun setupAboutFragment() {
        val instanceAboutFragment = (supportFragmentManager.findFragmentById(instanceAboutPageLayout.id) as InstanceAboutFragment?)
                ?: InstanceAboutFragment()

        supportFragmentManager
                .beginTransaction()
                .replace(instanceAboutPageLayout.id, instanceAboutFragment)
                .commitNowAllowingStateLoss()
    }

    private fun expandAboutFragment(instanceDetail: InstanceDetail, itemId: Long) {
        val instanceAboutFragment = (supportFragmentManager.findFragmentById(instanceAboutPageLayout.id) as InstanceAboutFragment?) ?: return
        showAbout(itemId)
        appBarLayout.setExpanded(false, true)
        instanceAboutFragment.populate(instanceDetail, itemId)
    }

    private fun collapseAbout() {
        instanceRecyclerView.collapse()
    }

    private fun showAbout(itemId: Long) {
        instanceRecyclerView.expandItem(itemId)
    }

    fun logOut(itemId: Long) {
        collapseAbout()
        launch {
            registrationRepository.logOut(itemId)
        }
    }

    override fun injectDependencies() {
        applicationComponent
                .inject(this)
    }
}