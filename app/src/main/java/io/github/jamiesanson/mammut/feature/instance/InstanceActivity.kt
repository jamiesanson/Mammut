package io.github.jamiesanson.mammut.feature.instance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.feature.base.BaseActivity
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceComponent
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceModule
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.ReselectListener
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.Tab
import kotlinx.android.synthetic.main.activity_instance.*

private const val EXTRA_INSTANCE_NAME = "instance_name"
private const val EXTRA_AUTH_CODE = "auth_code"

/**
 * Main fragment for hosting an instance. Handles navigation within the instance.
 */
class InstanceActivity : BaseActivity() {

    lateinit var component: InstanceComponent

    private var currentTab: Tab? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instance)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setupBottomNavigation()

        savedInstanceState?.let(::restoreTabState) ?: initialiseTabs()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(STATE_CURRENT_TAB, currentTab)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            selectTabById(item.itemId)
        }

        bottomNavigationView.setOnNavigationItemReselectedListener { item ->
            // On first initialisation, this is called instead of the navigation selected listener
            // Set up the current tab here if that's the case
            if (supportFragmentManager.fragments.none { !it.isDetached }) {
                selectTabById(item.itemId)
            }

            // Current tab reselected - let the fragment know
            (supportFragmentManager.findFragmentByTag(currentTab!!::class.java.simpleName) as? ReselectListener)
                    ?.onTabReselected()
        }
    }

    private fun selectTabById(menuItemId: Int): Boolean {
        when (menuItemId) {
            Tab.Home.menuItemId -> selectTab(Tab.Home)
            Tab.Local.menuItemId -> selectTab(Tab.Local)
            Tab.Federated.menuItemId -> selectTab(Tab.Federated)
            Tab.Profile.menuItemId -> selectTab(Tab.Profile)
            else -> return false
        }

        return true
    }

    private fun restoreTabState(savedInstanceState: Bundle) {
        currentTab = savedInstanceState.getParcelable(STATE_CURRENT_TAB)
    }

    private fun initialiseTabs() {
        currentTab = Tab.Home
        currentTab?.let {
            bottomNavigationView.selectedItemId = it.menuItemId
        }
    }

    private fun selectTab(tab: Tab) {
        val fragmentTag = tab::class.java.simpleName
        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag) ?: tab.fragment
        currentTab = tab

        val currentFragment = supportFragmentManager.fragments.firstOrNull { !it.isDetached }

        supportFragmentManager.beginTransaction()
                .apply {
                    if (!fragment.isAdded) add(fragmentContainer.id, fragment, fragmentTag)
                    if (currentFragment != null) detach(currentFragment)
                }
                .attach(fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commitNow()
    }

    override fun injectDependencies() {
        val instanceName: String = intent?.extras?.getString(EXTRA_INSTANCE_NAME)
                ?: throw NullPointerException("Instance name must not be null")
        val authCode: String = intent?.extras?.getString(EXTRA_AUTH_CODE)
                ?: throw NullPointerException("Auth code must not be null")

        component = applicationComponent
                .plus(InstanceModule(instanceName, authCode)).also {
                    it.inject(this)
                }
    }

    companion object {

        @JvmStatic
        fun launch(context: Context, instanceName: String, authCode: String) {
            Intent(context, InstanceActivity::class.java)
                    .apply {
                        putExtra(EXTRA_INSTANCE_NAME, instanceName)
                        putExtra(EXTRA_AUTH_CODE, authCode)
                    }.run {
                        context.startActivity(this)
                    }
        }

        private const val STATE_CURRENT_TAB = "current_tab"
    }
}