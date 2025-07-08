package com.booksaw.betterTeams.commands.team;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.util.ColorConversionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class KickCommand extends TeamSubCommand {

    @Override
    public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {
        if (args.length < 1) {
            return new CommandResponse("invalidUsage");
        }

        TeamPlayerResult result = getTeamPlayer(team, args[0]);
        if (result.isCR()) {
            return result.getCr();
        }

        TeamPlayer kicked = result.getPlayer();
        if (teamPlayer.getRank().value <= Objects.requireNonNull(kicked).getRank().value) {
            return new CommandResponse("kick.noPerm");
        }

        team.removePlayer(kicked);

        Player player = kicked.getPlayer().getPlayer();
        if (player != null) {
            MessageManager.sendMessage(player, "kick.notify", team.getName());

            if (Main.plugin.getConfig().getBoolean("titleRemoval")) {
                sendKickTitle(player);
            }
        }

        return new CommandResponse(true, "kick.success");
    }

    /**
     * Wysyła kick title używając Adventure API zamiast deprecated sendSubTitle()
     */
    private void sendKickTitle(Player player) {
        String kickTitleMessage = MessageManager.getMessage("kick.title");
        Component titleComponent = ColorConversionUtils.fromLegacy(kickTitleMessage);
        
        Title kickTitle = Title.title(
            Component.empty(),              // title (pusty)
            titleComponent,                 // subtitle (wiadomość o kick)
            Title.Times.times(
                Duration.ofMillis(500),     // fade in
                Duration.ofSeconds(3),      // stay (krótszy niż ban)
                Duration.ofSeconds(1)       // fade out
            )
        );
        
        player.showTitle(kickTitle);
    }

    @Override public String getCommand() { return "kick"; }
    @Override public String getNode() { return "kick"; }
    @Override public String getHelp() { return "Kick that player from your team"; }
    @Override public String getArguments() { return "<player>"; }
    @Override public int getMinimumArguments() { return 1; }
    @Override public int getMaximumArguments() { return 1; }

    @Override
    public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
        addPlayerStringList(options, (args.length == 0) ? "" : args[0]);
    }

    @Override public PlayerRank getDefaultRank() { return PlayerRank.ADMIN; }
}
