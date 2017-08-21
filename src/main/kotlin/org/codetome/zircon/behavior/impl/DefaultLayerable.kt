package org.codetome.zircon.behavior.impl

import org.codetome.zircon.Position
import org.codetome.zircon.Size
import org.codetome.zircon.TextCharacter
import org.codetome.zircon.behavior.Boundable
import org.codetome.zircon.behavior.Layerable
import org.codetome.zircon.graphics.layer.Layer
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class DefaultLayerable private constructor(boundable: Boundable)
    : Layerable, Boundable by boundable {

    constructor(size: Size) : this(DefaultBoundable(size))

    private val overlays: BlockingQueue<Layer> = LinkedBlockingQueue()

    override fun addLayer(layer: Layer) {
        overlays.add(layer)
    }

    override fun popLayer() = Optional.ofNullable(overlays.poll())

    override fun removeLayer(layer: Layer) {
        overlays.remove(layer)
    }

    override fun drainLayers() = mutableListOf<Layer>().also {
        overlays.drainTo(it)
    }

    override fun fetchOverlayZIntersection(absolutePosition: Position): List<TextCharacter> {
        return fetchZIntersectionFor(overlays, absolutePosition)
    }

    private fun fetchZIntersectionFor(queue: Queue<Layer>, position: Position): List<TextCharacter> {
        return queue.filter { layer ->
            layer.containsPosition(position)
        }.map { layer ->
            layer.getCharacterAt(position).get()
        }
    }
}