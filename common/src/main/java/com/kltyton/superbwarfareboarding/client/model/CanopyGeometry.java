package com.kltyton.superbwarfareboarding.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.superbwarfareboarding.boarding.VehicleMotion;

/** Splits stock aircraft canopies and helicopter doors with their real glass and frames intact. */
final class CanopyGeometry {
    private static final int[] J16_FRAME_CUBES = {164, 163, 162, 161, 154, 153, 152, 8, 7, 6, 5, 4, 3, 2, 1};
    private static final boolean[] J16_MOVING_GLASS_POLYS = new boolean[92];
    private static final int[] JU87_PILOT_HOOD_CUBES = {13, 12, 2, 1, 0};

    static {
        // J-16 body.poly_mesh polygons #0..#19 are canopy glass; #0,#5,#10,#15 are the fixed front windscreen.
        for (int idx : new int[]{1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14, 16, 17, 18, 19}) {
            J16_MOVING_GLASS_POLYS[idx] = true;
        }
    }

    private CanopyGeometry() {}

    /**
     * Separates the J-16 rear-hinged clamshell canopy metal frame AND curved poly_mesh bubble glass
     * together onto one bone while leaving the fixed front windscreen (#0,#5,#10,#15) on the fuselage.
     */
    static boolean attachJ16Canopy(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null || BoardingGeometry.findBone(bones, VehicleMotion.J16_CANOPY_BONE) != null) return false;
        JsonObject body = BoardingGeometry.findBone(bones, "body");
        if (body == null) return false;
        var bodyCubes = body.getAsJsonArray("cubes");
        if (bodyCubes == null || bodyCubes.size() < 165) return false;

        var canopyCubes = new JsonArray();
        for (int idx : J16_FRAME_CUBES) {
            canopyCubes.add(bodyCubes.remove(idx));
        }

        var canopy = new JsonObject();
        canopy.addProperty("name", VehicleMotion.J16_CANOPY_BONE);
        canopy.addProperty("parent", "body");
        canopy.add("pivot", vec3(0.0, 57.5, -48.2));
        canopy.add("cubes", canopyCubes);

        if (body.has("poly_mesh")) {
            var bodyMesh = body.getAsJsonObject("poly_mesh");
            var polys = bodyMesh.getAsJsonArray("polys");
            if (polys != null && polys.size() == J16_MOVING_GLASS_POLYS.length) {
                var fixedPolys = new JsonArray();
                var movingPolys = new JsonArray();
                for (int i = 0; i < polys.size(); i++) {
                    if (J16_MOVING_GLASS_POLYS[i]) movingPolys.add(polys.get(i));
                    else fixedPolys.add(polys.get(i));
                }
                bodyMesh.add("polys", fixedPolys);

                var canopyMesh = new JsonObject();
                if (bodyMesh.has("normalized_uvs")) canopyMesh.add("normalized_uvs", bodyMesh.get("normalized_uvs"));
                canopyMesh.add("positions", bodyMesh.get("positions").deepCopy());
                if (bodyMesh.has("normals")) canopyMesh.add("normals", bodyMesh.get("normals").deepCopy());
                canopyMesh.add("uvs", bodyMesh.get("uvs").deepCopy());
                canopyMesh.add("polys", movingPolys);
                canopy.add("poly_mesh", canopyMesh);
            }
        }
        bones.add(canopy);
        return true;
    }

    /**
     * Separates the A-10A rear bubble canopy (both rear glass glints AND the rear half of a10.poly_mesh
     * canopy frame/rim behind the fixed front windscreen arch bone61 at Z=-54.5) onto a10_canopy.
     */
    static boolean attachA10RearCanopy(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null || BoardingGeometry.findBone(bones, VehicleMotion.A10_CANOPY_BONE) != null) return false;
        JsonObject glass = BoardingGeometry.findBone(bones, "glass");
        JsonObject a10 = BoardingGeometry.findBone(bones, "a10");
        if (glass == null || a10 == null) return false;
        var glassCubes = glass.getAsJsonArray("cubes");
        if (glassCubes == null) return false;

        var canopyCubes = new JsonArray();
        for (int i = glassCubes.size() - 1; i >= 0; i--) {
            var cube = glassCubes.get(i).getAsJsonObject();
            var origin = cube.getAsJsonArray("origin");
            if (origin != null && origin.size() == 3 && origin.get(2).getAsDouble() >= -54.0) {
                canopyCubes.add(glassCubes.remove(i));
            }
        }

        var canopy = new JsonObject();
        canopy.addProperty("name", VehicleMotion.A10_CANOPY_BONE);
        canopy.addProperty("parent", "body");
        canopy.add("pivot", vec3(0.0, 44.5, -37.4));
        if (!canopyCubes.isEmpty()) canopy.add("cubes", canopyCubes);

        if (a10.has("poly_mesh")) {
            var mesh = a10.getAsJsonObject("poly_mesh");
            var positions = mesh.getAsJsonArray("positions");
            var uvs = mesh.getAsJsonArray("uvs");
            var polys = mesh.getAsJsonArray("polys");
            if (positions != null && uvs != null && polys != null && polys.size() == 8 && positions.size() == 16) {
                // Split side canopy rails #1 (0-4-5-1) and #5 (12-13-11-10) at Z=-54.5 (t=0.42 along rail).
                double t = 0.42;
                int p04 = lerpVec3(positions, 0, 4, t);
                int p15 = lerpVec3(positions, 1, 5, t);
                int p11_13 = lerpVec3(positions, 11, 13, t);
                int p10_12 = lerpVec3(positions, 10, 12, t);
                int u45 = lerpVec2(uvs, 4, 5, t);
                int u76 = lerpVec2(uvs, 7, 6, t);

                var fixedPolys = new JsonArray();
                var movingPolys = new JsonArray();
                fixedPolys.add(polys.get(0));
                fixedPolys.add(quad(v(0, 0, 4), v(p04, 0, u45), v(p15, 0, u76), v(1, 0, 7)));
                fixedPolys.add(polys.get(3));
                fixedPolys.add(polys.get(4));
                fixedPolys.add(quad(v(p10_12, 5, u76), v(p11_13, 5, u45), v(11, 5, 4), v(10, 5, 7)));

                movingPolys.add(quad(v(p04, 2, u45), v(4, 2, 5), v(5, 2, 6), v(p15, 2, u76)));
                movingPolys.add(polys.get(2));
                movingPolys.add(quad(v(12, 6, 6), v(13, 6, 5), v(p11_13, 6, u45), v(p10_12, 6, u76)));
                movingPolys.add(polys.get(6));
                movingPolys.add(polys.get(7));

                mesh.add("polys", fixedPolys);

                var canopyMesh = new JsonObject();
                if (mesh.has("normalized_uvs")) canopyMesh.add("normalized_uvs", mesh.get("normalized_uvs"));
                canopyMesh.add("positions", positions.deepCopy());
                canopyMesh.add("uvs", uvs.deepCopy());
                canopyMesh.add("polys", movingPolys);
                canopy.add("poly_mesh", canopyMesh);
            }
        }
        bones.add(canopy);
        return true;
    }

    /**
     * Splits ONLY the pilot's sliding canopy bow out of Ju 87's bone7 so the pilot hood slides cleanly
     * backward (+Z) while the fixed front windscreen, center bridge, and 4.5m radio antenna wire (bone9) stay still.
     */
    static boolean attachJu87SlidingCanopy(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null || BoardingGeometry.findBone(bones, VehicleMotion.JU87_PILOT_CANOPY_BONE) != null) {
            return false;
        }
        JsonObject bone7 = BoardingGeometry.findBone(bones, "bone7");
        if (bone7 == null) return false;
        var cubes = bone7.getAsJsonArray("cubes");
        if (cubes == null || cubes.size() < 14) return false;

        var hoodCubes = new JsonArray();
        for (int idx : JU87_PILOT_HOOD_CUBES) {
            hoodCubes.add(cubes.remove(idx));
        }

        var pilotHood = new JsonObject();
        pilotHood.addProperty("name", VehicleMotion.JU87_PILOT_CANOPY_BONE);
        pilotHood.addProperty("parent", "canggai");
        pilotHood.add("pivot", vec3(0.0, 41.02, -22.42));
        pilotHood.add("cubes", hoodCubes);
        bones.add(pilotHood);
        return true;
    }

    /**
     * Hinges Mi-28's authentic asymmetric side-opening cockpit doors along their real front vertical posts:
     * - Rear pilot (seat 0): starboard/right (-X) door = bone8 + glass#51..#61, hinged at (-8.4, 36.0, -39.2).
     * - Front gunner (seat 1): port/left (+X) door = bone5 + glass#5..#14, hinged at (+6.8, 34.0, -58.5).
     */
    static boolean attachMi28Doors(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null || BoardingGeometry.findBone(bones, VehicleMotion.MI28_PILOT_DOOR_BONE) != null) return false;
        JsonObject glass = BoardingGeometry.findBone(bones, "glass");
        if (glass == null) return false;
        var glassCubes = glass.getAsJsonArray("cubes");
        if (glassCubes == null || glassCubes.size() < 62) return false;

        if (!BoardingGeometry.wrapHinge(geometry, "bone8", VehicleMotion.MI28_PILOT_DOOR_BONE, -8.4, 36.0, -39.2)) {
            return false;
        }
        if (!BoardingGeometry.wrapHinge(geometry, "bone5", VehicleMotion.MI28_GUNNER_DOOR_BONE, 6.8, 34.0, -58.5)) {
            return false;
        }
        bones = geometry.getAsJsonArray("bones");
        JsonObject pilotDoor = BoardingGeometry.findBone(bones, VehicleMotion.MI28_PILOT_DOOR_BONE);
        JsonObject gunnerDoor = BoardingGeometry.findBone(bones, VehicleMotion.MI28_GUNNER_DOOR_BONE);
        if (pilotDoor == null || gunnerDoor == null) return false;

        // Remove descending by index so earlier indices stay valid.
        var pilotGlass = new JsonArray();
        for (int i = 61; i >= 51; i--) {
            pilotGlass.add(glassCubes.remove(i));
        }
        pilotDoor.add("cubes", pilotGlass);

        var gunnerGlass = new JsonArray();
        for (int i = 14; i >= 5; i--) {
            gunnerGlass.add(glassCubes.remove(i));
        }
        gunnerDoor.add("cubes", gunnerGlass);
        return true;
    }

    private static int lerpVec3(JsonArray array, int aIdx, int bIdx, double t) {
        var a = array.get(aIdx).getAsJsonArray();
        var b = array.get(bIdx).getAsJsonArray();
        int idx = array.size();
        array.add(vec3(
                a.get(0).getAsDouble() + (b.get(0).getAsDouble() - a.get(0).getAsDouble()) * t,
                a.get(1).getAsDouble() + (b.get(1).getAsDouble() - a.get(1).getAsDouble()) * t,
                a.get(2).getAsDouble() + (b.get(2).getAsDouble() - a.get(2).getAsDouble()) * t));
        return idx;
    }

    private static int lerpVec2(JsonArray array, int aIdx, int bIdx, double t) {
        var a = array.get(aIdx).getAsJsonArray();
        var b = array.get(bIdx).getAsJsonArray();
        int idx = array.size();
        var v = new JsonArray();
        v.add(a.get(0).getAsDouble() + (b.get(0).getAsDouble() - a.get(0).getAsDouble()) * t);
        v.add(a.get(1).getAsDouble() + (b.get(1).getAsDouble() - a.get(1).getAsDouble()) * t);
        array.add(v);
        return idx;
    }

    private static JsonArray v(int p, int n, int uv) {
        var arr = new JsonArray();
        arr.add(p);
        arr.add(n);
        arr.add(uv);
        return arr;
    }

    private static JsonArray quad(JsonArray a, JsonArray b, JsonArray c, JsonArray d) {
        var q = new JsonArray();
        q.add(a);
        q.add(b);
        q.add(c);
        q.add(d);
        return q;
    }

    private static JsonArray vec3(double x, double y, double z) {
        var arr = new JsonArray();
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }
}
