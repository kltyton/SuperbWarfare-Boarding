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
    public static final String JU87_PILOT_CANOPY_BONE = "superbwarfare_boarding:ju87_pilot_canopy";
    public static final String MI28_PILOT_DOOR_BONE = "superbwarfare_boarding:mi28_pilot_door";
    public static final String MI28_GUNNER_DOOR_BONE = "superbwarfare_boarding:mi28_gunner_door";
    public static final String T90_HATCH_BONE = "superbwarfare_boarding:t90_hatch";
    public static final String T90_COMMANDER_HATCH_BONE = "superbwarfare_boarding:t90_commander_hatch";
    public static final String PLZ05_HATCH_BONE = "superbwarfare_boarding:plz05_hatch";
    public static final String M1A2_HATCH_BONE = "superbwarfare_boarding:m1a2_hatch";
    public static final String BMP2_HATCH_BONE = "superbwarfare_boarding:bmp2_hatch";
    public static final String PRISM_HATCH_BONE = "superbwarfare_boarding:prism_hatch";
    public static final String LAV150_HATCH_BONE = "superbwarfare_boarding:lav150_hatch";
    public static final String ZTZ99_HATCH_BONE = "superbwarfare_boarding:ztz99_hatch";
    public static final double BRADLEY_DOOR_TILT = Math.toRadians(7.75);
    public static final double LAV_DOOR_TILT = Math.toRadians(15.5);
    private static final Set<String> MOTION_BONES = Set.of(
            "root", "bone", "tow", "bone3",
            "door_left", "door_right", "canggai", "cangmen", "door", "door2",
            "ctd2", "bone44", "bone11", "bone12", "bone15", "bone19", "bone5", "bone8", "octagon31",
            BRADLEY_DOOR_BONE, A10_CANOPY_BONE, J16_CANOPY_BONE, JU87_PILOT_CANOPY_BONE,
            MI28_PILOT_DOOR_BONE, MI28_GUNNER_DOOR_BONE, T90_HATCH_BONE, T90_COMMANDER_HATCH_BONE,
            PLZ05_HATCH_BONE, M1A2_HATCH_BONE, BMP2_HATCH_BONE, PRISM_HATCH_BONE, LAV150_HATCH_BONE,
            ZTZ99_HATCH_BONE);
    // LAV-25 / LAV-AD have side-hinged double rear doors tilted along the -15.5 deg rear armor slope.
    private static final List<Joint> LAV_DOUBLE_DOORS = List.of(
            new Joint("door_left", new Vec3(0, Math.cos(LAV_DOOR_TILT), -Math.sin(LAV_DOOR_TILT)).scale(1.15)),
            new Joint("door_right", new Vec3(0, -Math.cos(LAV_DOOR_TILT), Math.sin(LAV_DOOR_TILT)).scale(1.15)));
    private static final List<Joint> BRADLEY_DOOR =
            List.of(new Joint(BRADLEY_DOOR_BONE,
                    new Vec3(0, -Math.cos(BRADLEY_DOOR_TILT), Math.sin(BRADLEY_DOOR_TILT)).scale(1.36)));
    // T-90A front-hinged turret hatches swing cleanly forward & up (+115 deg) clear of rear antenna & rider.
    private static final List<Joint> T90_HATCH = List.of(new Joint(T90_HATCH_BONE, new Vec3(1.26, 0, 0)));
    private static final List<Joint> T90_COMMANDER_HATCH = List.of(new Joint(T90_COMMANDER_HATCH_BONE, new Vec3(1.26, 0, 0)));
    private static final List<Joint> PRISM_HATCH = List.of(new Joint(PRISM_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> PLZ05_HATCH = List.of(new Joint(PLZ05_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> BMP2_HATCH = List.of(new Joint(BMP2_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> LAV150_HATCH = List.of(new Joint(LAV150_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> ZTZ99_HATCH = List.of(new Joint(ZTZ99_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    private static final List<Joint> M1A2_HATCH = List.of(new Joint(M1A2_HATCH_BONE, new Vec3(-1.15, 0, 0)));
    // Ju 87 Stuka: full sliding greenhouse hood along fuselage rails (windscreen & 4.5m antenna wire stay still).
    private static final List<Joint> JU87_PILOT_CANOPY =
            List.of(Joint.slide(JU87_PILOT_CANOPY_BONE, new Vec3(0, 0, 12.0)));
    private static final List<Joint> JU87_GUNNER_CANOPY = List.of(
            Joint.slide("bone11", new Vec3(0, 0, -8.5)),
            Joint.slide("bone12", new Vec3(0, 0, -8.5)));
    private static final List<Joint> A10_CANOPY = List.of(new Joint(A10_CANOPY_BONE, new Vec3(-0.78, 0, 0)));
    private static final List<Joint> J16_CANOPY = List.of(new Joint(J16_CANOPY_BONE, new Vec3(-0.68, 0, 0)));
    // Mi-28 asymmetric front-hinged doors: pilot opens starboard/right (-X), gunner opens port/left (+X).
    private static final List<Joint> MI28_PILOT_DOOR = List.of(new Joint(MI28_PILOT_DOOR_BONE, new Vec3(0, -1.10, 0)));
    private static final List<Joint> MI28_GUNNER_DOOR = List.of(new Joint(MI28_GUNNER_DOOR_BONE, new Vec3(0, 1.10, 0)));

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
        return accessJoints(vehicle, -1, null);
    }

    public static List<Joint> accessJoints(VehicleEntity vehicle, int seatIndex, Vec3 riderLocal) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        if (!key.getNamespace().equals("superbwarfare")) return List.of();
        return switch (key.getPath()) {
            case "lav_25", "lav_ad" -> LAV_DOUBLE_DOORS;
            case "bradley" -> BRADLEY_DOOR;
            case "t_90a" -> seatIndex == 1 ? T90_COMMANDER_HATCH : T90_HATCH;
            case "prism_tank" -> PRISM_HATCH;
            case "plz_05" -> PLZ05_HATCH;
            case "bmp_2" -> BMP2_HATCH;
            case "lav_150" -> LAV150_HATCH;
            case "ztz_99a" -> ZTZ99_HATCH;
            case "m_1a_2" -> M1A2_HATCH;
            case "ju_87" -> seatIndex == 1
                    || (seatIndex < 0 && riderLocal != null && riderLocal.z < 0.60)
                    ? JU87_GUNNER_CANOPY : JU87_PILOT_CANOPY;
            case "a_10a" -> A10_CANOPY;
            case "j_16" -> J16_CANOPY;
            case "mi_28" -> seatIndex == 1
                    || (seatIndex < 0 && riderLocal != null && riderLocal.x > 0.15)
                    ? MI28_GUNNER_DOOR : MI28_PILOT_DOOR;
            default -> List.of();
        };
    }

    public record Joint(String name, Vec3 openingAxis, Vec3 slidePerNinetyDeg) {
        public Joint(String name, Vec3 openingAxis) {
            this(name, openingAxis, Vec3.ZERO);
        }

        public static Joint slide(String name, Vec3 slideAt90Deg) {
            return new Joint(name, Vec3.ZERO, slideAt90Deg);
        }
    }
}
