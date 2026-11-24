package com.kltyton.superbwarfareboarding.client.model;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.superbwarfareboarding.boarding.VehicleMotion;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.StringReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

/** Retains the native vehicle meshes while supplying accurate stock boarding hinges and decals. */
public final class BoardingGeometry {
    private BoardingGeometry() {}

    public static BedrockModelPOJO prepare(BedrockModelPOJO original, ResourceLocation location,
                                           ResourceManager resources, Gson gson) {
        if (!location.getNamespace().equals("superbwarfare")) return original;
        String model = location.getPath();
        model = model.substring(model.lastIndexOf('/') + 1);
        if (!handles(model)) return original;
        var resource = resources.getResource(location);
        if (resource.isEmpty()) return original;
        try (var reader = resource.get().openAsReader()) {
            JsonObject tree = JsonParser.parseReader(reader).getAsJsonObject();
            var geometries = tree.getAsJsonArray("minecraft:geometry");
            if (geometries != null && geometries.size() == 1) {
                var geometry = geometries.get(0).getAsJsonObject();
                boolean changed = switch (model) {
                    case "sodayo_pick_up.geo.json", "sodayo_pick_up_hmg.geo.json",
                         "sodayo_pick_up_rocket.geo.json", "sodayo_pick_up_tow.geo.json" ->
                            attachPickupDoorDecals(geometry);
                    case "fh_77bw.geo.json" -> Fh77CabModel.attach(geometry);
                    case "ju_87.geo.json" -> CanopyGeometry.attachJu87SlidingCanopy(geometry);
                    case "bradley.geo.json" -> BradleyDoorModel.attach(geometry);
                    case "lav_25.geo.json", "lav_ad.geo.json" -> LavRearDoorModel.attach(geometry);
                    case "a_10a.geo.json" -> CanopyGeometry.attachA10RearCanopy(geometry);
                    case "j_16.geo.json" -> CanopyGeometry.attachJ16Canopy(geometry);
                    case "mi_28.geo.json" -> CanopyGeometry.attachMi28Doors(geometry);
                    case "t_90a.geo.json" -> attachT90Hatches(geometry);
                    case "plz_05.geo.json" -> attachPlz05Hatches(geometry);
                    case "m_1a_2.geo.json" -> wrapHinge(geometry, "bone15", VehicleMotion.M1A2_HATCH_BONE, 10.0, 47.2, 22.0);
                    case "bmp_2.geo.json" -> wrapHinge(geometry, "bone44", VehicleMotion.BMP2_HATCH_BONE, 9.6, 48.2, 21.4);
                    case "prism_tank.geo.json" -> wrapHinge(geometry, "cangmen", VehicleMotion.PRISM_HATCH_BONE, -9.5, 36.2, -42.5);
                    case "lav_150.geo.json" -> attachLav150Hatch(geometry);
                    case "ztz_99a.geo.json" -> wrapHinge(geometry, "bone25", VehicleMotion.ZTZ99_HATCH_BONE, -19.6, 49.0, 3.35);
                    default -> false;
                };
                if (changed) {
                    return GsonHelper.fromJson(gson, new StringReader(tree.toString()), BedrockModelPOJO.class);
                }
            }
        } catch (IOException error) {
            LogUtils.getLogger().warn("Could not read the boarding hinge geometry for {}", location, error);
        }
        return original;
    }

    private static boolean handles(String model) {
        return switch (model) {
            case "sodayo_pick_up.geo.json", "sodayo_pick_up_hmg.geo.json",
                 "sodayo_pick_up_rocket.geo.json", "sodayo_pick_up_tow.geo.json",
                 "fh_77bw.geo.json",
                 "ju_87.geo.json", "bradley.geo.json", "lav_25.geo.json", "lav_ad.geo.json",
                 "a_10a.geo.json", "j_16.geo.json", "mi_28.geo.json", "t_90a.geo.json", "plz_05.geo.json",
                 "m_1a_2.geo.json", "bmp_2.geo.json", "prism_tank.geo.json", "lav_150.geo.json",
                 "ztz_99a.geo.json" -> true;
            default -> false;
        };
    }

    /** Moves the left/right "Sodayo" door decals from the static icon group onto the swinging doors. */
    private static boolean attachPickupDoorDecals(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        JsonObject icon = findBone(bones, "icon");
        JsonObject leftDoor = findBone(bones, "leftdoor");
        JsonObject rightDoor = findBone(bones, "rightdoor");
        if (icon == null || leftDoor == null || rightDoor == null) return false;
        var iconCubes = icon.getAsJsonArray("cubes");
        var leftCubes = leftDoor.getAsJsonArray("cubes");
        var rightCubes = rightDoor.getAsJsonArray("cubes");
        if (iconCubes == null || leftCubes == null || rightCubes == null) return false;
        boolean moved = false;
        for (int i = iconCubes.size() - 1; i >= 0; i--) {
            var cube = iconCubes.get(i).getAsJsonObject();
            var origin = cube.getAsJsonArray("origin");
            if (matches(origin, 16.82832, 15.97456, -24.5777)) {
                leftCubes.add(iconCubes.remove(i));
                moved = true;
            } else if (matches(origin, -33.21879, 15.97456, -13.43317)) {
                rightCubes.add(iconCubes.remove(i));
                moved = true;
            }
        }
        return moved;
    }

    /** Hinges both T-90A turret hatches at their real front edges so they swing cleanly forward & up. */
    private static boolean attachT90Hatches(JsonObject geometry) {
        boolean gunner = wrapHinge(geometry, "canggai", VehicleMotion.T90_HATCH_BONE, 11.5, 41.2, 2.4);
        boolean commander = wrapHinge(geometry, "bone19", VehicleMotion.T90_COMMANDER_HATCH_BONE, -11.32, 42.2, -3.0);
        return gunner || commander;
    }

    /** Hinges PLZ-05's front hull driver hatch AND its three turret roof hatches at their real edges. */
    private static boolean attachPlz05Hatches(JsonObject geometry) {
        boolean driver = wrapHinge(geometry, "ctd2", VehicleMotion.PLZ05_HATCH_BONE, 16.4, 32.8, -35.9);
        boolean turretLeftFront = wrapHinge(geometry, "move_titop1", VehicleMotion.PLZ05_TURRET_FRONT_HATCH_BONE, 18.4, 57.0, -0.9);
        boolean turretLeftRear = wrapHinge(geometry, "titop5", VehicleMotion.PLZ05_TURRET_LEFT_HATCH_BONE, 18.4, 57.0, 28.7);
        boolean turretRightRear = wrapHinge(geometry, "titop4", VehicleMotion.PLZ05_TURRET_RIGHT_HATCH_BONE, -19.2, 57.0, 27.8);
        return driver || turretLeftFront || turretLeftRear || turretRightRear;
    }

    /** Moves the LAV-150 turret hatch handle onto the hatch bone before adding the rear hinge. */
    private static boolean attachLav150Hatch(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        JsonObject turret = findBone(bones, "turret");
        JsonObject hatch = findBone(bones, "bone11");
        if (turret != null && hatch != null) {
            var tCubes = turret.getAsJsonArray("cubes");
            var hCubes = hatch.getAsJsonArray("cubes");
            if (tCubes != null && hCubes != null) {
                for (int i = tCubes.size() - 1; i >= 0; i--) {
                    var cube = tCubes.get(i).getAsJsonObject();
                    if (matches(cube.getAsJsonArray("origin"), 5.1, 49.5, -3.6)
                            && matches(cube.getAsJsonArray("size"), 3.6, 1.2, 1.2)) {
                        hCubes.add(tCubes.remove(i));
                        break;
                    }
                }
            }
        }
        return wrapHinge(geometry, "bone11", VehicleMotion.LAV150_HATCH_BONE, 6.0, 49.2, 6.0);
    }

    /** Inserts a zero-rotation hinge parent above an existing bone without disturbing its rest pose. */
    static boolean wrapHinge(JsonObject geometry, String targetName, String hingeName,
                             double pivotX, double pivotY, double pivotZ) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        int targetIndex = -1;
        JsonObject target = null;
        for (int index = 0; index < bones.size(); index++) {
            var bone = bones.get(index).getAsJsonObject();
            String name = bone.get("name").getAsString();
            if (name.equals(hingeName)) return false;
            if (name.equals(targetName)) {
                targetIndex = index;
                target = bone;
            }
        }
        if (target == null) return false;
        var hinge = new JsonObject();
        hinge.addProperty("name", hingeName);
        if (target.has("parent")) {
            hinge.addProperty("parent", target.get("parent").getAsString());
        }
        var pivot = new JsonArray();
        pivot.add(pivotX);
        pivot.add(pivotY);
        pivot.add(pivotZ);
        hinge.add("pivot", pivot);
        target.addProperty("parent", hingeName);
        // SimpleBedrockModel resolves parents before children in array order.
        var rebuilt = new JsonArray();
        for (int index = 0; index < bones.size(); index++) {
            if (index == targetIndex) rebuilt.add(hinge);
            rebuilt.add(bones.get(index));
        }
        geometry.add("bones", rebuilt);
        return true;
    }

    static JsonObject findBone(JsonArray bones, String name) {
        for (var element : bones) {
            var bone = element.getAsJsonObject();
            if (bone.get("name").getAsString().equals(name)) return bone;
        }
        return null;
    }

    static boolean matches(JsonArray vector, double x, double y, double z) {
        return vector != null && vector.size() == 3
                && Math.abs(vector.get(0).getAsDouble() - x) < 0.01
                && Math.abs(vector.get(1).getAsDouble() - y) < 0.01
                && Math.abs(vector.get(2).getAsDouble() - z) < 0.01;
    }
}
