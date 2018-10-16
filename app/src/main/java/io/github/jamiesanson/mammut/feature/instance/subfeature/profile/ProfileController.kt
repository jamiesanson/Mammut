package io.github.jamiesanson.mammut.feature.instance.subfeature.profile

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.GlideApp
import io.github.jamiesanson.mammut.component.retention.retained
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.data.models.Account
import io.github.jamiesanson.mammut.extension.observe
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.BaseController
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileModule
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileScope
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.fragment_profile.*
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class ProfileController(args: Bundle): BaseController(args) {

    private lateinit var viewModel: ProfileViewModel

    @Inject
    @ProfileScope
    lateinit var viewModelFactory: MammutViewModelFactory

    private val profileModule: ProfileModule by retained(key = { "Profile_${profileOpenCount.incrementAndGet()}" }) {
        val account: Account? = args.getParcelable(ARG_ACCOUNT)
        if (account == null && !args.getBoolean(ARG_IS_ME)) throw IllegalArgumentException("No Account or isMe flag set")
        ProfileModule(account)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as InstanceActivity)
                .component
                .plus(profileModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context, viewModelFactory)[ProfileViewModel::class.java]
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        viewModel.accountLiveData.observe(this, ::bindAccount)

        settingsButton.onClick {
            // TODO - open settings
        }
    }

    private fun bindAccount(account: Account) {
        // Profile image
        GlideApp.with(profileImageView)
                .load(account.avatar)
                .thumbnail(
                        GlideApp.with(profileImageView)
                                .load(ColorDrawable(
                                        ResourcesCompat.getColor(resources!!, R.color.standardPrimaryLightColor, activity!!.theme))
                                )
                                .apply(RequestOptions.circleCropTransform())
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(profileImageView)

        // Header image
        GlideApp.with(coverPhotoImageView)
                .load(account.header)
                .thumbnail(
                        GlideApp.with(coverPhotoImageView)
                                .load(ColorDrawable(
                                        ResourcesCompat.getColor(resources!!, R.color.standardPrimaryColor, activity!!.theme))
                                )
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.centerCropTransform())
                .into(coverPhotoImageView)

        usernameTextView.text = "@${account.userName}"
        displayNameTextView.text = if (account.displayName.isEmpty()) account.acct else account.displayName
    }

    companion object {
        @JvmStatic
        fun newInstance(account: Account? = null, isMe: Boolean = true): ProfileController =
                ProfileController(bundleOf(
                            ARG_ACCOUNT to account,
                            ARG_IS_ME to isMe
                    ))

        private val profileOpenCount = AtomicInteger()

        private const val ARG_ACCOUNT = "account"
        private const val ARG_IS_ME = "is_me"
    }
}