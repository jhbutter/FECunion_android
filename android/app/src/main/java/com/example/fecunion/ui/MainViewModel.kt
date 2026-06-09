package com.example.fecunion.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fecunion.jni.FECunionBridge
import com.example.fecunion.model.ProcessResult
import com.example.fecunion.model.TripleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val _filePath = MutableStateFlow<String?>(null)
    val filePath: StateFlow<String?> = _filePath

    private val _tolerance = MutableStateFlow(0.5)
    val tolerance: StateFlow<Double> = _tolerance

    private val _minSize = MutableStateFlow(10)
    val minSize: StateFlow<Int> = _minSize

    private val _maxN = MutableStateFlow(0)
    val maxN: StateFlow<Int> = _maxN

    private val _leafSize = MutableStateFlow(15)
    val leafSize: StateFlow<Int> = _leafSize

    private val _algorithm = MutableStateFlow(0)
    val algorithm: StateFlow<Int> = _algorithm

    private val _comparisonMode = MutableStateFlow(false)
    val comparisonMode: StateFlow<Boolean> = _comparisonMode

    private val _resultCache = arrayOfNulls<ProcessResult>(3)
    private val _singleResult = MutableStateFlow<ProcessResult?>(null)
    val singleResult: StateFlow<ProcessResult?> = _singleResult

    private val _tripleResult = MutableStateFlow<TripleResult?>(null)
    val tripleResult: StateFlow<TripleResult?> = _tripleResult

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName

    fun setFilePath(path: String, name: String) {
        _resultCache.fill(null)
        _singleResult.value = null
        _tripleResult.value = null
        _filePath.value = path
        _fileName.value = name
    }

    fun setTolerance(v: Double) { _tolerance.value = v }
    fun setMinSize(v: Int) { _minSize.value = v }
    fun setMaxN(v: Int) { _maxN.value = v }
    fun setLeafSize(v: Int) { _leafSize.value = v.coerceIn(5, 200) }
    fun setAlgorithm(v: Int) { _algorithm.value = v.coerceIn(0, 2); _singleResult.value = _resultCache[_algorithm.value] }
    fun setComparisonMode(v: Boolean) { _comparisonMode.value = v; if (v) _tripleResult.value = cachedTriple() }

    private fun cachedTriple() = TripleResult(_resultCache[0], _resultCache[1], _resultCache[2])

    fun resetParams() {
        _tolerance.value = 0.5
        _minSize.value = 10
        _maxN.value = 0
        _leafSize.value = 15
    }

    private suspend fun runSingle(path: String): ProcessResult {
        val tol = _tolerance.value; val ms = _minSize.value
        val mn = _maxN.value; val ls = _leafSize.value; val alg = _algorithm.value
        val json = withContext(Dispatchers.IO) {
            FECunionBridge.nativeProcess(path, tol, ms, mn, ls, alg)
        }
        return ProcessResult.fromJson(json)
    }

    private suspend fun runAll(path: String): TripleResult {
        val tol = _tolerance.value; val ms = _minSize.value
        val mn = _maxN.value; val ls = _leafSize.value
        return withContext(Dispatchers.IO) {
            coroutineScope {
                val d0 = async { FECunionBridge.nativeProcess(path, tol, ms, mn, ls, 0) }
                val d1 = async { FECunionBridge.nativeProcess(path, tol, ms, mn, ls, 1) }
                val d2 = async { FECunionBridge.nativeProcess(path, tol, ms, mn, ls, 2) }
                TripleResult(
                    ProcessResult.fromJson(d0.await()),
                    ProcessResult.fromJson(d1.await()),
                    ProcessResult.fromJson(d2.await())
                )
            }
        }
    }

    fun run() {
        val path = _filePath.value ?: return
        viewModelScope.launch {
            _running.value = true
            if (_comparisonMode.value) {
                _singleResult.value = null
                _tripleResult.value = null
                val r = runAll(path)
                _resultCache[0] = r.fecunion
                _resultCache[1] = r.fec
                _resultCache[2] = r.ec
                _singleResult.value = _resultCache[_algorithm.value]
                _tripleResult.value = r
            } else {
                val r = runSingle(path)
                _resultCache[_algorithm.value] = r
                _singleResult.value = r
                _tripleResult.value = cachedTriple()
            }
            _running.value = false
        }
    }

    fun loadAsset(context: Context, assetName: String, displayName: String) {
        viewModelScope.launch {
            _resultCache.fill(null)
            _singleResult.value = null
            _tripleResult.value = null
            _fileName.value = displayName
            val cacheDir = java.io.File(context.cacheDir, "fecunion")
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            val tmp = java.io.File(cacheDir, assetName)
            withContext(Dispatchers.IO) {
                context.assets.open(assetName).use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                _filePath.value = tmp.absolutePath
            }
        }
    }
}
