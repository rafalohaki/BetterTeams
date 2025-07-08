package com.booksaw.betterTeams.commands.team;

import java.util.List;
import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.Utils;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.text.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class InviteCommand extends TeamSubCommand {

	@Override
	public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {
		if (args.length < 1) {
			return new CommandResponse("invalidUsage"); // zabezpieczenie
		}

		Player toInvite = Bukkit.getPlayer(args[0]);
		if (toInvite == null || Utils.isVanished(toInvite)) {
			return new CommandResponse("noPlayer");
		}

		if (team.isBanned(toInvite)) {
			return new CommandResponse("invite.banned");
		}

		if (Team.getTeam(toInvite) != null) {
			return new CommandResponse("invite.inTeam");
		}

		int limit = team.getTeamLimit();
		if (limit > 0 && limit <= team.getMembers().size() + team.getInvitedPlayers().size()) {
			return new CommandResponse("invite.full");
		}

		// zapisz zaproszenie
		team.invite(toInvite.getUniqueId());

		// uzyskaj subkomendę join z messages.yml
		String joinSubcommand = MessageManager.getMessage("command.join");
		if (joinSubcommand == null || joinSubcommand.isEmpty()) {
			joinSubcommand = "join";
		} else if (joinSubcommand.startsWith("/")) {
			joinSubcommand = joinSubcommand.substring(1);
		}

		Component component = buildInviteComponent(toInvite, team, joinSubcommand);
		MessageManager.sendFullMessage(toInvite, component, true);

		return new CommandResponse(true, "invite.success");
	}

	private Component buildInviteComponent(Player recipient, Team team, String joinSubcommand) {
		String teamName = team.getName();

		Component base = Formatter.absolute().process(
			MessageManager.getMessage(recipient, "invite.invite", teamName)
		);

		Component hover = Formatter.absolute().process(
			MessageManager.getMessage(recipient, "invite.hover", teamName)
		);

		return base
			.clickEvent(ClickEvent.runCommand("/team " + joinSubcommand + " " + teamName))
			.hoverEvent(HoverEvent.showText(hover));
	}

	@Override
	public String getCommand() {
		return "invite";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public String getNode() {
		return "invite";
	}

	@Override
	public String getHelp() {
		return "Invite the specified player to your team";
	}

	@Override
	public String getArguments() {
		return "<player>";
	}

	@Override
	public int getMaximumArguments() {
		return 1;
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
