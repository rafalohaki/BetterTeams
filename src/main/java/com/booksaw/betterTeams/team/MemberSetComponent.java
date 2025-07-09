package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.customEvents.PlayerJoinTeamEvent;
import com.booksaw.betterTeams.customEvents.PlayerLeaveTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostPlayerJoinTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostPlayerLeaveTeamEvent;
import com.booksaw.betterTeams.exceptions.CancelledEventException;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.team.storage.team.TeamStorage;
import com.booksaw.betterTeams.util.ColorConversionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

public class MemberSetComponent extends TeamPlayerSetComponent {

    @Override
    public void add(Team team, TeamPlayer teamPlayer) {
        PlayerJoinTeamEvent event = new PlayerJoinTeamEvent(team, teamPlayer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new CancelledEventException(event);
        }

        OfflinePlayer p = teamPlayer.getPlayer();
        team.getInvitedPlayers().remove(p.getUniqueId());

        if (p.isOnline()) {
            String playerDisplayName = getPlayerDisplayName(p.getPlayer());

            for (TeamPlayer player : set) {
                if (player.getPlayer().isOnline()) {
                    MessageManager.sendMessage(player.getPlayer().getPlayer(), "join.notify", playerDisplayName);
                }
            }

            if (Main.plugin.teamManagement != null) {
                Main.plugin.teamManagement.displayBelowName(p.getPlayer());
            }
        }

        Team.getTeamManager().playerJoinTeam(team, teamPlayer);
        set.add(teamPlayer);
        Bukkit.getPluginManager().callEvent(new PostPlayerJoinTeamEvent(team, teamPlayer));
    }

    @Override
    public void remove(Team team, TeamPlayer teamPlayer) {
        if (teamPlayer == null) {
            Main.plugin.getLogger().warning("[BetterTeams] Tried to remove null TeamPlayer from team '"
                    + (team != null ? team.getName() : "null") + "'. Skipping removal.");
            return;
        }

        PlayerLeaveTeamEvent event = new PlayerLeaveTeamEvent(team, teamPlayer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new CancelledEventException(event);
        }

        OfflinePlayer p = teamPlayer.getPlayer();
        if (Main.plugin.teamManagement != null && p.isOnline()) {
            Main.plugin.teamManagement.remove(p.getPlayer());
        }

        Team.getTeamManager().playerLeaveTeam(team, teamPlayer);
        set.remove(teamPlayer);
        Bukkit.getPluginManager().callEvent(new PostPlayerLeaveTeamEvent(team, teamPlayer));
    }

    /**
     * Converts a Player's display name to a legacy-formatted string using Adventure.
     *
     * @param player The player whose display name should be fetched
     * @return Legacy-colored display name string
     */
    private String getPlayerDisplayName(Player player) {
        Component displayNameComponent = player.displayName();
        return ColorConversionUtils.toLegacy(displayNameComponent);
    }

    @Override
    public String getSectionHeading() {
        return "players";
    }

    @Override
    public void load(TeamStorage section) {
        set.clear();
        set.addAll(section.getPlayerList());
    }

    @Override
    public void save(TeamStorage storage) {
        storage.setPlayerList(getConvertedList());
    }
}
