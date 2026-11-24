package com.kltyton.superbwarfareboarding.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Aligns FH-77BW front door hinges and cuts the real cab doorway openings out of the solid side walls. */
final class Fh77CabModel {
    private static final double DOOR_MIN_Y = 41.9;
    private static final double DOOR_MAX_Y = 66.3;
    private static final double DOOR_MIN_Z = -80.7;
    private static final double DOOR_MAX_Z = -64.2;

    private Fh77CabModel() {}

    static boolean attach(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        boolean changed = false;

        JsonObject left = BoardingGeometry.findBone(bones, "door");
        if (left != null) {
            left.add("pivot", vec3(24.4875, 54.0, -82.58));
            changed = true;
        }
        JsonObject right = BoardingGeometry.findBone(bones, "door2");
        if (right != null) {
            right.add("pivot", vec3(-24.4875, 54.0, -82.58));
            changed = true;
        }

        JsonObject body = BoardingGeometry.findBone(bones, "bone");
        if (body != null) {
            var cubes = body.getAsJsonArray("cubes");
            if (cubes != null) {
                changed |= cutWall(cubes, 21.4875, 31.75416, -83.70192);
                changed |= cutWall(cubes, -22.9875, 31.75416, -83.70192);
            }
        }
        return changed;
    }

    private static boolean cutWall(JsonArray cubes, double ox, double oy, double oz) {
        int wallIdx = -1;
        for (int i = 0; i < cubes.size(); i++) {
            var cube = cubes.get(i).getAsJsonObject();
            if (BoardingGeometry.matches(cube.getAsJsonArray("origin"), ox, oy, oz)
                    && BoardingGeometry.matches(cube.getAsJsonArray("size"), 1.5, 36.0, 24.0)) {
                wallIdx = i;
                break;
            }
        }
        if (wallIdx < 0) return false;

        JsonObject wall = cubes.remove(wallIdx).getAsJsonObject();
        double y0 = oy;
        double y1 = oy + 36.0;
        double z0 = oz;
        double z1 = oz + 24.0;

        // Cut the wall into lower sill, upper lintel, front pillar, and rear pillar around the door aperture.
        cubes.add(sliceYZ(wall, y0, DOOR_MIN_Y, z0, z1));
        cubes.add(sliceYZ(wall, DOOR_MAX_Y, y1, z0, z1));
        cubes.add(sliceYZ(wall, DOOR_MIN_Y, DOOR_MAX_Y, z0, DOOR_MIN_Z));
        cubes.add(sliceYZ(wall, DOOR_MIN_Y, DOOR_MAX_Y, DOOR_MAX_Z, z1));
        return true;
    }

    private static JsonObject sliceYZ(JsonObject source, double ny0, double ny1, double nz0, double nz1) {
        var copy = source.deepCopy();
        var origOrigin = source.getAsJsonArray("origin");
        var origSize = source.getAsJsonArray("size");
        double y0 = origOrigin.get(1).getAsDouble();
        double yH = origSize.get(1).getAsDouble();
        double y1 = y0 + yH;
        double z0 = origOrigin.get(2).getAsDouble();
        double zL = origSize.get(2).getAsDouble();
        double z1 = z0 + zL;

        var origin = copy.getAsJsonArray("origin");
        origin.set(1, new JsonPrimitive(ny0));
        origin.set(2, new JsonPrimitive(nz0));
        var size = copy.getAsJsonArray("size");
        size.set(1, new JsonPrimitive(ny1 - ny0));
        size.set(2, new JsonPrimitive(nz1 - nz0));

        var uv = copy.getAsJsonObject("uv");
        if (uv != null) {
            double fyTop = (y1 - ny1) / yH;
            double fySpan = (ny1 - ny0) / yH;
            double fzFromNorth = (nz0 - z0) / zL;
            double fzFromSouth = (z1 - nz1) / zL;
            double fzSpan = (nz1 - nz0) / zL;

            scaleFace(uv.getAsJsonObject("east"), fzFromSouth, fzSpan, fyTop, fySpan);
            scaleFace(uv.getAsJsonObject("west"), fzFromNorth, fzSpan, fyTop, fySpan);
            scaleFace(uv.getAsJsonObject("north"), 0.0, 1.0, fyTop, fySpan);
            scaleFace(uv.getAsJsonObject("south"), 0.0, 1.0, fyTop, fySpan);
        }
        return copy;
    }

    private static void scaleFace(JsonObject face, double fuStart, double fuSpan, double fvStart, double fvSpan) {
        if (face == null) return;
        var u = face.getAsJsonArray("uv");
        var s = face.getAsJsonArray("uv_size");
        if (u == null || s == null) return;
        double u0 = u.get(0).getAsDouble();
        double v0 = u.get(1).getAsDouble();
        double uw = s.get(0).getAsDouble();
        double vh = s.get(1).getAsDouble();
        u.set(0, new JsonPrimitive(u0 + fuStart * uw));
        u.set(1, new JsonPrimitive(v0 + fvStart * vh));
        s.set(0, new JsonPrimitive(uw * fuSpan));
        s.set(1, new JsonPrimitive(vh * fvSpan));
    }

    private static JsonArray vec3(double x, double y, double z) {
        var arr = new JsonArray();
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }
}
