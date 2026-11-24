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
                    case "fh_77bw.geo.json" -> alignFh77Doors(geometry);
                    case "ju_87.geo.json" -> alignCanopyHinge(geometry);
                    case "bradley.geo.json" -> BradleyDoorModel.attach(geometry);
                    case "lav_25.geo.json", "lav_ad.geo.json" -> LavRearDoorModel.attach(geometry);
                    case "a_10a.geo.json" -> attachA10RearCanopy(geometry);
                    case "j_16.geo.json" -> attachJ16Canopy(geometry);
                    case "mi_28.geo.json" -> attachMi28Doors(geometry);
                    case "m_1a_2.geo.json" -> wrapHinge(geometry, "bone15", VehicleMotion.M1A2_HATCH_BONE, 10.0, 47.2, 22.0);
                    case "bmp_2.geo.json" -> wrapHinge(geometry, "bone44", VehicleMotion.BMP2_HATCH_BONE, 9.6, 48.2, 21.4);
                    case "prism_tank.geo.json" -> wrapHinge(geometry, "cangmen", VehicleMotion.PRISM_HATCH_BONE, -9.5, 36.2, -42.5);
                    case "lav_150.geo.json" -> attachLav150Hatch(geometry);
                    case "ztz_99a.geo.json" -> wrapHinge(geometry, "octagon31", VehicleMotion.ZTZ99_HATCH_BONE, -12.5, 48.0, -2.5);
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
                 "a_10a.geo.json", "j_16.geo.json", "mi_28.geo.json", "m_1a_2.geo.json",
                 "bmp_2.geo.json", "prism_tank.geo.json", "lav_150.geo.json", "ztz_99a.geo.json" -> true;
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

    /** Moves FH-77BW cab doors' pivots from top-middle to their true front vertical hinge edge (Z=-82.58). */
    private static boolean alignFh77Doors(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        boolean changed = false;
        JsonObject left = findBone(bones, "door");
        if (left != null) {
            var piv = new JsonArray();
            piv.add(24.4875);
            piv.add(54.0);
            piv.add(-82.58);
            left.add("pivot", piv);
            changed = true;
        }
        JsonObject right = findBone(bones, "door2");
        if (right != null) {
            var piv = new JsonArray();
            piv.add(-24.4875);
            piv.add(54.0);
            piv.add(-82.58);
            right.add("pivot", piv);
            changed = true;
        }
        return changed;
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

    /**
     * Separates the A-10A rear bubble canopy glass (Z >= -52.5) from the fixed armored front
     * windscreen and metal windscreen bow (bone61 stays completely fixed on the fuselage).
     */
    private static boolean attachA10RearCanopy(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        if (findBone(bones, VehicleMotion.A10_CANOPY_BONE) != null) return false;
        JsonObject glass = findBone(bones, "glass");
        if (glass == null) return false;
        var glassCubes = glass.getAsJsonArray("cubes");
        if (glassCubes == null) return false;

        var canopyCubes = new JsonArray();
        for (int i = glassCubes.size() - 1; i >= 0; i--) {
            var cube = glassCubes.get(i).getAsJsonObject();
            var origin = cube.getAsJsonArray("origin");
            if (origin != null && origin.size() == 3 && origin.get(2).getAsDouble() >= -52.5) {
                canopyCubes.add(glassCubes.remove(i));
            }
        }
        if (canopyCubes.isEmpty()) return false;

        var canopy = new JsonObject();
        canopy.addProperty("name", VehicleMotion.A10_CANOPY_BONE);
        canopy.addProperty("parent", "body");
        var pivot = new JsonArray();
        pivot.add(0.0);
        pivot.add(41.0);
        pivot.add(-39.5);
        canopy.add("pivot", pivot);
        canopy.add("cubes", canopyCubes);
        bones.add(canopy);
        return true;
    }

    /**
     * Separates the J-16 rear-hinged clamshell canopy rails and middle bow arch from the fixed
     * armored front windscreen and fuselage spine so the entire canopy lifts cleanly open.
     */
    private static boolean attachJ16Canopy(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null || findBone(bones, VehicleMotion.J16_CANOPY_BONE) != null) return false;
        JsonObject body = findBone(bones, "body");
        if (body == null) return false;
        var bodyCubes = body.getAsJsonArray("cubes");
        if (bodyCubes == null || bodyCubes.size() < 165) return false;

        int[] indices = {164, 163, 162, 161, 154, 153, 152, 8, 7, 6, 5, 4, 3, 2, 1};
        var canopyCubes = new JsonArray();
        for (int idx : indices) {
            canopyCubes.add(bodyCubes.remove(idx));
        }

        var canopy = new JsonObject();
        canopy.addProperty("name", VehicleMotion.J16_CANOPY_BONE);
        canopy.addProperty("parent", "body");
        var pivot = new JsonArray();
        pivot.add(0.0);
        pivot.add(56.0);
        pivot.add(-49.0);
        canopy.add("pivot", pivot);
        canopy.add("cubes", canopyCubes);
        bones.add(canopy);
        return true;
    }

    /**
     * Hinges the Mi-28 left-side rear pilot door (bone8 armored frame + glass#51..#61) and
     * left-side front gunner door (glass#15..#23) along their real front vertical posts so
     * both armored cockpit doors swing outward cleanly during boarding and dismount.
     */
    private static boolean attachMi28Doors(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null || findBone(bones, VehicleMotion.MI28_PILOT_DOOR_BONE) != null) return false;
        JsonObject glass = findBone(bones, "glass");
        if (glass == null) return false;
        var glassCubes = glass.getAsJsonArray("cubes");
        if (glassCubes == null || glassCubes.size() < 62) return false;

        // Wrap bone8 (the blue armored left pilot door frame) with the front vertical hinge first.
        if (!wrapHinge(geometry, "bone8", VehicleMotion.MI28_PILOT_DOOR_BONE, -6.8, 36.0, -38.5)) {
            return false;
        }
        bones = geometry.getAsJsonArray("bones");
        JsonObject pilotDoor = findBone(bones, VehicleMotion.MI28_PILOT_DOOR_BONE);
        if (pilotDoor == null) return false;

        var pilotGlass = new JsonArray();
        for (int i = 61; i >= 51; i--) {
            pilotGlass.add(glassCubes.remove(i));
        }
        pilotDoor.add("cubes", pilotGlass);

        var gunnerGlass = new JsonArray();
        for (int i = 23; i >= 15; i--) {
            gunnerGlass.add(glassCubes.remove(i));
        }
        var gunnerDoor = new JsonObject();
        gunnerDoor.addProperty("name", VehicleMotion.MI28_GUNNER_DOOR_BONE);
        gunnerDoor.addProperty("parent", "root");
        var gPivot = new JsonArray();
        gPivot.add(-6.8);
        gPivot.add(34.0);
        gPivot.add(-58.5);
        gunnerDoor.add("pivot", gPivot);
        gunnerDoor.add("cubes", gunnerGlass);
        bones.add(gunnerDoor);
        return true;
    }

    /** Inserts a zero-rotation hinge parent above an existing bone without disturbing its rest pose. */
    private static boolean wrapHinge(JsonObject geometry, String targetName, String hingeName,
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

    private static boolean alignCanopyHinge(JsonObject geometry) {
        var bones = geometry.getAsJsonArray("bones");
        if (bones == null) return false;
        for (var element : bones) {
            var bone = element.getAsJsonObject();
            if (!bone.get("name").getAsString().equals("canggai")
                    || !matches(bone.getAsJsonArray("pivot"), 0, -8, -38.562)
                    || bone.has("rotation")) continue;
            var pivot = new JsonArray();
            pivot.add(-8.2);
            pivot.add(40.5);
            pivot.add(-22.4);
            bone.add("pivot", pivot);
            return true;
        }
        return false;
    }

    private static JsonObject findBone(JsonArray bones, String name) {
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
