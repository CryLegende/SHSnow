package org.Cry_Legende.sHSnow;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
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
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register(SnowCommand.create(this), "Active ou coupe la chute de neige.", List.of("shsnow")));
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

    // An empty message in messages.yml sends nothing
    void send(CommandSender sender, String path) {
        String text = messages.getString(path);
        if (text != null && !text.isBlank()) {
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
