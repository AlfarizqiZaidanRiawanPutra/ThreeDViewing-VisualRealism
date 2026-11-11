package com.grafkom.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

    private final int programID;

    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexPath), vertexPath);
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentPath), fragmentPath);

        programID = glCreateProgram();
        glAttachShader(programID, vs);
        glAttachShader(programID, fs);
        glLinkProgram(programID);
        if (glGetProgrami(programID, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Link shader gagal: " + glGetProgramInfoLog(programID));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String source, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Compile shader gagal (" + name + "): "
                    + glGetShaderInfoLog(id));
        }
        return id;
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("File shader tidak ditemukan: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void use() { glUseProgram(programID); }

    private int loc(String name) { return glGetUniformLocation(programID, name); }

    public void setInt(String name, int v)       { glUniform1i(loc(name), v); }
    public void setBool(String name, boolean v)  { glUniform1i(loc(name), v ? 1 : 0); }
    public void setVec3(String name, Vector3f v) { glUniform3f(loc(name), v.x, v.y, v.z); }
    public void setMat4(String name, Matrix4f m) {
        glUniformMatrix4fv(loc(name), false, m.get(new float[16]));
    }
}