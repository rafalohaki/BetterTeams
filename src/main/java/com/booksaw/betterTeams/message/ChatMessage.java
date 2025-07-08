package com.booksaw.betterTeams.message;

import static java.util.Objects.requireNonNull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

	// Cache dla kolorów drużyn - lepsze performance
	private static final Map<TextColor, TextColor> COLOR_CACHE = new ConcurrentHashMap<>();

	// Enum dla spy types zamiast String literals
	public enum SpyType {
		TEAM("spy.team"),
		ALLY("spy.ally");
		
		private final String messageKey;
		
		SpyType(String messageKey) {
			this.messageKey = messageKey;
		}
		
		public String getMessageKey() {
			return messageKey;
		}
	}

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

	/**
	 * Tworzy wiadomość team chat.
	 * 
	 * @param team drużyna
	 * @param teamPlayer gracz w drużynie
	 * @param message treść wiadomości
	 * @return ChatMessage dla team chat
	 */
	public static ChatMessage teamChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @NotNull String message) {
		return teamChat(team, teamPlayer, null, message);
	}

	/**
	 * Tworzy wiadomość team chat z prefiksem.
	 * 
	 * @param team drużyna
	 * @param teamPlayer gracz w drużynie
	 * @param prefix opcjonalny prefix
	 * @param message treść wiadomości
	 * @return ChatMessage dla team chat
	 */
	public static ChatMessage teamChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @Nullable String prefix, @NotNull String message) {
		Player player = validateAndGetPlayer(team, teamPlayer);
		requireNonNull(message, "Team chat message cannot be null.");

		Component teamPrefix = buildTeamChatPrefix(team, teamPlayer, prefix);
		Component playerMessage = Formatter.player(player).process(message);
		Component fullTeamMessage = Component.text()
			.append(teamPrefix)
			.append(Component.text(" "))
			.append(playerMessage)
			.build();

		Component spyMessage = buildSpyMessage(team, player, playerMessage, SpyType.TEAM);
		return new ChatMessage(team, teamPlayer, fullTeamMessage, spyMessage);
	}

	/**
	 * Tworzy wiadomość ally chat z prefiksem.
	 * 
	 * @param team drużyna
	 * @param teamPlayer gracz w drużynie
	 * @param prefix opcjonalny prefix
	 * @param message treść wiadomości
	 * @return ChatMessage dla ally chat
	 */
	public static ChatMessage allyChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @Nullable String prefix, @NotNull String message) {
		Player player = validateAndGetPlayer(team, teamPlayer);
		requireNonNull(message, "Ally chat message cannot be null.");

		Component allyPrefix = buildAllyChatPrefix(team, teamPlayer, prefix);
		Component playerMessage = Formatter.player(player).process(message);
		Component fullAllyMessage = Component.text()
			.append(allyPrefix)
			.append(Component.text(" "))
			.append(playerMessage)
			.build();

		Component spyMessage = buildSpyMessage(team, player, playerMessage, SpyType.ALLY);
		return new ChatMessage(team, teamPlayer, fullAllyMessage, spyMessage);
	}

	/**
	 * Tworzy wiadomość team chat z custom syntax.
	 * 
	 * @param team drużyna
	 * @param teamPlayer gracz w drużynie
	 * @param prefix opcjonalny prefix
	 * @param message treść wiadomości
	 * @param syntax custom syntax
	 * @return ChatMessage z custom syntax
	 */
	public static ChatMessage customSyntaxTeamChat(@NotNull Team team, @NotNull TeamPlayer teamPlayer, @Nullable String prefix, @NotNull String message, @NotNull String syntax) {
		Player player = validateAndGetPlayer(team, teamPlayer);
		requireNonNull(message, "Team chat message cannot be null.");
		requireNonNull(syntax, "Team chat syntax cannot be null.");

		Component customPrefix = buildCustomSyntaxPrefix(teamPlayer, prefix, syntax);
		Component playerMessage = Formatter.player(player).process(message);
		Component fullCustomMessage = Component.text()
			.append(customPrefix)
			.append(Component.text(" "))
			.append(playerMessage)
			.build();

		Component spyMessage = buildSpyMessage(team, player, playerMessage, SpyType.TEAM);
		return new ChatMessage(team, teamPlayer, fullCustomMessage, spyMessage);
	}

	/**
	 * Waliduje parametry i zwraca gracza online.
	 * Wydzielona metoda eliminuje powtarzający się kod.
	 */
	private static Player validateAndGetPlayer(@NotNull Team team, @NotNull TeamPlayer teamPlayer) {
		requireNonNull(team, "The provided team cannot be null.");
		requireNonNull(teamPlayer, "The provided team player cannot be null.");
		
		if (!team.getMembers().getClone().contains(teamPlayer)) {
			throw new IllegalArgumentException("The provided team player must be a member of the provided team.");
		}
		
		Player player = teamPlayer.getPlayer().getPlayer();
		if (player == null) {
			throw new IllegalArgumentException("The provided team player must be online.");
		}
		
		return player;
	}

	/**
	 * Pobiera kolor drużyny z cache'owaniem dla lepszej performance.
	 */
	private static TextColor getTeamColor(Team team) {
		TextColor teamColor = team.getColor();
		if (teamColor == null) {
			return NamedTextColor.WHITE;
		}
		
		// Cache conversion dla lepszej performance
		return COLOR_CACHE.computeIfAbsent(teamColor, ColorConversionUtils::toNamed);
	}

	/**
	 * Buduje prefix dla team chat używając pełnego Component API.
	 */
	private static Component buildTeamChatPrefix(Team team, TeamPlayer teamPlayer, String prefix) {
		Player player = teamPlayer.getPlayer().getPlayer();
		TextColor teamColor = getTeamColor(team);

		var prefixBuilder = Component.text();
		
		// Team tag z kolorem drużyny
		prefixBuilder.append(Component.text("[", NamedTextColor.GRAY))
				.append(Component.text(team.getTag() != null ? team.getTag() : team.getName(), teamColor))
				.append(Component.text("] ", NamedTextColor.GRAY));

		// Custom prefix jeśli istnieje
		if (prefix != null && !prefix.isEmpty()) {
			prefixBuilder.append(ColorConversionUtils.fromLegacy(prefix));
		}

		// Player display name
		prefixBuilder.append(player.displayName())
				.append(Component.text(":", NamedTextColor.WHITE));

		return prefixBuilder.build();
	}

	/**
	 * Buduje prefix dla ally chat używając pełnego Component API.
	 */
	private static Component buildAllyChatPrefix(Team team, TeamPlayer teamPlayer, String prefix) {
		Player player = teamPlayer.getPlayer().getPlayer();
		TextColor teamColor = getTeamColor(team);

		return Component.text()
			// Ally indicator
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text("ALLY", NamedTextColor.GREEN))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Team name z kolorem
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text(team.getName(), teamColor))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Custom prefix jeśli istnieje
			.append(prefix != null && !prefix.isEmpty() ? 
				ColorConversionUtils.fromLegacy(prefix) : Component.empty())
			// Player display name
			.append(player.displayName())
			.append(Component.text(":", NamedTextColor.WHITE))
			.build();
	}

	/**
	 * Buduje custom syntax prefix używając Component API.
	 */
	private static Component buildCustomSyntaxPrefix(TeamPlayer teamPlayer, String prefix, String syntax) {
		Player player = teamPlayer.getPlayer().getPlayer();
		Component syntaxComponent = ColorConversionUtils.fromLegacy(syntax);
		
		return Component.text()
			.append(syntaxComponent)
			// Custom prefix jeśli istnieje
			.append(prefix != null && !prefix.isEmpty() ? 
				ColorConversionUtils.fromLegacy(prefix) : Component.empty())
			// Player display name
			.append(player.displayName())
			.append(Component.text(":", NamedTextColor.WHITE))
			.build();
	}

	/**
	 * Buduje spy message używając pełnego Component API.
	 */
	private static Component buildSpyMessage(Team team, Player player, Component playerMessage, SpyType spyType) {
		TextColor teamColor = getTeamColor(team);

		return Component.text()
			// Spy indicator
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text("SPY", NamedTextColor.YELLOW))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Team name z kolorem
			.append(Component.text("[", NamedTextColor.GRAY))
			.append(Component.text(team.getName(), teamColor))
			.append(Component.text("] ", NamedTextColor.GRAY))
			// Player display name
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
