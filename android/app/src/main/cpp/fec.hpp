#pragma once
#ifndef FEC_ALGO_HPP
#define FEC_ALGO_HPP

#include <vector>
#include <algorithm>
#include <chrono>
#include <cstdint>
#include "nanoflann.hpp"

using KDTreeFEC = nanoflann::KDTreeSingleIndexAdaptor<
    nanoflann::L2_Simple_Adaptor<float, PointCloudAdaptor>,
    PointCloudAdaptor, 3>;

struct FECResult {
    std::vector<std::vector<int>> clusters;
    double total_ms = 0.0;
    double build_ms = 0.0;
    double search_ms = 0.0;
    double merge_ms = 0.0;
    double final_ms = 0.0;
};

inline FECResult fec(const float* xyz, int N, double tolerance, int minSize, int maxN, int leafSize = 20) {
    using Clock = std::chrono::high_resolution_clock;

    FECResult result;
    auto t_total = Clock::now();

    if (!xyz || N < minSize) return result;

    PointCloudAdaptor adaptor{xyz, static_cast<std::size_t>(N)};

    auto t0 = Clock::now();
    KDTreeFEC tree(3, adaptor, nanoflann::KDTreeSingleIndexAdaptorParams(leafSize));
    tree.buildIndex();
    auto t1 = Clock::now();
    result.build_ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    std::vector<int> marked(N, 0);
    std::vector<nanoflann::ResultItem<std::uint32_t, float>> matches;
    if (maxN <= 0) maxN = N;
    matches.reserve(std::min(maxN, N));

    int tag_num = 1;
    float query[3];

    for (int i = 0; i < N; ++i) {
        if (marked[i] != 0) continue;

        query[0] = xyz[i * 3];
        query[1] = xyz[i * 3 + 1];
        query[2] = xyz[i * 3 + 2];
        matches.clear();

        auto ts0 = Clock::now();
        nanoflann::SearchParameters sparams;
        sparams.sorted = false;
        tree.radiusSearch(query, static_cast<float>(tolerance * tolerance), matches, sparams);
        if (static_cast<int>(matches.size()) > maxN) matches.resize(maxN);
        auto ts1 = Clock::now();
        result.search_ms += std::chrono::duration<double, std::milli>(ts1 - ts0).count();

        int min_tag = tag_num;
        for (const auto& m : matches) {
            int idx = static_cast<int>(m.first);
            if (marked[idx] > 0 && marked[idx] < min_tag) {
                min_tag = marked[idx];
            }
        }

        auto tm0 = Clock::now();
        for (const auto& m : matches) {
            int idx = static_cast<int>(m.first);
            int old_tag = marked[idx];
            if (old_tag > min_tag) {
                for (int k = 0; k < N; ++k) {
                    if (marked[k] == old_tag) marked[k] = min_tag;
                }
            }
            marked[idx] = min_tag;
        }
        auto tm1 = Clock::now();
        result.merge_ms += std::chrono::duration<double, std::milli>(tm1 - tm0).count();

        ++tag_num;
    }

    auto tf0 = Clock::now();

    struct IndexTag { int idx; int tag; };
    std::vector<IndexTag> items(N);
    for (int i = 0; i < N; ++i) {
        items[i].idx = i;
        items[i].tag = marked[i];
    }
    std::sort(items.begin(), items.end(),
              [](const IndexTag& a, const IndexTag& b) { return a.tag < b.tag; });

    int begin = 0;
    for (int i = 0; i < N; ++i) {
        if (items[i].tag != items[begin].tag) {
            int sz = i - begin;
            if (sz >= minSize && items[begin].tag > 0) {
                result.clusters.emplace_back();
                auto& c = result.clusters.back();
                c.reserve(sz);
                for (int j = begin; j < i; ++j) c.push_back(items[j].idx);
            }
            begin = i;
        }
    }
    int last_sz = N - begin;
    if (last_sz >= minSize && items[begin].tag > 0) {
        result.clusters.emplace_back();
        auto& c = result.clusters.back();
        c.reserve(last_sz);
        for (int j = begin; j < N; ++j) c.push_back(items[j].idx);
    }

    auto tf1 = Clock::now();
    result.final_ms = std::chrono::duration<double, std::milli>(tf1 - tf0).count();

    auto t_end = Clock::now();
    result.total_ms = std::chrono::duration<double, std::milli>(t_end - t_total).count();

    return result;
}

#endif
