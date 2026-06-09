#include <jni.h>
#include <string>
#include <cstring>
#include <cstdio>
#include <sstream>
#include "point_cloud.hpp"
#include "xyz_parser.hpp"
#include "pcd_parser.hpp"
#include "ply_parser.hpp"
#include "fecunion.hpp"
#include "fec.hpp"
#include "ec.hpp"

static const char* file_ext(const char* path) {
    const char* dot = strrchr(path, '.');
    return dot ? dot + 1 : "";
}

static bool ieq(const char* a, const char* b) {
    return strcasecmp(a, b) == 0;
}

static std::string build_json(
    const std::vector<std::vector<int>>& clusters,
    double total_ms, double build_ms, double search_ms,
    double merge_ms, double final_ms,
    int point_count, double tolerance, int min_size, int max_n)
{
    std::ostringstream ss;
    ss.precision(4);
    ss << "{"
       << "\"ok\":true,"
       << "\"clusters\":[";
    for (size_t i = 0; i < clusters.size(); ++i) {
        if (i > 0) ss << ",";
        ss << "{\"index\":" << i << ",\"size\":" << clusters[i].size() << "}";
    }
    ss << "],"
       << "\"total_ms\":" << total_ms << ","
       << "\"build_ms\":" << build_ms << ","
       << "\"search_ms\":" << search_ms << ","
       << "\"merge_ms\":" << merge_ms << ","
       << "\"final_ms\":" << final_ms << ","
       << "\"point_count\":" << point_count << ","
       << "\"tolerance\":" << tolerance << ","
       << "\"min_size\":" << min_size << ","
       << "\"max_n\":" << max_n
       << "}";
    return ss.str();
}

static std::string error_json(const char* msg) {
    std::ostringstream ss;
    ss << "{\"ok\":false,\"error\":\"" << msg << "\"}";
    return ss.str();
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_fecunion_jni_FECunionBridge_nativeProcess(
    JNIEnv* env, jclass /*clazz*/,
    jstring filepath,
    jdouble tolerance,
    jint minSize,
    jint maxN,
    jint leafSize,
    jint algorithm) {

    const char* path = env->GetStringUTFChars(filepath, nullptr);
    if (!path) return env->NewStringUTF(error_json("null path").c_str());

    std::vector<float> xyz;
    bool ok = false;
    const char* ext = file_ext(path);

    if (ieq(ext, "xyz") || ieq(ext, "txt")) {
        ok = parse_xyz(path, xyz);
    } else if (ieq(ext, "pcd")) {
        ok = parse_pcd(path, xyz);
    } else if (ieq(ext, "ply")) {
        ok = parse_ply(path, xyz);
    } else {
        ok = parse_xyz(path, xyz);
        if (!ok) ok = parse_pcd(path, xyz);
        if (!ok) ok = parse_ply(path, xyz);
    }

    env->ReleaseStringUTFChars(filepath, path);

    if (!ok) return env->NewStringUTF(error_json("parse failed").c_str());

    int N = static_cast<int>(xyz.size() / 3);
    std::string json;

    if (algorithm == 1) {
        FECResult r = fec(xyz.data(), N, tolerance, minSize, maxN, leafSize);
        json = build_json(r.clusters, r.total_ms, r.build_ms, r.search_ms, r.merge_ms, r.final_ms, N, tolerance, minSize, maxN);
    } else if (algorithm == 2) {
        ECResult r = ec(xyz.data(), N, tolerance, minSize, leafSize);
        json = build_json(r.clusters, r.total_ms, r.build_ms, r.search_ms, 0.0, r.final_ms, N, tolerance, minSize, maxN);
    } else {
        FECunionResult r = fecunion(xyz.data(), N, tolerance, minSize, maxN, leafSize);
        json = build_json(r.clusters, r.total_ms, r.build_ms, r.search_ms, r.merge_ms, r.final_ms, N, tolerance, minSize, maxN);
    }

    return env->NewStringUTF(json.c_str());
}

} // extern "C"
