package io.github.koss.mammut.feature.joininstance

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
import kotlinx.android.synthetic.main.activity_join_instance.*
import org.jetbrains.anko.contentView
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.startActivity
import javax.inject.Inject
import androidx.browser.customtabs.CustomTabsIntent
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.provideViewModel
import io.github.koss.mammut.base.util.snackbar
import io.github.koss.mammut.feature.multiinstance.MultiInstanceActivity
import org.jetbrains.anko.colorAttr
import saschpe.android.customtabs.CustomTabsHelper
import saschpe.android.customtabs.WebViewFallback

class JoinInstanceActivity: BaseActivity() {

    private lateinit var viewModel: JoinInstanceViewModel

    @Inject lateinit var viewModelFactory: MammutViewModelFactory

    private lateinit var resultsPopupWindow: InstanceSuggestionPopupWindow

    private var resultRecentlySelected: Boolean = false

    private val redirectUrl: String
        get() = "${getString(R.string.oauth_scheme)}://${BuildConfig.APPLICATION_ID}"

    private val clientName: String
        get() = getString(R.string.app_client_name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_instance)
        viewModel = provideViewModel(viewModelFactory)
        resultsPopupWindow = InstanceSuggestionPopupWindow(this, ::onResultSelected)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        instanceUrlTextInputLayout.editText?.addTextChangedListener(object : TextWatcher {
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

        joinInstanceButton.onClick {
            if (instanceUrlTextInputLayout.editText?.text?.isEmpty() == true) {
                showInputError(getString(R.string.error_empty_url))
                return@onClick
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
                    startActivity<MultiInstanceActivity>()
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

    private fun launchOauthUrl(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
                .addDefaultShareMenuItem()
                .setToolbarColor(colorAttr(R.attr.colorSurface))
                .setShowTitle(true)
                .build()

        CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)

        CustomTabsHelper.openCustomTab(this, customTabsIntent,
                Uri.parse(url),
                WebViewFallback())
    }

    private fun InstanceSuggestionPopupWindow.show() {
        isOutsideTouchable = true
        inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
        setBackgroundDrawable(null)
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = instanceUrlTextInputLayout.width

        contentView = LayoutInflater.from(this@JoinInstanceActivity).inflate(R.layout.instance_suggestion_popup_window, null)
        showAsDropDown(instanceUrlTextInputLayout.editText)
        update()
    }

    private fun onResultSelected(result: InstanceSearchResult) {
        resultRecentlySelected = true
        resultsPopupWindow.dismiss()
        instanceUrlTextInputLayout.editText?.setText(result.name)
        processLogin()
    }

    private fun processLogin() {
        instanceUrlTextInputLayout.editText?.text?.toString()?.let { url ->
            onLoginClicked(url)
        } ?: showInstanceUrlEmptyError()
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