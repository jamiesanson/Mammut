package io.github.koss.mammut.component.widget.scrollintercepting

interface NestedScrollListener {
    fun onScroll(direction: Direction)
}

enum class Direction {
    UP,
    DOWN
}