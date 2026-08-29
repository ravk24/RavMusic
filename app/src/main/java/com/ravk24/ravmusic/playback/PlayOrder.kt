package com.ravk24.ravmusic.playback

/** Media indices in the order they will play, and where the current song sits in that list. */
data class PlayOrder(val mediaIndices: List<Int>, val currentPosition: Int)

/**
 * Pure walk of a timeline's play order (design D2). [first] is the first index in play order
 * (Media3's `getFirstWindowIndex(shuffle)`), [next] maps an index to the one that follows it
 * (`getNextWindowIndex(i, REPEAT_MODE_OFF, shuffle)`) or null at the end. Defensive against a
 * `next` that loops: an index is never listed twice.
 */
fun playOrder(count: Int, currentIndex: Int, first: Int?, next: (Int) -> Int?): PlayOrder {
    if (count <= 0 || first == null || first !in 0 until count) return PlayOrder(emptyList(), -1)
    val order = ArrayList<Int>(count)
    val seen = HashSet<Int>()
    var i: Int? = first
    while (i != null && i in 0 until count && seen.add(i)) {
        order += i
        i = next(i)
    }
    return PlayOrder(order, order.indexOf(currentIndex))
}
