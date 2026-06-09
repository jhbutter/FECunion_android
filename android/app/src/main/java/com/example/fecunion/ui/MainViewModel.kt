package com.example.fecunion.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fecunion.jni.FECunionBridge
import com.example.fecunion.model.ProcessResult
import kotlinx.coroutines.Dispatchers
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

    private val _result = MutableStateFlow<ProcessResult?>(null)
    val result: StateFlow<ProcessResult?> = _result

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName

    fun setFilePath(path: String, name: String) {
        _result.value = null
        _filePath.value = path
        _fileName.value = name
    }

    fun setTolerance(v: Double) { _tolerance.value = v }
    fun setMinSize(v: Int) { _minSize.value = v }
    fun setMaxN(v: Int) { _maxN.value = v }
    fun setLeafSize(v: Int) { _leafSize.value = v.coerceIn(5, 200) }

    fun run() {
        val path = _filePath.value ?: return
        viewModelScope.launch {
            _running.value = true
            _result.value = null
            val json = withContext(Dispatchers.IO) {
                FECunionBridge.nativeProcess(path, _tolerance.value, _minSize.value, _maxN.value, _leafSize.value)
            }
            _result.value = ProcessResult.fromJson(json)
            _running.value = false
        }
    }

    fun runSample(context: Context, assetName: String, displayName: String) {
        viewModelScope.launch {
            _running.value = true
            _result.value = null
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
                val json = FECunionBridge.nativeProcess(tmp.absolutePath, _tolerance.value, _minSize.value, _maxN.value, _leafSize.value)
                _result.value = ProcessResult.fromJson(json)
            }
            _running.value = false
        }
    }
}
