package org.Cry_Legende.sHSnow;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SHSnow extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final RoofCache roofs = new RoofCache();
    // Set on players who turned the snow off; saved with their player data
    private NamespacedKey hiddenKey;
    private FileConfiguration messages;
    private BukkitTask task;

    @Override
    public void onEnable() {
        hiddenKey = new NamespacedKey(this, "hidden");
        saveDefaultConfig();
        load();
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(SnowCommand.player(this), "Active ou coupe la chute de neige.", List.of());
            event.registrar().register(SnowCommand.admin(this), "Administration de SHSnow.", List.of());
        });
    }

    /**
     * Rereads config.yml and messages.yml. Both are parsed first and nothing is applied unless both are valid,
     * so a broken file leaves the running configuration untouched.
     */
    void reload() throws ReloadException {
        parse("config.yml");
        parse("messages.yml");
        load();
    }

    private void parse(String name) throws ReloadException {
        try {
            new YamlConfiguration().load(new File(getDataFolder(), name));
        } catch (IOException | InvalidConfigurationException e) {
            String detail = e.getMessage() == null ? e.toString() : e.getMessage().lines().findFirst().orElse("");
            throw new ReloadException(name, detail, e);
        }
    }

    /** A configuration file that cannot be read. */
    static final class ReloadException extends Exception {

        final String file;
        final String detail;

        ReloadException(String file, String detail, Throwable cause) {
            super(file + ": " + detail, cause);
            this.file = file;
            this.detail = detail;
        }
    }

    void load() {
        reloadConfig();
        loadMessages();
        SnowSettings settings = SnowSettings.load(getConfig(), getLogger());
        roofs.configure(settings.ignoredRoofBlocks(), settings.roofCacheTicks());
        if (task != null) {
            task.cancel();
        }
        task = getServer().getScheduler().runTaskTimer(this, new SnowTask(this, settings, roofs),
            settings.intervalTicks(), settings.intervalTicks());
    }

    boolean isHidden(Player player) {
        return player.getPersistentDataContainer().has(hiddenKey);
    }

    /** Flips the player's choice and returns true when the snow is now hidden for them. */
    boolean toggle(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (data.has(hiddenKey)) {
            data.remove(hiddenKey);
            return false;
        }
        data.set(hiddenKey, PersistentDataType.BOOLEAN, true);
        return true;
    }

    // An empty message in messages.yml sends nothing; each pair of values replaces a {name} with its value
    void send(CommandSender sender, String path, String... pairs) {
        String text = messages.getString(path);
        if (text != null && !text.isBlank()) {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                text = text.replace("{" + pairs[i] + "}", pairs[i + 1]);
            }
            sender.sendMessage(LEGACY.deserialize(text));
        }
    }

    private void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        InputStream defaults = getResource("messages.yml");
        if (defaults != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }
}
