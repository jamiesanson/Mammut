package io.github.koss.mammut.feature.instancebrowser.recyclerview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.github.koss.mammut.R
import io.github.koss.mammut.component.GlideApp
import io.github.koss.mammut.instances.InstanceDetail
import io.github.koss.mammut.extension.inflate
import io.github.koss.mammut.extension.lifecycleOwner
import io.github.koss.mammut.extension.observe
import kotlinx.android.synthetic.main.view_holder_instance_card.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class InstanceViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(parent.inflate(R.layout.view_holder_instance_card)) {

    fun bind(instanceCardViewModel: InstanceCardViewModel, onAboutClicked: (InstanceDetail) -> Unit) {
        instanceCardViewModel.registrationInformation.observe(lifecycleOwner) {
            itemView.instanceTitleTextView.text = it.instanceName
        }

        instanceCardViewModel.instanceInformation.observe(lifecycleOwner) { detail ->
            detail ?: return@observe
            with (itemView) {
                TransitionManager.beginDelayedTransition(itemView as ViewGroup, AutoTransition())
                descriptionTextView.text = detail.info.shortDescription
                GlideApp.with(itemView)
                        .asBitmap()
                        .load(detail.thumbnail)
                        .apply(RequestOptions.centerCropTransform())
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .into(backgroundImageView)

                moreInformationButton.visibility = View.VISIBLE
                moreInformationButton.onClick {
                    onAboutClicked(detail)
                }
            }
        }

        instanceCardViewModel.title.observe(lifecycleOwner) {
            itemView.instanceTitleTextView.text = it
            itemView.instanceTitleTextView.isSelected = true
        }
    }
}