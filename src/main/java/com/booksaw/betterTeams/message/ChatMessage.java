package com.booksaw.betterTeams.message;

import static java.util.Objects.requireNonNull;
import java.util.Collection;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.text.Formatter;
import com.booksaw.betterTeams.util.ColorConversionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ChatMessage extends StaticComponentHolderMessage {

	@Getter
	private final Team team;

	@Getter
	private final TeamPlayer teamPlayer;

	@Getter
	private final Component spyMessage;

	public void sendSpyMessage(CommandSender recipient) {
		MessageManager.sendFullMessage(recipient, spyMessage);
	}
	
	public void sendSpyMessage(Collection<? extends CommandSender> recipients) {
		MessageManager.sendFullMessage(recipients, spyMessage);
	}

	private ChatMessage(Team team, TeamPlayer teamPlayer, Component message, Component spyMessage) {
		super(message);
		this.team = team;
		this.teamPlayer = teamPlayer;
		this.spyMessage = spyMessage;
	}

	public static ChatMessage teamChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @NotNull String message) {
		return teamChat(team, teamPlayer, null, message);
	}

	public static ChatMessage teamChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @Nullable String prefix, @NotNull String message) {
		if (!requireNonNull(team, "The provided team cannot be null.").getMembers().getClone().contains(requireNonNull(teamPlayer, "The provided team player cannot be null."))) {
			throw new IllegalArgumentException("The provided team player must be a member of the provided team.");
		}
		Player player = teamPlayer.getPlayer().getPlayer();
		if (player == null) {
			throw new IllegalArgumentException("The provided team player must be online.");
		}
		requireNonNull(message, "Team chat message cannot be null.");

		// Pełny Component API approach
		Component teamPrefix = buildTeamChatPrefix(team, teamPlayer, prefix);
		Component playerMessage = Formatter.player(player).process(message);
		Component fullTeamMessage = Component.text()
			.append(teamPrefix)
			.append(Component.text(" "))
			.append(playerMessage)
			.build();

		Component spyMessage = buildSpyMessage(team, player, playerMessage, "spy.team");
		return new ChatMessage(team, teamPlayer, fullTeamMessage, spyMessage);
	}

	public static ChatMessage allyChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @Nullable String prefix, @NotNull String message) {
		if (!requireNonNull(team, "The provided team cannot be null.").getMembers().getClone().contains(requireNonNull(teamPlayer, "The provided team player cannot be null."))) {
			throw new IllegalArgumentException("The provided team player must be a member of the provided team.");
		}
		Player player = teamPlayer.getPlayer().getPlayer();
		if (player == null) {
			throw new IllegalArgumentException("The provided team player must be online.");
		}
		requireNonNull(message, "Team chat message cannot be null.");

		// Pełny Component API approach
		Component allyPrefix = buildAllyChatPrefix(team, teamPlayer, prefix);
		Component playerMessage = Formatter.player(player).process(message);
		Component fullAllyMessage = Component.text()
			.append(allyPrefix)
			.append(Component.text(" "))
			.append(playerMessage)
			.build();

		Component spyMessage = buildSpyMessage(team, player, playerMessage, "spy.ally");
		return new ChatMessage(team, teamPlayer, fullAllyMessage, spyMessage);
	}

	public static ChatMessage customSyntaxTeamChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @Nullable String prefix, @NotNull String message, @NotNull String syntax) {
		if (!requireNonNull(team, "The provided team cannot be null.").getMembers().getClone().contains(requireNonNull(teamPlayer, "The provided team player cannot be null."))) {
			throw new IllegalArgumentException("The provided team player must be a member of the provided team.");
		}
		Player player = teamPlayer.getPlayer().getPlayer();
		if (player == null) {
			throw new IllegalArgumentException("The provided team player must be online.");
		}
		requireNonNull(message, "Team chat message cannot be null.");
		requireNonNull(syntax, "Team chat syntax cannot be null.");

		// Custom syntax z Component API
		Component customPrefix = buildCustomSyntaxPrefix(teamPlayer, prefix, syntax);
		Component playerMessage = Formatter.player(player).process(message);
		Component fullCustomMessage = Component.text()
			.append(customPrefix)
			.append(Component.text(" "))
			.append(playerMessage)
			.build();

		Component spyMessage = buildSpyMessage(team, player, playerMessage, "spy.team");
		return new ChatMessage(team, teamPlayer, fullCustomMessage, spyMessage);
	}

	/**
	 * Buduje prefix dla team chat używając pełnego Component API
	 * POPRAWIONE: Używa var zamiast Component.Builder dla type inference
	 */
	private static Component buildTeamChatPrefix(Team team, TeamPlayer teamPlayer, String prefix) {
		Player player = teamPlayer.getPlayer().getPlayer();
		
		// Pobierz kolor drużyny jako TextColor (Adventure API)
		TextColor teamColor = team.getColor() != null ? 
			ColorConversionUtils.toNamed(team.getColor()) : 
			NamedTextColor.WHITE;

		// POPRAWIONE: Używamy var zamiast Component.Builder
		var prefixBuilder = Component.text();
		
		// Team tag z kolorem drużyny
		prefixBuilder.append(Component.text("[", NamedTextColor.GRAY))
				.append(Component.text(team.getTag() != null ? team.getTag() : team.getName(), teamColor))
				.append(Component.text("] ", NamedTextColor.GRAY));

		// Custom prefix jeśli istnieje
		if (prefix != null && !prefix.isEmpty()) {
			prefixBuilder.append(ColorConversionUtils.fromLegacy(prefix));
		}

		// Player display name (Adventure API zamiast deprecated getDisplayName())
		prefixBuilder.append(player.displayName())
				.append(Component.text(":", NamedTextColor.WHITE));

		return prefixBuilder.build();
	}

	/**
	 * Buduje prefix dla ally chat używając pełnego Component API
	 * POPRAWIONE: Bezpośrednie fluent API
	 */
	private static Component buildAllyChatPrefix(Team team, TeamPlayer teamPlayer, String prefix) {
		Player player = teamPlayer.getPlayer().getPlayer();
		
		TextColor teamColor = team.getColor() != null ? 
			ColorConversionUtils.toNamed(team.getColor()) : 
			NamedTextColor.WHITE;

		// POPRAWIONE: Bezpośrednie fluent API zamiast Builder variable
		return Component.text()
			// Ally indicator
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text("ALLY", NamedTextColor.GREEN))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Team name z kolorem
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text(team.getDisplayName(), teamColor))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Custom prefix jeśli istnieje
			.append(prefix != null && !prefix.isEmpty() ? 
				ColorConversionUtils.fromLegacy(prefix) : Component.empty())
			// Player display name (Adventure API)
			.append(player.displayName())
			.append(Component.text(":", NamedTextColor.WHITE))
			.build();
	}

	/**
	 * Buduje custom syntax prefix używając Component API
	 * POPRAWIONE: Bezpośrednie fluent API
	 */
	private static Component buildCustomSyntaxPrefix(TeamPlayer teamPlayer, String prefix, String syntax) {
		Player player = teamPlayer.getPlayer().getPlayer();
		
		// Konwertuj legacy syntax na Component używając ColorConversionUtils
		Component syntaxComponent = ColorConversionUtils.fromLegacy(syntax);
		
		// POPRAWIONE: Bezpośrednie fluent API
		return Component.text()
			.append(syntaxComponent)
			// Custom prefix jeśli istnieje
			.append(prefix != null && !prefix.isEmpty() ? 
				ColorConversionUtils.fromLegacy(prefix) : Component.empty())
			// Player display name (Adventure API)
			.append(player.displayName())
			.append(Component.text(":", NamedTextColor.WHITE))
			.build();
	}

	/**
	 * Buduje spy message używając pełnego Component API
	 * POPRAWIONE: Bezpośrednie fluent API z rich formatting
	 */
	private static Component buildSpyMessage(Team team, Player player, Component playerMessage, String spyType) {
		TextColor teamColor = team.getColor() != null ? 
			ColorConversionUtils.toNamed(team.getColor()) : 
			NamedTextColor.WHITE;

		return Component.text()
			// Spy indicator
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text("SPY", NamedTextColor.YELLOW))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Team name z kolorem
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text(team.getName(), teamColor))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Player display name (Adventure API)
			.append(player.displayName())
			.append(Component.text(": ", NamedTextColor.WHITE))
			// Actual message
			.append(playerMessage)
			.build();
	}

	@Override
	public void sendMessage(CommandSender recipient) {
		MessageManager.sendFullMessage(recipient, message);
	}

	@Override
	public void sendMessage(Collection<? extends CommandSender> recipients) {
		MessageManager.sendFullMessage(recipients, message);
	}

	@Override
	public void sendTitle(Player recipient) {
		MessageManager.sendFullTitle(recipient, message);
	}

	@Override
	public void sendTitle(Collection<Player> recipients) {
		MessageManager.sendFullTitle(recipients, message);
	}
}
