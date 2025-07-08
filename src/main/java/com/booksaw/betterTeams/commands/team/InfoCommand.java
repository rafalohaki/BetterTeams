package com.booksaw.betterTeams.commands.team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.Utils;
import com.booksaw.betterTeams.commands.ParentCommand;
import com.booksaw.betterTeams.commands.SubCommand;
import com.booksaw.betterTeams.message.HelpMessage;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.team.SetTeamComponent;
import com.booksaw.betterTeams.util.ColorConversionUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * This class handles the command /team info [team/player]
 *
 * @author booksaw
 */
public class InfoCommand extends SubCommand {

	private final ParentCommand parentCommand;

	public InfoCommand(ParentCommand parentCommand) {
		this.parentCommand = parentCommand;
	}

	public static List<Component> getInfoComponents(Team team) {
		List<Component> infoMessages = new ArrayList<>();

		addIfNotNull(infoMessages, MessageManager.getMessage("info.name", team.getDisplayName()));

		if (team.getDescription() != null && !team.getDescription().isEmpty()) {
			addIfNotNull(infoMessages, MessageManager.getMessage("info.description", team.getDescription()));
		}

		addIfNotNull(infoMessages, MessageManager.getMessage("info.open", team.isOpen()));
		addIfNotNull(infoMessages, MessageManager.getMessage("info.score", team.getScore()));
		addIfNotNull(infoMessages, MessageManager.getMessage("info.money", team.getBalance()));
		addIfNotNull(infoMessages, MessageManager.getMessage("info.level", team.getLevel()));
		addIfNotNull(infoMessages, MessageManager.getMessage("info.tag", team.getTag()));

		if (Main.plugin.getConfig().getBoolean("anchor.enable")) {
			String anchor = MessageManager.getMessage("info.anchor", team.isAnchored());
			addIfNotNull(infoMessages, anchor);
		}

		addIfNotNull(infoMessages, getAlliesMessage(team));
		addIfNotNull(infoMessages, getPlayerList(team, PlayerRank.OWNER));
		addIfNotNull(infoMessages, getPlayerList(team, PlayerRank.ADMIN));
		addIfNotNull(infoMessages, getPlayerList(team, PlayerRank.DEFAULT));

		return infoMessages;
	}

	private static void addIfNotNull(List<Component> list, @Nullable String legacy) {
		if (legacy != null && !legacy.isEmpty()) {
			list.add(ColorConversionUtils.fromLegacy(legacy));
		}
	}


	private static String getSetComponentMessage(SetTeamComponent<UUID> teams, final String referenceMessage) {
		if (teams.get().isEmpty()) {
			return null;
		}
		
		List<String> teamNames = new ArrayList<>();
		for (UUID uuid : teams.get()) {
			Team ally = Team.getTeam(uuid);
			if (ally == null) {
				Main.plugin.getLogger().warning("Unable to locate team with UUID: " + uuid);
				continue;
			}
			teamNames.add(ally.getDisplayName());
		}
		
		if (teamNames.isEmpty()) {
			return null;
		}
		
		// Łączenie nazw drużyn z białymi separatorami
		String joinedNames = String.join(
			ColorConversionUtils.toLegacy(Component.text(", ", NamedTextColor.WHITE)), 
			teamNames
		);
		
		return MessageManager.getMessage(referenceMessage, joinedNames);
	}

	private static String getAlliesMessage(Team team) {
		return getSetComponentMessage(team.getAllies(), "info.ally");
	}

	private static String getPlayerList(Team team, PlayerRank rank) {
		List<TeamPlayer> users = team.getRank(rank);

		if (!users.isEmpty()) {
			String space = MessageManager.getMessage("info.playerListSpace");
			List<String> playerList = new ArrayList<>();
			for (TeamPlayer player : users) {
				OfflinePlayer offplayer = player.getPlayer();
				playerList.add(MessageManager.getMessage(offplayer, "info." + ((offplayer.isOnline()
						&& player.getOnlinePlayer().map(
								p -> !Utils.isVanished(p)).orElse(false)) ? "onlinePlayer" : "offlinePlayer"),
						player.getPrefix(null) + offplayer.getName()));
			}

			return MessageManager.getMessage("info." + rank.toString().toLowerCase(), String.join(space, playerList));
		}

		return null;
	}

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {
		if (args.length == 0) {
			if (!(sender instanceof Player)) {
				return new CommandResponse(new HelpMessage(this, label, parentCommand));
			}
			Team team = Team.getTeam((Player) sender);
			if (team == null) {
				return new CommandResponse(new HelpMessage(this, label, parentCommand));
			}
			displayTeamInfo(sender, team);
			return new CommandResponse(true);
		}

		// player or team has been entered
		// trying by team name
		Team team = Team.getTeam(args[0]);

		if (team != null) {
			displayTeamInfo(sender, team);
			return new CommandResponse(true);
		}

		// trying by player name
		/*
		 * method is depreciated as it does not guarantee the expected player, in most
		 * use cases this will work and it will be down to the user if it does not due
		 * to name changes This method is appropriate to use in this use case (so users
		 * can view offline users teams by name not just by team name)
		 */
		OfflinePlayer player = Utils.getOfflinePlayer(args[0]);

		if (player != null) {
			team = Team.getTeam(player);
			if (team != null) {
				displayTeamInfo(sender, team);
				return null;
			}
		}
		return new CommandResponse("info.needTeam");

	}

	private void displayTeamInfo(CommandSender sender, Team team) {
		List<Component> toDisplay = getInfoComponents(team);

		for (Component message : toDisplay) {
			MessageManager.sendFullMessage(sender, message);
		}
	}

	@Override
	public String getCommand() {
		return "info";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public String getNode() {
		return "info";
	}

	@Override
	public String getHelp() {
		return "View information about the specified player / team";
	}

	@Override
	public String getArguments() {
		return "[team/player]";
	}

	@Override
	public int getMaximumArguments() {
		return 2;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {

		addTeamStringList(options, (args.length == 0) ? "" : args[0]);
		addPlayerStringList(options, (args.length == 0) ? "" : args[0]);

	}

}
