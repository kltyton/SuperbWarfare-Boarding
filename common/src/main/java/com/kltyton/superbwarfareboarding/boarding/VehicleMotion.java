package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

/** Maps the boarding tracks to the actual rig used by each stock vehicle. */
public final class VehicleMotion {
    public static final String WEIGHT = "superbwarfare_boarding:vehicle_weight";
    public static final String ACCESS = "superbwarfare_boarding:vehicle_access";
    public static final String BRADLEY_DOOR_BONE = "superbwarfare_boarding:bradley_personnel_door";
    public static final String A10_CANOPY_BONE = "superbwarfare_boarding:a10_canopy";
    public static final String J16_CANOPY_BONE = "superbwarfare_boarding:j16_canopy";
    public static final String MI28_PILOT_DOOR_BONE = "superbwarfare_boarding:mi28_pilot_door";
    public static final String MI28_GUNNER_DOOR_BONE = "superbwarfare_boarding:mi28_gunner_door";
    public static final String M1A2_HATCH_BONE = "superbwarfare_boarding:m1a2_hatch";
    public static final String BMP2_HATCH_BONE = "superbwarfare_boarding:bmp2_hatch";
    public static final String PRISM_HATCH_BONE = "superbwarfare_boarding:prism_hatch";
    public static final String LAV150_HATCH_BONE = "superbwarfare_boarding:lav150_hatch";
    public static final String ZTZ99_HATCH_BONE = "superbwarfare_boarding:ztz99_hatch";
    public static final double BRADLEY_DOOR_TILT = Math.toRadians(7.75);
    public static final double LAV_DOOR_TILT = Math.toRadians(15.5);
    private static final Set<String> MOTION_BONES = Set.of(
            "root", "bone", "tow", "bone3",
            "door_left", "door_right", "canggai", "cangmen", "door", "door2", "door3",
            "ctd2", "bone44", "bone11", "bone15", "bone8", "octagon31",
            BRADLEY_DOOR_BONE, A10_CANOPY_BONE, J16_CANOPY_BONE,
            MI28_PILOT_DOOR_BONE, MI28_GUNNER_DOOR_BONE, M1A2_HATCH_BONE,
            BMP2_HATCH_BONE, PRISM_HATCH_BONE, LAV150_HATCH_BONE, ZTZ99_HATCH_BONE);
    // LAV-25 / LAV-AD have side-hinged double rear doors tilted along the -15.5 deg rear armor slope.
    private static final List<Joint> LAV_DOUBLE_DOORS = List.of(
            new Joint("door_left", new Vec3(0, Math.cos(LAV_DOOR_TILT), -Math.sin(LAV_DOOR_TILT)).scale(1.15)),
            new Joint("door_right", new Vec3(0, -Math.cos(LAV_DOOR_TILT), Math.sin(LAV_DOOR_TILT)).scale(1.15)));
    private static final List<Joint> BRADLEY_DOOR =
            List.of(new Joint(BRADLEY_DOOR_BONE,
                    new Vec3(0, -Math.cos(BRADLEY_DOOR_TILT), Math.sin(BRADLEY_DOOR_TILT)).scale(1.36)));
    private static final List<Joint> TURRET_HATCH = List.of(new Joint("canggai", new Vec3(-1.15, 0, 0)));
    private static final List<Joint> PRISM_HATCH = List.of(new Joint(PRISM_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> PLZ05_HATCH = List.of(new Joint("ctd2", new Vec3(-1.15, 0, 0)));
    private static final List<Joint> BMP2_HATCH = List.of(new Joint(BMP2_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> LAV150_HATCH = List.of(new Joint(LAV150_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> ZTZ99_HATCH = List.of(new Joint(ZTZ99_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> M1A2_HATCH = List.of(new Joint(M1A2_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> JU87_CANOPY = List.of(new Joint("canggai", new Vec3(0, 0, -0.6)));
    private static final List<Joint> A10_CANOPY = List.of(new Joint(A10_CANOPY_BONE, new Vec3(-0.72, 0, 0)));
    private static final List<Joint> J16_CANOPY = List.of(new Joint(J16_CANOPY_BONE, new Vec3(-0.68, 0, 0)));
    private static final List<Joint> MI28_DOORS = List.of(
            new Joint(MI28_PILOT_DOOR_BONE, new Vec3(0, -1.05, 0)),
            new Joint(MI28_GUNNER_DOOR_BONE, new Vec3(0, -1.05, 0)));
    private static final List<Joint> AC130_DOORS = List.of(
            new Joint("door2", new Vec3(0, 0, 1.05)),
            new Joint("door3", new Vec3(0, 0, -1.05)));

    private VehicleMotion() {}

    public static Set<String> motionBones() {
        return MOTION_BONES;
    }

    public static String loadBearingBone(VehicleEntity vehicle) {
        return switch (BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).getPath()) {
            case "tom_6" -> "bone";
            case "tow" -> "tow";
            case "vehicle_assembling_table" -> "bone3";
            default -> "root";
        };
    }

    public static List<Joint> accessJoints(VehicleEntity vehicle) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        if (!key.getNamespace().equals("superbwarfare")) return List.of();
        return switch (key.getPath()) {
            case "lav_25", "lav_ad" -> LAV_DOUBLE_DOORS;
            case "bradley" -> BRADLEY_DOOR;
            case "t_90a" -> TURRET_HATCH;
            case "prism_tank" -> PRISM_HATCH;
            case "plz_05" -> PLZ05_HATCH;
            case "bmp_2" -> BMP2_HATCH;
            case "lav_150" -> LAV150_HATCH;
            case "ztz_99a" -> ZTZ99_HATCH;
            case "m_1a_2" -> M1A2_HATCH;
            case "ju_87" -> JU87_CANOPY;
            case "a_10a" -> A10_CANOPY;
            case "j_16" -> J16_CANOPY;
            case "mi_28" -> MI28_DOORS;
            case "ac_130h" -> AC130_DOORS;
            default -> List.of();
        };
    }

    public record Joint(String name, Vec3 openingAxis) {}
}
