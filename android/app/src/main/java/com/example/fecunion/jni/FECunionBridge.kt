package com.example.fecunion.jni

object FECunionBridge {
    init {
        System.loadLibrary("fecunion")
    }

    external fun nativeProcess(
        filepath: String,
        tolerance: Double,
        minSize: Int,
        maxN: Int,
        leafSize: Int,
        algorithm: Int
    ): String
}
