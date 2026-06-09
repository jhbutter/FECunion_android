package com.example.fecunion.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fecunion.model.ProcessResult
import com.example.fecunion.model.TripleResult
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
    val algorithm by viewModel.algorithm.collectAsState()
    val comparisonMode by viewModel.comparisonMode.collectAsState()
    val singleResult by viewModel.singleResult.collectAsState()
    val tripleResult by viewModel.tripleResult.collectAsState()
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
                                onClick = { viewModel.loadAsset(context, asset, asset.substringBeforeLast('.')) },
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("参数设置", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { viewModel.resetParams() }) { Text("重置默认") }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !comparisonMode,
                            onClick = { viewModel.setComparisonMode(false) },
                            label = { Text("单算法") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = comparisonMode,
                            onClick = { viewModel.setComparisonMode(true) },
                            label = { Text("对比模式") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!comparisonMode) {
                        val algOptions = listOf("FECunion", "FEC", "EC")
                        var algExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = algExpanded,
                            onExpandedChange = { algExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = algOptions[algorithm],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("算法") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = algExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = algExpanded,
                                onDismissRequest = { algExpanded = false }
                            ) {
                                algOptions.forEachIndexed { i, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = { viewModel.setAlgorithm(i); algExpanded = false }
                                    )
                                }
                            }
                        }
                    }

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

            if (comparisonMode) {
                tripleResult?.let { ComparisonCard(it) }
            } else {
                singleResult?.let { SingleResultCard(it, algorithm) }
            }
        }
    }
}

private val ALGO_NAMES = listOf("FECunion", "FEC", "EC")
private val ALGO_COLORS = listOf(0xFF1565C0, 0xFF00897B, 0xFFE65100)

private data class TimingRow(val label: String, val fecunion: Double, val fec: Double, val ec: Double)

@Composable
private fun SingleResultCard(r: ProcessResult, algoIdx: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!r.ok) {
                Text("${ALGO_NAMES[algoIdx]}: ${r.error}", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Text("结果 — ${ALGO_NAMES[algoIdx]}", style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("点云点数"); Text("${r.pointCount}")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("簇数量"); Text("${r.clusters.size}")
            }

            HorizontalDivider()

            Text("耗时", style = MaterialTheme.typography.titleSmall)
            val df = DecimalFormat("0.0000")
            TimeRow("建树 Build", r.buildMs, df)
            TimeRow("搜索 Search", r.searchMs, df)
            TimeRow("合并 Merge", r.mergeMs, df)
            TimeRow("后处理 Final", r.finalMs, df)
            TimeRow("总计 Total", r.totalMs, df, bold = true)

            HorizontalDivider()

            HorizontalBarChart(r.clusters.map { it.size }, ALGO_NAMES[algoIdx], Color(ALGO_COLORS[algoIdx]))
        }
    }
}

@Composable
private fun TimeRow(label: String, ms: Double, df: DecimalFormat, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text("${df.format(ms)} ms", fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ComparisonCard(r: TripleResult) {
    if (!r.allOk) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf(r.fecunion, r.fec, r.ec).forEachIndexed { i, pr ->
                    if (!pr.ok) Text("${ALGO_NAMES[i]}: ${pr.error}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("结果 — 对比", style = MaterialTheme.typography.titleMedium)

            Text(
                "点数: ${r.fecunion.pointCount}  |  "
                + "簇数 — FECunion: ${r.fecunion.clusters.size}, FEC: ${r.fec.clusters.size}, EC: ${r.ec.clusters.size}",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            TimingTable(r)

            HorizontalDivider()

            Text("簇大小分布", style = MaterialTheme.typography.titleSmall)
            ALGO_NAMES.forEachIndexed { i, name ->
                val result = listOf(r.fecunion, r.fec, r.ec)[i]
                HorizontalBarChart(result.clusters.map { it.size }, name, Color(ALGO_COLORS[i]))
            }
        }
    }
}

@Composable
private fun TimingTable(r: TripleResult) {
    val df = DecimalFormat("0.0000")
    val rows = listOf(
        TimingRow("建树", r.fecunion.buildMs, r.fec.buildMs, r.ec.buildMs),
        TimingRow("搜索", r.fecunion.searchMs, r.fec.searchMs, r.ec.searchMs),
        TimingRow("合并", r.fecunion.mergeMs, r.fec.mergeMs, 0.0),
        TimingRow("后处理", r.fecunion.finalMs, r.fec.finalMs, r.ec.finalMs),
        TimingRow("总计", r.fecunion.totalMs, r.fec.totalMs, r.ec.totalMs),
    )

    Text("耗时 (ms)", style = MaterialTheme.typography.titleSmall)

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("", Modifier.width(56.dp))
            ALGO_NAMES.forEachIndexed { i, name ->
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = Color(ALGO_COLORS[i]), maxLines = 1)
            }
        }
        rows.forEach { row ->
            val isTotal = row.label == "总计"
            Row(
                Modifier.fillMaxWidth()
                    .background(if (isTotal) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(row.label, Modifier.width(56.dp),
                    style = if (isTotal) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal)
                val vs = listOf(row.fecunion, row.fec, row.ec)
                val ecMerge = row.label == "合并"
                vs.forEachIndexed { i, v ->
                    val valid = vs.filterIndexed { j, _ -> !(ecMerge && j == 2) }
                    val fastest = valid.minOrNull()
                    val isBest = v == fastest && !(ecMerge && i == 2) && (fastest ?: 0.0) > 0.0
                    Text(
                        if (ecMerge && i == 2) "-" else df.format(v),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                        color = if (isBest) Color(0xFF2E7D32) else Color.Unspecified,
                        fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal, maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalBarChart(sizes: List<Int>, title: String, color: Color) {
    Column(Modifier.padding(top = 4.dp)) {
        Text("$title (${sizes.size} 个簇)", style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.9f))
        Spacer(Modifier.height(4.dp))
        val maxSize = sizes.maxOrNull() ?: 1
        val display = sizes.take(10)
        display.forEachIndexed { i, sz ->
            val frac = sz.toFloat() / maxSize
            Row(Modifier.fillMaxWidth().height(14.dp).padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("#$i", Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace, maxLines = 1)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(color.copy(alpha = 0.7f)))
                }
                Spacer(Modifier.width(4.dp))
                Text("$sz", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace, maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
        if (sizes.size > 10) {
            Text("... 还有 ${sizes.size - 10} 个簇", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
