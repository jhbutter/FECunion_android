#pragma once
#ifndef PCL_SEGEMENT_FEC1_H
#define PCL_SEGEMENT_FEC1_H

#include <pcl/point_types.h>
#include <pcl/kdtree/kdtree_flann.h>
#include <pcl/PointIndices.h>

#include <vector>
#include <algorithm>
#include <unordered_map>
#include <chrono>
#include <iostream>
#include <iomanip>

struct PointIndex_Tag_FEC1 {
    int nPointIndex;
    int nNumberTag;
};

inline bool NumberTag_FEC1(const PointIndex_Tag_FEC1& p0, const PointIndex_Tag_FEC1& p1) {
    return p0.nNumberTag < p1.nNumberTag;
}

inline std::vector<pcl::PointIndices> FEC1(
    pcl::PointCloud<pcl::PointXYZ>::Ptr cloud,
    int min_component_size,
    double tolorance,
    int max_n
) {
    using Clock = std::chrono::high_resolution_clock;

    auto t_total_begin = Clock::now();
    double build_ms = 0.0, search_ms = 0.0, merge_ms = 0.0, final_ms = 0.0;

    if (cloud->size() < static_cast<std::size_t>(min_component_size)) {
        return {};
    }

    auto t0 = Clock::now();
    pcl::KdTreeFLANN<pcl::PointXYZ> cloud_kdtreeflann;
    cloud_kdtreeflann.setInputCloud(cloud);
    auto t1 = Clock::now();
    build_ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    const int cloud_size = static_cast<int>(cloud->size());
    std::vector<int> marked_indices(cloud_size, 0);

    std::vector<int> pointIdx;
    std::vector<float> pointSquaredDistance;
    pointIdx.reserve(max_n);
    pointSquaredDistance.reserve(max_n);

    int tag_num = 1;
    std::unordered_map<int, std::vector<int>> tag_to_indices;

    for (int i = 0; i < cloud_size; ++i) {
        if (marked_indices[i] == 0) {
            pointIdx.clear();
            pointSquaredDistance.clear();

            auto ts0 = Clock::now();
            cloud_kdtreeflann.radiusSearch(cloud->points[i], tolorance, pointIdx, pointSquaredDistance, max_n);
            auto ts1 = Clock::now();
            search_ms += std::chrono::duration<double, std::milli>(ts1 - ts0).count();

            int min_tag_num = tag_num;
            for (int j = 0; j < static_cast<int>(pointIdx.size()); ++j) {
                int idx = pointIdx[j];
                if (marked_indices[idx] > 0 && marked_indices[idx] < min_tag_num) {
                    min_tag_num = marked_indices[idx];
                }
            }

            auto tm0 = Clock::now();
            for (int j = 0; j < static_cast<int>(pointIdx.size()); ++j) {
                int idx = pointIdx[j];
                int current_tag = marked_indices[idx];
                if (current_tag > 0 && current_tag != min_tag_num) {
                    auto it = tag_to_indices.find(current_tag);
                    if (it != tag_to_indices.end()) {
                        auto& points = it->second;
                        for (int p : points) {
                            marked_indices[p] = min_tag_num;
                            tag_to_indices[min_tag_num].push_back(p);
                        }
                        tag_to_indices.erase(current_tag);
                    }
                }
                if (marked_indices[idx] != min_tag_num) {
                    marked_indices[idx] = min_tag_num;
                    tag_to_indices[min_tag_num].push_back(idx);
                }
            }
            auto tm1 = Clock::now();
            merge_ms += std::chrono::duration<double, std::milli>(tm1 - tm0).count();

            if (min_tag_num == tag_num) tag_num++;
        }
    }

    auto tf0 = Clock::now();

    std::vector<PointIndex_Tag_FEC1> indices_tags(cloud_size);
    for (int i = 0; i < cloud_size; ++i) {
        indices_tags[i].nPointIndex = i;
        indices_tags[i].nNumberTag = marked_indices[i];
    }

    std::sort(indices_tags.begin(), indices_tags.end(), NumberTag_FEC1);

    std::vector<pcl::PointIndices> cluster_indices;
    int begin_index = 0;
    for (int i = 0; i < cloud_size; ++i) {
        if (indices_tags[i].nNumberTag != indices_tags[begin_index].nNumberTag) {
            int cluster_size = i - begin_index;
            if (cluster_size >= min_component_size) {
                pcl::PointIndices inliers;
                inliers.indices.reserve(cluster_size);
                for (int j = begin_index; j < i; ++j) {
                    inliers.indices.push_back(indices_tags[j].nPointIndex);
                }
                cluster_indices.push_back(inliers);
            }
            begin_index = i;
        }
    }

    int final_cluster_size = cloud_size - begin_index;
    if (final_cluster_size >= min_component_size) {
        pcl::PointIndices inliers;
        inliers.indices.reserve(final_cluster_size);
        for (int j = begin_index; j < cloud_size; ++j) {
            inliers.indices.push_back(indices_tags[j].nPointIndex);
        }
        cluster_indices.push_back(inliers);
    }

    auto tf1 = Clock::now();
    final_ms = std::chrono::duration<double, std::milli>(tf1 - tf0).count();

    auto t_total_end = Clock::now();
    double total_ms = std::chrono::duration<double, std::milli>(t_total_end - t_total_begin).count();

    std::cout << std::fixed << std::setprecision(3)
              << "FEC1_KdTree total= " << total_ms << " ms "
              << "build= " << build_ms << " ms "
              << "search= " << search_ms << " ms "
              << "merge= " << merge_ms << " ms "
              << "final= " << final_ms << " ms "
              << "clusters=" << cluster_indices.size() << "\n";

    return cluster_indices;
}

#endif