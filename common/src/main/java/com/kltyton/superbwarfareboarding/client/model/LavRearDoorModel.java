package com.kltyton.superbwarfareboarding.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Converts the stock LAV-25 / LAV-AD rear double doors into side-hinged solid armor doors. */
final class LavRearDoorModel {
    private static final double DOOR_HALF_WIDTH = 13.75;
    private static final double HINGE_Y = 15.0052;
    private static final double HINGE_Z = 53.5052;

    private LavRearDoorModel() {}

    static boolean attach(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        JsonObject low = null;
        JsonObject doorLeft = null;
        JsonObject doorRight = null;
        for (var element : bones) {
            var bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            if (name.equals("low")) low = bone;
            else if (name.equals("door_left")) doorLeft = bone;
            else if (name.equals("door_right")) doorRight = bone;
        }
        if (low == null || doorLeft == null || doorRight == null) return false;
        var lowCubes = low.getAsJsonArray("cubes");
        var leftCubes = doorLeft.getAsJsonArray("cubes");
        var rightCubes = doorRight.getAsJsonArray("cubes");
        if (lowCubes == null || leftCubes == null || rightCubes == null) return false;

        int bulkheadIndex = -1;
        for (int i = 0; i < lowCubes.size(); i++) {
            var cube = lowCubes.get(i).getAsJsonObject();
            if (BoardingGeometry.matches(cube.getAsJsonArray("origin"), -16.25, 15.0052, 50.5052)
                    && BoardingGeometry.matches(cube.getAsJsonArray("size"), 32.5, 23.51953, 3)
                    && BoardingGeometry.matches(cube.getAsJsonArray("rotation"), -15.5, 0, 0)) {
                bulkheadIndex = i;
                break;
            }
        }
        if (bulkheadIndex < 0) return false;

        // Keep the outer corner taillights attached to the fixed rear hull instead of swinging with the doors.
        moveCubeByOrigin(leftCubes, lowCubes, 18.07, 29.50555, 57.67144);
        moveCubeByOrigin(rightCubes, lowCubes, -20.32, 29.50555, 57.67144);

        // Split the sloped rear bulkhead into fixed outer hull jambs + solid left/right door armor backings.
        JsonObject bulkhead = lowCubes.remove(bulkheadIndex).getAsJsonObject();
        lowCubes.add(sliceX(bulkhead, -16.25, -DOOR_HALF_WIDTH, false, true, true));
        lowCubes.add(sliceX(bulkhead, DOOR_HALF_WIDTH, 16.25, true, false, true));
        leftCubes.add(sliceX(bulkhead, 0.0, DOOR_HALF_WIDTH, true, true, true));
        rightCubes.add(sliceX(bulkhead, -DOOR_HALF_WIDTH, 0.0, true, true, true));

        // Re-anchor both rear doors to their actual outer side hinges along the -15.5 deg rear slope.
        doorLeft.add("pivot", vec3(DOOR_HALF_WIDTH, HINGE_Y, HINGE_Z));
        doorRight.add("pivot", vec3(-DOOR_HALF_WIDTH, HINGE_Y, HINGE_Z));
        return true;
    }

    private static void moveCubeByOrigin(JsonArray from, JsonArray to, double ox, double oy, double oz) {
        for (int i = from.size() - 1; i >= 0; i--) {
            var cube = from.get(i).getAsJsonObject();
            if (BoardingGeometry.matches(cube.getAsJsonArray("origin"), ox, oy, oz)) {
                to.add(from.remove(i));
            }
        }
    }

    private static JsonObject sliceX(JsonObject source, double x0, double x1,
                                     boolean addEastFace, boolean addWestFace, boolean addSolidInterior) {
        var copy = source.deepCopy();
        var origOrigin = source.getAsJsonArray("origin");
        var origSize = source.getAsJsonArray("size");
        double totalMin = origOrigin.get(0).getAsDouble();
        double totalW = origSize.get(0).getAsDouble();
        double totalMax = totalMin + totalW;

        var origin = copy.getAsJsonArray("origin");
        origin.set(0, new com.google.gson.JsonPrimitive(x0));
        var size = copy.getAsJsonArray("size");
        size.set(0, new com.google.gson.JsonPrimitive(x1 - x0));

        var uv = copy.getAsJsonObject("uv");
        JsonObject south = uv.getAsJsonObject("south");
        double baseU = 95.0;
        double baseV = 76.0;
        if (south != null) {
            var su = south.getAsJsonArray("uv");
            var ss = south.getAsJsonArray("uv_size");
            double u0 = su.get(0).getAsDouble();
            double v0 = su.get(1).getAsDouble();
            double uw = ss.get(0).getAsDouble();
            baseU = u0 + 4.0;
            baseV = v0 + 4.0;
            double subU = u0 + ((totalMax - x1) / totalW) * uw;
            double subW = ((x1 - x0) / totalW) * uw;
            su.set(0, new com.google.gson.JsonPrimitive(subU));
            ss.set(0, new com.google.gson.JsonPrimitive(subW));
        }
        if (addEastFace && !uv.has("east")) uv.add("east", solidFace(baseU, baseV, 3, 18));
        if (addWestFace && !uv.has("west")) uv.add("west", solidFace(baseU, baseV, 3, 18));
        if (addSolidInterior) {
            uv.add("north", solidFace(baseU, baseV, 8, 16));
            if (!uv.has("up")) uv.add("up", solidFace(baseU, baseV, 8, 3));
            if (!uv.has("down")) uv.add("down", solidFace(baseU, baseV, 8, 3));
        }
        return copy;
    }

    private static JsonObject solidFace(double u, double v, double w, double h) {
        var face = new JsonObject();
        var uv = new JsonArray();
        uv.add(u);
        uv.add(v);
        var size = new JsonArray();
        size.add(w);
        size.add(h);
        face.add("uv", uv);
        face.add("uv_size", size);
        return face;
    }

    private static JsonArray vec3(double x, double y, double z) {
        var array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
