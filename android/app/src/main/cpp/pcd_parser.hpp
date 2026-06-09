#pragma once
#ifndef FECUNION_PCD_PARSER_HPP
#define FECUNION_PCD_PARSER_HPP

#include <vector>
#include <string>
#include <cstring>
#include <cstdio>
#include <cstdlib>

struct PCDHeader {
    int width = 0, height = 0, points = 0;
    int x_offset = -1, y_offset = -1, z_offset = -1;
    int point_step = 0;
    bool binary = false;
};

inline bool parse_pcd_header(FILE* f, PCDHeader& hdr) {
    char line[512];
    char fields[256] = "";
    char type[256] = "";
    char size[256] = "";
    char data_type[64] = "";

    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "FIELDS", 6) == 0) {
            char* p = line + 6;
            while (*p == ' ') ++p;
            char* nl = strchr(p, '\n'); if (nl) *nl = 0;
            strncpy(fields, p, sizeof(fields) - 1);
        } else if (strncmp(line, "SIZE", 4) == 0) {
            char* p = line + 4;
            while (*p == ' ') ++p;
            char* nl = strchr(p, '\n'); if (nl) *nl = 0;
            strncpy(size, p, sizeof(size) - 1);
        } else if (strncmp(line, "TYPE", 4) == 0) {
            char* p = line + 4;
            while (*p == ' ') ++p;
            char* nl = strchr(p, '\n'); if (nl) *nl = 0;
            strncpy(type, p, sizeof(type) - 1);
        } else if (strncmp(line, "WIDTH", 5) == 0) {
            hdr.width = atoi(line + 5);
        } else if (strncmp(line, "HEIGHT", 6) == 0) {
            hdr.height = atoi(line + 6);
        } else if (strncmp(line, "POINTS", 6) == 0) {
            hdr.points = atoi(line + 6);
        } else if (strncmp(line, "DATA", 4) == 0) {
            char* p = line + 4;
            while (*p == ' ') ++p;
            char* nl = strchr(p, '\n'); if (nl) *nl = 0;
            strncpy(data_type, p, sizeof(data_type) - 1);
            break;
        }
    }

    if (hdr.points == 0) hdr.points = hdr.width * hdr.height;

    char* flds[32];
    int nf = 0;
    char* tok = strtok(fields, " ");
    while (tok && nf < 32) { flds[nf++] = tok; tok = strtok(nullptr, " "); }

    char* szs[32];
    int ns = 0;
    char stok[256];
    strncpy(stok, size, sizeof(stok) - 1);
    tok = strtok(stok, " ");
    while (tok && ns < 32) { szs[ns++] = tok; tok = strtok(nullptr, " "); }

    int point_step = 0;
    for (int i = 0; i < nf; ++i) {
        int s = (i < ns) ? atoi(szs[i]) : 4;
        if (strcmp(flds[i], "x") == 0) hdr.x_offset = point_step;
        if (strcmp(flds[i], "y") == 0) hdr.y_offset = point_step;
        if (strcmp(flds[i], "z") == 0) hdr.z_offset = point_step;
        point_step += s;
    }
    hdr.point_step = point_step;

    hdr.binary = (strcmp(data_type, "binary") == 0);

    return hdr.x_offset >= 0 && hdr.y_offset >= 0 && hdr.z_offset >= 0;
}

inline bool parse_pcd(const char* filepath, std::vector<float>& xyz) {
    FILE* f = fopen(filepath, "rb");
    if (!f) return false;

    PCDHeader hdr;
    if (!parse_pcd_header(f, hdr)) { fclose(f); return false; }

    xyz.clear();
    xyz.reserve(hdr.points * 3);

    if (hdr.binary) {
        long data_start = ftell(f);
        std::vector<unsigned char> buf(hdr.point_step);
        for (int i = 0; i < hdr.points; ++i) {
            if (fread(buf.data(), hdr.point_step, 1, f) != 1) break;
            float x, y, z;
            memcpy(&x, buf.data() + hdr.x_offset, 4);
            memcpy(&y, buf.data() + hdr.y_offset, 4);
            memcpy(&z, buf.data() + hdr.z_offset, 4);
            xyz.push_back(x);
            xyz.push_back(y);
            xyz.push_back(z);
        }
    } else {
        char line[512];
        float x, y, z, dummy;
        int fields_in_row = hdr.point_step / 4;
        for (int i = 0; i < hdr.points; ++i) {
            if (!fgets(line, sizeof(line), f)) break;

            int off = 0;
            float vals[32];
            char* p = line;
            int nv = 0;
            while (nv < fields_in_row && nv < 32) {
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
