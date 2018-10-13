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
import io.github.jamiesanson.mammut.feature.instance.subfeature.Tab
import kotlinx.android.synthetic.main.activity_instance.*

private const val EXTRA_INSTANCE_NAME = "instance_name"
private const val EXTRA_AUTH_CODE = "auth_code"

/**
 * Main fragment for hosting an instance. Handles navigation within the instance.
 */
class InstanceActivity : BaseActivity() {

    lateinit var component: InstanceComponent

    private lateinit var currentTab: Tab

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
            when (item.itemId) {
                Tab.Home.menuItemId -> selectTab(Tab.Home)
                Tab.Local.menuItemId -> selectTab(Tab.Local)
                Tab.Federated.menuItemId -> selectTab(Tab.Federated)
                Tab.Notifications.menuItemId -> selectTab(Tab.Notifications)
                else -> return@setOnNavigationItemSelectedListener false
            }
            return@setOnNavigationItemSelectedListener true
        }
    }

    private fun restoreTabState(savedInstanceState: Bundle) {
        currentTab = savedInstanceState.getParcelable(STATE_CURRENT_TAB)
                ?: throw IllegalStateException("Attempting to restore tab state but nothing found")

        bottomNavigationView.selectedItemId = currentTab.menuItemId
    }

    private fun initialiseTabs() {
        currentTab = Tab.Home
        bottomNavigationView.selectedItemId = currentTab.menuItemId
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
                .commit()
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