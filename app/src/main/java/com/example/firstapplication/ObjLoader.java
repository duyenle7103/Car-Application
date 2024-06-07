package com.example.firstapplication;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

public final class ObjLoader {
    public final int numVertices;

    public final float[] positions;
    public final float[] normals;
    public final float[] textureCoordinates;

    private Vector<Group> groups = new Vector<>();
    private Group currentGroup = new Group("default");

    private static class Group {
        String name;
        String material;
        Vector<String> faces;

        Group(String name) {
            this.name = name;
            this.faces = new Vector<>();
        }
    }

    public ObjLoader(Context context, String file) {
        Vector<Float> vertices = new Vector<>();
        Vector<Float> normals = new Vector<>();
        Vector<Float> textures = new Vector<>();

        BufferedReader reader = null;
        try {
            InputStreamReader in = new InputStreamReader(context.getAssets().open(file));
            reader = new BufferedReader(in);

            // Read file until EOF
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "v":
                        // Vertices
                        vertices.add(Float.valueOf(parts[1]));
                        vertices.add(Float.valueOf(parts[2]));
                        vertices.add(Float.valueOf(parts[3]));
                        break;
                    case "vn":
                        // Normals
                        normals.add(Float.valueOf(parts[1]));
                        normals.add(Float.valueOf(parts[2]));
                        normals.add(Float.valueOf(parts[3]));
                        break;
                    case "vt":
                        // Textures
                        textures.add(Float.valueOf(parts[1]));
                        textures.add(Float.valueOf(parts[2]));
                        textures.add(Float.valueOf(parts[3]));
                        break;
                    case "g":
                        // New group
                        currentGroup = new Group(parts[1]);
                        groups.add(currentGroup);
                        break;
                    case "usemtl":
                        // Use material
                        currentGroup.material = parts[1];
                        break;
                    case "f":
                        if (parts.length == 4) {
                            // Faces: vertex/vertex/vertex
                            currentGroup.faces.add(parts[1]);
                            currentGroup.faces.add(parts[2]);
                            currentGroup.faces.add(parts[3]);
                        } else if (parts.length == 5) {
                            // Faces: vertex/vertex/vertex/vertex
                            currentGroup.faces.add(parts[1]);
                            currentGroup.faces.add(parts[2]);
                            currentGroup.faces.add(parts[3]);

                            currentGroup.faces.add(parts[1]);
                            currentGroup.faces.add(parts[3]);
                            currentGroup.faces.add(parts[4]);
                        } else if (parts.length == 6) {
                            // Faces: vertex/vertex/vertex/vertex/vertex/vertex
                            currentGroup.faces.add(parts[1]);
                            currentGroup.faces.add(parts[2]);
                            currentGroup.faces.add(parts[3]);

                            currentGroup.faces.add(parts[1]);
                            currentGroup.faces.add(parts[3]);
                            currentGroup.faces.add(parts[4]);

                            currentGroup.faces.add(parts[1]);
                            currentGroup.faces.add(parts[4]);
                            currentGroup.faces.add(parts[5]);
                        }
                        break;
                }
            }
            if (!groups.contains(currentGroup)) {
                groups.add(currentGroup);
            }
        } catch (IOException e) {
            // Cannot load or read file
            Log.e("ObjLoader", "Error reading OBJ file: " + file, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Log the exception
                    Log.e("ObjLoader", "Error closing BufferedReader", e);
                }
            }
        }

        // Calculate the total number of vertices
        int totalVertices = 0;
        for (Group group : groups) {
            totalVertices += group.faces.size();
        }
        numVertices = totalVertices;

        // Initialize arrays for positions, normals, and texture coordinates
        this.positions = new float[numVertices * 3];
        this.normals = new float[numVertices * 3];
        this.textureCoordinates = new float[numVertices * 3];

        int positionIndex = 0;
        int normalIndex = 0;
        int textureIndex = 0;

//        Log.d("ObjLoader", "groups: " + groups);
//        Log.d("ObjLoader", "vertices: " + vertices.size());
//        Log.d("ObjLoader", "normals: " + normals.size());
//        Log.d("ObjLoader", "textures: " + textures.size());

        for (Group group : groups) {
            for (String face : group.faces) {
                String[] parts = face.split("/");

                if (parts.length == 3 && !parts[0].isEmpty() && !parts[1].isEmpty() && !parts[2].isEmpty()) {
                    int index = 3 * (Short.valueOf(parts[0]) - 1);
                    this.positions[positionIndex++] = vertices.get(index++);
                    this.positions[positionIndex++] = vertices.get(index++);
                    this.positions[positionIndex++] = vertices.get(index);

                    index = 3 * (Short.valueOf(parts[1]) - 1);
                    this.textureCoordinates[normalIndex++] = textures.get(index++);
                    this.textureCoordinates[normalIndex++] = textures.get(index++);
                    this.textureCoordinates[normalIndex++] = textures.get(index);

                    index = 3 * (Short.valueOf(parts[2]) - 1);
                    this.normals[textureIndex++] = normals.get(index++);
                    this.normals[textureIndex++] = normals.get(index++);
                    this.normals[textureIndex++] = normals.get(index);
                }
            }
        }
    }
}
