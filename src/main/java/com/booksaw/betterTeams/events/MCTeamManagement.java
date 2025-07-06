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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import lombok.Getter;

public class MCTeamManagement implements Listener {

    final Scoreboard board;
    @Getter
    private final BelowNameType type;

    public MCTeamManagement(BelowNameType type) {
        this.type = type;
        board = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
    }

    private void runSync(Runnable task) {
        // Central guard to ensure no scoreboard operations are attempted if the plugin is disabled.
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
        player.setScoreboard(board);

        Team team = Team.getTeam(player);
        if (team == null) return;

        if (!player.hasPermission("betterteams.teamName")) return;

        Bukkit.getPluginManager().callEvent(new BelowNameChangeEvent(player, ChangeType.ADD));

        try {
            team.getScoreboardTeam(board).addEntry(player.getName());
        } catch (IllegalStateException e) {
            Main.plugin.getLogger().severe("Could not register the team name in the tab menu due to a conflict, see https://github.com/booksaw/BetterTeams/wiki/Managing-the-TAB-Menu error: " + e.getMessage());
        }
    }

    public void removeAll() {
        removeAll(true);
    }

    /**
     * Used when the plugin is disabled
     */
    public void removeAll(boolean callEvent) {
        runSync(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                remove(p, callEvent);
            }

            // only loaded teams will have a team manager
            for (Entry<UUID, Team> t : Team.getTeamManager().getLoadedTeamListClone().entrySet()) {
                org.bukkit.scoreboard.Team team = t.getValue().getScoreboardTeamOrNull();

                if (team != null) {
                    team.unregister();
                }

            }
        });
    }

    public void remove(Player player) {
        remove(player, true);
    }

    /**
     * Used to remove the prefix / suffix from the specified player
     *
     * @param player    the player to remove the prefix/suffix from
     * @param callEvent if BelowNameChangeEvent should be called
     */
    public void remove(Player player, boolean callEvent) {
        runSync(() -> {
            if (player == null) {
                return;
            }

            Team team = Team.getTeam(player);
            if (team == null) {
                return;
            }

            org.bukkit.scoreboard.Team scoreboardTeam = team.getScoreboardTeamOrNull();
            if (scoreboardTeam == null || !scoreboardTeam.hasEntry(player.getName())) {
                return;
            }

            try {
                scoreboardTeam.removeEntry(player.getName());
            } catch (Exception e) {
                Main.plugin.getLogger().warning(
                        "Another plugin is conflicting with the functionality of the BetterTeams. See the wiki page: https://github.com/booksaw/BetterTeams/wiki/Managing-the-TAB-Menu for more information");
                return;
            }

            if (callEvent) {
                BelowNameChangeEvent event = new BelowNameChangeEvent(player, ChangeType.REMOVE);
                Bukkit.getPluginManager().callEvent(event);
            }
        });
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent e) {
        displayBelowName(e.getPlayer());
    }

    public void setupTeam(org.bukkit.scoreboard.Team team, String teamName) {
        // setting team name
        if (type == BelowNameType.PREFIX) {
            team.setPrefix(teamName);
        } else if (type == BelowNameType.SUFFIX) {
            team.setSuffix(" " + teamName);
        }

        if (!Main.plugin.getConfig().getBoolean("collide")) {
            team.setOption(Option.COLLISION_RULE, OptionStatus.FOR_OWN_TEAM);
        }

        if (Main.plugin.getConfig().getBoolean("privateDeath")) {
            team.setOption(Option.DEATH_MESSAGE_VISIBILITY, OptionStatus.FOR_OWN_TEAM);
        }

        if (Main.plugin.getConfig().getBoolean("privateName")) {
            // FIX: Corrected typo from FOR_OTHER_TEIMS to FOR_OTHER_TEAMS
            team.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
        }

        team.setCanSeeFriendlyInvisibles(Main.plugin.getConfig().getBoolean("canSeeFriendlyInvisibles"));

    }

    public enum BelowNameType {
        PREFIX, SUFFIX, FALSE;

        public static BelowNameType getType(String string) {

            switch (string.toLowerCase()) {
                case "prefix":
                case "true":
                    return PREFIX;
                case "suffix":
                    return SUFFIX;
                default:
                    return FALSE;
            }
        }
    }

}
