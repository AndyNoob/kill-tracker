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

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public final class KillTrackerMain extends JavaPlugin implements Listener {

    private final Map<UUID, KillData> kills = new HashMap<>();
    private final Gson gson = new GsonBuilder()
            .setLenient()
            .disableHtmlEscaping()
            .create();
    private final TypeToken<List<KillData>> killDataToken = new TypeToken<>() {
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
        }.runTaskTimer(this, 0, 20 * 5);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new KillTrackerExpansion(this).register();
            getLogger().info("Registered PAPI expansion.");
        } else getLogger().warning("Could not register PAPI expansion as the PAPI plugin is not loaded.");
    }

    @Override
    public void onDisable() {
        saveKills();
    }

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
        try (FileReader reader = new FileReader(dataFile);) {
            final List<KillData> data = gson.fromJson(reader, killDataToken);
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

    public KillData getKills(Player player) {
        final UUID id = player.getUniqueId();
        return kills.computeIfAbsent(id, k -> new KillData(id));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        getKills(killer).put(event.getEntity());
        System.out.println(kills);
    }

    @Data
    public static class KillData {

        private final UUID id;
        private final Map<Long, String> kills = new HashMap<>();

        public Set<Long> killTimestamps(String s) {
            return kills.entrySet().stream()
                    .filter(e -> e.getValue().equals(s))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }

        public Set<Long> killTimestamps(EntityType type) {
            return kills.entrySet().stream()
                    .filter(e -> e.getValue().equalsIgnoreCase(type.name())
                            || e.getValue().equalsIgnoreCase(type.key().asString())
                            || e.getValue().equalsIgnoreCase(type.key().value())
                    ).map(Map.Entry::getKey).collect(Collectors.toSet());
        }

        public Set<Long> killTimestamps(UUID id) {
            return kills.entrySet().stream()
                    .filter(e -> e.getValue().equals(id.toString()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }

        public void put(LivingEntity entity) {
            kills.put(System.currentTimeMillis(), entity instanceof Player ? entity.getUniqueId().toString() : entity.getType().name());
        }
    }

}
