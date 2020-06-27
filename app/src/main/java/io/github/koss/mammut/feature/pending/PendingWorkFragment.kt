package io.github.koss.mammut.feature.pending

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.R
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.databinding.PendingWorkFragmentBinding
import io.github.koss.mammut.databinding.ViewHolderPendingWorkBinding
import io.github.koss.mammut.extension.applicationComponent
import org.jetbrains.anko.colorAttr
import javax.inject.Inject

class PendingWorkFragment : Fragment(R.layout.pending_work_fragment) {

    private val binding by viewLifecycleLazy { PendingWorkFragmentBinding.bind(requireView()) }

    @Inject
    @ApplicationScope
    lateinit var themeEngine: ThemeEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().applicationComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp)
        binding.toolbar.navigationIcon?.setTint(view.colorAttr(R.attr.colorControlNormal))

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        themeEngine.applyFontToCollapsingLayout(binding.collapsingLayout)

        binding.collapsingLayout.doOnApplyWindowInsets { layout, insets, _ ->
            layout.updatePadding(top = insets.systemWindowInsetTop)
        }

        binding.pendingWorkRecyclerView.adapter = FlexAdapter<WorkInfo>().apply {
            register(layout = R.layout.view_holder_pending_work) { workInfo: WorkInfo, view: View, _: Int ->
                ViewHolderPendingWorkBinding.bind(view).infoTextView.text = workInfo.toString()
            }
        }

        WorkManager
                .getInstance(requireContext())
                .getWorkInfosByTagLiveData("mammut").observe(this) {
                    @Suppress("UNCHECKED_CAST")
                    (binding.pendingWorkRecyclerView.adapter as FlexAdapter<WorkInfo>).resetItems(it)
                }
    }

}