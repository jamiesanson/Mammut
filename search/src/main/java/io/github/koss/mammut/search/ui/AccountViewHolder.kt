package io.github.koss.mammut.search.ui

import io.github.koss.mammut.base.util.GlideApp
import io.github.koss.mammut.search.databinding.AccountViewHolderBinding
import io.github.koss.mammut.search.presentation.model.Account

class AccountViewHolder(
    private val binding: AccountViewHolderBinding,
    private val onClick: (accountId: Long) -> Unit
): SearchViewHolder(itemView = binding.root) {

    fun bind(account: Account) {
        binding.root.setOnClickListener {
            onClick(account.accountId)
        }

        binding.displayNameTextView.text = account.displayName
        binding.usernameTextView.text = account.acct

        GlideApp.with(binding.root)
                .load(account.avatar)
                .circleCrop()
                .into(binding.profileImageView)
    }
}