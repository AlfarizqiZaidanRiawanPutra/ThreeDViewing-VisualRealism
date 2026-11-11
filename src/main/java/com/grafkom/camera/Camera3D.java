package com.grafkom.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera3D {

    private final Vector3f eye = new Vector3f();
    private final Vector3f target;
    private final Vector3f up;
    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    public Camera3D(Vector3f target, Vector3f up,
                    float radius, float azimuthDeg, float elevationDeg) {
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);
        updateOrbitalPosition(radius, azimuthDeg, elevationDeg);
    }

    /** Matriks LookAt (View Matrix) dihitung manual: baris = u, v, n. */
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();          // n = (E - At) / |E - At|

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();              // u = (Up x n) / |Up x n|

        Vector3f v = new Vector3f();
        n.cross(u, v);                           // v = n x u

        // Konstruktor JOML bersifat column-major
        return new Matrix4f(
            u.x, v.x, n.x, 0f,
            u.y, v.y, n.y, 0f,
            u.z, v.z, n.z, 0f,
            -u.dot(eye), -v.dot(eye), -n.dot(eye), 1f);
    }

    /** Koordinat sferis: kamera mengorbit di sekeliling target. */
    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        this.radius = Math.max(3f, Math.min(30f, radius));
        this.azimuthDeg = azimuthDeg;
        this.elevationDeg = Math.max(-89f, Math.min(89f, elevationDeg));

        float az = (float) Math.toRadians(this.azimuthDeg);
        float el = (float) Math.toRadians(this.elevationDeg);
        eye.x = target.x + this.radius * (float) (Math.cos(el) * Math.sin(az));
        eye.y = target.y + this.radius * (float) Math.sin(el);
        eye.z = target.z + this.radius * (float) (Math.cos(el) * Math.cos(az));
    }

    public void rotate(float dAzimuth, float dElevation) {
        updateOrbitalPosition(radius, azimuthDeg + dAzimuth, elevationDeg + dElevation);
    }

    public void zoom(float dRadius) {
        updateOrbitalPosition(radius + dRadius, azimuthDeg, elevationDeg);
    }

    public void setAngles(float azimuthDeg, float elevationDeg) {
        updateOrbitalPosition(radius, azimuthDeg, elevationDeg);
    }

    public void reset(float azimuthDeg, float elevationDeg) {
        updateOrbitalPosition(8f, azimuthDeg, elevationDeg);
    }

    public float getRadius() { return radius; }
    public Vector3f getEye() { return new Vector3f(eye); }
}