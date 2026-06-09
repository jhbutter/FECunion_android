#pragma once
#ifndef FECUNION_PLY_PARSER_HPP
#define FECUNION_PLY_PARSER_HPP

#include <vector>
#include <string>
#include <cstring>
#include <cstdio>
#include <cstdlib>

struct PLYHeader {
    int vertex_count = 0;
    int x_offset = -1, y_offset = -1, z_offset = -1;
    int vertex_byte_size = 0;
    bool binary = false;
};

inline int ply_type_size(const char* t) {
    if (strcmp(t, "char") == 0 || strcmp(t, "int8") == 0) return 1;
    if (strcmp(t, "uchar") == 0 || strcmp(t, "uint8") == 0) return 1;
    if (strcmp(t, "short") == 0 || strcmp(t, "int16") == 0) return 2;
    if (strcmp(t, "ushort") == 0 || strcmp(t, "uint16") == 0) return 2;
    if (strcmp(t, "int") == 0 || strcmp(t, "int32") == 0) return 4;
    if (strcmp(t, "uint") == 0 || strcmp(t, "uint32") == 0) return 4;
    if (strcmp(t, "float") == 0 || strcmp(t, "float32") == 0) return 4;
    if (strcmp(t, "double") == 0 || strcmp(t, "float64") == 0) return 8;
    return 4;
}

inline bool parse_ply_header(FILE* f, PLYHeader& hdr) {
    char line[512];
    bool in_vertex = false;

    if (!fgets(line, sizeof(line), f)) return false;
    if (strncmp(line, "ply", 3) != 0) return false;

    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "format", 6) == 0) {
            hdr.binary = (strstr(line, "binary_little_endian") != nullptr);
        } else if (strncmp(line, "element vertex", 14) == 0) {
            hdr.vertex_count = atoi(line + 14);
            in_vertex = true;
        } else if (strncmp(line, "element ", 8) == 0) {
            in_vertex = false;
        } else if (in_vertex && strncmp(line, "property ", 9) == 0) {
            char* p = line + 9;
            while (*p == ' ' || *p == '\t') ++p;
            char type[32];
            char name[32];
            if (sscanf(p, "%31s %31s", type, name) == 2) {
                int sz = ply_type_size(type);
                int off = hdr.vertex_byte_size;
                if (strcmp(name, "x") == 0) hdr.x_offset = off;
                if (strcmp(name, "y") == 0) hdr.y_offset = off;
                if (strcmp(name, "z") == 0) hdr.z_offset = off;
                hdr.vertex_byte_size += sz;
            }
        } else if (strncmp(line, "end_header", 10) == 0) {
            break;
        }
    }

    return hdr.vertex_count > 0 && hdr.x_offset >= 0 && hdr.y_offset >= 0 && hdr.z_offset >= 0;
}

inline bool parse_ply(const char* filepath, std::vector<float>& xyz) {
    FILE* f = fopen(filepath, "rb");
    if (!f) return false;

    PLYHeader hdr;
    if (!parse_ply_header(f, hdr)) { fclose(f); return false; }

    xyz.clear();
    xyz.reserve(hdr.vertex_count * 3);

    if (hdr.binary) {
        std::vector<unsigned char> buf(hdr.vertex_byte_size);
        for (int i = 0; i < hdr.vertex_count; ++i) {
            if (fread(buf.data(), hdr.vertex_byte_size, 1, f) != 1) break;
            float x, y, z;
            memcpy(&x, buf.data() + hdr.x_offset, 4);
            memcpy(&y, buf.data() + hdr.y_offset, 4);
            memcpy(&z, buf.data() + hdr.z_offset, 4);
            xyz.push_back(x);
            xyz.push_back(y);
            xyz.push_back(z);
        }
    } else {
        char line[1024];
        int field_count = hdr.vertex_byte_size / 4;
        for (int i = 0; i < hdr.vertex_count; ++i) {
            if (!fgets(line, sizeof(line), f)) break;

            float vals[32];
            char* p = line;
            int nv = 0;
            while (nv < field_count && nv < 32) {
                while (*p == ' ' || *p == '\t') ++p;
                if (*p == 0 || *p == '\n') break;
                vals[nv++] = strtof(p, &p);
            }

            if (hdr.x_offset / 4 < nv && hdr.y_offset / 4 < nv && hdr.z_offset / 4 < nv) {
                xyz.push_back(vals[hdr.x_offset / 4]);
                xyz.push_back(vals[hdr.y_offset / 4]);
                xyz.push_back(vals[hdr.z_offset / 4]);
            }
        }
    }

    fclose(f);
    return static_cast<int>(xyz.size()) / 3 > 0;
}

#endif
