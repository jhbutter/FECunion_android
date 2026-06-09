#pragma once
#ifndef PCL_SEGEMENT_FEC_H
#define PCL_SEGEMENT_FEC_H

#include <pcl/point_types.h>
#include <pcl/point_cloud.h>
#include <pcl/kdtree/kdtree_flann.h>
#include <pcl/PointIndices.h>

#include <vector>
#include <algorithm>
#include <chrono>

struct PointIndex_NumberTag {
    float nPointIndex;
    float nNumberTag;
};

inline bool NumberTag(const PointIndex_NumberTag& p0, const PointIndex_NumberTag& p1) {
    return p0.nNumberTag < p1.nNumberTag;
}

struct FECStageStats {
    double total_ms  = 0.0;
    double build_ms  = 0.0;
    double search_ms = 0.0;
    double merge_ms  = 0.0;
    double final_ms  = 0.0;
    int clusters     = 0;
};

inline std::vector<pcl::PointIndices> FEC(
    pcl::PointCloud<pcl::PointXYZ>::Ptr cloud,
    int min_component_size,
    double tolorance,
    int max_n,
    FECStageStats* stats = nullptr
) {
    using Clock = std::chrono::high_resolution_clock;

    auto t_total_begin = Clock::now();
    double build_ms = 0.0;
    double search_ms = 0.0;
    double merge_ms = 0.0;
    double final_ms = 0.0;

    if (!cloud || cloud->size() < static_cast<std::size_t>(min_component_size)) {
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
    pcl::KdTreeFLANN<pcl::PointXYZ> cloud_kdtreeflann;
    cloud_kdtreeflann.setInputCloud(cloud);
    auto t1 = Clock::now();
    build_ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    const int cloud_size = static_cast<int>(cloud->size());
    std::vector<int> marked_indices(cloud_size, 0);

    std::vector<int> pointIdx;
    std::vector<float> pointquaredDistance;
    pointIdx.reserve(max_n > 0 ? std::min(max_n, cloud_size) : cloud_size);
    pointquaredDistance.reserve(max_n > 0 ? std::min(max_n, cloud_size) : cloud_size);

    int tag_num = 1;
    int temp_tag_num = -1;

    for (int i = 0; i < cloud_size; ++i) {
        if (marked_indices[i] == 0) {
            pointIdx.clear();
            pointquaredDistance.clear();

            auto ts0 = Clock::now();
            cloud_kdtreeflann.radiusSearch(
                cloud->points[i], tolorance, pointIdx, pointquaredDistance, max_n
            );
            auto ts1 = Clock::now();
            search_ms += std::chrono::duration<double, std::milli>(ts1 - ts0).count();

            int min_tag_num = tag_num;
            for (int j = 0; j < static_cast<int>(pointIdx.size()); ++j) {
                const int idx = pointIdx[j];
                if ((marked_indices[idx] > 0) && (marked_indices[idx] < min_tag_num)) {
                    min_tag_num = marked_indices[idx];
                }
            }

            auto tm0 = Clock::now();
            for (int j = 0; j < static_cast<int>(pointIdx.size()); ++j) {
                temp_tag_num = marked_indices[pointIdx[j]];
                if (temp_tag_num > min_tag_num) {
                    for (int k = 0; k < cloud_size; ++k) {
                        if (marked_indices[k] == temp_tag_num) {
                            marked_indices[k] = min_tag_num;
                        }
                    }
                }
                marked_indices[pointIdx[j]] = min_tag_num;
            }
            auto tm1 = Clock::now();
            merge_ms += std::chrono::duration<double, std::milli>(tm1 - tm0).count();

            ++tag_num;
        }
    }

    auto tf0 = Clock::now();

    std::vector<PointIndex_NumberTag> indices_tags(cloud_size);
    std::vector<pcl::PointIndices> cluster_indices;
    pcl::PointIndices::Ptr inliers(new pcl::PointIndices);

    for (int i = 0; i < cloud_size; ++i) {
        PointIndex_NumberTag temp_index_tag;
        temp_index_tag.nPointIndex = static_cast<float>(i);
        temp_index_tag.nNumberTag = static_cast<float>(marked_indices[i]);
        indices_tags[i] = temp_index_tag;
    }

    std::sort(indices_tags.begin(), indices_tags.end(), NumberTag);

    int begin_index = 0;
    for (int i = 0; i < cloud_size; ++i) {
        if (indices_tags[i].nNumberTag != indices_tags[begin_index].nNumberTag) {
            if ((i - begin_index) >= min_component_size) {
                inliers->indices.resize(i - begin_index);
                int m = 0;
                for (int j = begin_index; j < i; ++j) {
                    inliers->indices[m++] = static_cast<int>(indices_tags[j].nPointIndex);
                }
                cluster_indices.push_back(*inliers);
            }
            begin_index = i;
        }
    }

    if ((cloud_size - begin_index) >= min_component_size) {
        inliers->indices.resize(cloud_size - begin_index);
        int m = 0;
        for (int j = begin_index; j < cloud_size; ++j) {
            inliers->indices[m++] = static_cast<int>(indices_tags[j].nPointIndex);
        }
        cluster_indices.push_back(*inliers);
    }

    auto tf1 = Clock::now();
    final_ms = std::chrono::duration<double, std::milli>(tf1 - tf0).count();

    auto t_total_end = Clock::now();
    double total_ms = std::chrono::duration<double, std::milli>(t_total_end - t_total_begin).count();

    if (stats) {
        stats->total_ms = total_ms;
        stats->build_ms = build_ms;
        stats->search_ms = search_ms;
        stats->merge_ms = merge_ms;
        stats->final_ms = final_ms;
        stats->clusters = static_cast<int>(cluster_indices.size());
    }

    return cluster_indices;
}

#endif // PCL_SEGEMENT_FEC_H