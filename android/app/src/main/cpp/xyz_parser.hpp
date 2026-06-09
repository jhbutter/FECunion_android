#pragma once
#ifndef FECUNION_XYZ_PARSER_HPP
#define FECUNION_XYZ_PARSER_HPP

#include <vector>
#include <string>
#include <cstdio>
#include <cstdlib>

inline bool parse_xyz(const char* filepath, std::vector<float>& xyz) {
    FILE* f = fopen(filepath, "r");
    if (!f) return false;

    xyz.clear();
    char line[512];
    float x, y, z;
    int n = 0;

    while (fgets(line, sizeof(line), f)) {
        if (sscanf(line, "%f %f %f", &x, &y, &z) == 3) {
            xyz.push_back(x);
            xyz.push_back(y);
            xyz.push_back(z);
            ++n;
        }
    }

    fclose(f);
    return n > 0;
}

#endif
