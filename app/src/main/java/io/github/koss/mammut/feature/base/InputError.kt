package io.github.jamiesanson.mammut.feature.base

import android.content.res.Resources

class InputError(val error: (Resources) -> String): Event<(Resources) -> String>(error)