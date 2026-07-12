package com.mkmemories.copilot.feature.roadtrip

import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine

/**
 * Optimiseur d'étapes — plus proche voisin puis 2-opt, en local. Les étapes
 * à heure fixe restent des ancres immuables : seules les étapes libres entre
 * elles sont réordonnées.
 */
object RouteOptimizer {

    /** Réordonne les étapes d'une journée pour minimiser la route. */
    fun optimize(stops: List<TripStop>, startLat: Double? = null, startLng: Double? = null): List<TripStop> {
        if (stops.size < 3) return stops
        // Les étapes horodatées sont des ancres : on optimise chaque segment libre
        if (stops.any { it.time != null }) return optimizeBetweenAnchors(stops)
        return twoOpt(nearestNeighbor(stops, startLat, startLng))
    }

    private fun optimizeBetweenAnchors(stops: List<TripStop>): List<TripStop> {
        val result = mutableListOf<TripStop>()
        var segment = mutableListOf<TripStop>()
        var lastAnchor: TripStop? = null
        fun flush(nextAnchor: TripStop?) {
            if (segment.isNotEmpty()) {
                val ordered = twoOpt(nearestNeighbor(segment, lastAnchor?.latitude, lastAnchor?.longitude))
                result.addAll(ordered)
                segment = mutableListOf()
            }
            nextAnchor?.let {
                result.add(it)
                lastAnchor = it
            }
        }
        stops.forEach { stop -> if (stop.time != null) flush(stop) else segment.add(stop) }
        flush(null)
        return result
    }

    internal fun nearestNeighbor(stops: List<TripStop>, startLat: Double?, startLng: Double?): List<TripStop> {
        val remaining = stops.toMutableList()
        val ordered = mutableListOf<TripStop>()
        var lat = startLat ?: remaining.first().latitude
        var lng = startLng ?: remaining.first().longitude
        if (startLat == null) {
            ordered.add(remaining.removeAt(0))
        }
        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull {
                ZoneAlertEngine.distanceMeters(lat, lng, it.latitude, it.longitude)
            }!!
            remaining.remove(next)
            ordered.add(next)
            lat = next.latitude
            lng = next.longitude
        }
        return ordered
    }

    internal fun twoOpt(stops: List<TripStop>): List<TripStop> {
        if (stops.size < 4) return stops
        var best = stops.toMutableList()
        var improved = true
        while (improved) {
            improved = false
            for (i in 0 until best.size - 2) {
                for (j in i + 2 until best.size) {
                    val delta = swapGain(best, i, j)
                    if (delta < -1.0) { // au moins 1 m de mieux
                        best = (
                            best.subList(0, i + 1) +
                                best.subList(i + 1, j + 1).reversed() +
                                best.subList(j + 1, best.size)
                            ).toMutableList()
                        improved = true
                    }
                }
            }
        }
        return best
    }

    private fun swapGain(stops: List<TripStop>, i: Int, j: Int): Double {
        fun d(a: TripStop, b: TripStop) =
            ZoneAlertEngine.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        val after = d(stops[i], stops[j]) +
            (if (j + 1 < stops.size) d(stops[i + 1], stops[j + 1]) else 0.0)
        val before = d(stops[i], stops[i + 1]) +
            (if (j + 1 < stops.size) d(stops[j], stops[j + 1]) else 0.0)
        return after - before
    }

    /** Longueur totale d'un ordre donné (pour tests et affichage). */
    fun totalMeters(stops: List<TripStop>): Double =
        stops.zipWithNext().sumOf { (a, b) ->
            ZoneAlertEngine.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
}
