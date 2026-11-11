package com.grafkom.projection;

import com.grafkom.projection.PerspectiveProjection;
import static org.lwjgl.opengl.GL11.*;

public class ProjectionTaxonomyManager {

    public enum ProjectionType {
        ORTHOGRAPHIC      ("1: Ortografik",           0f,  0f,     false),
        ISOMETRIC         ("2: Isometrik",           45f, 35.264f, false),
        OBLIQUE_CAVALIER  ("3: Oblik Cavalier",       0f,  0f,     false),
        PERSPECTIVE_1POINT("4: Perspektif 1 Titik",   0f,  0f,     true),
        PERSPECTIVE_2POINT("5: Perspektif 2 Titik",  40f,  0f,     true),
        PERSPECTIVE_3POINT("6: Perspektif 3 Titik",  40f, 30f,     true);

        public final String label;
        public final float presetAzimuth;
        public final float presetElevation;
        public final boolean perspective;

        ProjectionType(String label, float az, float el, boolean perspective) {
            this.label = label;
            this.presetAzimuth = az;
            this.presetElevation = el;
            this.perspective = perspective;
        }
    }

    public static void applyProjection(ProjectionType type, float aspect,
                                       float orthoHalfHeight, PerspectiveProjection persp,
                                       float camDistance) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        double h = orthoHalfHeight;
        switch (type) {
            case ORTHOGRAPHIC, ISOMETRIC ->
                glOrtho(-h * aspect, h * aspect, -h, h, 0.1, 100.0);
            case OBLIQUE_CAVALIER -> {
                glOrtho(-h * aspect, h * aspect, -h, h, 0.1, 100.0);
                glMultMatrixf(obliqueShear(1.0f, 45f, camDistance));   // Cavalier: L = 1
            }
            default ->   // tiga mode perspektif
                glLoadMatrixf(persp.calculateProjectionMatrix().get(new float[16]));
        }
        glMatrixMode(GL_MODELVIEW);
    }

    /**
     * Shear oblik (column-major) dengan bidang acuan di jarak d dari kamera
     * (bidang yang melewati target tidak bergeser):
     *   x' = x - L*cos(a)*(z + d),  y' = y - L*sin(a)*(z + d)
     */
    private static float[] obliqueShear(float L, float angleDeg, float d) {
        float a = (float) Math.toRadians(angleDeg);
        float kx = L * (float) Math.cos(a);
        float ky = L * (float) Math.sin(a);
        return new float[] {
            1f,      0f,      0f, 0f,
            0f,      1f,      0f, 0f,
            -kx,     -ky,     1f, 0f,
            -kx * d, -ky * d, 0f, 1f
        };
    }
}