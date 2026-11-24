package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The model-anchored doorway, hatch, step, and interior points for a vehicle seat. */
record EntryPortal(Vec3 lip, Vec3 approachHint, Vec3 exteriorStep, Vec3 foothold, Vec3 interior, Vec3 aimPoint) {
    static EntryPortal resolve(VehicleEntity vehicle, int seat, Vec3 seatPosition, Vec3 start) {
        return VehicleRigCatalog.resolve(vehicle, seat, seatPosition, start);
    }

    Vec3 entrance(AABB bounds, Vec3 hint, Vec3 start) {
        return approachHint != null
                ? EntryPath.outside(bounds, new Vec3(approachHint.x, start.y, approachHint.z), lip)
                : EntryPath.entrance(bounds, lip, hint, start);
    }

    Vec3 aim() {
        return aimPoint != null ? aimPoint : lip;
    }

    EntryPortal oppositeSide(VehicleEntity vehicle) {
        if (!VehicleRigCatalog.hasSymmetricSide(vehicle)) return null;
        return new EntryPortal(
                new Vec3(-lip.x, lip.y, lip.z),
                approachHint == null ? null : new Vec3(-approachHint.x, approachHint.y, approachHint.z),
                exteriorStep == null ? null : new Vec3(-exteriorStep.x, exteriorStep.y, exteriorStep.z),
                foothold == null ? null : new Vec3(-foothold.x, foothold.y, foothold.z),
                interior == null ? null : new Vec3(-interior.x, interior.y, interior.z),
                aimPoint);
    }
}
