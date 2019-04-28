package io.github.koss.mammut.toot.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.toot.R
import io.github.koss.mammut.toot.model.iconRes
import kotlinx.android.synthetic.main.layout_privacy.view.*
import kotlinx.android.synthetic.main.layout_privacy_cell.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class TootVisibilityChooser @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.layout_privacy, this)
        bind()
    }

    private data class CellConfig(val titleId: Int, val subtitleId: Int, val visibilityValue: Status.Visibility)

    private val config
        get() = listOf(
                CellConfig(R.string.privacy_public, R.string.privacy_public_description, Status.Visibility.Public),
                CellConfig(R.string.privacy_unlisted, R.string.privacy_unlisted_description, Status.Visibility.Unlisted),
                CellConfig(R.string.privacy_followers_only, R.string.privacy_followers_only_description, Status.Visibility.Private),
                CellConfig(R.string.privacy_direct, R.string.privacy_direct_description, Status.Visibility.Direct)
        )

    private val cells
        get() = listOf(
                privacyLayout.publicCell,
                privacyLayout.unlistedCell,
                privacyLayout.followersOnlyCell,
                privacyLayout.directCell
        )

    private val listeners = arrayListOf<(Status.Visibility) -> Unit>()

    var selectedVisibility: Status.Visibility
        get() = cells.zip(config).find { it.first.isSelected }!!.second.visibilityValue
        set(value) {
            if (!value.isAlreadySelected) {
                TransitionManager.beginDelayedTransition(this, AutoTransition().apply {
                    duration = 200L
                })

                cells.zip(config)
                        .onEach { it.first.selectedImageView.isVisible = false }
                        .find { it.second.visibilityValue == value }!!
                        .apply { first.selectedImageView.isVisible = true }

                listeners.forEach { it(value) }
            }
        }

    private fun bind() {
        cells.zip(config).forEach { (cell, config) ->
            with(cell) {
                iconImageView.setImageResource(config.visibilityValue.iconRes)
                titleTextView.setText(config.titleId)
                subtitleTextView.setText(config.subtitleId)
                onClick {
                    selectedVisibility = config.visibilityValue
                }
            }
        }
    }

    private val (Status.Visibility).isAlreadySelected: Boolean get() =
        cells.zip(config)
                .find { it.second.visibilityValue == this }
                ?.first
                ?.selectedImageView
                ?.isVisible ?: false

    fun setOnVisibilityChangedListener(listener: (Status.Visibility) -> Unit) {
        listeners += listener
    }
}