#pragma once
#ifndef FECUNION_ALGO_HPP
#define FECUNION_ALGO_HPP

#include <vector>
#include <algorithm>
#include <chrono>
#include <cstdint>
#include "nanoflann.hpp"

struct PointCloudAdaptor {
    const float* pts;
    std::size_t n;
    std::size_t kdtree_get_point_count() const { return n; }
    float kdtree_get_pt(std::size_t idx, int dim) const { return pts[idx * 3 + dim]; }
    template <class BBOX> bool kdtree_get_bbox(BBOX&) const { return false; }
};

struct LabelUnionFind {
    std::vector<int> parent;
    std::vector<int> min_label;

    explicit LabelUnionFind(int n) : parent(n), min_label(n) {
        for (int i = 0; i < n; ++i) {
            parent[i] = i;
            min_label[i] = i;
        }
    }

    int find(int x) {
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != x) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    void unite(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra != rb) {
            if (min_label[ra] < min_label[rb]) {
                parent[rb] = ra;
            } else {
                parent[ra] = rb;
                min_label[rb] = std::min(min_label[ra], min_label[rb]);
            }
        }
    }

    int canonical_label(int x) { return min_label[find(x)]; }
};

struct FECunionResult {
    std::vector<std::vector<int>> clusters;
    double total_ms = 0.0;
    double build_ms = 0.0;
    double search_ms = 0.0;
    double merge_ms = 0.0;
    double final_ms = 0.0;
};

using KDTree = nanoflann::KDTreeSingleIndexAdaptor<
    nanoflann::L2_Simple_Adaptor<float, PointCloudAdaptor>,
    PointCloudAdaptor, 3>;

inline FECunionResult fecunion(const float* xyz, int N, double tolerance, int minSize, int maxN, int leafSize = 20) {
    using Clock = std::chrono::high_resolution_clock;

    FECunionResult result;
    auto t_total = Clock::now();

    if (!xyz || N < minSize) return result;

    PointCloudAdaptor adaptor{xyz, static_cast<std::size_t>(N)};

    auto t0 = Clock::now();
    KDTree tree(3, adaptor, nanoflann::KDTreeSingleIndexAdaptorParams(leafSize));
    tree.buildIndex();
    auto t1 = Clock::now();
    result.build_ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    std::vector<int> marked(N, 0);
    std::vector<nanoflann::ResultItem<std::uint32_t, float>> matches;
    if (maxN <= 0) maxN = N;
    matches.reserve(std::min(maxN, N));

    int tag_num = 1;
    LabelUnionFind uf(N + 1);

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
        if (static_cast<int>(matches.size()) > maxN) {
            matches.resize(maxN);
        }
        auto ts1 = Clock::now();
        result.search_ms += std::chrono::duration<double, std::milli>(ts1 - ts0).count();

        int min_tag = tag_num;
        for (const auto& m : matches) {
            int idx = static_cast<int>(m.first);
            if (marked[idx] > 0) {
                int ct = uf.canonical_label(marked[idx]);
                if (ct < min_tag) min_tag = ct;
            }
        }

        if (min_tag == tag_num) ++tag_num;

        auto tm0 = Clock::now();
        for (const auto& m : matches) {
            int idx = static_cast<int>(m.first);
            if (marked[idx] > 0) {
                uf.unite(marked[idx], min_tag);
            } else {
                marked[idx] = min_tag;
            }
        }
        auto tm1 = Clock::now();
        result.merge_ms += std::chrono::duration<double, std::milli>(tm1 - tm0).count();
    }

    auto tf0 = Clock::now();

    std::vector<int> counts(tag_num, 0);
    for (int i = 0; i < N; ++i) {
        if (marked[i] > 0) {
            marked[i] = uf.canonical_label(marked[i]);
            ++counts[marked[i]];
        }
    }

    std::vector<int> label_to_id(tag_num, -1);
    for (int label = 1; label < tag_num; ++label) {
        if (counts[label] >= minSize) {
            label_to_id[label] = static_cast<int>(result.clusters.size());
            result.clusters.emplace_back();
            result.clusters.back().reserve(counts[label]);
        }
    }

    for (int i = 0; i < N; ++i) {
        int label = marked[i];
        if (label > 0) {
            int cid = label_to_id[label];
            if (cid != -1) {
                result.clusters[cid].push_back(i);
            }
        }
    }

    auto tf1 = Clock::now();
    result.final_ms = std::chrono::duration<double, std::milli>(tf1 - tf0).count();

    auto t_end = Clock::now();
    result.total_ms = std::chrono::duration<double, std::milli>(t_end - t_total).count();

    return result;
}

#endif
