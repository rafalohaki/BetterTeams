package com.booksaw.betterTeams.commands.team;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.Utils;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.util.ColorConversionUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class BanCommand extends TeamSubCommand {

	@Override
	public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

		/*
		 * method is depreciated as it does not guarantee the expected player, in most
		 * use cases this will work and it will be down to the user if it does not due
		 * to name changes This method is appropriate to use in this use case (so users
		 * can view offline users teams by name not just by team name)
		 */
		OfflinePlayer player = Utils.getOfflinePlayer(args[0]);

		if (player == null) {
			return new CommandResponse("noPlayer");
		}

		Team otherTeam = Team.getTeam(player);

		if (team.isBanned(player)) {
			return new CommandResponse("ban.already");
		}

		if (team != otherTeam) {
			team.banPlayer(player);
			if (player.isOnline()) {
				MessageManager.sendMessage(player.getPlayer(), "ban.notify", team.getName());
			}
			return new CommandResponse("ban.success");
		}

		TeamPlayer kickedPlayer = team.getTeamPlayer(player);

		// ensuring the player they are banning has less perms than them
		if (teamPlayer.getRank().value <= Objects.requireNonNull(kickedPlayer).getRank().value) {
			return new CommandResponse("ban.noPerm");
		}

		// player is in the team, so removing them
		team.removePlayer(player);
		team.banPlayer(player);

		if (player.isOnline()) {
			MessageManager.sendMessage(player.getPlayer(), "ban.notify", team.getName());

			if (Main.plugin.getConfig().getBoolean("titleRemoval")) {
				sendBanTitle(player.getPlayer());
			}
		}

		return new CommandResponse(true, "ban.success");
	}

	/**
	 * Wysyła title o banie używając Adventure API zamiast deprecated sendTitle()
	 */
	private void sendBanTitle(org.bukkit.entity.Player player) {
		// Konwertuj message na Component
		String banTitleMessage = MessageManager.getMessage("ban.title");
		Component titleComponent = ColorConversionUtils.fromLegacy(banTitleMessage);
		
		// Utwórz Title używając Adventure API
		Title banTitle = Title.title(
			Component.empty(),              // title (pusty)
			titleComponent,                 // subtitle (wiadomość o banie)
			Title.Times.times(
				Duration.ofMillis(500),     // fade in (10 ticks = 500ms)
				Duration.ofSeconds(5),      // stay (100 ticks = 5s)
				Duration.ofSeconds(1)       // fade out (20 ticks = 1s)
			)
		);
		
		// Wyślij title używając Adventure API
		player.showTitle(banTitle);
	}

	@Override
	public String getCommand() {
		return "ban";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public String getNode() {
		return "ban";
	}

	@Override
	public String getHelp() {
		return "Bans the specified player from your team";
	}

	@Override
	public String getArguments() {
		return "<player>";
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		addPlayerStringList(options, (args.length == 0) ? "" : args[0]);
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.ADMIN;
	}
}
