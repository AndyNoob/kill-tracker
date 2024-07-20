package comfortable_andy.killtracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public final class KillTrackerMain extends JavaPlugin implements Listener {

    private final Map<UUID, KillsData> kills = new HashMap<>();
    private final Gson gson = new GsonBuilder()
            .setLenient()
            .disableHtmlEscaping()
            .create();
    private final TypeToken<List<KillsData>> killDataToken = new TypeToken<>() {
    };
    private File dataFile;

    @Override
    public void onEnable() {
        getLogger().info("Data folder: " + getDataFolder());
        dataFile = new File(getDataFolder(), "kills.json");
        getServer().getPluginManager().registerEvents(this, this);
        readKills();
        new BukkitRunnable() {
            @Override
            public void run() {
                saveKills();
            }
        }.runTaskTimer(this, 20 * 5, 20 * 5);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new KillTrackerExpansion(this).register();
            getLogger().info("Registered PAPI expansion.");
        } else getLogger().warning("Could not register PAPI expansion as the PAPI plugin is not loaded.");
    }

    @Override
    public void onDisable() {
        saveKills();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tryCreateDataFile() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void readKills() {
        tryCreateDataFile();
        try (FileReader reader = new FileReader(dataFile)) {
            final List<KillsData> data = gson.fromJson(reader, killDataToken);
            if (data != null)
                data.forEach(k -> kills.put(k.id, k));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveKills() {
        tryCreateDataFile();
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(gson.toJson(kills.values()));
        } catch (IOException e) {
            getLogger().severe("Could not save kills.");
            getLogger().severe(kills.values().toString());
            throw new RuntimeException(e);
        }
    }

    public KillsData getKills(Player player) {
        final UUID id = player.getUniqueId();
        return kills.computeIfAbsent(id, k -> new KillsData(id));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        getKills(killer).put(event.getEntity());
    }

    @SuppressWarnings("unused")
    @Data
    public static class KillsData {

        private final UUID id;
        private final Map<Long, KillData> kills = new HashMap<>();

        public Set<Long> killTimestamps(String s) {
            return kills.entrySet().stream()
                    .filter(e -> e.getValue().is(s))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }

        public Set<Long> killTimestamps(EntityType type) {
            return kills.entrySet().stream()
                    .filter(e -> e.getValue().is(type))
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
        }

        public Set<Long> killTimestamps(UUID id) {
            return kills.entrySet().stream()
                    .filter(e -> e.getValue().is(id))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }

        public void put(LivingEntity entity) {
            kills.put(System.currentTimeMillis(), new KillData(entity.getType(), entity instanceof Player ? entity.getUniqueId() : null));
        }

        public record KillData(EntityType type, @Nullable UUID uuid) {
            public boolean is(Object obj) {
                if (obj == null) return false;
                String val = "";
                if (obj instanceof String str) {
                    val = str;
                } else if (obj instanceof UUID id) {
                    return uuid != null && uuid.equals(id);
                } else if (obj instanceof EntityType t)
                    val = t.name();
                return (uuid != null && uuid.toString().equals(val))
                        || (val.equalsIgnoreCase(type.name())
                        || val.equalsIgnoreCase(type.key().asString())
                        || val.equalsIgnoreCase(type.key().value()));
            }
        }

    }

}
