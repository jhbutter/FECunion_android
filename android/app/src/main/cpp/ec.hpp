#pragma once
#ifndef EC_ALGO_HPP
#define EC_ALGO_HPP

#include <vector>
#include <queue>
#include <chrono>
#include <cstdint>
#include "nanoflann.hpp"

using KDTreeEC = nanoflann::KDTreeSingleIndexAdaptor<
    nanoflann::L2_Simple_Adaptor<float, PointCloudAdaptor>,
    PointCloudAdaptor, 3>;

struct ECResult {
    std::vector<std::vector<int>> clusters;
    double total_ms = 0.0;
    double build_ms = 0.0;
    double search_ms = 0.0;
    double final_ms = 0.0;
};

inline ECResult ec(const float* xyz, int N, double tolerance, int minSize, int leafSize = 20) {
    using Clock = std::chrono::high_resolution_clock;

    ECResult result;
    auto t_total = Clock::now();

    if (!xyz || N < minSize) return result;

    PointCloudAdaptor adaptor{xyz, static_cast<std::size_t>(N)};

    auto t0 = Clock::now();
    KDTreeEC tree(3, adaptor, nanoflann::KDTreeSingleIndexAdaptorParams(leafSize));
    tree.buildIndex();
    auto t1 = Clock::now();
    result.build_ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    std::vector<int> marked(N, 0);
    std::vector<nanoflann::ResultItem<std::uint32_t, float>> matches;
    matches.reserve(64);
    std::queue<int> q;
    float query[3];

    for (int i = 0; i < N; ++i) {
        if (marked[i] != 0) continue;

        std::vector<int> cluster;
        q.push(i);
        marked[i] = 1;

        auto ts0 = Clock::now();
        while (!q.empty()) {
            int p = q.front(); q.pop();
            cluster.push_back(p);

            query[0] = xyz[p * 3];
            query[1] = xyz[p * 3 + 1];
            query[2] = xyz[p * 3 + 2];
            matches.clear();

            nanoflann::SearchParameters sparams;
            sparams.sorted = false;
            tree.radiusSearch(query, static_cast<float>(tolerance * tolerance), matches, sparams);

            for (const auto& m : matches) {
                int idx = static_cast<int>(m.first);
                if (marked[idx] == 0) {
                    marked[idx] = 1;
                    q.push(idx);
                }
            }
        }
        auto ts1 = Clock::now();
        result.search_ms += std::chrono::duration<double, std::milli>(ts1 - ts0).count();

        if (static_cast<int>(cluster.size()) >= minSize) {
            result.clusters.push_back(std::move(cluster));
        }
    }

    auto t_end = Clock::now();
    result.total_ms = std::chrono::duration<double, std::milli>(t_end - t_total).count();

    return result;
}

#endif
