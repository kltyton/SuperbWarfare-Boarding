package com.kltyton.superbwarfareboarding.boarding.surface;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.superbwarfareboarding.boarding.EntryPath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Universal visual-surface collision & support solver derived from Superb Warfare's
 * real .geo.json models (including live rotated turret solids).
 * Ensures ground approach footprints, exterior stand-off positions, fender/wing/gunwale climbs,
 * and turret-cheek transitions NEVER clip through any vehicle's visual surface.
 */
public final class SurfaceClearance {
    /** Full player half-width plus visual skin safety margin (0.30m player half + 0.06m margin). */
    public static final double BODY_RADIUS = 0.36;
    public static final double FOOT_RADIUS = 0.24;
    public static final double PLAYER_HEIGHT = 1.75;

    private SurfaceClearance() {}

    public static String vehicleId(VehicleEntity vehicle) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        return key.getNamespace().equals("superbwarfare") ? key.getPath() : "";
    }

    public static VisualSurfaceCatalog.VehicleSurface surfaceOf(VehicleEntity vehicle) {
        return VisualSurfaceCatalog.get(vehicleId(vehicle));
    }

    /** Authoritative ground-level visual footprint for EntryPath (covers hoods, noses, tracks, bumpers, tails). */
    public static AABB visualFootprint(VehicleEntity vehicle) {
        var surface = surfaceOf(vehicle);
        return surface != null ? surface.groundFootprint() : null;
    }

    /** Authoritative low-wing / sponson / appendage visual approach obstacles. */
    public static List<AABB> visualApproachObstacles(VehicleEntity vehicle) {
        var surface = surfaceOf(vehicle);
        return surface != null ? surface.approachObstacles() : List.of();
    }

    /** Returns all visual solid boxes of this vehicle in vehicle-local space, with turret solids live-rotated. */
    public static List<AABB> allSolidsLocal(VehicleEntity vehicle) {
        var surface = surfaceOf(vehicle);
        if (surface == null) return List.of();
        if (surface.turretSolids().isEmpty()) return surface.hullSolids();
        var out = new ArrayList<AABB>(surface.hullSolids().size() + surface.turretSolids().size());
        out.addAll(surface.hullSolids());
        out.addAll(turretSolidsLocal(vehicle, surface));
        return out;
    }

    /** Returns only the live-rotated turret solid boxes in vehicle-local space. */
    public static List<AABB> turretSolidsLocal(VehicleEntity vehicle) {
        var surface = surfaceOf(vehicle);
        if (surface == null || surface.turretSolids().isEmpty()) return List.of();
        return turretSolidsLocal(vehicle, surface);
    }

    private static List<AABB> turretSolidsLocal(VehicleEntity vehicle, VisualSurfaceCatalog.VehicleSurface surface) {
        var transform = vehicle.getTransformFromString("Turret");
        Vec3 origin = toLocalTurret(vehicle, transform, 0, 0, 0);
        Vec3 axisX = toLocalTurret(vehicle, transform, 1, 0, 0).subtract(origin);
        Vec3 axisY = toLocalTurret(vehicle, transform, 0, 1, 0).subtract(origin);
        Vec3 axisZ = toLocalTurret(vehicle, transform, 0, 0, 1).subtract(origin);
        var rotated = new ArrayList<AABB>(surface.turretSolids().size());
        for (AABB box : surface.turretSolids()) {
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            double[] xs = {box.minX, box.maxX};
            double[] ys = {box.minY, box.maxY};
            double[] zs = {box.minZ, box.maxZ};
            for (double x : xs) {
                for (double y : ys) {
                    for (double z : zs) {
                        double wx = origin.x + axisX.x * x + axisY.x * y + axisZ.x * z;
                        double wy = origin.y + axisX.y * x + axisY.y * y + axisZ.y * z;
                        double wz = origin.z + axisX.z * x + axisY.z * y + axisZ.z * z;
                        if (wx < minX) minX = wx;
                        if (wx > maxX) maxX = wx;
                        if (wy < minY) minY = wy;
                        if (wy > maxY) maxY = wy;
                        if (wz < minZ) minZ = wz;
                        if (wz > maxZ) maxZ = wz;
                    }
                }
            }
            rotated.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        }
        return rotated;
    }

    private static Vec3 toLocalTurret(VehicleEntity vehicle,
                                      org.joml.Matrix4d transform,
                                      double x, double y, double z) {
        var p = vehicle.transformPosition(transform, x, y, z);
        return EntryPath.toLocal(vehicle, new Vec3(p.x, p.y, p.z));
    }

    /**
     * Highest solid visual surface top Y under a player's feet at (x, z), bounded to realistic
     * surface steps within [refY - 0.65, refY + maxStepUp] so overhead cab roofs never pull a doorway up.
     */
    public static double supportTopY(List<AABB> solids, double x, double z, double refY,
                                     double footRadius, double maxStepUp) {
        double best = refY;
        for (AABB box : solids) {
            if (x + footRadius < box.minX || x - footRadius > box.maxX) continue;
            if (z + footRadius < box.minZ || z - footRadius > box.maxZ) continue;
            if (box.maxY >= refY - 0.65 && box.maxY <= refY + maxStepUp) {
                if (box.maxY + 0.02 > best) best = box.maxY + 0.02;
            }
        }
        return best;
    }

    /**
     * Returns true if an upright player body at `pos` with horizontal radius `radius`
     * penetrates any solid box in `solids` across vertical span [pos.y + 0.16, pos.y + 1.65].
     */
    public static boolean bodyHitsAny(List<AABB> solids, Vec3 pos, double radius) {
        double y0 = pos.y + 0.16;
        double y1 = pos.y + PLAYER_HEIGHT;
        for (AABB box : solids) {
            if (box.maxY <= y0 || box.minY >= y1) continue;
            if (pos.x + radius > box.minX && pos.x - radius < box.maxX
                    && pos.z + radius > box.minZ && pos.z - radius < box.maxZ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pushes an exterior ground/standoff point cleanly outside any penetrating visual solid wall
     * along the outward direction away from `interiorRef`.
     */
    public static Vec3 pushClearOutward(List<AABB> solids, Vec3 point, Vec3 interiorRef, double radius) {
        if (solids.isEmpty() || !bodyHitsAny(solids, point, radius)) return point;
        Vec3 dir = new Vec3(point.x - interiorRef.x, 0, point.z - interiorRef.z);
        if (dir.horizontalDistanceSqr() < 1.0E-4) {
            dir = new Vec3(point.x >= 0 ? 1 : -1, 0, 0);
        } else {
            dir = dir.normalize();
        }
        for (double step = 0.12; step <= 2.40; step += 0.12) {
            Vec3 candidate = new Vec3(point.x + dir.x * step, point.y, point.z + dir.z * step);
            if (!bodyHitsAny(solids, candidate, radius)) return candidate;
        }
        return point;
    }

    /**
     * Computes a guaranteed cheek-clear turret-climb base point on the fender/hull deck:
     * marches from `onFender` toward `besideHatch` and stops cleanly OUTSIDE every live-rotated
     * turret solid box (with full BODY_RADIUS clearance) before climbing straight UP onto the turret roof!
     */
    public static Vec3 findTurretClimbBase(List<AABB> turretSolids, Vec3 onFender, Vec3 besideHatch) {
        if (turretSolids.isEmpty()) {
            return new Vec3(onFender.x, onFender.y, onFender.z + (besideHatch.z - onFender.z) * 0.45);
        }
        double cheekClear = BODY_RADIUS + 0.10;
        Vec3 bestClear = onFender;
        for (int i = 1; i <= 20; i++) {
            double frac = i / 20.0;
            Vec3 cand = new Vec3(
                    onFender.x,
                    onFender.y,
                    onFender.z + (besideHatch.z - onFender.z) * frac);
            if (bodyHitsAny(turretSolids, cand, cheekClear)) {
                break;
            }
            bestClear = cand;
        }
        if (bodyHitsAny(turretSolids, bestClear, cheekClear)) {
            bestClear = pushClearOutward(turretSolids, bestClear, besideHatch, cheekClear);
        }
        return bestClear;
    }

    /**
     * Finds the highest solid obstacle roof height blocking the horizontal segment from `a` to `b`
     * at player height `[min(a.y, b.y) + 0.18, min(a.y, b.y) + 1.65]`.
     * Returns Double.NEGATIVE_INFINITY if the segment is completely clear.
     */
    public static double maxBlockingTopY(List<AABB> solids, Vec3 a, Vec3 b, double radius) {
        double maxTop = Double.NEGATIVE_INFINITY;
        for (int i = 1; i <= 8; i++) {
            double t = i / 9.0;
            Vec3 p = a.lerp(b, t);
            double y0 = p.y + 0.22;
            double y1 = p.y + PLAYER_HEIGHT;
            for (AABB box : solids) {
                if (box.maxY <= y0 || box.minY >= y1) continue;
                if (p.x + radius > box.minX && p.x - radius < box.maxX
                        && p.z + radius > box.minZ && p.z - radius < box.maxZ) {
                    if (box.maxY > maxTop) maxTop = box.maxY;
                }
            }
        }
        return maxTop;
    }
}
