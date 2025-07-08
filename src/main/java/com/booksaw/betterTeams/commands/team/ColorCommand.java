package com.booksaw.betterTeams.commands.team;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.util.ColorConversionUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.format.NamedTextColor;

@SuppressWarnings("deprecation") // ChatColor potrzebny dla kompatybilności
public class ColorCommand extends TeamSubCommand {

	private final Set<Character> alwaysBanned = new HashSet<>(Arrays.asList('l', 'n', 'o', 'k', 'n', 'r'));
	private final Set<Character> banned = new HashSet<>(alwaysBanned);

	public ColorCommand() {
		banned.addAll(Main.plugin.getConfig().getString("bannedColors").chars().mapToObj(c -> (char) c)
				.collect(Collectors.toList()));
	}

	@Override
	public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

		NamedTextColor namedColor = parseColorFromInput(args[0]);
		
		if (namedColor == null) {
			return new CommandResponse("color.fail");
		}

		// Sprawdź czy kolor jest zbanowany (konwertuj na ChatColor dla kompatybilności z banned chars)
		ChatColor legacyColor = ColorConversionUtils.toChat(namedColor);
		if (banned.contains(legacyColor.getChar())) {
			return new CommandResponse("color.banned");
		}

		team.setColor(namedColor);
		return new CommandResponse(true, "color.success");
	}

	/**
	 * Parsuje input gracza na NamedTextColor używając Adventure API
	 */
	private NamedTextColor parseColorFromInput(String input) {
		if (input == null || input.isEmpty()) {
			return null;
		}

		// Spróbuj najpierw jako nazwę koloru (np. "RED", "BLUE")
		try {
			ChatColor chatColor = ChatColor.valueOf(input.toUpperCase());
			return ColorConversionUtils.toNamed(chatColor);
		} catch (IllegalArgumentException e) {
			// Ignoruj błąd - spróbuj jako character
		}

		// Spróbuj jako character code (np. "c", "9") 
		if (input.length() == 1) {
			char colorChar = input.charAt(0);
			return ColorConversionUtils.fromChar(colorChar);
		}

		return null;
	}

	@Override
	public String getCommand() {
		return "color";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public String getNode() {
		return "color";
	}

	@Override
	public String getHelp() {
		return "Change your teams color";
	}

	@Override
	public String getArguments() {
		return "<color code>";
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			// Używamy NamedTextColor zamiast deprecated ChatColor.values()
			addAvailableColors(options, args[0]);
		}
	}

	/**
	 * Dodaje dostępne kolory do tab completion
	 */
	private void addAvailableColors(List<String> options, String input) {
		// Lista wszystkich named colors
		NamedTextColor[] namedColors = {
			NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN,
			NamedTextColor.DARK_AQUA, NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE,
			NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_GRAY,
			NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
			NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW,
			NamedTextColor.WHITE
		};

		for (NamedTextColor namedColor : namedColors) {
			// Konwertuj na ChatColor żeby sprawdzić banned characters
			ChatColor chatColor = ColorConversionUtils.toChat(namedColor);
			
			if (!banned.contains(chatColor.getChar())) {
				String colorName = chatColor.name().toLowerCase();
				if (colorName.startsWith(input.toLowerCase())) {
					options.add(colorName);
				}
			}
		}
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.OWNER;
	}
}
