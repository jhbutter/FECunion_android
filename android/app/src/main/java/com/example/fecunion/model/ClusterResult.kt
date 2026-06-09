package com.example.fecunion.model

import org.json.JSONArray
import org.json.JSONObject

data class ClusterInfo(val index: Int, val size: Int)

data class ProcessResult(
    val ok: Boolean,
    val error: String = "",
    val clusters: List<ClusterInfo> = emptyList(),
    val totalMs: Double = 0.0,
    val buildMs: Double = 0.0,
    val searchMs: Double = 0.0,
    val mergeMs: Double = 0.0,
    val finalMs: Double = 0.0,
    val pointCount: Int = 0,
    val tolerance: Double = 0.0,
    val minSize: Int = 0,
    val maxN: Int = 0
) {
    companion object {
        fun fromJson(json: String): ProcessResult {
            return try {
                val obj = JSONObject(json)
                if (!obj.getBoolean("ok")) {
                    return ProcessResult(ok = false, error = obj.optString("error", "unknown"))
                }
                val clusters = mutableListOf<ClusterInfo>()
                val arr: JSONArray = obj.getJSONArray("clusters")
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    clusters.add(ClusterInfo(c.getInt("index"), c.getInt("size")))
                }
                ProcessResult(
                    ok = true,
                    clusters = clusters,
                    totalMs = obj.getDouble("total_ms"),
                    buildMs = obj.getDouble("build_ms"),
                    searchMs = obj.getDouble("search_ms"),
                    mergeMs = obj.getDouble("merge_ms"),
                    finalMs = obj.getDouble("final_ms"),
                    pointCount = obj.getInt("point_count"),
                    tolerance = obj.getDouble("tolerance"),
                    minSize = obj.getInt("min_size"),
                    maxN = obj.getInt("max_n")
                )
            } catch (e: Exception) {
                ProcessResult(ok = false, error = "parse error: ${e.message}")
            }
        }
    }
}

data class TripleResult(
    val fecunion: ProcessResult?,
    val fec: ProcessResult?,
    val ec: ProcessResult?
) {
    val allOk get() = listOfNotNull(fecunion, fec, ec).all { it.ok }
}
