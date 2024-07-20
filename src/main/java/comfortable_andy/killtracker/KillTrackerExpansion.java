package comfortable_andy.killtracker;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class KillTrackerExpansion extends PlaceholderExpansion {

    private final KillTrackerMain main;

    @Override
    public @NotNull String getIdentifier() {
        return "killtracker";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Comfortable_Andy";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        final String[] split = params.split("_");
        System.out.println("hi");
        if (split[0].equalsIgnoreCase("kills")) {
            if (split.length == 1)
                return main.getKills(player).getKills().size() + "";
            else {
                return main.getKills(player).killTimestamps(split[1]).size() + "";
            }
        }
        return null;
    }
}
