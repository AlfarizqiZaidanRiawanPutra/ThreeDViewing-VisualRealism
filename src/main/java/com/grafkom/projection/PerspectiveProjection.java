package com.grafkom.projection;

import org.joml.Matrix4f;

public class PerspectiveProjection {

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = fovDegree;
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    public Matrix4f calculateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(f / aspectRatio);
        proj.m11(f);
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2.0f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1.0f);
        proj.m33(0.0f);
        return proj;
    }

    public float getFovDegree()   { return fovDegree; }
    public float getAspectRatio() { return aspectRatio; }
    public float getZNear()       { return zNear; }
    public float getZFar()        { return zFar; }

    public void setFovDegree(float fovDegree) {
        this.fovDegree = Math.max(10f, Math.min(120f, fovDegree));
    }
    public void setAspectRatio(float aspectRatio) { this.aspectRatio = aspectRatio; }
}