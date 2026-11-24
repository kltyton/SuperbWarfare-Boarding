package com.kltyton.superbwarfareboarding.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.superbwarfareboarding.boarding.VehicleMotion;
import net.minecraft.world.phys.Vec3;

/** Separates the inset personnel door from the stock rear armor at model bake time. */
final class BradleyDoorModel {
    private static final double SINE = Math.sin(VehicleMotion.BRADLEY_DOOR_TILT);
    private static final double COSINE = Math.cos(VehicleMotion.BRADLEY_DOOR_TILT);
    private static final Section BOTTOM = new Section(11.89, 4.31, 10.12);
    private static final Section LOWER_CORNER = new Section(14.08, 0.86, 13.588);
    private static final Section UPPER_CORNER = new Section(29.73, 0.86, 13.588);
    private static final Section TOP = new Section(32.76, 4.30, 10.148);

    private BradleyDoorModel() {}

    static boolean attach(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        for (var element : bones) {
            var hull = element.getAsJsonObject();
            if (!hull.get("name").getAsString().equals("body")) continue;
            var cubes = hull.getAsJsonArray("cubes");
            if (cubes == null || cubes.size() < 15
                    || !BoardingGeometry.matches(cubes.get(1).getAsJsonObject().getAsJsonArray("origin"), -16.34, 8.17676, 51.68033)
                    || !BoardingGeometry.matches(cubes.get(2).getAsJsonObject().getAsJsonArray("size"), 31.304, 26.53788, 1.72)
                    || !BoardingGeometry.matches(cubes.get(7).getAsJsonObject().getAsJsonArray("origin"), 0.86, 13.96683, 55.72223)
                    || !BoardingGeometry.matches(cubes.get(8).getAsJsonObject().getAsJsonArray("origin"), 12.728, 19.81483, 56.06623)) {
                return false;
            }

            // Both rear panels cover the doorway in the stock model. Keep their outer edges
            // and split just the south faces along the bevels of the inset door.
            var fixed = new Mesh();
            var moving = new Mesh();
            var armor = Surface.south(cubes.get(2).getAsJsonObject());
            split(Surface.south(cubes.get(1).getAsJsonObject()), fixed, moving);
            split(armor, fixed, moving);
            addDoorThickness(armor, moving);
            var fittings = new JsonArray();
            for (int index = 7; index <= 14; index++) fittings.add(cubes.get(index));
            for (int index = 14; index >= 7; index--) cubes.remove(index);

            var aperture = bone("superbwarfare_boarding:bradley_door_frame", Vec3.ZERO);
            aperture.add("poly_mesh", fixed.json);
            var door = bone(VehicleMotion.BRADLEY_DOOR_BONE, armor.point(0.86, 22.04, armor.depth + 0.40));
            door.add("cubes", fittings);
            door.add("poly_mesh", moving.json);
            bones.add(aperture);
            bones.add(door);
            return true;
        }
        return false;
    }

    private static void split(Surface surface, Mesh frame, Mesh door) {
        Section floor = new Section(surface.bottom, BOTTOM.left, BOTTOM.right);
        Section ceiling = new Section(surface.top, TOP.left, TOP.right);
        surface.strip(frame, floor, BOTTOM, surface.left, surface.right, surface.left, surface.right);
        surface.strip(frame, TOP, ceiling, surface.left, surface.right, surface.left, surface.right);
        Section[] outline = {BOTTOM, LOWER_CORNER, UPPER_CORNER, TOP};
        for (int i = 1; i < outline.length; i++) {
            Section low = outline[i - 1];
            Section high = outline[i];
            surface.strip(frame, low, high, surface.left, low.left, surface.left, high.left);
            surface.strip(frame, low, high, low.right, surface.right, high.right, surface.right);
            surface.strip(door, low, high, low.left, low.right, high.left, high.right);
        }
    }

    private static void addDoorThickness(Surface surface, Mesh door) {
        Vec3[] outline = {
                new Vec3(BOTTOM.left, BOTTOM.y, 0), new Vec3(BOTTOM.right, BOTTOM.y, 0),
                new Vec3(LOWER_CORNER.right, LOWER_CORNER.y, 0), new Vec3(UPPER_CORNER.right, UPPER_CORNER.y, 0),
                new Vec3(TOP.right, TOP.y, 0), new Vec3(TOP.left, TOP.y, 0),
                new Vec3(UPPER_CORNER.left, UPPER_CORNER.y, 0), new Vec3(LOWER_CORNER.left, LOWER_CORNER.y, 0)
        };
        Vec3 material = new Vec3(surface.u + 1.5, surface.v + 4.5, 0);
        double back = surface.depth - 3.8;
        for (int i = 0; i < outline.length; i++) {
            Vec3 a = outline[i];
            Vec3 b = outline[(i + 1) % outline.length];
            door.quad(surface.point(b.x, b.y, surface.depth), surface.point(a.x, a.y, surface.depth),
                    surface.point(a.x, a.y, back), surface.point(b.x, b.y, back),
                    material, material, material, material);
        }
        for (int i = 2; i < outline.length; i++) {
            Vec3 a = outline[0], b = outline[i - 1], c = outline[i];
            Vec3 first = surface.point(a.x, a.y, back);
            door.quad(first, surface.point(b.x, b.y, back), surface.point(c.x, c.y, back), first,
                    material, material, material, material);
        }
    }

    private static JsonObject bone(String name, Vec3 pivot) {
        var bone = new JsonObject();
        bone.addProperty("name", name);
        bone.addProperty("parent", "body");
        bone.add("pivot", array(pivot.x, pivot.y, pivot.z));
        return bone;
    }

    private static JsonArray array(double... values) {
        var array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }

    private record Section(double y, double left, double right) {}

    private record Surface(double left, double right, double bottom, double top, double depth, double pivotY,
                           double pivotZ, double u, double v, double width, double height) {
        static Surface south(JsonObject cube) {
            var origin = cube.getAsJsonArray("origin");
            var size = cube.getAsJsonArray("size");
            var pivot = cube.getAsJsonArray("pivot");
            var face = cube.getAsJsonObject("uv").remove("south").getAsJsonObject();
            var uv = face.getAsJsonArray("uv");
            var sizeUv = face.getAsJsonArray("uv_size");
            double x = origin.get(0).getAsDouble();
            double y = origin.get(1).getAsDouble();
            return new Surface(x, x + size.get(0).getAsDouble(), y, y + size.get(1).getAsDouble(),
                    origin.get(2).getAsDouble() + size.get(2).getAsDouble(),
                    pivot.get(1).getAsDouble(), pivot.get(2).getAsDouble(), uv.get(0).getAsDouble(),
                    uv.get(1).getAsDouble(), sizeUv.get(0).getAsDouble(), sizeUv.get(1).getAsDouble());
        }

        Vec3 point(double x, double y, double z) {
            return new Vec3(x, pivotY + (y - pivotY) * COSINE - (z - pivotZ) * SINE,
                    pivotZ + (y - pivotY) * SINE + (z - pivotZ) * COSINE);
        }

        Vec3 texture(double x, double y) {
            return new Vec3(u + (right - x) / (right - left) * width, v + (top - y) / (top - bottom) * height, 0);
        }

        void strip(Mesh mesh, Section low, Section high, double lowLeft, double lowRight,
                   double highLeft, double highRight) {
            mesh.quad(point(lowRight, low.y, depth), point(lowLeft, low.y, depth),
                    point(highLeft, high.y, depth), point(highRight, high.y, depth),
                    texture(lowRight, low.y), texture(lowLeft, low.y), texture(highLeft, high.y), texture(highRight, high.y));
        }
    }

    private static final class Mesh {
        private final JsonObject json = new JsonObject();
        private final JsonArray positions = new JsonArray();
        private final JsonArray uvs = new JsonArray();
        private final JsonArray polygons = new JsonArray();

        Mesh() {
            json.add("positions", positions);
            json.add("uvs", uvs);
            json.add("polys", polygons);
        }

        void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 uvA, Vec3 uvB, Vec3 uvC, Vec3 uvD) {
            var face = new JsonArray();
            vertex(face, a, uvA);
            vertex(face, b, uvB);
            vertex(face, c, uvC);
            if (!a.equals(d)) vertex(face, d, uvD);
            polygons.add(face);
        }

        private void vertex(JsonArray polygon, Vec3 point, Vec3 uv) {
            int index = positions.size();
            positions.add(array(point.x, point.y, point.z));
            uvs.add(array(uv.x, 512 - uv.y));
            polygon.add(array(index, 0, index));
        }
    }
}

