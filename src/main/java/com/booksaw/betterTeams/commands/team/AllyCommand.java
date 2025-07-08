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
			return requests.isEmpty()
				? new CommandResponse(true, "ally.noRequests")
				: new CommandResponse(true, new ReferencedFormatMessage("ally.from", requests));
		}

		Team toAlly = Team.getTeam(args[0]);
		if (toAlly == null) {
			return new CommandResponse("noTeam");
		}

		if (toAlly == team) {
			return new CommandResponse("ally.self");
		}

		if (toAlly.isAlly(team)) {
			return new CommandResponse("ally.already");
		}

		if (team.hasMaxAllies() || toAlly.hasMaxAllies()) {
			return new CommandResponse("ally.limit");
		}

		if (toAlly.hasRequested(team)) {
			return new CommandResponse("ally.alreadyrequest");
		}

		if (team.hasRequested(toAlly)) {
			toAlly.addAlly(team, false);
			team.addAlly(toAlly, true);
			toAlly.removeAllyRequest(team);
			team.removeAllyRequest(toAlly);
			return new CommandResponse(true, "ally.success");
		}

		toAlly.addAllyRequest(team);
		return new CommandResponse(true, "ally.requested");
	}

	private String buildAllyRequestsList(Team team) {
		StringJoiner joiner = new StringJoiner(", ");

		for (UUID uuid : team.getAllyRequests()) {
			Team other = Team.getTeam(uuid);
			if (other == null) continue;

			Component name = Component.text(other.getDisplayName(), NamedTextColor.WHITE);
			String legacy = ColorConversionUtils.toLegacy(name);
			joiner.add(legacy);
		}

		return joiner.toString();
	}

	@Override public String getCommand() { return "ally"; }
	@Override public String getNode() { return "ally"; }
	@Override public String getHelp() { return "Used to request an alliance with another team"; }
	@Override public String getArguments() { return "<team>"; }
	@Override public int getMinimumArguments() { return 0; }
	@Override public int getMaximumArguments() { return 1; }

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			Team myTeam = getMyTeam(sender);
			addTeamStringList(options, args[0], myTeam != null ? ImmutableSet.of(myTeam.getID()) : null, null);
		}
	}

	@Override public PlayerRank getDefaultRank() { return PlayerRank.OWNER; }
}
