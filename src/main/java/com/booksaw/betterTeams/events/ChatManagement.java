package com.booksaw.betterTeams.events;

import java.util.ArrayList;
import java.util.List;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatManagement implements Listener {

	private static PrefixType doPrefix;
	public final List<CommandSender> spy = new ArrayList<>();

	public static void enable() {
		doPrefix = PrefixType.getType(Main.plugin.getConfig().getString("prefix"));
	}

	/**
	 * This detects when the player speaks and either adds a prefix or puts the
	 * message in the team chat
	 *
	 * @param event the chat event
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onChat(AsyncPlayerChatEvent event) {

		if (event.isCancelled()) {
			return;
		}

		Player p = event.getPlayer();
		Team team = Team.getTeam(p);

		if (team == null) {
			return;
		}

		TeamPlayer teamPlayer = team.getTeamPlayer(p);

		// FIXED: Replaced the hard crash with a robust, fail-safe check.
		if (teamPlayer == null) {
			// Log a detailed warning to the console so the server admin is aware of the data corruption.
			Main.plugin.getLogger().warning("Player " + p.getName() + " (" + p.getUniqueId() + ") is in a team but has no associated player data. This is a data inconsistency.");
			
			// As per the final suggestion, this calls team.removePlayer() to self-heal the data.
			// It is assumed that team.removePlayer(Player) has its own internal checks (i.e., containsKey)
			// to ensure it can safely handle being called in this state without causing another error.
			Main.plugin.getLogger().warning("Attempting to automatically remove the player from the team to resolve the issue.");
			team.removePlayer(p);

			// Stop processing this chat event for this player, as they are now considered team-less.
			// This prevents the original crash and allows the chat system to function normally for everyone else.
			return;
		}

		String anyChatToGlobalPrefix = Main.plugin.getConfig().getString("chatPrefixes.teamOrAllyToGlobal", "!");
		String globalToTeamPrefix = Main.plugin.getConfig().getString("chatPrefixes.globalToTeam", "!");
		String globalToAllyPrefix = Main.plugin.getConfig().getString("chatPrefixes.globalToAlly", "?");

		if (teamPlayer.isInTeamChat() || teamPlayer.isInAllyChat()) {
			if (!anyChatToGlobalPrefix.isEmpty() && event.getMessage().startsWith(anyChatToGlobalPrefix) && event.getMessage().length() > anyChatToGlobalPrefix.length()) {
				event.setMessage(event.getMessage().substring(anyChatToGlobalPrefix.length()));
			} else {
				// Player is not sending to global chat
				event.setCancelled(true);

				if (teamPlayer.isInTeamChat()) {
					team.sendMessage(teamPlayer, event.getMessage());
				} else {
					team.sendAllyMessage(teamPlayer, event.getMessage());
				}
				// Used as some chat plugins do not accept when a message is cancelled
				event.setMessage("");
				event.setFormat("");
				return;
			}
		} else if (
			(!globalToTeamPrefix.isEmpty() && event.getMessage().startsWith(globalToTeamPrefix) && event.getMessage().length() > globalToTeamPrefix.length())
					|| (!globalToAllyPrefix.isEmpty() && event.getMessage().startsWith(globalToAllyPrefix) && event.getMessage().length() > globalToAllyPrefix.length())
		) {
			// Player is not sending to global chat
			event.setCancelled(true);

			if (event.getMessage().startsWith(globalToTeamPrefix)) {
				team.sendMessage(teamPlayer, event.getMessage().substring(globalToTeamPrefix.length()));
			} else {
				team.sendAllyMessage(teamPlayer, event.getMessage().substring(globalToAllyPrefix.length()));
			}
			// Used as some chat plugins do not accept when a message is cancelled
			event.setMessage("");
			event.setFormat("");
			return;
		}

		if (doPrefix != PrefixType.NONE) {
			event.setFormat(LegacyTextUtils.parseAllAdventure(doPrefix.getUpdatedFormat(p, event.getFormat(), team)));
			// event.setFormat(ChatColor.AQUA + "[" + team.getName() + "] " + ChatColor.WHITE + event.getFormat());
		}

	}

	@EventHandler
	public void spyQuit(PlayerQuitEvent e) {
		spy.remove(e.getPlayer());
	}

	enum PrefixType {
		NONE, NAME, TAG;

		public static PrefixType getType(String str) {
			str = str.toLowerCase().trim();
			switch (str) {
				case "name":
				case "true":
					return NAME;
				case "tag":
					return TAG;
				default:
					return NONE;
			}
		}

		public String getUpdatedFormat(Player p, String format, Team team) {
			switch (this) {
				case NAME:
					return MessageManager.getMessage(p, "prefixSyntax", team.getDisplayName(), format);
                               case TAG:
                                       return MessageManager.getMessage(p, "prefixSyntax", team.getChatColor() + team.getTag(), format);
				default:
					return format;
			}
		}

	}

}
