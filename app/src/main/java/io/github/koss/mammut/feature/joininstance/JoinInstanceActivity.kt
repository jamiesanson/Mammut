package io.github.koss.mammut.feature.joininstance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import io.github.koss.mammut.BuildConfig
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.data.models.InstanceSearchResult
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.base.InputError
import io.github.koss.mammut.feature.joininstance.dagger.JoinInstanceModule
import io.github.koss.mammut.feature.joininstance.suggestion.InstanceSuggestionPopupWindow
import javax.inject.Inject
import androidx.browser.customtabs.CustomTabsIntent
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.base.anko.colorAttr
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.provideViewModel
import io.github.koss.mammut.base.util.snackbar
import io.github.koss.mammut.databinding.ActivityJoinInstanceBinding
import io.github.koss.mammut.feature.multiinstance.MultiInstanceActivity
import io.github.koss.mammut.feature.webview.MammutWebViewFallback
import saschpe.android.customtabs.CustomTabsHelper

class JoinInstanceActivity: BaseActivity() {

    private lateinit var viewModel: JoinInstanceViewModel

    @Inject lateinit var viewModelFactory: MammutViewModelFactory

    private lateinit var resultsPopupWindow: InstanceSuggestionPopupWindow

    private lateinit var binding: ActivityJoinInstanceBinding

    private var resultRecentlySelected: Boolean = false

    private val redirectUrl: String
        get() = "${getString(R.string.oauth_scheme)}://${BuildConfig.APPLICATION_ID}"

    private val clientName: String
        get() = getString(R.string.app_client_name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinInstanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = provideViewModel(viewModelFactory)
        resultsPopupWindow = InstanceSuggestionPopupWindow(this, ::onResultSelected)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        binding.instanceUrlTextInputLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(query: Editable?) {
                query?.let {
                    if (!resultRecentlySelected) {
                        viewModel.onQueryChanged(it.toString())
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        })

        binding.joinInstanceButton.setOnClickListener {
            if (binding.instanceUrlTextInputLayout.editText?.text?.isEmpty() == true) {
                showInputError(getString(R.string.error_empty_url))
                return@setOnClickListener
            }

            processLogin()
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
                it.getContentIfNotHandled()?.let(::launchOauthUrl)
            }

            registrationCompleteEvent.observe(this@JoinInstanceActivity) {
                if (!it.hasBeenHandled) {
                    it.getContentIfNotHandled()
                    startActivity(Intent(this@JoinInstanceActivity, MultiInstanceActivity::class.java))
                    finish()
                }
            }

            searchResults.observe(this@JoinInstanceActivity) {
                if (it.isEmpty()) {
                    resultsPopupWindow.onNewResults(it)
                    resultsPopupWindow.dismiss()
                } else {
                    resultsPopupWindow.onNewResults(it)
                    resultsPopupWindow.show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            MammutWebViewFallback.RESULT_CODE -> data?.data?.let(::onUri)
            else -> viewModel.loginFailed()
        }
    }

    private fun launchOauthUrl(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
                .addDefaultShareMenuItem()
                .setToolbarColor(colorAttr(com.google.android.material.R.attr.colorSurface))
                .setShowTitle(true)
                .build()

        CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)

        CustomTabsHelper.openCustomTab(this, customTabsIntent,
                Uri.parse(url),
                MammutWebViewFallback(redirectUrl))
    }

    private fun InstanceSuggestionPopupWindow.show() {
        isOutsideTouchable = true
        inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
        setBackgroundDrawable(null)
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = binding.instanceUrlTextInputLayout.width

        contentView = LayoutInflater.from(this@JoinInstanceActivity).inflate(R.layout.instance_suggestion_popup_window, null)
        showAsDropDown(binding.instanceUrlTextInputLayout.editText)
        update()
    }

    private fun onResultSelected(result: InstanceSearchResult) {
        resultRecentlySelected = true
        resultsPopupWindow.dismiss()
        binding.instanceUrlTextInputLayout.editText?.setText(result.name)
        processLogin()
    }

    private fun processLogin() {
        binding.instanceUrlTextInputLayout.editText?.text?.toString()?.let { url ->
            onLoginClicked(url)
        } ?: showInstanceUrlEmptyError()
    }

    private fun showInstanceUrlEmptyError() {
        binding.instanceUrlTextInputLayout.error = getString(R.string.error_url_empty)
    }

    private fun startLoading() {
        with (binding) {
            TransitionManager.beginDelayedTransition(root, AutoTransition())
            loadingGroup.visibility = View.VISIBLE
            inputGroup.visibility = View.INVISIBLE
        }
    }

    private fun stopLoading() {
        with (binding) {
            TransitionManager.beginDelayedTransition(root, AutoTransition())
            loadingGroup.visibility = View.GONE
            inputGroup.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        snackbar(message)
    }

    private fun showInputError(message: String) {
        binding.instanceUrlTextInputLayout.error = message
    }

    private fun onLoginClicked(url: String) {
        viewModel.login(url, redirectUrl, clientName)
    }

    private fun onUri(uri: Uri) {
        if (uri.toString().startsWith(redirectUrl)) {
            viewModel.finishLogin(uri)
        }
    }

    override fun onStart() {
        super.onStart()
        intent.data?.let(::onUri)
    }

    override fun injectDependencies() {
        applicationComponent
                .plus(JoinInstanceModule)
                .inject(this)
    }
}