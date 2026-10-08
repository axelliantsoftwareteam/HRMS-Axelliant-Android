package com.axelliant.hris.model.team

/**
 * Lays a team tree out as a top-down chart (port of the iOS `OrganogramLayout`): each person is a box,
 * children sit in a row under their manager and the manager is centred over them. Pure maths in
 * design units, so it can be unit tested; the chart view scales the units to pixels.
 */
object OrgLayout {
    const val NODE_WIDTH = 184f
    const val NODE_HEIGHT = 128f
    const val H_GAP = 24f
    const val V_GAP = 36f

    /** A person and where their box sits: [x]/[y] are the top-left corner. */
    data class Placed(val node: OrgNode, val depth: Int, val x: Float, val y: Float)

    /** A line from the bottom-centre of a manager's box to the top-centre of a report's box. */
    data class Link(val fromX: Float, val fromY: Float, val toX: Float, val toY: Float)

    data class Result(
        val placed: List<Placed> = emptyList(),
        val links: List<Link> = emptyList(),
        val width: Float = 0f,
        val height: Float = 0f
    )

    /** Places every person in [roots] (each root tree side by side) and their reports. */
    fun layout(roots: List<OrgNode>): Result {
        val placed = ArrayList<Placed>()
        val links = ArrayList<Link>()
        var left = 0f
        for (root in roots) {
            place(root, left, 0, placed, links)
            left += width(root) + H_GAP
        }
        val maxX = placed.maxOfOrNull { it.x + NODE_WIDTH } ?: 0f
        val maxY = placed.maxOfOrNull { it.y + NODE_HEIGHT } ?: 0f
        return Result(placed, links, maxX, maxY)
    }

    /** The width a person and everything under them needs. */
    fun width(node: OrgNode): Float {
        val kids = node.childs.orEmpty()
        if (kids.isEmpty()) return NODE_WIDTH
        val children = kids.sumOf { width(it).toDouble() }.toFloat() + H_GAP * (kids.size - 1)
        return maxOf(NODE_WIDTH, children)
    }

    /** Places [node] and returns the x of its centre. */
    private fun place(node: OrgNode, left: Float, depth: Int, placed: MutableList<Placed>, links: MutableList<Link>): Float {
        val subtreeWidth = width(node)
        val y = depth * (NODE_HEIGHT + V_GAP)
        var centerX = left + subtreeWidth / 2f
        val childCenters = ArrayList<Float>()
        val kids = node.childs.orEmpty()
        if (kids.isNotEmpty()) {
            val childrenWidth = kids.sumOf { width(it).toDouble() }.toFloat() + H_GAP * (kids.size - 1)
            var x = left + (subtreeWidth - childrenWidth) / 2f
            for (child in kids) {
                childCenters.add(place(child, x, depth + 1, placed, links))
                x += width(child) + H_GAP
            }
            centerX = (childCenters.first() + childCenters.last()) / 2f
        }
        placed.add(Placed(node, depth, centerX - NODE_WIDTH / 2f, y))
        for (childX in childCenters) {
            links.add(Link(centerX, y + NODE_HEIGHT, childX, y + NODE_HEIGHT + V_GAP))
        }
        return centerX
    }
}
