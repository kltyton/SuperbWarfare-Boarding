package com.kltyton.superbwarfareboarding.client.pose;

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Vehicle-class-specific seated riding & driving poses replacing Minecraft's stiff straight-leg vanilla sit.
 * Computes exact 3D FK-welded transforms (in Minecraft ModelPart pixel/degree space, matching PAL + BendableCuboids
 * and Forge BendyLib) so knees bend naturally down to floorboards/pedals/footrests and hands grip steering wheels,
 * flight sticks, throttles, turret handles, tank levers, or wheelchair push-rims.
 */
public final class VehicleRidingPose {
    public enum Posture {
        STEERING_WHEEL,
        FLIGHT_STICK,
        TANK_CREW,
        TURRET_GUNNER,
        WHEELCHAIR,
        HELM_OR_SADDLE,
        PASSENGER
    }

    public record BonePose(float posX, float posY, float posZ,
                           float rotX, float rotY, float rotZ,
                           float bendX, float bendY, float bendZ) {}

    public record Snapshot(Posture posture,
                           float headPosX, float headPosY, float headPosZ,
                           BonePose torso,
                           BonePose leftArm,
                           BonePose rightArm,
                           BonePose leftLeg,
                           BonePose rightLeg) {}

    private VehicleRidingPose() {}

    public static Posture classify(VehicleEntity vehicle, int seat) {
        if (vehicle.computed().getEngineType() == EngineType.WHEELCHAIR) return Posture.WHEELCHAIR;
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        String id = key.getNamespace().equals("superbwarfare") ? key.getPath() : "";
        boolean driver = seat <= 0;

        return switch (id) {
            case "wheel_chair" -> Posture.WHEELCHAIR;
            case "sodayo_pick_up", "sodayo_pick_up_hmg", "sodayo_pick_up_rocket", "sodayo_pick_up_tow" -> {
                if (seat == 0) yield Posture.STEERING_WHEEL;
                if (seat == 1) yield Posture.PASSENGER;
                yield Posture.TURRET_GUNNER;
            }
            case "truck" -> driver ? Posture.STEERING_WHEEL : Posture.PASSENGER;
            case "fh_77bw" -> {
                if (seat == 0) yield Posture.STEERING_WHEEL;
                if (seat == 1) yield Posture.TURRET_GUNNER;
                yield Posture.PASSENGER;
            }
            case "j_16", "a_10a", "ju_87", "mi_28", "ah_6", "tom_6", "kv_16" ->
                    driver ? Posture.FLIGHT_STICK : (seat == 1 && (id.equals("mi_28") || id.equals("ju_87"))
                            ? Posture.TURRET_GUNNER : Posture.PASSENGER);
            case "t_90a", "m_1a_2", "ztz_99a", "plz_05", "bmp_2", "bradley", "lav_25", "lav_ad",
                 "lav_150", "prism_tank", "yx_100" -> Posture.TANK_CREW;
            case "tow", "hpj_11", "mk_42", "mle_1934", "bl_132", "annihilator" -> Posture.TURRET_GUNNER;
            case "speedboat", "tiny_speedboat", "kirov", "ac_130h", "happiest_ghast", "air_sheep" ->
                    driver ? Posture.HELM_OR_SADDLE : Posture.PASSENGER;
            default -> fallback(vehicle.computed().getType(), driver);
        };
    }

    private static Posture fallback(VehicleType type, boolean driver) {
        return switch (type.name()) {
            case "CAR" -> driver ? Posture.STEERING_WHEEL : Posture.PASSENGER;
            case "AIRPLANE", "HELICOPTER" -> driver ? Posture.FLIGHT_STICK : Posture.PASSENGER;
            case "TANK", "APC", "AA" -> Posture.TANK_CREW;
            case "ARTILLERY", "DEFENSE" -> Posture.TURRET_GUNNER;
            case "BOAT", "AIRSHIP" -> driver ? Posture.HELM_OR_SADDLE : Posture.PASSENGER;
            default -> Posture.PASSENGER;
        };
    }

    /** Evaluates the complete FK-welded riding posture with subtle live steering / flight / wheel motion. */
    public static Snapshot sample(Player rider, VehicleEntity vehicle, float partialTick) {
        int seat = vehicle.getSeatIndex(rider);
        Posture posture = classify(vehicle, seat);

        // Live motion inputs (smooth, bounded, zero jitter)
        float yawRate = Mth.clamp(Mth.wrapDegrees(vehicle.getYRot() - vehicle.yRotO) / 5.0f, -1.0f, 1.0f);
        float pitchDeg = Mth.clamp(vehicle.getXRot(), -30.0f, 30.0f) / 30.0f;
        double speed = vehicle.getDeltaMovement().horizontalDistance();
        float time = (rider.tickCount + partialTick) * 0.08f;
        float breathe = Mth.sin(time) * 0.7f;

        float tRx = 0f, tRy = 0f, tRz = 0f, tBendX = 0f;
        float laRx = 0f, laRy = 0f, laRz = 0f, laBend = 0f;
        float raRx = 0f, raRy = 0f, raRz = 0f, raBend = 0f;
        float llRx = 0f, llRy = 0f, llRz = 0f, llBend = 0f;
        float rlRx = 0f, rlRy = 0f, rlRz = 0f, rlBend = 0f;

        switch (posture) {
            case STEERING_WHEEL -> {
                // Upright driver posture, knees bent down to pedals, hands gripping steering wheel with live turn!
                tRx = -3.0f + breathe * 0.4f;
                tRz = yawRate * -3.5f;
                tBendX = 4.0f;
                float steerShift = yawRate * 14.0f;
                laRx = -56.0f - steerShift;
                laRy = 8.0f + yawRate * 4.0f;
                laRz = -7.0f;
                laBend = -34.0f + steerShift * 0.35f;

                raRx = -56.0f + steerShift;
                raRy = -8.0f + yawRate * 4.0f;
                raRz = 7.0f;
                raBend = -34.0f - steerShift * 0.35f;

                llRx = -74.0f; llRy = 3.0f;  llRz = -5.5f; llBend = 78.0f;
                rlRx = -74.0f; rlRy = -3.0f; rlRz = 5.5f;  rlBend = 76.0f;
            }
            case FLIGHT_STICK -> {
                // Reclined fighter/helicopter pilot: right hand on flight stick, left hand on throttle, feet on rudder pedals!
                tRx = -9.5f + breathe * 0.35f;
                tRz = yawRate * -4.0f;
                tBendX = 4.5f;
                // Left hand on side throttle quadrant
                laRx = -38.0f - (float) Math.min(10.0, speed * 8.0);
                laRy = 4.0f;
                laRz = -13.0f;
                laBend = -48.0f;
                // Right hand on flight stick (responds subtly to pitch & yaw!)
                raRx = -44.0f + pitchDeg * 9.0f;
                raRy = -11.0f + yawRate * 6.0f;
                raRz = 6.0f + yawRate * 5.0f;
                raBend = -56.0f - pitchDeg * 5.0f;

                llRx = -68.0f; llRy = 2.0f;  llRz = -6.0f; llBend = 66.0f;
                rlRx = -68.0f; rlRy = -2.0f; rlRz = 6.0f;  rlBend = 66.0f;
            }
            case TANK_CREW -> {
                // Compact armored-seat posture: tightly tucked knees inside hull, hands forward on twin levers/optics!
                tRx = 4.0f + breathe * 0.3f;
                tBendX = 5.0f;
                laRx = -46.0f - yawRate * 8.0f; laRy = 7.0f;  laRz = -6.0f; laBend = -52.0f;
                raRx = -46.0f + yawRate * 8.0f; raRy = -7.0f; raRz = 6.0f;  raBend = -52.0f;

                llRx = -72.0f; llRy = 2.0f;  llRz = -5.0f; llBend = 86.0f;
                rlRx = -72.0f; rlRy = -2.0f; rlRz = 5.0f;  rlBend = 86.0f;
            }
            case TURRET_GUNNER -> {
                // Focused gunner posture: slight forward lean, braced bent knees, both hands gripping dual turret handles!
                tRx = 9.0f + breathe * 0.35f;
                tBendX = 5.5f;
                laRx = -62.0f; laRy = 9.0f;  laRz = -6.5f; laBend = -42.0f;
                raRx = -62.0f; raRy = -9.0f; raRz = 6.5f;  raBend = -42.0f;

                llRx = -66.0f; llRy = 3.0f;  llRz = -8.0f; llBend = 72.0f;
                rlRx = -66.0f; rlRy = -3.0f; rlRz = 8.0f;  rlBend = 72.0f;
            }
            case WHEELCHAIR -> {
                // Wheelchair posture: bent knees resting on footrests, hands on side wheel push-rims with push cycle when rolling!
                float pushWave = speed > 0.02 ? Mth.sin((rider.tickCount + partialTick) * 0.35f) * 14.0f : 0.0f;
                tRx = 3.0f + Math.max(0.0f, pushWave * 0.25f) + breathe * 0.4f;
                tBendX = 3.5f;
                laRx = -24.0f - pushWave; laRy = 0.0f; laRz = -16.0f; laBend = -52.0f + pushWave * 0.4f;
                raRx = -24.0f - pushWave; raRy = 0.0f; raRz = 16.0f;  raBend = -52.0f + pushWave * 0.4f;

                llRx = -76.0f; llRy = 1.5f;  llRz = -4.0f; llBend = 84.0f;
                rlRx = -76.0f; rlRy = -1.5f; rlRz = 4.0f;  rlBend = 84.0f;
            }
            case HELM_OR_SADDLE -> {
                // Boat helm / airship rudder / ghast saddle: athletic seated helm grip with natural bent knees!
                tRx = 5.0f + breathe * 0.4f;
                tRz = yawRate * -4.0f;
                tBendX = 4.0f;
                float helmShift = yawRate * 10.0f;
                laRx = -52.0f - helmShift; laRy = 6.0f;  laRz = -8.0f; laBend = -38.0f;
                raRx = -52.0f + helmShift; raRy = -6.0f; raRz = 8.0f;  raBend = -38.0f;

                llRx = -70.0f; llRy = 3.0f;  llRz = -7.0f; llBend = 74.0f;
                rlRx = -70.0f; rlRy = -3.0f; rlRz = 7.0f;  rlBend = 74.0f;
            }
            case PASSENGER -> {
                // Relaxed passenger: natural bent knees down on floorboard, hands resting comfortably on knees/lap!
                tRx = -2.0f + breathe * 0.45f;
                tBendX = 3.0f;
                laRx = -30.0f; laRy = 8.0f;  laRz = -6.0f; laBend = -48.0f;
                raRx = -30.0f; raRy = -8.0f; raRz = 6.0f;  raBend = -48.0f;

                llRx = -74.0f; llRy = 2.0f;  llRz = -5.0f; llBend = 80.0f;
                rlRx = -74.0f; rlRy = -2.0f; rlRz = 5.0f;  rlBend = 80.0f;
            }
        }

        return solveWelded(posture, tRx, tRy, tRz, tBendX,
                laRx, laRy, laRz, laBend,
                raRx, raRy, raRz, raBend,
                llRx, llRy, llRz, llBend,
                rlRx, rlRy, rlRz, rlBend);
    }

    /**
     * Exact 3D FK welding around seated waist p_hip = (0, 12, 0) (matching pal_fk.py).
     * Guarantees zero shoulder or neck detachment when torso reclines or leans forward!
     */
    private static Snapshot solveWelded(Posture posture,
                                        float tRx, float tRy, float tRz, float tBendX,
                                        float laRx, float laRy, float laRz, float laBend,
                                        float raRx, float raRy, float raRz, float raBend,
                                        float llRx, float llRy, float llRz, float llBend,
                                        float rlRx, float rlRy, float rlRz, float rlBend) {
        float[][] rBody = rotZyx(tRx, tRy, tRz);
        float[] waistVec = mul(rBody, 0f, 12f, 0f);
        float bodyX = -waistVec[0];
        float bodyY = 12f - waistVec[1];
        float bodyZ = -waistVec[2];

        float[] midVec = mul(rBody, 0f, 6f, 0f);
        float midX = bodyX + midVec[0];
        float midY = bodyY + midVec[1];
        float midZ = bodyZ + midVec[2];

        float[][] rUpper = mulMat(rBody, rotZyx(tBendX, 0f, 0f));
        float[] neckOff = mul(rUpper, 0f, -6f, 0f);
        float headX = midX + neckOff[0];
        float headY = midY + neckOff[1];
        float headZ = midZ + neckOff[2];

        float[] laOff = mul(rUpper, 5f, -4f, 0f);
        float[] raOff = mul(rUpper, -5f, -4f, 0f);

        // Convert ModelPart absolute positions -> PAL delta positions: (p.x - init.x, -(p.y - init.y), p.z - init.z)
        BonePose torso = new BonePose(bodyX, -bodyY, bodyZ, tRx, tRy, tRz, tBendX, 0f, 0f);
        BonePose leftArm = new BonePose(midX + laOff[0] - 5f, -(midY + laOff[1] - 2f), midZ + laOff[2],
                laRx, laRy, laRz, laBend, 0f, 0f);
        BonePose rightArm = new BonePose(midX + raOff[0] + 5f, -(midY + raOff[1] - 2f), midZ + raOff[2],
                raRx, raRy, raRz, raBend, 0f, 0f);
        BonePose leftLeg = new BonePose(0f, 0f, 0f, llRx, llRy, llRz, llBend, 0f, 0f);
        BonePose rightLeg = new BonePose(0f, 0f, 0f, rlRx, rlRy, rlRz, rlBend, 0f, 0f);

        return new Snapshot(posture, headX, -headY, headZ, torso, leftArm, rightArm, leftLeg, rightLeg);
    }

    private static float[][] rotZyx(float rxDeg, float ryDeg, float rzDeg) {
        float ax = (float) Math.toRadians(rxDeg);
        float ay = (float) Math.toRadians(ryDeg);
        float az = (float) Math.toRadians(rzDeg);
        float cx = Mth.cos(ax), sx = Mth.sin(ax);
        float cy = Mth.cos(ay), sy = Mth.sin(ay);
        float cz = Mth.cos(az), sz = Mth.sin(az);
        float[][] rx = {{1, 0, 0}, {0, cx, -sx}, {0, sx, cx}};
        float[][] ry = {{cy, 0, sy}, {0, 1, 0}, {-sy, 0, cy}};
        float[][] rz = {{cz, -sz, 0}, {sz, cz, 0}, {0, 0, 1}};
        return mulMat(rz, mulMat(ry, rx));
    }

    private static float[][] mulMat(float[][] a, float[][] b) {
        float[][] out = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return out;
    }

    private static float[] mul(float[][] m, float x, float y, float z) {
        return new float[]{
                m[0][0] * x + m[0][1] * y + m[0][2] * z,
                m[1][0] * x + m[1][1] * y + m[1][2] * z,
                m[2][0] * x + m[2][1] * y + m[2][2] * z
        };
    }
}
