package org.Cry_Legende.sHSnow;

import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One wave of snow. The main thread only reads what needs the world (weather, player positions, roof tables);
 * flakes are then drawn, checked against the roofs and sent from an async task.
 */
final class SnowTask implements Runnable {

    // A flake is sent only when it starts at least this many blocks above the roof block of its column
    private static final int ROOF_CLEARANCE = 2;

    private final SHSnow plugin;
    private final SnowSettings settings;
    private final RoofCache roofs;

    SnowTask(SHSnow plugin, SnowSettings settings, RoofCache roofs) {
        this.plugin = plugin;
        this.settings = settings;
        this.roofs = roofs;
    }

    @Override
    public void run() {
        if (settings.particles().isEmpty() || settings.particlesPerPlayer() == 0) {
            return;
        }
        Server server = plugin.getServer();
        int tick = server.getCurrentTick();
        roofs.purge(tick);

        List<Viewer> viewers = new ArrayList<>();
        for (String name : settings.worlds()) {
            World world = server.getWorld(name);
            if (world == null || settings.stopInRain() && world.hasStorm()) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                if (plugin.isHidden(player)) {
                    continue;
                }
                double x = player.getX();
                double z = player.getZ();
                viewers.add(new Viewer(player, x, player.getY(), z, roofs.around(world, x, z, settings.radius(), tick)));
            }
        }
        if (!viewers.isEmpty()) {
            server.getScheduler().runTaskAsynchronously(plugin, () -> viewers.forEach(this::snow));
        }
    }

    // Async: works on the snapshot taken by the main thread, never reads the world
    private void snow(Viewer viewer) {
        Player player = viewer.player();
        if (!player.isOnline()) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Particle> particles = settings.particles();
        int kinds = particles.size();
        double radius = settings.radius();
        double speed = settings.fallSpeed();
        int sent = 0;
        for (int i = 0; i < settings.particlesPerPlayer(); i++) {
            double x = viewer.x() + random.nextDouble(-radius, radius);
            double y = viewer.y() + random.nextDouble(-radius, radius);
            double z = viewer.z() + random.nextDouble(-radius, radius);
            if (y - ROOF_CLEARANCE < viewer.roofs().roofY((int) Math.floor(x), (int) Math.floor(z))) {
                continue;
            }
            // With a count of 0 the offset is the flake's direction and the extra value its speed
            player.spawnParticle(particles.get(sent++ % kinds), x, y, z, 0, 0, -1, 0, speed);
        }
    }

    private record Viewer(Player player, double x, double y, double z, RoofCache.RoofGrid roofs) {
    }
}
