package com.booksaw.betterTeams;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.booksaw.betterTeams.commands.ParentCommand;
import com.booksaw.betterTeams.commands.SubCommand;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Enhanced command registration system with NUCLEAR thread safety approach
 * for Paper 1.21.8+ and Leaf compatibility.
 * 
 * CRITICAL FIXES APPLIED:
 * - NUCLEAR OPTION: ALL team commands forced to run synchronously
 * - Complete elimination of async scheduling for scoreboard operations
 * - Enhanced main thread enforcement to prevent waypoint connection errors
 * - Comprehensive plugin state validation and error handling
 * - Fixed command detection for ALL team-related operations including COLOR
 * - Simplified execution logic with bulletproof thread safety
 * 
 * @author booksaw (original)
 * @author enhanced for Paper 1.21.8+ compatibility
 * @version 2.0.0 - NUCLEAR THREAD SAFETY
 * @since BetterTeams 4.13.4+
 */
@Getter
@Setter
public class BooksawCommand extends BukkitCommand {

    private SubCommand subCommand;

    /**
     * Constructs a new BooksawCommand with enhanced error handling
     * 
     * @param command Command name
     * @param subCommand Associated subcommand handler
     * @param permission Required permission
     * @param description Command description
     * @param aliases Command aliases
     */
    public BooksawCommand(String command, SubCommand subCommand, String permission, String description,
            List<String> aliases) {
        super(command);
        this.description = description;
        this.usageMessage = "/" + command + " help";
        setPermission(permission);
        setAliases(aliases);
        this.subCommand = subCommand;

        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            commandMap.register(command, this);
        } catch (Exception e) {
            Main.plugin.getLogger().severe("Failed to register command '" + command + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * NUCLEAR OPTION: Enhanced command execution with FORCED synchronous execution
     * to eliminate ALL threading violations on Paper 1.21.8+ and Leaf servers.
     * 
     * This completely eliminates async scheduling for team commands to prevent:
     * - "Cannot create waypoint connections off-main" errors
     * - IllegalPluginAccessException during plugin disable
     * - Scoreboard threading violations
     * 
     * @param sender Command sender
     * @param label Command label used
     * @param args Command arguments
     * @return true if command was processed successfully
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        // Process entity selectors first
        if (checkPointers(sender, label, args)) {
            return true;
        }

        // CRITICAL: Plugin state validation
        if (!isPluginEnabled()) {
            sender.sendMessage("§cPlugin is disabled - command cannot be executed");
            return false;
        }

        // NUCLEAR OPTION: ALL team commands MUST run synchronously
        // This completely eliminates async execution that causes waypoint errors
        try {
            if (Bukkit.isPrimaryThread()) {
                // Already on main thread - execute directly
                runExecution(sender, label, args);
            } else {
                // Force synchronous execution on main thread
                if (!isPluginEnabled()) {
                    sender.sendMessage("§cPlugin disabled during execution setup");
                    return false;
                }
                
                Bukkit.getScheduler().runTask(Main.plugin, () -> {
                    if (isPluginEnabled()) {
                        runExecution(sender, label, args);
                    } else {
                        sender.sendMessage("§cExecution cancelled - plugin disabled");
                        logSkippedExecution("main thread", label, args);
                    }
                });
            }
        } catch (IllegalPluginAccessException e) {
            handleSchedulingException(sender, label, args, e);
            return false;
        } catch (Exception e) {
            handleUnexpectedException(sender, label, args, e);
            return false;
        }

        return true;
    }

    /**
     * DEPRECATED: Async scheduling completely disabled for thread safety
     * This method should never be called in the nuclear approach
     */
    @Deprecated
    private void scheduleAsyncExecution(CommandSender sender, String label, String[] args) {
        throw new UnsupportedOperationException("Async execution disabled for thread safety - all commands run synchronously");
    }

    /**
     * DEPRECATED: All scheduling now handled in main execute method
     * This method is no longer used in the nuclear approach
     */
    @Deprecated
    private void scheduleMainThreadExecution(CommandSender sender, String label, String[] args) {
        throw new UnsupportedOperationException("Scheduling methods deprecated - use main execute method");
    }

    /**
     * NUCLEAR APPROACH: All team commands require main thread execution
     * This eliminates ANY possibility of scoreboard threading violations
     * 
     * @param args Command arguments
     * @return true - ALL commands require main thread in nuclear approach
     */
    private boolean requiresMainThreadExecution(String[] args) {
        // NUCLEAR OPTION: ALL team commands require main thread - no exceptions
        // This includes: create, join, leave, disband, kick, promote, demote, 
        // sethome, home, invite, accept, color, tag, title, description, 
        // name, open, ban, unban, warp, setwarp, delwarp, warps, echest,
        // deposit, withdraw, bal, baltop, top, rank, rankup, anchor, 
        // setanchor, pvp, setowner, chest, info, list, chat, ally, neutral
        return true;
    }

    /**
     * Enhanced command execution with comprehensive error handling
     */
    public void runExecution(CommandSender sender, String label, String[] args) {
        try {
            CommandResponse response;
            if (subCommand instanceof ParentCommand) {
                response = ((ParentCommand) subCommand).onCommand(sender, label, args, true);
            } else {
                response = subCommand.onCommand(sender, label, args);
            }

            if (response != null) {
                response.sendResponseMessage(sender);
            }
        } catch (Exception e) {
            Main.plugin.getLogger().severe(
                "Critical error executing command '/" + label + " " + String.join(" ", args) + "': " + e.getMessage());
            Main.plugin.getLogger().severe(
                "Please report this issue: https://github.com/booksaw/BetterTeams/issues/new/choose");
            e.printStackTrace();
            MessageManager.sendMessage(sender, "internalError");
        }
    }

    /**
     * Enhanced tab completion with type safety
     */
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        List<String> options = new ArrayList<>();
        try {
            subCommand.onTabComplete(options, sender, label, args);
        } catch (Exception e) {
            Main.plugin.getLogger().warning("Error during tab completion for /" + label + ": " + e.getMessage());
        }
        return options;
    }

    /**
     * Enhanced entity selector processing with proper error handling
     */
    public boolean checkPointers(@NotNull CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("betterteams.admin.selector")) {
            return false;
        }

        for (String str : args) {
            if (!str.startsWith("@")) {
                continue;
            }

            boolean found = false;
            try {
                for (Entity e : Bukkit.selectEntities(sender, str)) {
                    if (e instanceof Player) {
                        found = true;
                        String[] newArgs = args.clone();
                        for (int j = 0; j < newArgs.length; j++) {
                            if (newArgs[j].equals(str)) {
                                newArgs[j] = e.getName();
                            }
                        }
                        execute(sender, label, newArgs);
                    }
                }
            } catch (Exception e) {
                Main.plugin.getLogger().warning("Error processing entity selector '" + str + "': " + e.getMessage());
                return false;
            }

            return found;
        }

        return false;
    }

    // ===== UTILITY METHODS =====

    /**
     * Thread-safe plugin state check
     */
    private boolean isPluginEnabled() {
        try {
            return Main.plugin != null && Main.plugin.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handles scheduling exceptions with detailed logging
     */
    private void handleSchedulingException(CommandSender sender, String label, String[] args, 
            IllegalPluginAccessException e) {
        String commandStr = "/" + label + " " + String.join(" ", args);
        Main.plugin.getLogger().severe("CRITICAL: Failed to schedule task for command '" + commandStr + 
            "' - plugin disabled during scheduling: " + e.getMessage());
        sender.sendMessage("§cCommand failed: Plugin was disabled during execution.");
    }

    /**
     * Handles unexpected exceptions during command processing
     */
    private void handleUnexpectedException(CommandSender sender, String label, String[] args, Exception e) {
        String commandStr = "/" + label + " " + String.join(" ", args);
        Main.plugin.getLogger().severe("Unexpected error during command execution setup for '" + commandStr + "': " + e.getMessage());
        e.printStackTrace();
        sender.sendMessage("§cAn unexpected internal error occurred while processing your command.");
    }

    /**
     * Logs skipped execution for debugging purposes
     */
    private void logSkippedExecution(String threadType, String label, String[] args) {
        String commandStr = "/" + label + " " + String.join(" ", args);
        Main.plugin.getLogger().warning(threadType.substring(0, 1).toUpperCase() + threadType.substring(1) + 
            " task skipped - plugin disabled during execution for command: " + commandStr);
    }
}
