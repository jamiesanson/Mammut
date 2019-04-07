package io.github.koss.mammut.notifications

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

class NotificationsViewModel @Inject constructor(
) : ViewModel(), CoroutineScope by GlobalScope