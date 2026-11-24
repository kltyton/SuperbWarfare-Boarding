package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;

enum BoardingProfile {
    STEP(24), CAB(30), CHAIR(24), DECK(28), HATCH(56), COCKPIT(72), MI28_DOOR(64), LADDER(72), RAMP(56),
    BRADLEY_DOOR(64), T90_HATCH(76), JU87_COCKPIT(68), GHAST_CLIMB(76);

    private final int ticks;

    BoardingProfile(int ticks) {
        this.ticks = ticks;
    }

    int ticks() {
        return ticks;
    }

    boolean climbs() {
        return this == HATCH || this == COCKPIT || this == MI28_DOOR || this == LADDER || this == T90_HATCH
                || this == JU87_COCKPIT || this == GHAST_CLIMB;
    }

    static BoardingProfile of(VehicleEntity vehicle, int seat, double seatRise) {
        if (vehicle.computed().getEngineType() == EngineType.WHEELCHAIR) return CHAIR;
        String model = VehicleRigCatalog.id(vehicle);
        return switch (model) {
            case "bradley" -> BRADLEY_DOOR;
            case "lav_25", "lav_ad" -> RAMP;
            case "ju_87" -> JU87_COCKPIT;
            case "t_90a", "prism_tank", "bmp_2", "lav_150", "m_1a_2", "ztz_99a", "plz_05" ->
                    seatRise > 0.70 ? T90_HATCH : HATCH;
            case "yx_100" -> seatRise > 1.30 ? T90_HATCH : HATCH;
            case "a_10a", "j_16" -> COCKPIT;
            case "mi_28" -> MI28_DOOR;
            case "ah_6", "kv_16", "tom_6", "tow", "hpj_11", "vehicle_assembling_table" -> STEP;
            case "happiest_ghast" -> seatRise > 1.25 ? GHAST_CLIMB : DECK;
            case "air_sheep" -> seat == 1 && seatRise > 1.25 ? GHAST_CLIMB : DECK;
            case "speedboat", "tiny_speedboat" -> DECK;
            case "kirov", "ac_130h" -> seatRise > 1.25 ? LADDER : DECK;
            case "annihilator", "bl_132", "mk_42", "mle_1934" -> seatRise > 1.25 ? LADDER : DECK;
            default -> fallback(vehicle, seat, seatRise);
        };
    }

    private static BoardingProfile fallback(VehicleEntity vehicle, int seat, double seatRise) {
        VehicleType type = vehicle.computed().getType();
        return switch (type.name()) {
            case "BOAT" -> DECK;
            case "ARTILLERY", "DEFENSE" -> seatRise > 1.30 ? LADDER : STEP;
            case "AIRPLANE", "HELICOPTER" -> seatRise > 2.6 ? LADDER : seatRise > 0.9 ? COCKPIT : STEP;
            case "AIRSHIP" -> seatRise <= 1.1 ? DECK : LADDER;
            case "TANK", "APC", "AA" -> HATCH;
            case "CAR" -> seatRise > 1.35 ? LADDER : vehicle.isEnclosed(seat) || seatRise > 0.85 ? CAB : STEP;
            default -> seatRise > 1.35 ? CAB : STEP;
        };
    }

    String animation() {
        return "animation.boarding." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
