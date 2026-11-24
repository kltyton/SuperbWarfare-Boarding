package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Per-vehicle hull footprints, wing obstacles, and model-anchored entry geometry
 * derived directly from Superb Warfare's stock .geo.json surface measurements.
 */
final class VehicleRigCatalog {
    private VehicleRigCatalog() {}

    static String id(VehicleEntity vehicle) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        return key.getNamespace().equals("superbwarfare") ? key.getPath() : "";
    }

    static boolean hasSymmetricSide(VehicleEntity vehicle) {
        return switch (id(vehicle)) {
            case "kirov", "annihilator", "bl_132", "mk_42", "mle_1934", "happiest_ghast",
                 "a_10a", "j_16", "ju_87", "speedboat", "tiny_speedboat", "air_sheep",
                 "kv_16", "tom_6" -> true;
            default -> false;
        };
    }

    /** Tight boarding footprint excluding wide wingspans, gun barrels, and airship envelopes. */
    static AABB customFootprint(VehicleEntity vehicle) {
        return switch (id(vehicle)) {
            case "a_10a" -> new AABB(-0.88, 0.55, -5.80, 0.88, 2.65, 5.70);
            case "j_16" -> new AABB(-1.55, 0.64, -10.80, 1.55, 3.40, 7.60);
            case "ju_87" -> new AABB(-0.62, 0.75, -4.60, 0.62, 2.10, 4.55);
            case "mi_28" -> new AABB(-0.96, 0.33, -9.60, 0.96, 3.40, 4.45);
            case "ah_6" -> new AABB(-0.96, 0.0, -5.16, 0.96, 2.65, 2.36);
            case "ac_130h" -> new AABB(-3.1875, 1.0, -7.125, 3.1875, 4.5, 16.125);
            case "kirov" -> new AABB(-2.625, 5.6665, 9.1247, 2.625, 8.1, 16.6873);
            case "air_sheep" -> new AABB(-0.85, 0.0, -0.85, 0.85, 2.0, 0.85);
            case "kv_16" -> new AABB(-0.84, 0.0, -0.92, 0.84, 1.29, 0.70);
            case "tom_6" -> new AABB(-0.60, 0.0, -1.10, 0.60, 1.35, 1.10);
            case "fh_77bw" -> new AABB(-1.58, 0.0, -7.30, 1.58, 4.35, 6.20);
            case "annihilator" -> new AABB(-5.60, 0.0, -8.60, 5.60, 5.20, 8.60);
            case "bl_132" -> new AABB(-4.25, 0.0, -5.20, 4.25, 5.50, 4.50);
            case "mk_42" -> new AABB(-2.05, 0.0, -2.65, 2.05, 3.80, 2.25);
            case "mle_1934" -> new AABB(-2.45, 0.0, -3.50, 2.45, 3.60, 3.50);
            case "hpj_11" -> new AABB(-1.45, 0.0, -1.65, 1.45, 3.60, 1.65);
            default -> null;
        };
    }

    /** Low wing or fuselage appendages that ground pathing must walk around rather than under. */
    static List<AABB> localApproachObstacles(VehicleEntity vehicle) {
        return switch (id(vehicle)) {
            // Leave A-10A inner wing-root trailing edge (|X| < 1.68) reachable so pilot climbs directly onto wing root!
            case "a_10a" -> List.of(
                    new AABB(-6.63, 1.05, -1.40, -1.68, 2.18, 0.78),
                    new AABB(1.68, 1.05, -1.40, 6.63, 2.18, 0.78));
            case "j_16" -> List.of(new AABB(-8.52, 1.95, -7.62, 8.52, 2.50, 0.75));
            case "mi_28" -> List.of(new AABB(-2.58, 0.85, -1.02, 2.58, 2.20, 0.90));
            default -> List.of();
        };
    }

    static EntryPortal resolve(VehicleEntity vehicle, int seat, Vec3 seatPosition, Vec3 start) {
        String model = id(vehicle);
        if (model.isEmpty()) return null;
        EntryPortal portal = carsAndTrucks(model, seat, seatPosition, start);
        if (portal != null) return portal;
        portal = apcsAndTanks(vehicle, model, seat, seatPosition, start);
        if (portal != null) return portal;
        portal = aircraftAndHelis(vehicle, model, seat, seatPosition, start);
        if (portal != null) return portal;
        portal = boatsAndAirships(model, seat, seatPosition, start);
        if (portal != null) return portal;
        return emplacements(model, seat, seatPosition, start);
    }

    private static EntryPortal carsAndTrucks(String model, int seat, Vec3 seatPosition, Vec3 start) {
        return switch (model) {
            case "sodayo_pick_up", "sodayo_pick_up_hmg", "sodayo_pick_up_rocket", "sodayo_pick_up_tow" -> {
                if (seat <= 1) {
                    double side = seat == 0 ? 1 : -1;
                    // .geo.json verified: cab floor bottom#0 Y_top=0.59, door pivot=(+-1.13, 1.38, +1.82), length=1.41m.
                    // At 74 deg open, door tip is at (+-2.49, 1.35, +1.43); rear half of doorway Z=0.41..1.25 is wide open!
                    yield new EntryPortal(
                            new Vec3(side * 1.06, 0.58, 0.72),
                            new Vec3(side * 1.56, start.y, 0.76),
                            new Vec3(side * 2.18, start.y, 0.76),
                            new Vec3(side * 1.50, start.y, 0.74),
                            null,
                            new Vec3(side * 1.18, 1.25, 0.92));
                }
                double side = seatPosition.x > 0.2 ? 1 : seatPosition.x < -0.2 ? -1 : (start.x < 0 ? -1 : 1);
                if (seatPosition.z < -2.35) {
                    yield new EntryPortal(
                            new Vec3(seatPosition.x, 0.92, -2.75),
                            new Vec3(seatPosition.x, start.y, -3.35),
                            new Vec3(seatPosition.x, Math.max(start.y, 0.58), -2.98),
                            null, null, seatPosition);
                }
                yield new EntryPortal(
                        new Vec3(side * 1.10, Math.max(0.95, seatPosition.y + 0.15), seatPosition.z),
                        new Vec3(side * 1.72, start.y, seatPosition.z),
                        new Vec3(side * 1.32, Math.max(start.y, 0.62), seatPosition.z),
                        null, null, seatPosition);
            }
            case "truck" -> {
                double side = seat == 0 ? 1 : -1;
                // .geo.json verified: door_left hinge=(1.17, 1.41, 4.66), length 1.55m -> open 86 deg sits at Z=4.55!
                // Real metal footstep head#24 is at |X|=1.20..1.33, Y_top=0.72, Z=4.04..4.55; cab floor head#142 Y_top=1.32!
                yield new EntryPortal(
                        new Vec3(side * 0.96, 1.32, 3.88),
                        new Vec3(side * 1.66, start.y, 3.44),
                        new Vec3(side * 2.26, start.y, 3.16),
                        new Vec3(side * 1.24, Math.max(start.y, 0.72), 4.16),
                        null,
                        new Vec3(side * 1.18, 1.65, 3.92));
            }
            case "fh_77bw" -> {
                if (seat == 1) {
                    double side = start.x < 0 ? -1 : 1;
                    yield new EntryPortal(
                            new Vec3(side * 0.45, 3.20, -6.38),
                            new Vec3(side * 2.35, start.y, -6.38),
                            new Vec3(side * 1.85, Math.max(start.y, 1.45), -6.38),
                            new Vec3(side * 1.35, Math.max(start.y, 2.45), -6.38),
                            new Vec3(0.0, 2.20, -6.38),
                            new Vec3(0.0, 2.80, -6.38));
                }
                double side = seat == 4 ? -1 : 1;
                // .geo.json verified: tizi/tizi2 exact 4 rungs at Z=4.13:
                // #7 (2.35, 0.65), #5 (2.14, 0.98), #3 (1.94, 1.30), #0 (1.82, 1.63); front-hinged door at Z=5.16,
                // doorway sill Y=41.9/16=2.62m (cut cleanly out of bone#99/#132).
                yield new EntryPortal(
                        new Vec3(side * 1.36, 2.58, 4.30),
                        new Vec3(side * 2.62, start.y, 4.13),
                        new Vec3(side * 2.35, Math.max(start.y, 0.65), 4.13),
                        new Vec3(side * 1.82, Math.max(start.y, 1.63), 4.13),
                        null,
                        new Vec3(side * 1.50, 2.95, 4.35));
            }
            case "wheel_chair" -> new EntryPortal(
                    new Vec3(0.0, 0.35, 0.45),
                    new Vec3(0.0, start.y, 1.15),
                    null, null, null, new Vec3(0.0, 0.55, 0.0));
            case "vehicle_assembling_table" -> new EntryPortal(
                    new Vec3(-0.40, Math.max(start.y, 0.08), 0.72),
                    new Vec3(-0.40, start.y, 1.28),
                    null, null, null, seatPosition);
            default -> null;
        };
    }

    private static EntryPortal apcsAndTanks(VehicleEntity vehicle, String model, int seat,
                                            Vec3 seatPosition, Vec3 start) {
        return switch (model) {
            case "bradley" -> new EntryPortal(
                    // .geo.json verified: left-hinged door X=0.05..0.85, Z=-3.48, bottom sill body#12 Y_top=0.93!
                    new Vec3(0.46, 0.92, -3.44),
                    new Vec3(0.86, start.y, -4.08),
                    new Vec3(0.84, start.y, -4.46),
                    new Vec3(0.46, 0.88, -3.02),
                    new Vec3(0.45, 0.82, -2.52),
                    new Vec3(0.48, 1.35, -3.48));
            case "lav_25", "lav_ad" -> new EntryPortal(
                    // .geo.json verified: double rear doors hinged at X=+-0.86, Z=-3.34, rear bumper/sill low#2/#9 Y_top=0.96!
                    new Vec3(0.0, 0.96, -3.38),
                    new Vec3(0.0, start.y, -4.46),
                    new Vec3(0.0, start.y, -4.78),
                    new Vec3(0.0, 0.94, -2.92),
                    new Vec3(0.0, 0.88, -2.30),
                    new Vec3(0.0, 1.35, -3.38));
            case "t_90a" -> {
                // Seat 1 is right-side (-X) commander cupola (bone19); seats 0/2 use left-side (+X) gunner hatch (canggai).
                // Both rim and besideHatch use turretPoint(...) so they always track turret yaw!
                boolean commander = seat == 1;
                Vec3 rim = commander
                        ? turretPoint(vehicle, -0.71, 0.74, -0.29)
                        : turretPoint(vehicle, 0.72, 0.64, -0.49);
                Vec3 besideHatch = commander
                        ? turretPoint(vehicle, -1.36, 0.58, -0.44)
                        : turretPoint(vehicle, 1.38, 0.56, -0.60);
                double side = rim.x >= 0 ? 1 : -1;
                // .geo.json raycast verified: outer ERA skirt |X|=2.28 -> stand 0.48m clear outside at |X|=2.76!
                // Front fender top Y=1.72 at |X|=1.86, Z=2.15; flat turret rear-left deck Y=2.36 clear of bone20 optics!
                yield new EntryPortal(
                        rim,
                        new Vec3(side * 2.86, start.y, 2.15),
                        new Vec3(side * 1.86, Math.max(start.y, 1.72), 2.15),
                        new Vec3(besideHatch.x, Math.max(start.y, besideHatch.y), besideHatch.z),
                        new Vec3(rim.x, 1.48, rim.z),
                        rim);
            }
            case "prism_tank" -> new EntryPortal(
                    // .geo.json verified: outer blue fender max|X|=2.65 -> stand 0.47m clear at X=-3.12!
                    new Vec3(-0.594, 2.26, 3.062),
                    new Vec3(-3.12, start.y, 3.062),
                    new Vec3(-2.14, Math.max(start.y, 1.80), 3.062),
                    new Vec3(-1.42, Math.max(start.y, 2.24), 3.062),
                    new Vec3(-0.594, 1.32, 3.062),
                    new Vec3(-0.594, 2.26, 3.062));
            case "plz_05" -> {
                if (seat == 0) {
                    // .geo.json verified: sidearmorL max|X|=2.22 -> stand 0.48m clear at X=2.70!
                    yield new EntryPortal(
                            new Vec3(1.00, 1.98, 2.68),
                            new Vec3(2.70, start.y, 2.68),
                            new Vec3(1.94, Math.max(start.y, 1.44), 2.68),
                            new Vec3(1.52, Math.max(start.y, 1.96), 2.68),
                            new Vec3(1.00, 1.18, 2.68),
                            new Vec3(1.00, 1.98, 2.68));
                }
                double tx = seat == 3 ? -1.10 : 1.10;
                double tz = seat == 1 ? 0.30 : -1.35;
                Vec3 rim = turretPoint(vehicle, tx, 1.68, tz);
                Vec3 besideHatch = turretPoint(vehicle, Math.copySign(1.56, tx), 1.32, tz);
                double side = rim.x >= 0 ? 1 : -1;
                yield new EntryPortal(
                        rim,
                        new Vec3(side * 2.70, start.y, 2.68),
                        new Vec3(side * 1.94, Math.max(start.y, 1.96), 2.68),
                        new Vec3(besideHatch.x, Math.max(start.y, besideHatch.y), besideHatch.z),
                        seat == 1 ? new Vec3(rim.x, 2.25, rim.z) : null,
                        rim);
            }
            case "bmp_2" -> {
                Vec3 rim = turretPoint(vehicle, 0.45, 0.79, -0.35);
                Vec3 besideHatch = turretPoint(vehicle, 1.02, 0.46, -0.35);
                double side = rim.x >= 0 ? 1 : -1;
                // .geo.json verified: outer max|X|=2.33 -> stand 0.47m clear at |X|=2.80!
                yield new EntryPortal(
                        rim,
                        new Vec3(side * 2.80, start.y, 1.95),
                        new Vec3(side * 1.82, Math.max(start.y, 1.78), 1.95),
                        new Vec3(besideHatch.x, Math.max(start.y, besideHatch.y), besideHatch.z),
                        new Vec3(rim.x, 1.68, rim.z),
                        rim);
            }
            case "lav_150" -> {
                Vec3 rim = turretPoint(vehicle, 0.38, 0.70, 0.0);
                Vec3 besideHatch = turretPoint(vehicle, 0.96, 0.08, 0.40);
                double side = rim.x >= 0 ? 1 : -1;
                yield new EntryPortal(
                        rim,
                        new Vec3(side * 2.22, start.y, 1.40),
                        new Vec3(side * 1.46, Math.max(start.y, 1.24), 1.40),
                        new Vec3(besideHatch.x, Math.max(start.y, besideHatch.y), besideHatch.z),
                        new Vec3(rim.x, 1.80, rim.z),
                        rim);
            }
            case "m_1a_2" -> {
                // .geo.json verified: outer max|X|=2.32 -> stand 0.48m clear at |X|=2.80!
                Vec3 rim = turretPoint(vehicle, 0.61, 1.09, -0.81);
                Vec3 besideHatch = turretPoint(vehicle, 1.24, 1.05, -0.45);
                double side = rim.x >= 0 ? 1 : -1;
                yield new EntryPortal(
                        rim,
                        new Vec3(side * 2.80, start.y, 2.25),
                        new Vec3(side * 1.88, Math.max(start.y, 1.70), 2.25),
                        new Vec3(besideHatch.x, Math.max(start.y, besideHatch.y), besideHatch.z),
                        new Vec3(rim.x, 1.60, rim.z),
                        rim);
            }
            case "ztz_99a" -> {
                // .geo.json verified: outer max|X|=2.29 -> stand 0.47m clear at |X|=2.76!
                Vec3 rim = turretPoint(vehicle, -0.78, 1.16, 0.16);
                Vec3 besideHatch = turretPoint(vehicle, -1.32, 1.12, 0.45);
                double side = rim.x >= 0 ? 1 : -1;
                yield new EntryPortal(
                        rim,
                        new Vec3(side * 2.76, start.y, 2.35),
                        new Vec3(side * 1.86, Math.max(start.y, 1.68), 2.35),
                        new Vec3(besideHatch.x, Math.max(start.y, besideHatch.y), besideHatch.z),
                        new Vec3(rim.x, 1.72, rim.z),
                        rim);
            }
            case "yx_100" -> {
                // .geo.json verified: outer max|X|=2.49 -> stand 0.47m clear at |X|=2.96!
                Vec3 rim = turretPoint(vehicle, 0.85, 0.48, 0.24);
                Vec3 besideHatch = turretPoint(vehicle, 1.48, 0.26, 0.45);
                double side = rim.x >= 0 ? 1 : -1;
                yield new EntryPortal(
                        rim,
                        new Vec3(side * 2.96, start.y, 2.40),
                        new Vec3(side * 2.02, Math.max(start.y, 1.88), 2.40),
                        new Vec3(besideHatch.x, Math.max(start.y, besideHatch.y), besideHatch.z),
                        new Vec3(rim.x, 2.05, rim.z),
                        rim);
            }
            default -> null;
        };
    }

    private static EntryPortal aircraftAndHelis(VehicleEntity vehicle, String model, int seat,
                                                Vec3 seatPosition, Vec3 start) {
        return switch (model) {
            case "a_10a" -> {
                double side = start.x < 0 ? -1 : 1;
                // .geo.json raycast verified: wing trailing edge Z=-1.40 (Y=1.79), wing peak Z=+0.20 (Y=2.15),
                // wing leading edge Z=+0.74 (Y=1.83), fuselage chine X=0.74 Z=+1.2..+2.4 (Y=1.92..2.26).
                // Stand >=0.54m CLEAR outside trailing/leading edge so L-shaped hoist & drop NEVER clip the wing!
                if (start.z < -0.35) {
                    yield new EntryPortal(
                            new Vec3(side * 0.24, 2.46, 2.74),
                            new Vec3(side * 1.88, start.y, -1.94),
                            new Vec3(side * 1.58, Math.max(start.y, 1.84), -1.04),
                            new Vec3(side * 0.82, Math.max(start.y, 2.16), 0.34),
                            null,
                            new Vec3(0.0, 2.20, 2.74));
                }
                yield new EntryPortal(
                        new Vec3(side * 0.24, 2.46, 2.74),
                        new Vec3(side * 1.46, start.y, 1.34),
                        new Vec3(side * 1.18, Math.max(start.y, 1.86), 0.62),
                        new Vec3(side * 0.78, Math.max(start.y, 2.04), 1.38),
                        null,
                        new Vec3(0.0, 2.20, 2.74));
            }
            case "j_16" -> {
                double side = start.x < 0 ? -1 : 1;
                double z = seat == 0 ? 5.62 : 4.00;
                // .geo.json raycast verified: LERX outer edge |X|=2.07 at Z=2.60 (Y=2.53),
                // rises smoothly along LERX ramp to Y=3.02 cockpit shoulder at |X|=1.02, Z=4.0..5.4!
                // Stand 0.49m CLEAR outside LERX edge (|X|=2.56) so hoist & drop NEVER clip underneath the LERX!
                yield new EntryPortal(
                        new Vec3(side * 0.24, 3.12, z),
                        new Vec3(side * 2.56, start.y, 2.60),
                        new Vec3(side * 1.72, Math.max(start.y, 2.54), 2.60),
                        new Vec3(side * 1.32, Math.max(start.y, 2.86), Math.min(z - 1.15, 3.85)),
                        null,
                        new Vec3(0.0, 2.88, z));
            }
            case "ju_87" -> {
                double side = start.x < seatPosition.x ? -1 : 1;
                // .geo.json raycast verified: wing trailing edge Z=-0.60 -> stand 0.56m clear behind at Z=-1.16!
                yield new EntryPortal(
                        modelPoint(vehicle, side * 0.22, 2.10, seatPosition.z),
                        new Vec3(side * 1.22, start.y, -1.16),
                        modelPoint(vehicle, side * 0.96, 1.54, -0.36),
                        modelPoint(vehicle, side * 0.82, 1.58, Math.min(1.12, seatPosition.z - 0.08)),
                        null,
                        modelPoint(vehicle, 0.0, 1.88, seatPosition.z));
            }
            case "mi_28" -> {
                // Authentic Mi-28 asymmetric doors:
                // - seat 0 (rear pilot): starboard/right (-X) door; wheel gear2#4 Y=0.54 -> sponson Y=1.06 -> over "24" armor sill Y=1.88!
                // - seat 1 (front gunner): port/left (+X) door; wheel gear Y=0.54 -> left sponson Y=1.06 -> over left sill Y=1.74!
                if (seat == 1) {
                    yield new EntryPortal(
                            new Vec3(0.68, 1.74, 2.86),
                            new Vec3(1.58, start.y, 1.65),
                            new Vec3(0.98, Math.max(start.y, 0.54), 1.32),
                            new Vec3(0.92, Math.max(start.y, 1.06), 2.18),
                            new Vec3(0.0, 1.18, 2.86),
                            new Vec3(0.0, 1.92, 2.86));
                }
                yield new EntryPortal(
                        new Vec3(-0.76, 1.88, 1.72),
                        new Vec3(-1.62, start.y, 1.48),
                        new Vec3(-0.98, Math.max(start.y, 0.54), 1.32),
                        new Vec3(-0.96, Math.max(start.y, 1.06), 1.48),
                        new Vec3(0.0, 1.50, 1.72),
                        new Vec3(0.0, 2.05, 1.72));
            }
            case "ah_6" -> {
                double side = seat == 0 || seat == 3 ? 1 : -1;
                double z = seat <= 1 ? 0.75 : 0.0;
                yield new EntryPortal(
                        new Vec3(side * 0.68, Math.max(start.y, 0.66), z),
                        new Vec3(side * 1.50, start.y, z),
                        new Vec3(side * 1.04, Math.max(start.y, 0.34), z),
                        null, null, seatPosition);
            }
            case "ac_130h" -> new EntryPortal(
                    new Vec3(2.45, Math.max(start.y, 2.85), 11.44),
                    new Vec3(3.75, start.y, 11.44),
                    new Vec3(3.10, Math.max(start.y, 2.55), 11.44),
                    new Vec3(1.48, Math.max(start.y, 3.20), 12.85),
                    null,
                    new Vec3(2.45, 3.20, 11.44));
            case "kv_16" -> {
                double side = start.x < 0 ? -1 : 1;
                yield new EntryPortal(
                        new Vec3(side * 0.56, Math.max(start.y, 0.38), -0.44),
                        new Vec3(side * 1.28, start.y, -0.44),
                        null, null, null, seatPosition);
            }
            case "tom_6" -> {
                double side = start.x < 0 ? -1 : 1;
                yield new EntryPortal(
                        new Vec3(side * 0.50, Math.max(start.y, 0.66), -0.45),
                        new Vec3(side * 1.15, start.y, -0.65),
                        null, null, null, seatPosition);
            }
            default -> null;
        };
    }

    private static EntryPortal boatsAndAirships(String model, int seat, Vec3 seatPosition, Vec3 start) {
        return switch (model) {
            case "speedboat" -> {
                double side = start.x < 0 ? -1 : 1;
                double z = seatPosition.z;
                yield new EntryPortal(
                        new Vec3(side * 0.98, Math.max(start.y, 0.94), z),
                        new Vec3(side * 1.66, start.y, z),
                        new Vec3(side * 1.24, Math.max(start.y, 0.58), z),
                        null, null, seatPosition);
            }
            case "tiny_speedboat" -> {
                double side = start.x < 0 ? -1 : 1;
                yield new EntryPortal(
                        new Vec3(side * 0.62, Math.max(start.y, 0.44), -0.06),
                        new Vec3(side * 1.22, start.y, -0.06),
                        null, null, null, seatPosition);
            }
            case "air_sheep" -> {
                double side = start.x < 0 ? -1 : 1;
                if (seat == 1) {
                    yield new EntryPortal(
                            new Vec3(side * 1.10, 4.75, -0.30),
                            new Vec3(side * 1.95, start.y, -0.30),
                            null, null, null, seatPosition);
                }
                yield new EntryPortal(
                        new Vec3(side * 0.66, Math.max(start.y, 0.48), -0.30),
                        new Vec3(side * 1.26, start.y, -0.30),
                        null, null, null, seatPosition);
            }
            case "happiest_ghast" -> {
                double side = start.x < 0 ? -1 : 1;
                // Side cannons occupy Z=[-0.75..+1.00], |X|=[1.82..2.98], Y=[1.53..2.39].
                // ALWAYS climb up/down along rear-side clear wall Z=-1.55 (0.80m behind cannon, beside rear tentacles)!
                double wallZ = -1.55;
                yield new EntryPortal(
                        new Vec3(side * 1.48, Math.max(start.y, 4.16), wallZ),
                        new Vec3(side * 2.46, start.y, wallZ),
                        new Vec3(side * 2.36, Math.max(start.y, 2.05), wallZ),
                        new Vec3(side * 2.32, Math.max(start.y, 4.02), wallZ),
                        null,
                        new Vec3(0.0, 4.00, wallZ));
            }
            case "kirov" -> {
                double side = start.x < seatPosition.x ? -1 : 1;
                yield new EntryPortal(
                        new Vec3(side * 2.22, Math.max(start.y, 7.76), 14.20),
                        new Vec3(side * 3.35, start.y, 13.70),
                        new Vec3(side * 2.75, Math.max(start.y, 7.72), 14.00),
                        new Vec3(side * 1.05, Math.max(start.y, 7.74), 14.55),
                        null,
                        new Vec3(0.0, 8.20, 14.55));
            }
            default -> null;
        };
    }

    private static EntryPortal emplacements(String model, int seat, Vec3 seatPosition, Vec3 start) {
        double side = start.x < 0 ? -1 : 1;
        return switch (model) {
            case "annihilator" -> new EntryPortal(
                    new Vec3(side * 1.20, Math.max(start.y, 3.92), 1.00),
                    new Vec3(side * 6.20, start.y, 1.00),
                    new Vec3(side * 4.80, Math.max(start.y, 3.78), 1.00),
                    new Vec3(side * 2.60, Math.max(start.y, 3.85), 1.00),
                    new Vec3(0.0, 3.55, 1.50),
                    new Vec3(0.0, 4.00, 1.50));
            case "bl_132" -> new EntryPortal(
                    new Vec3(side * 1.15, Math.max(start.y, 4.15), -2.00),
                    new Vec3(side * 4.78, start.y, -2.00),
                    new Vec3(side * 3.60, Math.max(start.y, 4.02), -2.00),
                    new Vec3(side * 2.15, Math.max(start.y, 4.08), -2.00),
                    new Vec3(0.0, 3.85, -2.00),
                    new Vec3(0.0, 4.20, -2.00));
            case "mk_42" -> new EntryPortal(
                    new Vec3(side * 0.72, Math.max(start.y, 2.42), -0.35),
                    new Vec3(side * 2.55, start.y, -0.35),
                    new Vec3(side * 1.95, Math.max(start.y, 1.35), -0.35),
                    new Vec3(side * 1.32, Math.max(start.y, 2.34), -0.35),
                    new Vec3(0.0, 2.15, 0.0),
                    new Vec3(0.0, 2.45, 0.0));
            case "mle_1934" -> new EntryPortal(
                    new Vec3(side * 0.85, Math.max(start.y, 2.18), -0.25),
                    new Vec3(side * 2.95, start.y, -0.25),
                    new Vec3(side * 2.25, Math.max(start.y, 1.12), -0.25),
                    new Vec3(side * 1.55, Math.max(start.y, 2.08), -0.25),
                    new Vec3(0.0, 1.85, 0.35),
                    new Vec3(0.0, 2.20, 0.35));
            case "hpj_11" -> new EntryPortal(
                    new Vec3(0.0, Math.max(start.y, 0.25), -1.15),
                    new Vec3(0.0, start.y, -2.05),
                    new Vec3(0.0, Math.max(start.y, 0.22), -1.55),
                    null,
                    new Vec3(0.0, Math.max(start.y, 0.16), -0.25),
                    new Vec3(0.0, 0.85, 0.0));
            case "tow" -> new EntryPortal(
                    new Vec3(0.42, start.y, -0.82),
                    new Vec3(0.95, start.y, -1.12),
                    null, null, null, new Vec3(0.0, 0.90, 0.0));
            default -> null;
        };
    }

    private static Vec3 modelPoint(VehicleEntity vehicle, double x, double y, double z) {
        var point = vehicle.transformPosition(vehicle.getTransformFromString("Vehicle"), x, y, z);
        return EntryPath.toLocal(vehicle, new Vec3(point.x, point.y, point.z));
    }

    private static Vec3 turretPoint(VehicleEntity vehicle, double x, double y, double z) {
        var point = vehicle.transformPosition(vehicle.getTransformFromString("Turret"), x, y, z);
        return EntryPath.toLocal(vehicle, new Vec3(point.x, point.y, point.z));
    }
}
