package io.github.koss.mammut.feature.about

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.BuildConfig
import io.github.koss.mammut.R
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.databinding.AboutAppFragmentBinding

class AboutAppFragment: Fragment(R.layout.about_app_fragment) {

    private val binding by viewLifecycleLazy { AboutAppFragmentBinding.bind(requireView()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with (binding) {
            ossCell.setOnClickListener {
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            }

            contributorsCell.setOnClickListener {
                findNavController().navigate(AboutAppFragmentDirections.actionAboutAppFragmentToContributorsFragment())
            }

            // Setup insets
            view.doOnApplyWindowInsets { layout, insets, _ ->
                layout.updatePadding(top = insets.systemWindowInsetTop)
            }

            closeButton.setOnClickListener {
                activity?.onBackPressed()
            }

            versionTextView.text = "Mammut build ${BuildConfig.VERSION_NAME}/${BuildConfig.BUILD_TYPE}"
        }
    }

}