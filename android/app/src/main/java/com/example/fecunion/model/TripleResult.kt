package com.example.fecunion.model

data class TripleResult(
    val fecunion: ProcessResult,
    val fec: ProcessResult,
    val ec: ProcessResult
) {
    val allOk get() = fecunion.ok && fec.ok && ec.ok
}
