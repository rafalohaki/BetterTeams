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
 * Utility class for converting between different color APIs in Minecraft ecosystem.
 * Updated for Paper 1.21.7 with Adventure API support, eliminating Bungee API dependencies.
 * 
 * @author BetterTeams
 * @since 4.13.4
 */
@SuppressWarnings("deprecation") // ChatColor jest potrzebny dla kompatybilności
public final class ColorConversionUtils {
    
    // Używamy HashMap zamiast Map.of() aby uniknąć limitu 10 argumentów
    private static final Map<ChatColor, NamedTextColor> CHAT_TO_TEXT = new HashMap<>();
    private static final Map<NamedTextColor, ChatColor> TEXT_TO_CHAT = new HashMap<>();
    
    /** Default fallback color when conversion fails or input is null */
    private static final NamedTextColor DEFAULT_NAMED_COLOR = NamedTextColor.WHITE;
    private static final ChatColor DEFAULT_CHAT_COLOR = ChatColor.WHITE;

    static {
        // Inicjalizacja mapowań kolorów
        initializeColorMappings();
    }
    
    /**
     * Inicjalizuje mapowania kolorów - rozwiązuje problem z Map.of() limit
     */
    private static void initializeColorMappings() {
        // Wszystkie 16 kolorów Minecraft
        addColorMapping(ChatColor.BLACK, NamedTextColor.BLACK);
        addColorMapping(ChatColor.DARK_BLUE, NamedTextColor.DARK_BLUE);
        addColorMapping(ChatColor.DARK_GREEN, NamedTextColor.DARK_GREEN);
        addColorMapping(ChatColor.DARK_AQUA, NamedTextColor.DARK_AQUA);
        addColorMapping(ChatColor.DARK_RED, NamedTextColor.DARK_RED);
        addColorMapping(ChatColor.DARK_PURPLE, NamedTextColor.DARK_PURPLE);
        addColorMapping(ChatColor.GOLD, NamedTextColor.GOLD);
        addColorMapping(ChatColor.GRAY, NamedTextColor.GRAY);
        addColorMapping(ChatColor.DARK_GRAY, NamedTextColor.DARK_GRAY);
        addColorMapping(ChatColor.BLUE, NamedTextColor.BLUE);
        addColorMapping(ChatColor.GREEN, NamedTextColor.GREEN);
        addColorMapping(ChatColor.AQUA, NamedTextColor.AQUA);
        addColorMapping(ChatColor.RED, NamedTextColor.RED);
        addColorMapping(ChatColor.LIGHT_PURPLE, NamedTextColor.LIGHT_PURPLE);
        addColorMapping(ChatColor.YELLOW, NamedTextColor.YELLOW);
        addColorMapping(ChatColor.WHITE, NamedTextColor.WHITE);
    }
    
    /**
     * Dodaje bidirektional mapping między kolorami
     */
    private static void addColorMapping(ChatColor chatColor, NamedTextColor namedColor) {
        CHAT_TO_TEXT.put(chatColor, namedColor);
        TEXT_TO_CHAT.put(namedColor, chatColor);
    }

    private ColorConversionUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // === ZACHOWANE METODY - Pełna kompatybilność z Team.java ===

    /**
     * Converts a Bukkit ChatColor to Adventure NamedTextColor.
     */
    public static NamedTextColor toNamed(ChatColor chatColor) {
        if (chatColor == null) {
            return DEFAULT_NAMED_COLOR;
        }
        return CHAT_TO_TEXT.getOrDefault(chatColor, DEFAULT_NAMED_COLOR);
    }

    /**
     * Converts a Bukkit ChatColor to Adventure NamedTextColor with strict validation.
     */
    public static NamedTextColor toNamed(ChatColor chatColor, boolean strict) {
        if (strict && chatColor == null) {
            throw new IllegalArgumentException("ChatColor cannot be null in strict mode");
        }
        
        if (chatColor == null) {
            return DEFAULT_NAMED_COLOR;
        }
        
        NamedTextColor result = CHAT_TO_TEXT.get(chatColor);
        if (strict && result == null) {
            throw new IllegalArgumentException("Unknown ChatColor: " + chatColor);
        }
        
        return result != null ? result : DEFAULT_NAMED_COLOR;
    }

    /**
     * Converts an Adventure NamedTextColor to Bukkit ChatColor.
     */
    public static ChatColor toChat(NamedTextColor color) {
        if (color == null) {
            return DEFAULT_CHAT_COLOR;
        }
        return TEXT_TO_CHAT.getOrDefault(color, DEFAULT_CHAT_COLOR);
    }

    /**
     * Converts an Adventure TextColor to Bukkit ChatColor.
     */
    public static ChatColor toChat(TextColor color) {
        if (color == null) {
            return DEFAULT_CHAT_COLOR;
        }
        
        if (color instanceof NamedTextColor) {
            return toChat((NamedTextColor) color);
        }
        
        return findClosestChatColor(color);
    }

    /**
     * Converts an Adventure TextColor to NamedTextColor.
     */
    public static NamedTextColor toNamed(TextColor color) {
        if (color == null) {
            return DEFAULT_NAMED_COLOR;
        }
        
        if (color instanceof NamedTextColor) {
            return (NamedTextColor) color;
        }
        
        return toNamed(toChat(color));
    }

    /**
     * Creates a NamedTextColor from a color character.
     */
    public static NamedTextColor fromChar(char c) {
        ChatColor chatColor = ChatColor.getByChar(c);
        return toNamed(chatColor);
    }

    /**
     * Returns the character representation of a NamedTextColor.
     */
    public static char toChar(NamedTextColor color) {
        if (color == null) {
            return 'f'; // white
        }
        char result = toChat(color).getChar();
        return result;
    }

    // === METODY KOMPATYBILNOŚCI Z LegacyTextUtils ===

    /**
     * Kompatybilność z LegacyTextUtils.colorToAdventure() - konwertuje Component na String
     */
    public static String componentToLegacyString(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    /**
     * Kompatybilność z LegacyTextUtils.colorToAdventure() - konwertuje Component na ChatColor reprezentację
     */
    public static String componentToColorString(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    // === NOWE METODY - Adventure API zamiast Bungee API ===

    /**
     * Converts a NamedTextColor to Adventure Component (zastępuje Bungee API).
     */
    public static Component toComponent(NamedTextColor color, String text) {
        if (color == null) {
            return Component.text(text != null ? text : "");
        }
        return Component.text(text != null ? text : "", color);
    }

    /**
     * Converts a TextColor to Adventure Component (zastępuje Bungee API).
     */
    public static Component toComponent(TextColor color, String text) {
        if (color == null) {
            return Component.text(text != null ? text : "");
        }
        return Component.text(text != null ? text : "", color);
    }

    /**
     * Tworzy Component z hex kolorem (nowa funkcjonalność).
     */
    public static Component createHexComponent(String text, String hexColor) {
        if (text == null) text = "";
        if (hexColor == null || hexColor.isEmpty()) {
            return Component.text(text);
        }
        
        try {
            TextColor color = TextColor.fromHexString(hexColor.startsWith("#") ? hexColor : "#" + hexColor);
            return Component.text(text, color);
        } catch (IllegalArgumentException e) {
            return Component.text(text);
        }
    }

    /**
     * Konwertuje legacy string na Component (zastępuje Bungee deserializer).
     */
    public static Component fromLegacy(String legacyText) {
        if (legacyText == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyText);
    }

    /**
     * Konwertuje Component na legacy string (dla kompatybilności).
     */
    public static String toLegacy(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    // === ZACHOWANE METODY BUNGEE (emulowane przez Adventure API) ===

    /**
     * Emuluje Bungee ChatColor przez Adventure API - zachowuje kompatybilność.
     * @deprecated Użyj toComponent() zamiast tego
     */
    @Deprecated
    public static String toBungeeString(NamedTextColor color) {
        if (color == null) return "";
        return toLegacy(Component.text("", color));
    }

    /**
     * Emuluje Bungee ChatColor przez Adventure API - zachowuje kompatybilność.
     * @deprecated Użyj toComponent() zamiast tego
     */
    @Deprecated
    public static String toBungeeString(TextColor color) {
        return toBungeeString(toNamed(color));
    }

    /**
     * Zachowuje API kompatybilność - zwraca String zamiast Component dla kompatybilności z Team.java
     */
    public static String toBungee(NamedTextColor color) {
        if (color == null) return "";
        return toChat(color).toString();
    }

    /**
     * Zachowuje API kompatybilność - zwraca String zamiast Component dla kompatybilności z Team.java
     */
    public static String toBungee(TextColor color) {
        return toBungee(toNamed(color));
    }

    // === UTILITY METHODS ===

    /**
     * Sprawdza czy string jest hex kolorem.
     */
    public static boolean isHexColor(String color) {
        if (color == null || color.isEmpty()) return false;
        String hex = color.startsWith("#") ? color.substring(1) : color;
        return hex.length() == 6 && hex.matches("[0-9A-Fa-f]+");
    }

    /**
     * Tworzy TextColor z hex stringa.
     */
    public static TextColor fromHex(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return DEFAULT_NAMED_COLOR;
        }
        
        try {
            return TextColor.fromHexString(hexColor.startsWith("#") ? hexColor : "#" + hexColor);
        } catch (IllegalArgumentException e) {
            return DEFAULT_NAMED_COLOR;
        }
    }

    /**
     * Finds the closest ChatColor for a given custom TextColor.
     */
    private static ChatColor findClosestChatColor(TextColor color) {
        Objects.requireNonNull(color, "Color cannot be null");
        
        int targetRed = color.red();
        int targetGreen = color.green();
        int targetBlue = color.blue();
        
        ChatColor closest = DEFAULT_CHAT_COLOR;
        double minDistance = Double.MAX_VALUE;
        
        for (Map.Entry<ChatColor, NamedTextColor> entry : CHAT_TO_TEXT.entrySet()) {
            NamedTextColor namedColor = entry.getValue();
            double distance = colorDistance(targetRed, targetGreen, targetBlue, 
                                          namedColor.red(), namedColor.green(), namedColor.blue());
            
            if (distance < minDistance) {
                minDistance = distance;
                closest = entry.getKey();
            }
        }
        
        return closest;
    }

    /**
     * Calculates the Euclidean distance between two colors in RGB space.
     */
    private static double colorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        return Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));
    }
}
