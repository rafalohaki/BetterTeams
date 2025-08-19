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

    /**
     * Used to track a list of all listeners
     *
     * @param type The type prefixing that should be done
     */
    public MCTeamManagement(BelowNameType type) {
        this.type = type;
        Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        board = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void displayBelowNameForAll() {
        // CRITICAL FIX: Ensure all scoreboard operations run on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, this::displayBelowNameForAll);
            return;
        }
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            displayBelowName(p);
        }
    }

    /**
     * CRITICAL FIX: This method MUST run on main thread due to scoreboard operations
     */
    public void displayBelowName(Player player) {
        // CRITICAL FIX: Ensure this runs on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, () -> displayBelowName(player));
            return;
        }
        
        // CRITICAL FIX: Check if plugin is enabled before scoreboard operations
        if (!Main.plugin.isEnabled()) {
            Main.plugin.getLogger().warning("Cannot display below name - plugin is disabled");
            return;
        }

        player.setScoreboard(board);
        Team team = Team.getTeam(player);

        if (team == null) {
            return;
        }

        // checking the player has the correct permission node
        if (!player.hasPermission("betterTeams.teamName")) {
            // player does not have permission to have their team name displayed.
            return;
        }

        BelowNameChangeEvent event = new BelowNameChangeEvent(player, ChangeType.ADD);
        Bukkit.getPluginManager().callEvent(event);

        try {
            // CRITICAL FIX: This scoreboard operation now guaranteed to run on main thread
            team.getScoreboardTeam(board).addEntry(player.getName());
        } catch (IllegalStateException e) {
            Main.plugin.getLogger().severe("Could not register the team name in the tab menu due to a conflict, see https://betterteams.booksaw.dev/docs/configuration/Managing-the-TAB-Menu#plugin-conflicts error:" + e.getMessage());
        } catch (Exception e) {
            Main.plugin.getLogger().severe("Unexpected error in displayBelowName: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeAll() {
        removeAll(true);
    }

    /**
     * Used when the plugin is disabled
     */
    public void removeAll(boolean callEvent) {
        // CRITICAL FIX: Ensure removal runs on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, () -> removeAll(callEvent));
            return;
        }
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            remove(p, callEvent);
        }

        // only loaded teams will have a team manager
        for (Entry<UUID, Team> t : Team.getTeamManager().getLoadedTeamListClone().entrySet()) {
            org.bukkit.scoreboard.Team team = t.getValue().getScoreboardTeamOrNull();
            if (team != null) {
                try {
                    team.unregister();
                } catch (Exception e) {
                    Main.plugin.getLogger().warning("Failed to unregister team: " + e.getMessage());
                }
            }
        }
    }

    public void remove(Player player) {
        remove(player, true);
    }

    /**
     * Used to remove the prefix / suffix from the specified player
     *
     * @param player the player to remove the prefix/suffix from
     * @param callEvent if BelowNameChangeEvent should be called
     */
    public void remove(Player player, boolean callEvent) {
        // CRITICAL FIX: Ensure removal runs on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, () -> remove(player, callEvent));
            return;
        }
        
        if (player == null) {
            return;
        }

        Team team = Team.getTeam(player);
        if (team == null) {
            return;
        }

        if (!team.getScoreboardTeam(board).hasEntry(player.getName())) {
            return;
        }

        try {
            // CRITICAL FIX: This scoreboard operation now guaranteed to run on main thread
            team.getScoreboardTeam(board).removeEntry(player.getName());
        } catch (Exception e) {
            Main.plugin.getLogger().warning(
                    "Another plugin is conflicting with the functionality of the BetterTeams. See the wiki page: https://betterteams.booksaw.dev/docs/configuration/Managing-the-TAB-Menu#plugin-conflicts for more information");
            return;
        }

        if (callEvent) {
            BelowNameChangeEvent event = new BelowNameChangeEvent(player, ChangeType.REMOVE);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent e) {
        // CRITICAL FIX: Use runTask instead of runTaskAsynchronously for scoreboard operations
        // Scoreboard/team operations MUST run on the main thread to avoid "waypoint connections off-main" errors
        if (Main.plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(Main.plugin, () -> {
                displayBelowName(e.getPlayer());
            });
        }
    }

    /**
     * CRITICAL FIX: Ensure setup operations run on main thread
     */
    public void setupTeam(org.bukkit.scoreboard.Team team, String teamName) {
        // CRITICAL FIX: Ensure this runs on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, () -> setupTeam(team, teamName));
            return;
        }
        
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
