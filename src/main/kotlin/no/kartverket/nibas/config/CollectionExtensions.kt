package no.kartverket.nibas.config

fun <T> List<T>.overlappedWindow(windowSize: Int, step: Int = 1): List<List<T>> =
    windowed(windowSize, step, true).map { window ->
        var newWindow = window
        repeat(windowSize - window.size) { index ->
            if (index < size) {
                newWindow = newWindow.plus(get(index))
            }
        }
        newWindow
    }

