package no.kartverket.nibas.service.autosync

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

/**
 * Service for finding paths through spatial network of grenser.
 * Uses Dijkstra's algorithm for correct pathfinding with proper backtracking.
 */
@Service
class SpatialNetworkPathfinder(
    private val coordinateService: CoordinateService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Uses Dijkstra to find shortest path and validates all required grenser are included.
     * @throws PathfindingException if path cannot be found or validation fails
     */
    fun ensureCompleteChain(
        toArchive: List<GrenseArchiveService.GrenseToArchive>,
        gapStart: Point,
        gapEnd: Point,
        allGrensekoblinger: List<GrunnkretsGrenseKobling>,
        gyldigFra: LocalDate
    ): List<GrenseArchiveService.GrenseToArchive> {
        log.info("  Finding complete path from gapStart to gapEnd using Dijkstra")


        val borderGrenser = allGrensekoblinger.filter { kobling ->
            kobling.grense.grensetype == Grensetype.Kommunegrense ||
            kobling.grense.grensetype == Grensetype.Fylkesgrense
        }

        log.info("  Filtered to {} border grenser (kommunegrense/fylkesgrense) from {} total",
            borderGrenser.size, allGrensekoblinger.size)

        val graph = buildGraph(borderGrenser)
        val path = findShortestPath(graph, gapStart, gapEnd)
            ?: throw PathfindingException(
                "Cannot find path from gapStart [${gapStart.x}, ${gapStart.y}] " +
                "to gapEnd [${gapEnd.x}, ${gapEnd.y}] through border network"
            )

        log.info("  Found path with {} grenser", path.size)

        // Validate that ALL original toArchive grenser are in the path - otherwise indicates multiple paths exist
        val originalLokalids = toArchive.map { it.grense.lokalid }.toSet()
        val pathLokalids = path.map { it.lokalid }.toSet()
        val missingLokalids = originalLokalids - pathLokalids

        if (missingLokalids.isNotEmpty()) {
            throw PathfindingException(
                "Path validation failed: found path from gapStart to gapEnd but it misses " +
                "${missingLokalids.size} required grenser: $missingLokalids. " +
                "This indicates multiple paths exist and the shortest path doesn't include all avvik segments."
            )
        }

        log.info("  Validation passed: all {} original grenser are in the path", originalLokalids.size)


        return path.map { grense ->
            GrenseArchiveService.GrenseToArchive(
                grense,
                coordinateService.extractCoordinates(grense)
            )
        }
    }

    private fun buildGraph(grenser: List<GrunnkretsGrenseKobling>): Graph {
        val graph = Graph()

        for (kobling in grenser) {
            val grense = kobling.grense
            val coords = coordinateService.extractCoordinates(grense)
            if (coords.size < 2) continue

            val start = coords.first()
            val end = coords.last()
            val weight = coords.zipWithNext().sumOf { (p1, p2) -> p1.distanceTo(p2) }

            graph.addEdge(start, end, grense, weight)
        }

        log.info("  Built graph with {} nodes and {} edges", graph.nodeCount(), graph.edgeCount())
        return graph
    }

    private fun findShortestPath(
        graph: Graph,
        start: Point,
        goal: Point
    ): List<Grense>? {
        val startNode = graph.findNode(start) ?: run {
            log.error("Start point not found in graph: [${start.x}, ${start.y}]")
            return null
        }
        val goalNode = graph.findNode(goal) ?: run {
            log.error("Goal point not found in graph: [${goal.x}, ${goal.y}]")
            return null
        }

        val distances = mutableMapOf<Point, Double>()
        val previous = mutableMapOf<Point, Pair<Point, Grense>>()
        val queue = PriorityQueue<Pair<Point, Double>>(compareBy { it.second })
        val visited = mutableSetOf<Point>()

        distances[startNode] = 0.0
        queue.add(Pair(startNode, 0.0))

        while (queue.isNotEmpty()) {
            val (current, currentDist) = queue.poll()

            if (current in visited) continue
            visited.add(current)

            if (coordinateService.pointsMatch(current, goalNode)) {
                return reconstructPath(previous, startNode, goalNode)
            }


            for (edge in graph.getEdges(current)) {
                val neighbor = edge.otherEnd
                if (neighbor in visited) continue

                val newDist = currentDist + edge.weight
                val oldDist = distances[neighbor] ?: Double.MAX_VALUE

                if (newDist < oldDist) {
                    distances[neighbor] = newDist
                    previous[neighbor] = Pair(current, edge.grense)
                    queue.add(Pair(neighbor, newDist))
                }
            }
        }

        log.error("No path found from start to goal after exploring {} nodes", visited.size)
        return null
    }

    private fun reconstructPath(
        previous: Map<Point, Pair<Point, Grense>>,
        start: Point,
        goal: Point
    ): List<Grense> {
        val path = mutableListOf<Grense>()
        var current = goal

        while (!coordinateService.pointsMatch(current, start)) {
            val (prev, grense) = previous[current]
                ?: throw IllegalStateException("Path reconstruction failed at $current")
            path.add(0, grense)
            current = prev
        }

        return path
    }

    private inner class Graph {
        private val adjacencyList = mutableMapOf<Point, MutableList<Edge>>()

        fun addEdge(from: Point, to: Point, grense: Grense, weight: Double) {
            val fromNode = findOrCreateNode(from)
            val toNode = findOrCreateNode(to)

            adjacencyList.getOrPut(fromNode) { mutableListOf() }
                .add(Edge(toNode, grense, weight))
            adjacencyList.getOrPut(toNode) { mutableListOf() }
                .add(Edge(fromNode, grense, weight))
        }

        fun getEdges(node: Point): List<Edge> {
            val actualNode = findNode(node) ?: return emptyList()
            return adjacencyList[actualNode] ?: emptyList()
        }

        fun findNode(point: Point): Point? {
            return adjacencyList.keys.find { coordinateService.pointsMatch(it, point) }
        }

        private fun findOrCreateNode(point: Point): Point {
            return findNode(point) ?: point
        }

        fun nodeCount(): Int = adjacencyList.size
        fun edgeCount(): Int = adjacencyList.values.sumOf { it.size } / 2
    }

    private data class Edge(
        val otherEnd: Point,
        val grense: Grense,
        val weight: Double
    )
}

class PathfindingException(message: String) : RuntimeException(message)
