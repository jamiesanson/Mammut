package io.github.koss.mammut.feature.base

import android.content.res.Resources

class InputError(val error: (Resources) -> String): Event<(Resources) -> String>(error)