package com.grafkom.viewing3d;

import com.grafkom.camera.Camera3D;
import com.grafkom.projection.PerspectiveProjection;
import com.grafkom.render.HSRManager;
import com.grafkom.render.Mesh3D;
import com.grafkom.render.ShaderProgram;
import com.grafkom.render.ShadowMapFBO;
import com.grafkom.render.Texture2D;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class ThreeDViewing {

    private long window;
    private Camera3D camera;
    private PerspectiveProjection projection;

    private ShaderProgram phongShader, shadowShader;
    private ShadowMapFBO shadowFBO;
    private Mesh3D sphere, ground;
    private Texture2D texture;

    private final Vector3f lightPos = new Vector3f(4f, 8f, 3f);
    private final Vector3f lightColor = new Vector3f(1f, 1f, 1f);

    // Status fitur (tombol dari modul: B, F, Z, C; T untuk filter tekstur)
    private boolean blinnPhong = true;
    private boolean smooth = true;
    private boolean depthTest = true;
    private boolean cullFace = true;
    private boolean nearest = false;

    private boolean dragging = false;
    private double lastX, lastY;

    private int frames = 0;
    private double fps = 0, fpsTimer = 0;

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }

    public void run() {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(960, 600, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);          // VSync mati agar FPS antar-mode bisa dibandingkan
        GL.createCapabilities();

        camera = new Camera3D(new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), 10f, 30f, 25f);
        projection = new PerspectiveProjection(45f, 960f / 600f, 0.1f, 100f);

        phongShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        shadowShader = new ShaderProgram("/shaders/shadow.vert", "/shaders/shadow.frag");
        shadowFBO = new ShadowMapFBO();

        sphere = Mesh3D.createSphere(1.5f, 16, 24);     // kasar agar beda Flat vs Smooth terlihat
        ground = Mesh3D.createPlane(8f, 1f);
        texture = new Texture2D("src/main/resources/textures/texture.png");

        glfwSetKeyCallback(window, (w, key, sc, action, mods) -> onKey(key, action));
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });
        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                camera.rotate(-(float) (x - lastX) * 0.4f, (float) (y - lastY) * 0.4f);
            }
            lastX = x;
            lastY = y;
        });
        glfwSetScrollCallback(window, (w, dx, dy) -> camera.zoom(-(float) dy * 0.5f));

        glClearColor(0.10f, 0.10f, 0.14f, 1f);
        glfwShowWindow(window);
        fpsTimer = glfwGetTime();
        updateTitle();
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            render();
            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++;
            double now = glfwGetTime();
            if (now - fpsTimer >= 1.0) {
                fps = frames / (now - fpsTimer);
                frames = 0;
                fpsTimer = now;
                updateTitle();
            }
        }
    }

    private void render() {
        int[] fw = new int[1], fh = new int[1];
        glfwGetFramebufferSize(window, fw, fh);
        if (fw[0] == 0 || fh[0] == 0) return;        // jendela diminimize
        projection.setAspectRatio((float) fw[0] / fh[0]);

        // Matriks cahaya: proyeksi ortografik (cahaya directional) menuju pusat adegan
        Matrix4f lightProj = new Matrix4f().ortho(-12f, 12f, -12f, 12f, 1f, 25f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        // ===== PASS 1: render kedalaman dari sudut pandang cahaya =====
        shadowFBO.bindForWriting();
        glEnable(GL_DEPTH_TEST);                     // wajib aktif walau tombol Z mematikannya
        shadowShader.use();
        shadowShader.setMat4("lightSpaceMatrix", lightSpace);
        drawScene(shadowShader, false);
        shadowFBO.unbind(fw[0], fh[0]);

        // ===== PASS 2: render adegan dari kamera dengan shader Phong =====
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(cullFace);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        phongShader.use();
        phongShader.setMat4("view", camera.calculateLookAtMatrix());
        phongShader.setMat4("projection", projection.calculateProjectionMatrix());
        phongShader.setMat4("lightSpaceMatrix", lightSpace);
        phongShader.setVec3("lightPos", lightPos);
        phongShader.setVec3("lightColor", lightColor);
        phongShader.setVec3("viewPos", camera.getEye());
        phongShader.setBool("useBlinnPhong", blinnPhong);
        phongShader.setInt("textureSampler", 0);
        phongShader.setInt("shadowMap", 1);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowFBO.getDepthTextureID());

        drawScene(phongShader, true);
    }

    /** Bola digambar lebih dulu, lantai sesudahnya, agar efek Z-Buffer OFF terlihat jelas. */
    private void drawScene(ShaderProgram shader, boolean lit) {
        shader.setMat4("model", new Matrix4f().translation(0f, 2f, 0f));
        if (lit) {
            shader.setBool("useTexture", true);
            texture.bind(0);
        }
        sphere.draw(smooth);

        shader.setMat4("model", new Matrix4f());
        if (lit) {
            shader.setBool("useTexture", false);
            shader.setVec3("objectColor", new Vector3f(0.75f, 0.75f, 0.78f));
        }
        ground.draw(smooth);
    }

    private void onKey(int key, int action) {
        if (action == GLFW_RELEASE) return;
        boolean press = (action == GLFW_PRESS);

        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
            case GLFW_KEY_B -> { if (press) blinnPhong = !blinnPhong; }
            case GLFW_KEY_F -> { if (press) smooth = !smooth; }
            case GLFW_KEY_Z -> { if (press) depthTest = !depthTest; }
            case GLFW_KEY_C -> { if (press) cullFace = !cullFace; }
            case GLFW_KEY_T -> {
                if (press) {
                    nearest = !nearest;
                    texture.setNearest(nearest);
                }
            }
            case GLFW_KEY_R -> {
                if (press) {
                    camera.updateOrbitalPosition(10f, 30f, 25f);
                    projection.setFovDegree(45f);
                }
            }
            case GLFW_KEY_LEFT  -> camera.rotate(-3f, 0f);
            case GLFW_KEY_RIGHT -> camera.rotate(3f, 0f);
            case GLFW_KEY_UP    -> camera.rotate(0f, 3f);
            case GLFW_KEY_DOWN  -> camera.rotate(0f, -3f);
            case GLFW_KEY_Q -> projection.setFovDegree(projection.getFovDegree() - 2f);
            case GLFW_KEY_E -> projection.setFovDegree(projection.getFovDegree() + 2f);
            default -> { }
        }
        updateTitle();
    }

    private void updateTitle() {
        glfwSetWindowTitle(window, String.format(
            "Visual Realism | %s | %s | Depth: %s | Cull: %s | Tekstur: %s | FPS %.0f",
            blinnPhong ? "Blinn-Phong" : "Phong",
            smooth ? "Smooth" : "Flat",
            depthTest ? "ON" : "OFF",
            cullFace ? "ON" : "OFF",
            nearest ? "Nearest" : "Bilinear+Mipmap",
            fps));
    }
}