package io.github.jamiesanson.mammut.feature.instancebrowser

import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import arrow.instance
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.widget.scrollintercepting.Direction
import io.github.jamiesanson.mammut.component.widget.scrollintercepting.NestedScrollListener
import io.github.jamiesanson.mammut.component.widget.scrollintercepting.ScrollInterceptionBehavior
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.data.remote.response.InstanceDetail
import io.github.jamiesanson.mammut.data.repo.RegistrationRepository
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.extension.observe
import io.github.jamiesanson.mammut.feature.base.BaseActivity
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instancebrowser.about.InstanceAboutFragment
import io.github.jamiesanson.mammut.feature.instancebrowser.recyclerview.InstanceBrowserAdapter
import io.github.jamiesanson.mammut.feature.joininstance.JoinInstanceActivity
import kotlinx.android.synthetic.main.activity_instance_browser.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.anko.sdk25.coroutines.onClick
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