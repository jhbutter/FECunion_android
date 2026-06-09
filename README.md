# FECunion — 快速欧式聚类 (Fast Euclidean Clustering)

基于 KD-tree 的点云欧式距离聚类算法，支持三种算法、双模 UI。从 PCL 依赖的原始实现移植为 nanoflann + Android NDK 的零依赖版本。

## 项目结构

```
android/              # Android 应用 (Kotlin + JNI + NDK)
  app/src/main/
    cpp/              # C++ 原生层
      fecunion.hpp    # FECunion 聚类 (Union-Find 合并)
      fec.hpp         # FEC 聚类 (暴力合并)
      ec.hpp          # EC 聚类 (BFS 区域生长)
      jni_bridge.cpp  # JNI 桥接 + 算法分发
      nanoflann.hpp   # KD-tree 库 (header-only)
      xyz_parser.hpp  # XYZ/TXT 解析
      pcd_parser.hpp  # PCD 解析 (ASCII + binary)
      ply_parser.hpp  # PLY 解析 (ASCII + binary)
    java/             # Kotlin UI + ViewModel
      model/          # ProcessResult, TripleResult
    assets/           # 内置测试点云
origination/          # 原始 PCL 版本 (参考)
  FECunion.h          # 含 Union-Find 优化的 FEC
  FEC1.h              # 基于 unordered_map 的 FEC
  FEC.h               # 原始 FEC (暴力合并)
  EC.h                # PCL 内置欧式聚类
```

## 三种算法

| 算法 | 合并策略 | 特点 |
|---|---|---|
| FECunion | Union-Find + 路径压缩 | O(N·α(N)) 合并，带 min_label 追踪 |
| FEC | 暴力全扫描换标签 | O(N²) per conflict，排序收集簇 |
| EC | BFS 队列区域生长 | 无合并阶段，逐点入队出队 |

三种算法共享同一个 KD-tree 搜索前端，差异仅在如何分配合并标签。

## 双模式 UI

- **单算法模式**：选择一个算法独立运行，显示耗时明细和簇分布柱状图
- **对比模式**：同时运行三种算法，并列展示时间对比表（最快项绿色高亮）和各自的簇分布

## 超参数

| 参数 | 默认值 | 含义 |
|---|---|---|
| tolerance | 0.5 | 半径搜索的欧氏距离阈值 |
| minSize | 10 | 最小簇点数，不足的丢弃 |
| maxN | 0 | 最大邻居数，0 = 不限 |
| leafSize | 15 | KD-tree 叶节点大小，范围 5-200 |

EC 算法不使用 maxN 参数。

## 支持的文件格式

- XYZ / TXT：每行 `x y z`
- PCD：ASCII 和 binary
- PLY：ASCII 和 binary

## 构建

```
cd android && ./gradlew assembleDebug
```

需要 NDK 25+、CMake 3.22.1+、AGP 9.2+。Android Studio 需打开 `android/` 子目录而非项目根目录。

## 内置测试数据

点击"内置数据"列表中的文件名即可载入（不自动运行），添加新测试文件：丢到 `android/app/src/main/assets/`，重新构建即可。

## 依赖

| 组件 | 说明 |
|---|---|
| nanoflann | header-only KD-tree，唯一原生依赖 |
| Android NDK | 原生编译 |
| Jetpack Compose | UI 框架 |
| AGP 9.x | 构建系统 |

无 PCL、无第三方原生库。
