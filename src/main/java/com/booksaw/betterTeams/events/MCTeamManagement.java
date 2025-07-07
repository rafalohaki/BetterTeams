package com.booksaw.betterTeams.events;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.customEvents.BelowNameChangeEvent;
import com.booksaw.betterTeams.customEvents.BelowNameChangeEvent.ChangeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;

public class MCTeamManagement implements Listener {

    final Scoreboard board;
    @Getter
    private final BelowNameType type;

    public MCTeamManagement(BelowNameType type) {
        this.type = type;
        board = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
    }

    private void runSync(Runnable task) {
        if (!Main.plugin.isEnabled()) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(Main.plugin, task);
        }
    }

    public void displayBelowNameForAll() {
        runSync(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                displayBelowNameSync(p);
            }
        });
    }

    public void displayBelowName(Player player) {
        if (player == null) return;
        runSync(() -> displayBelowNameSync(player));
    }

    private void displayBelowNameSync(Player player) {
        // ✅ 1. Only assign the scoreboard if it's not already the one managed by this plugin.
        // This avoids redundant packet sending and potential flicker/conflicts with other plugins.
        if (!player.getScoreboard().equals(board)) {
            player.setScoreboard(board);
        }

        Team team = Team.getTeam(player);
        if (team == null) return;
        if (!player.hasPermission("betterteams.teamName")) return;

        try {
            org.bukkit.scoreboard.Team scoreboardTeam = team.getScoreboardTeam(board);
            scoreboardTeam.addEntry(player.getName());
            Bukkit.getPluginManager().callEvent(new BelowNameChangeEvent(player, ChangeType.ADD));
        } catch (IllegalStateException e) {
            Main.plugin.getLogger().severe("Could not register the team name for " + player.getName() + " due to a conflict. See wiki for details. Error: " + e.getMessage());
        }
    }

    public void removeAll() {
        removeAll(true);
    }

    public void removeAll(boolean callEvent) {
        runSync(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                remove(p, callEvent);
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            // Note: A more robust implementation would track and unregister only teams created by this plugin.
            for (Entry<UUID, Team> t : Team.getTeamManager().getLoadedTeamListClone().entrySet()) {
                org.bukkit.scoreboard.Team team = t.getValue().getScoreboardTeamOrNull();
                if (team != null) {
                    try {
                        team.unregister();
                    } catch (IllegalStateException e) {
                        // Can be thrown on shutdown, safe to ignore.
                    }
                }
            }
        });
    }
    
    public void remove(Player player) {
        remove(player, true);
    }

    public void remove(Player player, boolean callEvent) {
        runSync(() -> {
            if (player == null) return;
            Team team = Team.getTeam(player);
            if (team == null) return;
            org.bukkit.scoreboard.Team scoreboardTeam = team.getScoreboardTeamOrNull();
            if (scoreboardTeam == null) {
                Main.plugin.getLogger().info("Attempted to remove " + player.getName() + " from a scoreboard team that no longer exists. No action needed.");
                return;
            }
            if (!scoreboardTeam.hasEntry(player.getName())) return;
            try {
                scoreboardTeam.removeEntry(player.getName());
                if (callEvent) {
                    Bukkit.getPluginManager().callEvent(new BelowNameChangeEvent(player, ChangeType.REMOVE));
                }
            } catch (Exception e) {
                Main.plugin.getLogger().warning("Could not remove scoreboard entry for " + player.getName() + ". Another plugin may be interfering. See wiki for details.");
            }
        });
    }
    
    public void refreshDisplay(Player player) {
        remove(player, false);
        displayBelowName(player);
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent e) {
        displayBelowName(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        displayBelowName(e.getPlayer());
    }

    public void setupTeam(org.bukkit.scoreboard.Team team, String teamName) {
        String display = teamName;

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Player context = team.getEntries().stream()
                    .map(Bukkit::getPlayerExact)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (context != null) {
                // Add a fail-safe try-catch block to prevent errors from external placeholders
                // from crashing the scoreboard update logic.
                try {
                    display = PlaceholderAPI.setPlaceholders(context, display);
                } catch (Exception ex) {
                    Main.plugin.getLogger().warning("Failed to parse PlaceholderAPI in scoreboard display for " + context.getName() + ": " + ex.getMessage());
                }
            }
        }
        
        if (type == BelowNameType.PREFIX) {
            team.setPrefix(trimLegacyText(display, 16));
        } else if (type == BelowNameType.SUFFIX) {
            team.setSuffix(trimLegacyText(" " + display, 16));
        }

        if (!Main.plugin.getConfig().getBoolean("collide")) team.setOption(Option.COLLISION_RULE, OptionStatus.FOR_OWN_TEAM);
        if (Main.plugin.getConfig().getBoolean("privateDeath")) team.setOption(Option.DEATH_MESSAGE_VISIBILITY, OptionStatus.FOR_OWN_TEAM);
        if (Main.plugin.getConfig().getBoolean("privateName")) team.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
        team.setCanSeeFriendlyInvisibles(Main.plugin.getConfig().getBoolean("canSeeFriendlyInvisibles"));
    }
    
    private String trimLegacyText(String input, int maxLength) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        int length = 0;
        boolean inColorCode = false;
        for (char c : input.toCharArray()) {
            if (c == '§') inColorCode = true;
            else if (inColorCode) inColorCode = false;
            else length++;
            if (length > maxLength) break;
            result.append(c);
        }
        return result.toString();
    }
    
    public enum BelowNameType {
        PREFIX, SUFFIX, FALSE;
        public static BelowNameType getType(String string) {
            switch (string.toLowerCase()) {
                case "prefix": case "true": return PREFIX;
                case "suffix": return SUFFIX;
                default: return FALSE;
            }
        }
    }
}
