package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import com.kltyton.interactiveanimation.scene.playback.SceneCollision;
import com.kltyton.superbwarfareboarding.boarding.surface.SurfaceClearance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finds the shortest walk to the entrance along the outside of the vehicle's footprint. */
public final class EntryPath {
    private EntryPath() {}

    public static AABB footprint(VehicleEntity vehicle) {
        AABB visual = SurfaceClearance.visualFootprint(vehicle);
        if (visual != null) return visual;
        var corners = new ArrayList<Vec3>(16);
        var obbs = vehicle.getOBBs();
        if (obbs != null && !obbs.isEmpty()) {
            double vehY = vehicle.getY();
            for (OBB part : obbs) {
                AABB world = OBB.getWorldAABB(part);
                if (world.minY > vehY + 2.05) continue;
                for (var point : part.getVertices()) corners.add(new Vec3(point.x, point.y, point.z));
            }
        }
        if (corners.isEmpty()) {
            var obb = vehicle.getCollisionOBB();
            if (obb != null) {
                for (var point : obb.getVertices()) corners.add(new Vec3(point.x, point.y, point.z));
            } else {
                AABB box = vehicle.getBoundingBox();
                for (double x : new double[]{box.minX, box.maxX}) {
                    for (double z : new double[]{box.minZ, box.maxZ}) corners.add(new Vec3(x, box.minY, z));
                }
            }
        }
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Vec3 point : corners) {
            Vec3 local = toLocal(vehicle, point);
            minX = Math.min(minX, local.x);
            maxX = Math.max(maxX, local.x);
            minZ = Math.min(minZ, local.z);
            maxZ = Math.max(maxZ, local.z);
            minY = Math.min(minY, local.y);
            maxY = Math.max(maxY, local.y);
        }
        if (maxY < 0.5) maxY = vehicle.getBoundingBox().maxY - vehicle.getY();
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Returns the exterior normal where the rider is presently approaching the vehicle. */
    public static Vec3 outward(VehicleEntity vehicle, Vec3 position, AABB footprint) {
        Vec3 point = toLocal(vehicle, position);
        double x = point.x - Mth.clamp(point.x, footprint.minX, footprint.maxX);
        double z = point.z - Mth.clamp(point.z, footprint.minZ, footprint.maxZ);
        return new Vec3(x, 0, z).yRot(-vehicle.getYRot() * ((float) Math.PI / 180));
    }

    static Vec3 outside(AABB bounds, Vec3 point, Vec3 reference) {
        if (!contains(bounds, point)) return point;
        Vec3 direction = point.subtract(reference);
        if (direction.horizontalDistanceSqr() < 0.0001) {
            Vec3 edge = nearestEdge(bounds, point);
            Vec3 push = edge.subtract(bounds.getCenter());
            return push.horizontalDistanceSqr() < 0.0001
                    ? edge.add(0.04, 0, 0)
                    : edge.add(new Vec3(push.x, 0, push.z).normalize().scale(0.04));
        }
        double tx = exitTime(point.x, direction.x, bounds.minX - 0.04, bounds.maxX + 0.04);
        double tz = exitTime(point.z, direction.z, bounds.minZ - 0.04, bounds.maxZ + 0.04);
        double t = Math.min(tx, tz);
        return new Vec3(point.x + t * direction.x, point.y, point.z + t * direction.z);
    }

    static Vec3 entrance(AABB bounds, Vec3 seat, Vec3 hint, Vec3 start) {
        Vec3 direction = hint == null ? start.subtract(seat) : hint.subtract(seat);
        if (direction.horizontalDistanceSqr() < 0.01) direction = start.subtract(seat);
        double dx = direction.x;
        double dz = direction.z;
        if (Math.abs(dx) + Math.abs(dz) < 0.01) dx = seat.x < bounds.getCenter().x ? -1 : 1;
        double tx = exitTime(seat.x, dx, bounds.minX, bounds.maxX);
        double tz = exitTime(seat.z, dz, bounds.minZ, bounds.maxZ);
        if (contains(bounds, seat)) {
            double t = Math.min(tx, tz);
            return new Vec3(seat.x + t * dx, start.y, seat.z + t * dz);
        }
        Vec3 exterior = new Vec3(seat.x + dx, start.y, seat.z + dz);
        return contains(bounds, exterior) ? nearestEdge(bounds, exterior) : exterior;
    }

    static List<Vec3> find(VehicleEntity vehicle, Player player, AABB bounds, Vec3 start, Vec3 entry) {
        double y = start.y;
        Vec3 exterior = contains(bounds, start) ? nearestEdge(bounds, start) : start;
        var nodes = new ArrayList<>(List.of(exterior, entry,
                new Vec3(bounds.minX, y, bounds.minZ), new Vec3(bounds.minX, y, bounds.maxZ),
                new Vec3(bounds.maxX, y, bounds.minZ), new Vec3(bounds.maxX, y, bounds.maxZ)));
        Vec3 fromWorld = toWorld(vehicle, start);
        Vec3 toWorld = toWorld(vehicle, entry);
        var obstacles = otherCarriers(vehicle, new AABB(fromWorld, toWorld).inflate(4.0, 2.2, 4.0));
        var approachParts = approachParts(vehicle);
        var localObstacles = localObstacles(vehicle, player, y);
        for (OBB part : approachParts) {
            addCorners(nodes, vehicle, player, fromWorld.y, y, OBB.getWorldAABB(part));
        }
        for (AABB wing : localObstacles) {
            double margin = player.getBbWidth() * 0.5 + 0.34;
            nodes.addAll(List.of(
                    new Vec3(wing.minX - margin, y, wing.minZ - margin),
                    new Vec3(wing.minX - margin, y, wing.maxZ + margin),
                    new Vec3(wing.maxX + margin, y, wing.minZ - margin),
                    new Vec3(wing.maxX + margin, y, wing.maxZ + margin),
                    new Vec3(start.x, y, wing.minZ - margin),
                    new Vec3(start.x, y, wing.maxZ + margin)));
        }
        for (VehicleEntity obstacle : obstacles) {
            var box = obstacle.getCollisionOBB();
            AABB shell = box == null ? obstacle.getBoundingBox() : OBB.getWorldAABB(box);
            addCorners(nodes, vehicle, player, fromWorld.y, y, shell);
        }
        AABB inside = new AABB(bounds.minX + 0.001, y - 1, bounds.minZ + 0.001,
                bounds.maxX - 0.001, y + 1, bounds.maxZ - 0.001);
        double[] distance = new double[nodes.size()];
        int[] previous = new int[nodes.size()];
        boolean[] visited = new boolean[nodes.size()];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        Arrays.fill(previous, -1);
        distance[0] = 0;
        if (exterior.distanceToSqr(start) > 0.000001
                && !clearWalk(vehicle, player, fromWorld, toWorld(vehicle, exterior), obstacles, approachParts, localObstacles)) {
            return List.of();
        }
        for (int count = 0; count < nodes.size(); count++) {
            int nearest = -1;
            for (int i = 0; i < nodes.size(); i++) {
                if (!visited[i] && (nearest < 0 || distance[i] < distance[nearest])) nearest = i;
            }
            if (nearest < 0 || !Double.isFinite(distance[nearest]) || nearest == 1) break;
            visited[nearest] = true;
            Vec3 from = nodes.get(nearest);
            for (int i = 1; i < nodes.size(); i++) {
                if (visited[i]) continue;
                Vec3 to = nodes.get(i);
                double cost = distance[nearest] + from.distanceTo(to);
                if (cost >= distance[i] || contains(inside, to) || inside.clip(from, to).isPresent()) continue;
                if (!clearWalk(vehicle, player, toWorld(vehicle, from), toWorld(vehicle, to), obstacles, approachParts, localObstacles)) {
                    continue;
                }
                distance[i] = cost;
                previous[i] = nearest;
            }
        }
        if (previous[1] < 0) return List.of();
        var route = new ArrayList<Vec3>();
        for (int i = 1; i >= 0; i = previous[i]) route.add(nodes.get(i));
        if (exterior.distanceToSqr(start) > 0.000001) route.add(start);
        Collections.reverse(route);
        return roundCorners(vehicle, player, route, obstacles, approachParts, localObstacles);
    }

    private static List<AABB> localObstacles(VehicleEntity vehicle, Player player, double localY) {
        var raw = SurfaceClearance.visualApproachObstacles(vehicle);
        if (raw.isEmpty()) return List.of();
        var blocking = new ArrayList<AABB>(raw.size());
        double riderTop = localY + player.getBbHeight();
        for (AABB box : raw) {
            if (box.maxY >= localY && box.minY <= riderTop) blocking.add(box);
        }
        return blocking;
    }

    private static void addCorners(List<Vec3> nodes, VehicleEntity vehicle, Player player, double worldY,
                                   double y, AABB shell) {
        if (shell.maxY < worldY || shell.minY > worldY + player.getBbHeight()) return;
        Vec3 a = toLocal(vehicle, new Vec3(shell.minX, worldY, shell.minZ));
        Vec3 b = toLocal(vehicle, new Vec3(shell.minX, worldY, shell.maxZ));
        Vec3 c = toLocal(vehicle, new Vec3(shell.maxX, worldY, shell.minZ));
        Vec3 d = toLocal(vehicle, new Vec3(shell.maxX, worldY, shell.maxZ));
        double margin = player.getBbWidth() * 0.5 + 0.36;
        double left = Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)) - margin;
        double right = Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)) + margin;
        double back = Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z)) - margin;
        double front = Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z)) + margin;
        nodes.addAll(List.of(new Vec3(left, y, back), new Vec3(left, y, front),
                new Vec3(right, y, back), new Vec3(right, y, front)));
    }

    private static List<Vec3> roundCorners(VehicleEntity vehicle, Player player, List<Vec3> route,
                                           List<VehicleEntity> obstacles, List<OBB> approachParts,
                                           List<AABB> localObstacles) {
        if (route.size() < 3) return List.copyOf(route);
        var rounded = new ArrayList<Vec3>();
        rounded.add(route.get(0));
        for (int i = 1; i < route.size() - 1; i++) {
            Vec3 corner = route.get(i);
            Vec3 previous = route.get(i - 1).subtract(corner);
            Vec3 next = route.get(i + 1).subtract(corner);
            double trim = Math.min(0.28, 0.32 * Math.min(previous.length(), next.length()));
            if (trim < 0.02) {
                rounded.add(corner);
                continue;
            }
            Vec3 entry = corner.add(previous.normalize().scale(trim));
            Vec3 exit = corner.add(next.normalize().scale(trim));
            for (int sample = 0; sample <= 5; sample++) {
                double time = sample / 5.0;
                rounded.add(entry.lerp(corner, time).lerp(corner.lerp(exit, time), time));
            }
        }
        rounded.add(route.get(route.size() - 1));
        for (int i = 1; i < rounded.size(); i++) {
            if (!clearWalk(vehicle, player, toWorld(vehicle, rounded.get(i - 1)), toWorld(vehicle, rounded.get(i)),
                    obstacles, approachParts, localObstacles)) {
                return List.copyOf(route);
            }
        }
        return List.copyOf(rounded);
    }

    static boolean clear(VehicleEntity vehicle, Player player, Vec3 from, Vec3 to) {
        var box = player.getBoundingBox().move(from.subtract(player.position())).expandTowards(to.subtract(from));
        return clear(player, from, to, otherCarriers(vehicle, box.inflate(0.7)));
    }

    /** Checks solid world blocks only (ignoring adjacent parked vehicles during an active exterior climb). */
    static boolean clearBlocksOnly(Player player, Vec3 from, Vec3 to) {
        return clear(player, from, to, List.of());
    }

    static boolean clearWalk(VehicleEntity vehicle, Player player, Vec3 from, Vec3 to) {
        var box = player.getBoundingBox().move(from.subtract(player.position())).expandTowards(to.subtract(from));
        return clearWalk(vehicle, player, from, to, otherCarriers(vehicle, box.inflate(0.7)),
                approachParts(vehicle), localObstacles(vehicle, player, toLocal(vehicle, from).y));
    }

    static List<OBB> approachParts(VehicleEntity vehicle) {
        return VehicleRigCatalog.id(vehicle).equals("ju_87") ? vehicle.getOBBs() : List.of();
    }

    private static boolean clearWalk(VehicleEntity vehicle, Player player, Vec3 from, Vec3 to,
                                     List<VehicleEntity> obstacles, List<OBB> parts, List<AABB> localObstacles) {
        if (!clear(player, from, to, obstacles)) return false;
        if (!localObstacles.isEmpty()) {
            Vec3 localFrom = toLocal(vehicle, from);
            Vec3 localTo = toLocal(vehicle, to);
            double halfW = player.getBbWidth() * 0.5;
            for (AABB box : localObstacles) {
                AABB inflated = new AABB(box.minX - halfW, localFrom.y - 1, box.minZ - halfW,
                        box.maxX + halfW, localFrom.y + 1, box.maxZ + halfW);
                if (contains(inflated, localFrom)) {
                    if (contains(inflated, localTo) && depthInside(inflated, localTo) > depthInside(inflated, localFrom) + 0.0001) {
                        return false;
                    }
                } else if (contains(inflated, localTo) || inflated.clip(localFrom, localTo).isPresent()) {
                    return false;
                }
            }
        }
        if (parts.isEmpty()) return true;
        var base = player.getBoundingBox().move(from.subtract(player.position())).deflate(0.018);
        Vec3 travel = to.subtract(from);
        var swept = base.expandTowards(travel);
        int steps = Math.max(1, (int) Math.ceil(travel.length() / 0.23));
        for (OBB part : parts) {
            if (!OBB.getWorldAABB(part).intersects(swept)) continue;
            for (int step = 1; step <= steps; step++) {
                if (OBB.isColliding(part, base.move(travel.scale((double) step / steps)))) return false;
            }
        }
        return true;
    }

    private static List<VehicleEntity> otherCarriers(VehicleEntity vehicle, AABB bounds) {
        return vehicle.level().getEntitiesOfClass(VehicleEntity.class, bounds, other -> other != vehicle && other.isAlive());
    }

    private static boolean clear(Player player, Vec3 from, Vec3 to, List<VehicleEntity> obstacles) {
        if (!SceneCollision.canApproach(player, from, to)) return false;
        if (obstacles.isEmpty()) return true;
        var base = player.getBoundingBox().move(from.subtract(player.position())).deflate(0.018);
        Vec3 travel = to.subtract(from);
        var swept = base.expandTowards(travel);
        int steps = Math.max(1, (int) Math.ceil(travel.length() / 0.23));
        for (VehicleEntity obstacle : obstacles) {
            if (!obstacle.getBoundingBox().intersects(swept)) continue;
            var parts = obstacle.getOBBs();
            for (int step = 1; step <= steps; step++) {
                var body = base.move(travel.scale((double) step / steps));
                if (parts.isEmpty() ? obstacle.getBoundingBox().intersects(body)
                        : parts.stream().anyMatch(part -> OBB.isColliding(part, body))) return false;
            }
        }
        return true;
    }

    public static Vec3 toLocal(VehicleEntity vehicle, Vec3 world) {
        return world.subtract(vehicle.position()).yRot(vehicle.getYRot() * ((float) Math.PI / 180));
    }

    static Vec3 toWorld(VehicleEntity vehicle, Vec3 local) {
        return vehicle.position().add(local.yRot(-vehicle.getYRot() * ((float) Math.PI / 180)));
    }

    static Vec3 standingPoint(Player player, Vec3 seat, double probeHeight) {
        if (seat.y >= probeHeight) return seat;
        Vec3 above = new Vec3(seat.x, probeHeight, seat.z);
        var box = player.getBoundingBox().move(above.subtract(player.position()));
        if (player.level().getBlockCollisions(player, box).iterator().hasNext()) return seat;
        Vec3 drop = Entity.collideBoundingBox(player, seat.subtract(above), box, player.level(), List.of());
        return above.add(drop);
    }

    private static double exitTime(double origin, double step, double min, double max) {
        if (step > 0) return (max - origin) / step;
        if (step < 0) return (min - origin) / step;
        return Double.POSITIVE_INFINITY;
    }

    private static boolean contains(AABB box, Vec3 point) {
        return point.x > box.minX && point.x < box.maxX && point.z > box.minZ && point.z < box.maxZ;
    }

    private static double depthInside(AABB box, Vec3 point) {
        return Math.min(Math.min(point.x - box.minX, box.maxX - point.x),
                Math.min(point.z - box.minZ, box.maxZ - point.z));
    }

    private static Vec3 nearestEdge(AABB bounds, Vec3 point) {
        List<Vec3> edges = List.of(new Vec3(bounds.minX, point.y, point.z), new Vec3(bounds.maxX, point.y, point.z),
                new Vec3(point.x, point.y, bounds.minZ), new Vec3(point.x, point.y, bounds.maxZ));
        Vec3 nearest = edges.get(0);
        for (Vec3 edge : edges) if (edge.distanceToSqr(point) < nearest.distanceToSqr(point)) nearest = edge;
        return nearest;
    }
}
