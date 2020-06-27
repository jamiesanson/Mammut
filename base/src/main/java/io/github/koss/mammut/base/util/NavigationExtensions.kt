package io.github.koss.mammut.base.util

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

/**
 * Extension function for traversing the fragment stack and finding the outer-most Nav controller
 */
fun Fragment.findRootNavController(): NavController {
    var findFragment: Fragment? = this
    var foundController : NavController? = null

    while (findFragment != null) {
        if (findFragment is NavHostFragment) {
            foundController = findFragment.navController
        }

        val primaryNavFragment = findFragment.parentFragmentManager
                .primaryNavigationFragment
        if (primaryNavFragment is NavHostFragment) {
            foundController = primaryNavFragment.navController
        }
        findFragment = findFragment.parentFragment
    }

    return foundController ?: throw IllegalStateException("Fragment " + this
            + " does not have a NavController set")
}