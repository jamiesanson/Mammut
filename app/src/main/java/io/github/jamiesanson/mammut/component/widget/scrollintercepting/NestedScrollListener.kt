package io.github.jamiesanson.mammut.component.widget.scrollintercepting

interface NestedScrollListener {
    fun onScroll(direction: Direction)
}

enum class Direction {
    UP,
    DOWN
}