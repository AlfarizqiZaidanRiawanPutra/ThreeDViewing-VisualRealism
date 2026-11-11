package com.grafkom.render;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

public class Mesh3D {

    private final int vaoFlat, vboFlat, vaoSmooth, vboSmooth;
    private final int vertexCount;

    /** positions: 3 float/vertex, uvs: 2 float/vertex, indices: segitiga CCW. */
    public Mesh3D(float[] positions, float[] uvs, int[] indices) {
        int triCount = indices.length / 3;
        vertexCount = triCount * 3;

        // 1) Normal per-face
        Vector3f[] faceNormal = new Vector3f[triCount];
        for (int t = 0; t < triCount; t++) {
            Vector3f p0 = pos(positions, indices[3 * t]);
            Vector3f p1 = pos(positions, indices[3 * t + 1]);
            Vector3f p2 = pos(positions, indices[3 * t + 2]);
            Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0));
            if (n.lengthSquared() > 1e-12f) n.normalize(); else n.zero();
            faceNormal[t] = n;
        }

        // 2) Normal per-vertex: jumlahkan normal semua face yang berbagi posisi, lalu normalisasi
        Map<String, Vector3f> sum = new HashMap<>();
        for (int t = 0; t < triCount; t++) {
            for (int k = 0; k < 3; k++) {
                String key = key(pos(positions, indices[3 * t + k]));
                sum.computeIfAbsent(key, x -> new Vector3f()).add(faceNormal[t]);
            }
        }
        for (Vector3f v : sum.values()) {
            if (v.lengthSquared() > 1e-12f) v.normalize();
        }

        // 3) Susun data interleaved: pos(3) normal(3) uv(2), tanpa index (non-indexed)
        float[] flat = new float[vertexCount * 8];
        float[] smooth = new float[vertexCount * 8];
        int o = 0;
        for (int t = 0; t < triCount; t++) {
            for (int k = 0; k < 3; k++) {
                int idx = indices[3 * t + k];
                Vector3f p = pos(positions, idx);
                Vector3f nf = faceNormal[t];
                Vector3f ns = sum.get(key(p));
                put(flat, o, p, nf, uvs[2 * idx], uvs[2 * idx + 1]);
                put(smooth, o, p, ns, uvs[2 * idx], uvs[2 * idx + 1]);
                o += 8;
            }
        }

        vaoFlat = glGenVertexArrays();
        vboFlat = glGenBuffers();
        upload(vaoFlat, vboFlat, flat);

        vaoSmooth = glGenVertexArrays();
        vboSmooth = glGenBuffers();
        upload(vaoSmooth, vboSmooth, smooth);
    }

    private static Vector3f pos(float[] a, int i) {
        return new Vector3f(a[3 * i], a[3 * i + 1], a[3 * i + 2]);
    }

    private static String key(Vector3f p) {
        return Math.round(p.x * 10000) + "," + Math.round(p.y * 10000) + "," + Math.round(p.z * 10000);
    }

    private static void put(float[] d, int o, Vector3f p, Vector3f n, float u, float v) {
        d[o] = p.x;     d[o + 1] = p.y; d[o + 2] = p.z;
        d[o + 3] = n.x; d[o + 4] = n.y; d[o + 5] = n.z;
        d[o + 6] = u;   d[o + 7] = v;
    }

    private static void upload(int vao, int vbo, float[] data) {
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    /** Sphere: sumbu Y ke atas, V=1 di kutub utara (cocok dengan flip vertikal stb_image). */
    public static Mesh3D createSphere(float radius, int stacks, int sectors) {
        int vCount = (stacks + 1) * (sectors + 1);
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];
        int p = 0, q = 0;
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI / 2 - i * Math.PI / stacks;
            for (int j = 0; j <= sectors; j++) {
                double theta = j * 2 * Math.PI / sectors;
                pos[p++] = (float) (radius * Math.cos(phi) * Math.cos(theta));
                pos[p++] = (float) (radius * Math.sin(phi));
                pos[p++] = (float) (radius * Math.cos(phi) * Math.sin(theta));
                uv[q++] = (float) j / sectors;
                uv[q++] = 1f - (float) i / stacks;
            }
        }
        int[] idx = new int[stacks * sectors * 6];
        int n = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                int k1 = i * (sectors + 1) + j;
                int k2 = k1 + sectors + 1;
                if (i != 0) {                       // lewati segitiga degenerate di kutub
                    idx[n++] = k1; idx[n++] = k1 + 1; idx[n++] = k2;
                }
                if (i != stacks - 1) {
                    idx[n++] = k1 + 1; idx[n++] = k2 + 1; idx[n++] = k2;
                }
            }
        }
        int[] used = new int[n];
        System.arraycopy(idx, 0, used, 0, n);
        return new Mesh3D(pos, uv, used);
    }

    /** Lantai di y = 0, normal ke atas (+Y). */
    public static Mesh3D createPlane(float half, float uvRepeat) {
        float[] pos = { -half, 0, -half,  -half, 0, half,  half, 0, half,  half, 0, -half };
        float[] uv  = { 0, 0,  0, uvRepeat,  uvRepeat, uvRepeat,  uvRepeat, 0 };
        int[] idx   = { 0, 1, 2,  0, 2, 3 };
        return new Mesh3D(pos, uv, idx);
    }
}