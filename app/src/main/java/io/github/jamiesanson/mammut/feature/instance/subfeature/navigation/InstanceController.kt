package io.github.jamiesanson.mammut.feature.instance.subfeature.navigation

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedController
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedType
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.ProfileController
import kotlinx.android.synthetic.main.controller_instance.view.*

private const val ROUTER_STATES_KEY = "STATE"

/**
 * The following is an adaption of a brilliant Gist which did everything I wanted.
 * Check out the gist here: https://gist.github.com/StefMa/5a6d99a8948f0a1b80cfbf5bd4b51c20
 *
 * This is the base implementation of a [Controller] which works hand in hand with the [BottomNavigationView].
 *
 * It is designed to work like that:
 * * [Textual explanation](https://i.imgur.com/EqqQyOY.png)
 * * [Visual explanation](https://i.imgur.com/FDb6EGU.png)
 *
 * In other words. It should be behave exactly like the [iOS TabBar](http://apple.co/2y6XIrL)
 *
 * **How does it work?**
 *
 * If one item in the [BottomNavigationView] we do three things:
 * * Save the current [Router.saveInstanceState] in the [routerStates] with the [BottomNavigationView.getSelectedItemId] as key. See [saveStateFromCurrentTab]
 * * Clear the current [Router] hierachy and backstack and everything (cleanup). See [clearStateFromChildRouter]
 * * Try to restore the [Router.restoreInstanceState] with the saved state contains in [routerStates]. See [tryToRestoreStateFromNewTab] and [onNavigationItemSelected]
 *
 * The main idea came from [this PR](https://github.com/bluelinelabs/Conductor/pull/316).
 */
class InstanceController : BaseController(), BottomNavigationView.OnNavigationItemSelectedListener {

    /**
     * This will hold all the information about the tabs.
     *
     * This needs to be a var because we have to reassign it in [onRestoreInstanceState]
     */
    private var routerStates = SparseArray<Bundle>()

    private lateinit var childRouter: Router

    /**
     * This is the current selected item id from the [BottomNavigationView]
     */
    @IdRes
    private var currentSelectedItemId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_instance, container, false)

        childRouter = getChildRouter(view.container)

        view.bottomNavigationView.setOnNavigationItemSelectedListener(this)

        view.bottomNavigationView.setOnNavigationItemReselectedListener {
            (childRouter.backstack.last().controller() as? ReselectListener)?.onTabReselected()
        }

        // We have not a single bundle/state saved.
        // Looks like this [HomeController] was created for the first time
        if (routerStates.size() == 0) {
            // Select the first item
            currentSelectedItemId = R.id.homeDestination
            childRouter.setRoot(RouterTransaction.with(FeedController.newInstance(FeedType.Home)))
        } else {
            // We have something in the back stack. Maybe an orientation change happen?
            // We can just rebind the current router
            childRouter.rebindIfNeeded()
        }

        return view
    }

    /**
     * Listener which get called if a item from the [BottomNavigationView] is selected
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Save the state from the current tab so that we can restore it later - if needed
        saveStateFromCurrentTab(currentSelectedItemId)
        currentSelectedItemId = item.itemId
        // Clear all the hierarchy and backstack from the router. We have saved it already in the [routerStates]
        clearStateFromChildRouter()
        // Try to restore the state from the new selected tab.
        val bundleState = tryToRestoreStateFromNewTab(currentSelectedItemId)

        if (bundleState is Bundle) {
            // We have found a state (hierarchy/backstack etc.) and can just restore it to the [childRouter]
            childRouter.restoreInstanceState(bundleState)
            childRouter.rebindIfNeeded()
            return true
        }

        // There is no state (hierarchy/backstack etc.) saved in the [routerBundles].
        // We have to create a new [Controller] and set as root
        return selectTabById(item.itemId)
    }

    private fun selectTabById(menuItemId: Int): Boolean {
        val controller = when (menuItemId) {
            Tab.Home.menuItemId -> FeedController.newInstance(FeedType.Home)
            Tab.Local.menuItemId -> FeedController.newInstance(FeedType.Local)
            Tab.Federated.menuItemId -> FeedController.newInstance(FeedType.Federated)
            Tab.Profile.menuItemId -> ProfileController.newInstance(isMe = true)
            else -> return false
        }

        childRouter.setRoot(RouterTransaction.with(controller))

        return true
    }

    /**
     * Try to restore the state (which was saved via [saveStateFromCurrentTab]) from the [routerStates].
     *
     * @return either a valid [Bundle] state or null if no state is available
     */
    private fun tryToRestoreStateFromNewTab(itemId: Int): Bundle? {
        return routerStates.get(itemId)
    }

    /**
     * This will clear the state (hierarchy/backstack etc.) from the [childRouter] and goes back to root.
     */
    private fun clearStateFromChildRouter() {
        childRouter.setPopsLastView(true) /* Ensure the last view can be removed while we do this */
        childRouter.popToRoot()
        childRouter.popCurrentController()
        childRouter.setPopsLastView(false)
    }

    /**
     * This will save the current state of the tab (hierarchy/backstack etc.) from the [childRouter] in a [Bundle]
     * and put it into the [routerStates] with the tab id as key
     */
    private fun saveStateFromCurrentTab(itemId: Int) {
        val routerBundle = Bundle()
        childRouter.saveInstanceState(routerBundle)
        routerStates.put(itemId, routerBundle)
    }

    /**
     * Save our [routerStates] into the instanceState so we don't loose them on orientation change
     */
    override fun onSaveInstanceState(outState: Bundle) {
        saveStateFromCurrentTab(currentSelectedItemId)
        outState.putSparseParcelableArray(ROUTER_STATES_KEY, routerStates)
        super.onSaveInstanceState(outState)
    }

    /**
     * Restore our [routerStates]
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        routerStates = savedInstanceState.getSparseParcelableArray(ROUTER_STATES_KEY) ?: SparseArray()
    }
}
