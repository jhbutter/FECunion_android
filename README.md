# FECunion — 快速欧式聚类 (Fast Euclidean Clustering)

基于 KD-tree + Union-Find 的点云欧式距离聚类算法。从 PCL 依赖的原始实现移植为 nanoflann + Android NDK 的零依赖版本。

## 项目结构

```
android/              # Android 应用 (Kotlin + JNI + NDK)
  app/src/main/
    cpp/              # C++ 原生层
      fecunion.hpp    # 核心聚类算法
      jni_bridge.cpp  # JNI 桥接层
      nanoflann.hpp   # KD-tree 库 (header-only)
      xyz_parser.hpp  # XYZ/TXT 解析
      pcd_parser.hpp  # PCD 解析 (ASCII + binary)
      ply_parser.hpp  # PLY 解析 (ASCII + binary)
    java/             # Kotlin UI + ViewModel
    assets/           # 内置测试点云
origination/          # 原始 PCL 版本 (参考)
  FECunion.h          # 含 Union-Find 优化的 FEC
  FEC1.h              # 基于 unordered_map 的 FEC
  FEC.h               # 原始 FEC (暴力合并)
  EC.h                # PCL 内置欧式聚类
  RG.h                # 区域生长分割
```

## 算法

```
点云 → KD-tree 建树 → 单遍扫描(半径搜索 + 并查集合并) → 按标签收集 → 簇列表
       O(N log N)       O(N × k × α(N))                    O(N)
```

### 超参数

| 参数 | 默认值 | 含义 |
|---|---|---|
| tolerance | 0.5 | 半径搜索的欧氏距离阈值 |
| minSize | 10 | 最小簇点数，不足的丢弃 |
| maxN | 0 | 最大邻居数，0 = 不限 |
| leafSize | 15 | KD-tree 叶节点大小 |

## 支持的文件格式

- XYZ / TXT：每行 `x y z`
- PCD：ASCII 和 binary
- PLY：ASCII 和 binary

## 构建

1. Android Studio 打开 `android/` 目录
2. 需要 NDK 25+、CMake 3.22.1+、AGP 9.2+
3. `./gradlew assembleDebug`

## 内置测试数据

点"内置数据"列表中的文件名即可自动加载并运行。添加新测试文件：丢到 `android/app/src/main/assets/`，重新构建即可。

## 依赖

| 组件 | 说明 |
|---|---|
| nanoflann | header-only KD-tree，唯一原生依赖 |
| Android NDK | 原生编译 |
| Jetpack Compose | UI 框架 |
| AGP 9.x | 构建系统 |

无 PCL、无第三方原生库。
