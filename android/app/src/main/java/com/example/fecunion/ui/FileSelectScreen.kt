package com.example.fecunion.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fecunion.model.ProcessResult
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val cacheDir = remember { File(context.cacheDir, "fecunion") }
    val fileName by viewModel.fileName.collectAsState()
    val tolerance by viewModel.tolerance.collectAsState()
    val minSize by viewModel.minSize.collectAsState()
    val maxN by viewModel.maxN.collectAsState()
    val leafSize by viewModel.leafSize.collectAsState()
    val result by viewModel.result.collectAsState()
    val running by viewModel.running.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment ?: "unknown"
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            val ext = name.substringAfterLast('.', "")
            val tmp = File(cacheDir, "input$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.setFilePath(tmp.absolutePath, name)
        } catch (e: Exception) {
            // handled via result state if needed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FECunion") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择文件", style = MaterialTheme.typography.titleMedium)

                    OutlinedButton(
                        onClick = {
                            filePicker.launch(arrayOf(
                                "application/octet-stream",
                                "text/plain",
                                "*/*"
                            ))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (fileName.isEmpty()) "选择点云文件" else fileName)
                    }

                    val assets = remember {
                        runCatching { context.assets.list("") }
                            .getOrNull()
                            ?.filter { it.endsWith(".xyz") || it.endsWith(".ply") || it.endsWith(".pcd") }
                            ?.sorted()
                            ?: emptyList()
                    }

                    Text(
                        "内置数据",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (assets.isEmpty()) {
                        Text(
                            "无内置数据文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        assets.forEach { asset ->
                            TextButton(
                                onClick = { viewModel.runSample(context, asset, asset.substringBeforeLast('.')) },
                                enabled = !running,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    asset.substringBeforeLast('.'),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    asset.substringAfterLast('.').uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("参数设置", style = MaterialTheme.typography.titleMedium)

                    Text("搜索半径 (tolerance)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = DecimalFormat("0.000").format(tolerance),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.setTolerance(v.coerceAtLeast(0.001)) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Slider(
                        value = tolerance.toFloat(),
                        onValueChange = { viewModel.setTolerance(it.toDouble()) },
                        valueRange = 0.001f..20.0f
                    )

                    OutlinedTextField(
                        value = minSize.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.setMinSize(v.coerceAtLeast(1)) } },
                        label = { Text("最小簇大小") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = if (maxN == 0) "" else maxN.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: 0
                            viewModel.setMaxN(v.coerceAtLeast(0))
                        },
                        label = { Text("最大邻居数 (0=不限制)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = leafSize.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.setLeafSize(v) } },
                        label = { Text("KD-tree 叶大小 (5-200)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = { viewModel.run() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !running && fileName.isNotEmpty()
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("处理中...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("运行")
                }
            }

            result?.let { ResultCard(it) }
        }
    }
}

@Composable
private fun ResultCard(r: ProcessResult) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = if (r.ok) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.errorContainer
    )) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!r.ok) {
                Text("错误: ${r.error}", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Text("结果", style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("点云点数")
                Text("${r.pointCount}")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("簇数量")
                Text("${r.clusters.size}")
            }

            HorizontalDivider()

            Text("耗时统计", style = MaterialTheme.typography.titleSmall)
            TimeStatRow("建树 (build)", r.buildMs)
            TimeStatRow("搜索 (search)", r.searchMs)
            TimeStatRow("合并 (merge)", r.mergeMs)
            TimeStatRow("后处理 (final)", r.finalMs)
            TimeStatRow("总计 (total)", r.totalMs, bold = true)

            if (r.clusters.isNotEmpty()) {
                HorizontalDivider()
                Text("簇大小分布 (前20)", style = MaterialTheme.typography.titleSmall)
                Text(
                    r.clusters.take(20).joinToString(", ") { "#${it.index}:${it.size}" },
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
                if (r.clusters.size > 20) {
                    Text("... 还有 ${r.clusters.size - 20} 个簇", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TimeStatRow(label: String, ms: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall)
        Text(
            if (ms < 1000) "${DecimalFormat("0.0").format(ms)} ms"
            else "${DecimalFormat("0.00").format(ms / 1000.0)} s",
            style = if (bold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}
