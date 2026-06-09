# CLAUDE.md

## 项目概述

FECunion — 快速欧式距离点云聚类。算法核心在 C++ header 中，通过 JNI 桥接到 Android Compose UI。

## 架构

```
Kotlin UI → ViewModel → JNI Bridge → C++ parser → fecunion() → JSON → UI
```

- C++ 代码均为 header-only，只有 `jni_bridge.cpp` 一个编译单元
- 构建产物：`libfecunion.so`
- 唯一的原生依赖：nanoflann（vendor 在 cpp 目录中）

## 关键文件

| 文件 | 角色 |
|---|---|
| `android/app/src/main/cpp/fecunion.hpp` | 核心聚类算法 |
| `android/app/src/main/cpp/jni_bridge.cpp` | JNI 入口 + JSON 序列化 |
| `android/app/src/main/java/.../ui/MainViewModel.kt` | 状态管理 + 异步调用 |
| `android/app/src/main/java/.../ui/FileSelectScreen.kt` | 全部 UI |
| `android/app/src/main/java/.../jni/FECunionBridge.kt` | JNI 声明 |
| `android/app/src/main/java/.../model/ClusterResult.kt` | 结果反序列化 |
| `android/gradle/libs.versions.toml` | 版本目录 |
| `origination/` | 原始 PCL 版参考，不影响构建 |

## 构建

```
cd android && ./gradlew assembleDebug
```

Gradle 9.4.1, AGP 9.2.1, NDK ≥25, CMake 3.22.1。Android Studio 需打开 `android/` 子目录而非项目根目录。

## UI 参数默认值

| 参数 | 默认 | 定义位置 |
|---|---|---|
| tolerance | 0.5 | MainViewModel._tolerance |
| minSize | 10 | MainViewModel._minSize |
| maxN | 0 | MainViewModel._maxN |
| leafSize | 15 | MainViewModel._leafSize |

## 内置数据

文件名列表由 `context.assets.list("")` 动态读取，只显示 `.xyz/.ply/.pcd` 文件。添加新数据：丢文件到 `android/app/src/main/assets/`，重新 build。

## 文件写入

- 使用 Bash heredoc：`cat > file << 'DELIM' ... DELIM` 或 `cat >> file << 'DELIM' ... DELIM`
- 不要用 Write 工具

## 代码风格

- 不写注释，除非 WHY 非显而易见
- 优先改现有文件，不新建除非必要
- 不改超出当前任务范围的代码
