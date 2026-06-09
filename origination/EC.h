#pragma once
#ifndef PCL_SEGEMENT_EC_H
#define PCL_SEGEMENT_EC_H

#include <pcl/point_types.h>
#include <pcl/point_cloud.h>
#include <pcl/kdtree/kdtree.h>
#include <pcl/segmentation/extract_clusters.h>
#include <pcl/PointIndices.h>

#include <vector>
#include <chrono>

struct ECStageStats {
    double total_ms  = 0.0;
    double build_ms  = 0.0;
    double search_ms = 0.0;
    double merge_ms  = 0.0;
    double final_ms  = 0.0;
    int clusters     = 0;
};

inline std::vector<pcl::PointIndices> EC(
    pcl::PointCloud<pcl::PointXYZ>::Ptr cloud,
    double cluster_tolerance,
    int min_cluster_size,
    ECStageStats* stats = nullptr
) {
    using Clock = std::chrono::high_resolution_clock;

    auto t_total_begin = Clock::now();

    double build_ms = 0.0;
    double search_ms = 0.0;
    double merge_ms = 0.0;
    double final_ms = 0.0;

    if (!cloud || cloud->empty()) {
        if (stats) {
            stats->total_ms = 0.0;
            stats->build_ms = 0.0;
            stats->search_ms = 0.0;
            stats->merge_ms = 0.0;
            stats->final_ms = 0.0;
            stats->clusters = 0;
        }
        return {};
    }

    auto t0 = Clock::now();
    pcl::search::KdTree<pcl::PointXYZ>::Ptr tree(new pcl::search::KdTree<pcl::PointXYZ>);
    tree->setInputCloud(cloud);
    auto t1 = Clock::now();
    build_ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    std::vector<pcl::PointIndices> cluster_indices;

    auto t2 = Clock::now();
    pcl::EuclideanClusterExtraction<pcl::PointXYZ> ec;
    ec.setClusterTolerance(cluster_tolerance);
    ec.setMinClusterSize(min_cluster_size);
    ec.setSearchMethod(tree);
    ec.setInputCloud(cloud);
    ec.extract(cluster_indices);
    auto t3 = Clock::now();

    // PCL 内部没有把 search/merge 单独暴露出来，这里统一记到 final 阶段
    final_ms = std::chrono::duration<double, std::milli>(t3 - t2).count();

    auto t_total_end = Clock::now();
    double total_ms = std::chrono::duration<double, std::milli>(t_total_end - t_total_begin).count();

    if (stats) {
        stats->total_ms = total_ms;
        stats->build_ms = build_ms;
        stats->search_ms = search_ms; // 保持 0
        stats->merge_ms = merge_ms;   // 保持 0
        stats->final_ms = final_ms;
        stats->clusters = static_cast<int>(cluster_indices.size());
    }

    return cluster_indices;
}

#endif // PCL_SEGEMENT_EC_H