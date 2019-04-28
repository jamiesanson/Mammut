package io.github.koss.mammut.toot.model

data class SubmissionState(
        val isSubmitting: Boolean,
        val hasSubmitted: Boolean = false,
        val error: String? = null
)