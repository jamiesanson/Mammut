package io.github.jamiesanson.mammut.feature.instancebrowser

import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.widget.scrollintercepting.Direction
import io.github.jamiesanson.mammut.component.widget.scrollintercepting.NestedScrollListener
import io.github.jamiesanson.mammut.component.widget.scrollintercepting.ScrollInterceptionBehavior
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.data.repo.RegistrationRepository
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.feature.base.BaseActivity
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
        instanceRecyclerView.layoutManager = LinearLayoutManager(this)
        instanceRecyclerView.adapter = InstanceBrowserAdapter(ViewModelProviders.of(this, viewModelFactory))

        addInstanceButton.onClick {
            startActivity<JoinInstanceActivity>()
        }

        launch {
            val registrations = registrationRepository.getAllRegistrations()

            withContext(UI) {
                (instanceRecyclerView.adapter as? InstanceBrowserAdapter)?.submitList(registrations)
            }
        }
    }

    override fun onScroll(direction: Direction) {
        when (direction) {
            Direction.DOWN -> addInstanceButton.collapse(duration = 200L)
            Direction.UP -> addInstanceButton.expand(duration = 200L)
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

    override fun injectDependencies() {
        applicationComponent
                .inject(this)
    }
}