package com.booksaw.betterTeams.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Clean utility for color conversion between ChatColor and Adventure API.
 * Focused on Paper 1.21.7+ with minimal compatibility overhead.
 * 
 * @author BetterTeams
 * @since 4.13.4
 */
@SuppressWarnings("deprecation")
public final class ColorConversionUtils {
    
    private static final Map<ChatColor, NamedTextColor> CHAT_TO_NAMED = new HashMap<>();
    private static final Map<NamedTextColor, ChatColor> NAMED_TO_CHAT = new HashMap<>();
    
    static {
        // 16 podstawowych kolorów Minecraft
        map(ChatColor.BLACK, NamedTextColor.BLACK);
        map(ChatColor.DARK_BLUE, NamedTextColor.DARK_BLUE);
        map(ChatColor.DARK_GREEN, NamedTextColor.DARK_GREEN);
        map(ChatColor.DARK_AQUA, NamedTextColor.DARK_AQUA);
        map(ChatColor.DARK_RED, NamedTextColor.DARK_RED);
        map(ChatColor.DARK_PURPLE, NamedTextColor.DARK_PURPLE);
        map(ChatColor.GOLD, NamedTextColor.GOLD);
        map(ChatColor.GRAY, NamedTextColor.GRAY);
        map(ChatColor.DARK_GRAY, NamedTextColor.DARK_GRAY);
        map(ChatColor.BLUE, NamedTextColor.BLUE);
        map(ChatColor.GREEN, NamedTextColor.GREEN);
        map(ChatColor.AQUA, NamedTextColor.AQUA);
        map(ChatColor.RED, NamedTextColor.RED);
        map(ChatColor.LIGHT_PURPLE, NamedTextColor.LIGHT_PURPLE);
        map(ChatColor.YELLOW, NamedTextColor.YELLOW);
        map(ChatColor.WHITE, NamedTextColor.WHITE);
    }
    
    private static void map(ChatColor chat, NamedTextColor named) {
        CHAT_TO_NAMED.put(chat, named);
        NAMED_TO_CHAT.put(named, chat);
    }

    private ColorConversionUtils() {}

    // === PODSTAWOWE KONWERSJE ===
    
    /**
     * ChatColor → NamedTextColor
     */
    public static NamedTextColor toNamed(ChatColor chatColor) {
        return CHAT_TO_NAMED.getOrDefault(chatColor, NamedTextColor.WHITE);
    }

    /**
     * NamedTextColor → ChatColor
     */
    public static ChatColor toChat(NamedTextColor namedColor) {
        return NAMED_TO_CHAT.getOrDefault(namedColor, ChatColor.WHITE);
    }

    /**
     * TextColor → ChatColor (z obsługą hex)
     */
    public static ChatColor toChat(TextColor textColor) {
        if (textColor instanceof NamedTextColor) {
            return toChat((NamedTextColor) textColor);
        }
        return findClosestChatColor(textColor);
    }

    /**
     * TextColor → NamedTextColor
     */
    public static NamedTextColor toNamed(TextColor textColor) {
        if (textColor instanceof NamedTextColor) {
            return (NamedTextColor) textColor;
        }
        return toNamed(toChat(textColor));
    }

    // === ADVENTURE API METHODS ===
    
    /**
     * Tworzy kolorowy Component
     */
    public static Component text(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    /**
     * Tworzy kolorowy Component z TextColor
     */
    public static Component text(String text, TextColor color) {
        return Component.text(text, color);
    }

    /**
     * Tworzy Component z hex kolorem
     */
    public static Component hexText(String text, String hexColor) {
        try {
            TextColor color = TextColor.fromHexString(
                hexColor.startsWith("#") ? hexColor : "#" + hexColor
            );
            return Component.text(text, color);
        } catch (IllegalArgumentException e) {
            return Component.text(text);
        }
    }

    // === LEGACY SUPPORT ===
    
    /**
     * Legacy string → Component
     */
    public static Component fromLegacy(String legacyText) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyText);
    }

    /**
     * Component → Legacy string
     */
    public static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    /**
     * Char → NamedTextColor
     */
    public static NamedTextColor fromChar(char c) {
        return toNamed(ChatColor.getByChar(c));
    }

    /**
     * NamedTextColor → Char
     */
    public static char toChar(NamedTextColor color) {
        return toChat(color).getChar();
    }

    // === HEX UTILITIES ===
    
    /**
     * String → TextColor (hex support)
     */
    public static TextColor fromHex(String hexColor) {
        try {
            return TextColor.fromHexString(
                hexColor.startsWith("#") ? hexColor : "#" + hexColor
            );
        } catch (IllegalArgumentException e) {
            return NamedTextColor.WHITE;
        }
    }

    /**
     * Sprawdza czy string to hex kolor
     */
    public static boolean isHex(String color) {
        if (color == null || color.isEmpty()) return false;
        String hex = color.startsWith("#") ? color.substring(1) : color;
        return hex.length() == 6 && hex.matches("[0-9A-Fa-f]+");
    }

    // === PRIVATE HELPERS ===
    
    /**
     * Znajduje najbliższy ChatColor dla custom TextColor
     */
    private static ChatColor findClosestChatColor(TextColor color) {
        if (color == null) return ChatColor.WHITE;
        
        ChatColor closest = ChatColor.WHITE;
        double minDistance = Double.MAX_VALUE;
        
        for (Map.Entry<ChatColor, NamedTextColor> entry : CHAT_TO_NAMED.entrySet()) {
            double distance = colorDistance(color, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                closest = entry.getKey();
            }
        }
        
        return closest;
    }

    /**
     * Oblicza odległość między kolorami
     */
    private static double colorDistance(TextColor c1, TextColor c2) {
        return Math.sqrt(
            Math.pow(c1.red() - c2.red(), 2) +
            Math.pow(c1.green() - c2.green(), 2) +
            Math.pow(c1.blue() - c2.blue(), 2)
        );
    }
}
