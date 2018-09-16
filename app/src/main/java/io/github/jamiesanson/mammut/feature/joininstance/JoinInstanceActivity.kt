package io.github.jamiesanson.mammut.feature.joininstance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import io.github.jamiesanson.mammut.BuildConfig
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.extension.observe
import io.github.jamiesanson.mammut.extension.provideViewModel
import io.github.jamiesanson.mammut.extension.snackbar
import io.github.jamiesanson.mammut.feature.base.BaseActivity
import io.github.jamiesanson.mammut.feature.base.InputError
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceModule
import kotlinx.android.synthetic.main.activity_join_instance.*
import org.jetbrains.anko.contentView
import org.jetbrains.anko.sdk25.coroutines.onClick
import javax.inject.Inject

class JoinInstanceActivity: BaseActivity() {

    private lateinit var viewModel: JoinInstanceViewModel

    @Inject lateinit var viewModelFactory: MammutViewModelFactory

    private val redirectUrl: String
        get() = "${getString(R.string.oauth_scheme)}://${BuildConfig.APPLICATION_ID}"

    private val clientName: String
        get() = getString(R.string.app_client_name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_instance)
        viewModel = provideViewModel(viewModelFactory)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        joinInstanceButton.onClick {
            if (instanceUrlTextInputLayout.editText?.text?.isEmpty() == true) {
                showInputError(getString(R.string.error_empty_url))
                return@onClick
            }

            instanceUrlTextInputLayout.editText?.text?.toString()?.let { url ->
                onLoginClicked(url)
            } ?: showInstanceUrlEmptyError()
        }

        with (viewModel) {
            isLoading.observe(this@JoinInstanceActivity) {
                if (it) startLoading() else stopLoading()
            }

            errorMessage.observe(this@JoinInstanceActivity) {
                it.getContentIfNotHandled()?.let { stringResolver ->
                    when (it) {
                        is InputError -> showInputError(stringResolver(resources))
                        else -> showError(stringResolver(resources))
                    }
                }
            }

            oauthUrl.observe(this@JoinInstanceActivity) {
                it.getContentIfNotHandled()?.let { url ->
                    val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    if (viewIntent.resolveActivity(packageManager) != null) {
                        startActivity(viewIntent)
                    } else {
                        stopLoading()
                        showError(getString(R.string.error_no_browser))
                    }
                }
            }

            registrationCompleteEvent.observe(this@JoinInstanceActivity) {
                if (!it.hasBeenHandled) {
                    it.getContentIfNotHandled()
                    // TODO - Navigate to instance browser/main activity
                    showError("Logged in successfully")
                }
            }
        }
    }

    private fun showInstanceUrlEmptyError() {
        instanceUrlTextInputLayout.error = getString(R.string.error_url_empty)
    }

    private fun startLoading() {
        TransitionManager.beginDelayedTransition(contentView as ViewGroup, AutoTransition())
        loadingGroup.visibility = View.VISIBLE
        inputGroup.visibility = View.INVISIBLE
    }

    private fun stopLoading() {
        TransitionManager.beginDelayedTransition(contentView as ViewGroup, AutoTransition())
        loadingGroup.visibility = View.GONE
        inputGroup.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        snackbar(message)
    }

    private fun showInputError(message: String) {
        instanceUrlTextInputLayout.error = message
    }

    private fun onLoginClicked(url: String) {
        viewModel.login(url, redirectUrl, clientName)
    }

    override fun onStart() {
        super.onStart()

        val uri = intent.data
        val redirectUri = "${getString(R.string.oauth_scheme)}://${BuildConfig.APPLICATION_ID}"

        if (uri != null && uri.toString().startsWith(redirectUri)) {
            viewModel.finishLogin(uri)
        }
    }

    override fun injectDependencies() {
        applicationComponent
                .plus(JoinInstanceModule)
                .inject(this)
    }
}