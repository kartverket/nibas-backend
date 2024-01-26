package no.kartverket.nibas.common

fun <T> MutableList<T>.pop(): T {
    val element = this[0] ?: throw RuntimeException("Tried to pop from an empty list.")
    this.removeAt(0)
    return element
}

fun <T> MutableList<T>.findAndRemove(predicate: (item: T) -> Boolean): T? {
    val element = this.find(predicate)
    this.remove(element)
    return element
}
