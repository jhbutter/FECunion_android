# CLAUDE.md

## 项目概述

FECunion — 快速欧式距离点云聚类。三种算法（FECunion / FEC / EC）核心在 C++ header 中，通过 JNI 桥接到 Android Compose UI，支持单算法运行和对比模式。

## 架构

```
Kotlin UI → ViewModel → JNI Bridge → parser → algorithm() → JSON → UI
                                          ↑
                               algorithm 参数分发到:
                               fecunion() / fec() / ec()

对比模式: coroutineScope + async × 3 并行执行三种算法
```

- C++ 代码均为 header-only，只有 `jni_bridge.cpp` 一个编译单元
- 构建产物：`libfecunion.so`
- 唯一的原生依赖：nanoflann（vendor 在 cpp 目录中）

## 关键文件

| 文件 | 角色 |
|---|---|
| `android/app/src/main/cpp/fecunion.hpp` | FECunion 算法 (Union-Find 合并) |
| `android/app/src/main/cpp/fec.hpp` | FEC 算法 (暴力 O(N²) 合并) |
| `android/app/src/main/cpp/ec.hpp` | EC 算法 (BFS 区域生长) |
| `android/app/src/main/cpp/jni_bridge.cpp` | JNI 入口 + JSON 序列化 + 算法分发 |
| `android/app/src/main/java/.../ui/MainViewModel.kt` | 状态管理 + 异步调用 |
| `android/app/src/main/java/.../ui/FileSelectScreen.kt` | 全部 UI |
| `android/app/src/main/java/.../jni/FECunionBridge.kt` | JNI 声明 |
| `android/app/src/main/java/.../model/ClusterResult.kt` | ProcessResult + TripleResult 反序列化 |
| `android/gradle/libs.versions.toml` | 版本目录 |
| `origination/` | 原始 PCL 版参考，不影响构建 |

## 构建

```
cd android && ./gradlew assembleDebug
```

Gradle 9.4.1, AGP 9.2.1, NDK ≥25, CMake 3.22.1。Android Studio 需打开 `android/` 子目录而非项目根目录。

## 算法差异

| | FECunion | FEC | EC |
|---|---|---|---|
| 合并方式 | Union-Find + 路径压缩 | 暴力全扫描换标签 | 无合并 (BFS 队列) |
| merge_ms | 有 | 有 | 传入 0.0 |
| maxN | 有 | 有 | 无 |
| 收集方式 | UF canonical_label | 排序按 tag 收集 | BFS 出队时直接收集 |

JNI `algorithm` 参数：0=FECunion, 1=FEC, 2=EC。

## UI 参数默认值

| 参数 | 默认 | 定义位置 |
|---|---|---|
| tolerance | 0.5 | MainViewModel._tolerance |
| minSize | 10 | MainViewModel._minSize |
| maxN | 0 | MainViewModel._maxN |
| leafSize | 15 | MainViewModel._leafSize |

resetParams() 恢复以上默认值。

## 内置数据

文件名列表由 `context.assets.list("")` 动态读取，只显示 `.xyz/.ply/.pcd` 文件。添加新数据：丢文件到 `android/app/src/main/assets/`，重新 build。

## 文件写入

- 使用 Bash heredoc：`cat > file << 'DELIM' ... DELIM` 或 `cat >> file << 'DELIM' ... DELIM`
- 不要用 Write 工具

## 代码风格

- 不写注释，除非 WHY 非显而易见
- 优先改现有文件，不新建除非必要
- 不改超出当前任务范围的代码
