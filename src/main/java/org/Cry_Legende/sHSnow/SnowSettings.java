package org.Cry_Legende.sHSnow;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

record SnowSettings(
    List<String> worlds,
    List<Particle> particles,
    int particlesPerPlayer,
    double radius,
    double fallSpeed,
    int intervalTicks,
    boolean stopInRain,
    Set<Material> ignoredRoofBlocks,
    int roofCacheTicks) {

    static SnowSettings load(ConfigurationSection config, Logger logger) {
        List<Particle> particles = new ArrayList<>();
        for (String id : config.getStringList("particles")) {
            NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
            Particle particle = key == null ? null : Registry.PARTICLE_TYPE.get(key);
            if (particle == null) {
                logger.warning("Unknown particle in config.yml: " + id);
            } else if (particle.getDataType() != Void.class) {
                logger.warning("Particle " + id + " needs extra data (color, block or item) and is skipped");
            } else {
                particles.add(particle);
            }
        }
        if (particles.isEmpty()) {
            logger.warning("No usable particle in config.yml, snow is off");
        }

        Set<Material> ignored = EnumSet.noneOf(Material.class);
        for (String id : config.getStringList("roof.ignored-blocks")) {
            Material material = Material.matchMaterial(id);
            if (material == null || !material.isBlock()) {
                logger.warning("Unknown block in config.yml: " + id);
            } else {
                ignored.add(material);
            }
        }

        return new SnowSettings(
            List.copyOf(config.getStringList("worlds")),
            List.copyOf(particles),
            Math.max(0, config.getInt("particles-per-player", 150)),
            Math.max(1, config.getDouble("radius", 25)),
            config.getDouble("fall-speed", 0.09),
            Math.max(1, config.getInt("interval-ticks", 11)),
            config.getBoolean("stop-in-rain", true),
            ignored,
            Math.max(1, config.getInt("roof.cache-seconds", 10)) * 20);
    }
}
