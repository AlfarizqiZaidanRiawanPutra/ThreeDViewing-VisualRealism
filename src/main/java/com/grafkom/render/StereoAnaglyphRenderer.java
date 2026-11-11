package com.grafkom.render;

import com.grafkom.projection.PerspectiveProjection;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class StereoAnaglyphRenderer {

    private float ipd = 0.3f;            // Interpupillary Distance (satuan dunia)
    private float focalLength = 8.0f;    // jarak ke bidang layar (zero parallax)

    public void renderAnaglyph(Matrix4f view, PerspectiveProjection p, Runnable scene) {
        float[] viewArr = view.get(new float[16]);

        // Bersihkan SEMUA kanal dulu (mask penuh), baru atur mask per mata
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PASS 1: mata kiri -> kanal merah
        glColorMask(true, false, false, true);
        renderEye(+1, viewArr, p, scene);

        // PASS 2: mata kanan -> kanal cyan (hijau + biru); hanya depth yang dibersihkan
        glClear(GL_DEPTH_BUFFER_BIT);
        glColorMask(false, true, true, true);
        renderEye(-1, viewArr, p, scene);

        glColorMask(true, true, true, true);   // kembalikan ke normal
    }

    /** side = +1 mata kiri, -1 mata kanan. */
    private void renderEye(int side, float[] view, PerspectiveProjection p, Runnable scene) {
        float near = p.getZNear();
        float top = near * (float) Math.tan(Math.toRadians(p.getFovDegree()) / 2.0);
        float right = top * p.getAspectRatio();
        float shift = (ipd / 2f) * near / focalLength;   // geseran frustum asimetris

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glFrustum(-right + side * shift, right + side * shift, -top, top, near, p.getZFar());

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslatef(side * ipd / 2f, 0f, 0f);   // geser kamera ke kiri/kanan
        glMultMatrixf(view);                     // lalu terapkan kamera orbital
        scene.run();
    }

    public float getIpd() { return ipd; }
    public void setIpd(float ipd) { this.ipd = Math.max(0f, Math.min(1.5f, ipd)); }
    public void setFocalLength(float focalLength) { this.focalLength = focalLength; }
}