package com.booksaw.betterTeams.commands.team;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.ReferencedFormatMessage;
import com.booksaw.betterTeams.util.ColorConversionUtils;
import com.google.common.collect.ImmutableSet;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class AllyCommand extends TeamSubCommand {

	@Override
	public CommandResponse onCommand(TeamPlayer player, String label, String[] args, Team team) {

		if (args.length == 0) {
			String requests = buildAllyRequestsList(team);
			
			if (!requests.isEmpty()) {
				return new CommandResponse(true, new ReferencedFormatMessage("ally.from", requests));
			} else {
				return new CommandResponse(true, "ally.noRequests");
			}
		}

		Team toAlly = Team.getTeam(args[0]);
		if (toAlly == null) {
			return new CommandResponse("noTeam");
		} else if (toAlly == team) {
			return new CommandResponse("ally.self");
		}

		// check if they are already allies
		if (toAlly.isAlly(team)) {
			return new CommandResponse("ally.already");
		}

		// checking limit
		if (team.hasMaxAllies() || toAlly.hasMaxAllies()) {
			return new CommandResponse("ally.limit");
		}

		// checking if they have already sent an ally request
		if (toAlly.hasRequested(team)) {
			return new CommandResponse("ally.alreadyrequest");
		}

		// checking if an ally request has been sent
		if (team.hasRequested(toAlly)) {
			toAlly.addAlly(team, false);
			team.addAlly(toAlly, true);
			toAlly.removeAllyRequest(team);
			team.removeAllyRequest(toAlly);
			return new CommandResponse(true, "ally.success");
		}

		// sending an ally request
		toAlly.addAllyRequest(team);

		return new CommandResponse(true, "ally.requested");
	}

	/**
	 * Buduje listę requestów sojuszniczych używając Adventure API
	 */
	private String buildAllyRequestsList(Team team) {
		StringJoiner joiner = new StringJoiner(", ");
		
		for (UUID uuid : team.getAllyRequests()) {
			Team uuidteam = Team.getTeam(uuid);
			if (uuidteam == null) {
				continue;
			}
			
			// Używamy Adventure API do formatowania nazwy drużyny z białym kolorem
			Component teamName = Component.text()
				.append(Component.text(uuidteam.getDisplayName()))
				.color(NamedTextColor.WHITE)
				.build();
			
			// Konwertujemy Component na legacy string dla kompatybilności
			String formattedName = ColorConversionUtils.toLegacy(teamName);
			joiner.add(formattedName);
		}
		
		return joiner.toString();
	}

	@Override
	public String getCommand() {
		return "ally";
	}

	@Override
	public String getNode() {
		return "ally";
	}

	@Override
	public String getHelp() {
		return "Used to request an alliance with another team";
	}

	@Override
	public String getArguments() {
		return "<team>";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			Team myTeam = getMyTeam(sender);

			addTeamStringList(options, args[0], myTeam != null ? ImmutableSet.of(myTeam.getID()) : null, null);
		}
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.OWNER;
	}
}
