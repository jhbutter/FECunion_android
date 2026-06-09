#pragma once
#ifndef FECUNION_POINT_CLOUD_HPP
#define FECUNION_POINT_CLOUD_HPP

#include <vector>
#include <string>
#include <cstdio>

struct PointCloud {
    std::vector<float> xyz;
    int size() const { return static_cast<int>(xyz.size() / 3); }
    bool empty() const { return xyz.empty(); }
    const float* ptr() const { return xyz.data(); }
};

#endif
