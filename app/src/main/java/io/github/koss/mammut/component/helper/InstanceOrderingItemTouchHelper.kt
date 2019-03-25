package io.github.koss.mammut.component.helper

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import io.github.koss.mammut.repo.RegistrationRepository

class InstanceOrderingItemTouchHelper(
        private val registrationRepository: RegistrationRepository
): ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0) {

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        registrationRepository.moveRegistrationOrdering(fromIndex = viewHolder.adapterPosition, toIndex = target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // NO-OP
    }

}