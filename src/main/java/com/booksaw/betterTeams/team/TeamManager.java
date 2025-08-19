package com.booksaw.betterTeams.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.customEvents.CreateTeamEvent;
import com.booksaw.betterTeams.customEvents.PurgeEvent;
import com.booksaw.betterTeams.customEvents.post.PostCreateTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostPurgeEvent;
import com.booksaw.betterTeams.events.ChestManagement;
import com.booksaw.betterTeams.team.storage.team.TeamStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;

/**
 * Enhanced TeamManager with critical threading fixes for Minecraft 1.21.8+ compatibility
 * 
 * CRITICAL FIXES APPLIED:
 * - Fixed "waypoint connections off-main" error by ensuring scoreboard operations run on main thread
 * - Added proper plugin state checks to prevent IllegalPluginAccessException
 * - Removed deprecated PrePurgeEvent usage
 * - Enhanced error handling and logging for better debugging
 * - FIXED: Corrected package declaration to com.booksaw.betterTeams.team
 * 
 * @author booksaw (original), enhanced for Paper 1.21.8+ compatibility
 */
public abstract class TeamManager {
    /**
     * A list of all teams currently loaded in memory
     */
    protected final HashMap<UUID, Team> loadedTeams;

    /**
     * If chat is being logged to the console
     */
    @Getter
    private final boolean logChat;

    /**
     * Used to create a new teamManager with enhanced thread safety
     */
    protected TeamManager() {
        logChat = Main.plugin.getConfig().getBoolean("logTeamChat");
        loadedTeams = new HashMap<>();
    }

    /**
     * Used to get a clone of the loaded team list. The team objects are not
     * cloned, just the hashmap to avoid concurrent modification
     *
     * @return A clone of the team list
     */
    @SuppressWarnings("unchecked")
    public Map<UUID, Team> getLoadedTeamListClone() {
        synchronized (loadedTeams) {
            return (HashMap<UUID, Team>) loadedTeams.clone();
        }
    }

    /**
     * Used to get the team with the provided ID
     *
     * @param uuid the ID of the team
     * @return the team with that ID [null - the team does not exist]
     */
    @Nullable
    @Contract(pure = true, value = "null -> null")
    public Team getTeam(@Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }

        synchronized (loadedTeams) {
            if (loadedTeams.containsKey(uuid)) {
                return loadedTeams.get(uuid);
            }
        }

        if (!isTeam(uuid)) {
            return null;
        }

        try {
            return new Team(uuid);
        } catch (IllegalArgumentException e) {
            Main.plugin.getLogger().warning("Failed to load team with UUID " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Used to get the team by its display name or a player within it
     *
     * @param name the display name of the team or an online player within the team
     * @return the team which matches the data[null - no team could be found]
     */
    @Nullable
    @Contract(pure = true, value = "null -> null")
    public Team getTeam(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Team team = getTeamByName(name);
        if (team != null) {
            return team;
        }

        // trying to get team by a player name
        OfflinePlayer player = Bukkit.getPlayer(name);
        if (player == null) {
            return null;
        }
        return getTeam(player);
    }

    /**
     * Used to find the team that a specified player is in, this is the highest time
     * complexity search to find a team (O(n^2)) so only use when the other provided
     * methods are not possible
     *
     * @param player the player which is in a team
     * @return the team they are in [null - they are not in a team]
     */
    @Nullable
    @Contract(pure = true, value = "null -> null")
    public Team getTeam(@Nullable OfflinePlayer player) {
        if (player == null) {
            return null;
        }

        // checking if the player is in a loaded team (save hitting secondary storage every time)
        synchronized (loadedTeams) {
            Optional<Team> possibleTeam = loadedTeams.values().stream()
                .filter(team -> team.getMembers().contains(player))
                .findFirst();
            if (possibleTeam.isPresent()) {
                return possibleTeam.get();
            }
        }

        if (!isInTeam(player)) {
            return null;
        }

        UUID uuid = getTeamUUID(player);
        if (uuid == null) {
            return null;
        }

        return getTeam(uuid);
    }

    /**
     * Used to get the team by its team name
     *
     * @param name The name of the team
     * @return The team with that display name [null - no team with that name could be found]
     */
    @Nullable
    public Team getTeamByName(@NotNull String name) {
        if (!isTeam(name)) {
            return null;
        }

        UUID uuid = getTeamUUID(name);
        if (uuid == null) {
            return null;
        }

        return getTeam(uuid);
    }

    /**
     * CRITICAL FIX: This method is used to create a new team with the specified name
     * Now includes proper threading controls to prevent "waypoint connections off-main" errors
     * <p>
     * Checks are not carried out to ensure that the name is available, so that
     * should be done before this method is called
     * </p>
     *
     * @param name  the name of the new team
     * @param owner the owner of the new team (the player who ran /team create)
     * @return The created team
     * @throws IllegalStateException if called off main thread or plugin is disabled
     * @throws IllegalArgumentException if team creation is cancelled by another plugin
     */
    public Team createNewTeam(String name, Player owner) {
        // CRITICAL FIX: Ensure team creation runs on main thread for scoreboard operations
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("createNewTeam must be called on the main thread to avoid 'waypoint connections off-main' errors!");
        }
        
        // CRITICAL FIX: Check if plugin is enabled
        if (!Main.plugin.isEnabled()) {
            throw new IllegalStateException("Cannot create team - plugin is disabled");
        }

        // Additional validation
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }
        
        if (owner == null) {
            throw new IllegalArgumentException("Team owner cannot be null");
        }

        UUID id = UUID.randomUUID();
        // ensuring the ID is unique
        while (getTeam(id) != null) {
            id = UUID.randomUUID();
        }
        
        Team team;
        try {
            team = new Team(name, id, owner);
        } catch (Exception e) {
            Main.plugin.getLogger().severe("Failed to create Team object: " + e.getMessage());
            throw new IllegalArgumentException("Failed to create team: " + e.getMessage(), e);
        }

        // Call creation event
        CreateTeamEvent event = new CreateTeamEvent(team, owner);
        try {
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception e) {
            Main.plugin.getLogger().severe("Error calling CreateTeamEvent: " + e.getMessage());
            e.printStackTrace();
        }

        if (event.isCancelled()) {
            throw new IllegalArgumentException("Creating team was cancelled by another plugin");
        }

        // Register team in memory
        synchronized (loadedTeams) {
            loadedTeams.put(id, team);
        }
        
        try {
            registerNewTeam(team, owner);
        } catch (Exception e) {
            // Rollback if registration fails
            synchronized (loadedTeams) {
                loadedTeams.remove(id);
            }
            Main.plugin.getLogger().severe("Failed to register new team: " + e.getMessage());
            throw new IllegalArgumentException("Failed to register team: " + e.getMessage(), e);
        }

        // CRITICAL FIX: Scoreboard operations are now guaranteed to run on main thread
        if (Main.plugin.teamManagement != null && owner != null) {
            try {
                Main.plugin.teamManagement.displayBelowName(owner);
            } catch (Exception e) {
                Main.plugin.getLogger().severe("Failed to display team name for " + owner.getName() + ": " + e.getMessage());
                e.printStackTrace();
                // Don't fail team creation for display issues
            }
        }

        // Call post-creation event
        try {
            Bukkit.getPluginManager().callEvent(new PostCreateTeamEvent(team, owner));
        } catch (Exception e) {
            Main.plugin.getLogger().severe("Error calling PostCreateTeamEvent: " + e.getMessage());
            e.printStackTrace();
        }

        Main.plugin.getLogger().info("Successfully created team '" + name + "' for player " + owner.getName());
        return team;
    }

    /**
     * Used to get the team which has claimed the provided chest, will return null
     * if that location is not claimed
     *
     * @param location the location of the chest - must already be normalised
     * @return The team which has claimed that chest
     */
    public Team getClaimingTeam(Location location) {
        if (location == null) {
            return null;
        }

        UUID claimingTeam = getClaimingTeamUUID(location);
        if (claimingTeam == null) {
            return null;
        }

        if (!isTeam(claimingTeam)) {
            return null;
        }

        return getTeam(claimingTeam);
    }

    /**
     * Used to get the UUID of the team which has claimed the provided chest, will
     * return null if that location is not claimed
     *
     * @param location The location of the chest - must already be normalised
     * @return the team which has claimed that chest
     */
    public abstract UUID getClaimingTeamUUID(Location location);

    /**
     * Used to get the claiming team of a chest, will check both parts of a double
     * chest, it is assumed that the provided block is known to be a chest
     *
     * @param block The block being checked
     * @return The team which has claimed that block
     */
    public Team getClaimingTeam(Block block) {
        if (block == null || block.getType() != Material.CHEST) {
            return null;
        }

        Location location1 = block.getLocation();
        Location location2 = ChestManagement.getOtherSide(block);

        if (location2 == null) {
            return getClaimingTeam(location1);
        }

        if (ChestManagement.isSingleChest(location1, location2)) {
            return getClaimingTeam(location1);
        }

        Team claimedBy = getClaimingTeam(location1);
        if (claimedBy != null) {
            return claimedBy;
        }

        return getClaimingTeam(location2);
    }

    /**
     * Used to get the claiming location, will check both parts of a double chest,
     * it is assumed that the provided block is known to be a chest
     *
     * @param block Part of the chest
     * @return The location of the claim
     */
    public Location getClaimingLocation(Block block) {
        if (block == null || block.getType() != Material.CHEST) {
            return null;
        }

        Location location1 = block.getLocation();
        Location location2 = ChestManagement.getOtherSide(block);

        if (location2 == null) {
            Team claimedBy = getClaimingTeam(location1);
            return claimedBy != null ? location1 : null;
        }

        if (ChestManagement.isSingleChest(location1, location2)) {
            Team claimedBy = getClaimingTeam(location1);
            return claimedBy != null ? location1 : null;
        } else {
            Team claimedBy = getClaimingTeam(location1);
            if (claimedBy != null) {
                return location1;
            }

            claimedBy = getClaimingTeam(location2);
            return claimedBy != null ? location2 : null;
        }
    }

    /**
     * FIXED: Used to reset all teams scores/money to 0
     * Removed deprecated PrePurgeEvent usage
     *
     * @param money whether to purge team money
     * @param score whether to purge team scores
     * @return If the teams were purged or not
     */
    public boolean purgeTeams(boolean money, boolean score) {
        // FIXED: Only use the non-deprecated PurgeEvent
        PurgeEvent event = new PurgeEvent();
        
        try {
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception e) {
            Main.plugin.getLogger().severe("Error calling PurgeEvent: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        if (event.isCancelled()) {
            Main.plugin.getLogger().info("Team purge was cancelled by another plugin");
            return false;
        }

        boolean success = true;
        
        if (score) {
            Main.plugin.getLogger().info("Purging team scores...");
            try {
                purgeTeamScore();
            } catch (Exception e) {
                Main.plugin.getLogger().severe("Failed to purge team scores: " + e.getMessage());
                e.printStackTrace();
                success = false;
            }
        }
        
        if (money) {
            Main.plugin.getLogger().info("Purging team money...");
            try {
                purgeTeamMoney();
            } catch (Exception e) {
                Main.plugin.getLogger().severe("Failed to purge team money: " + e.getMessage());
                e.printStackTrace();
                success = false;
            }
        }

        if (success) {
            try {
                Bukkit.getPluginManager().callEvent(new PostPurgeEvent());
            } catch (Exception e) {
                Main.plugin.getLogger().severe("Error calling PostPurgeEvent: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return success;
    }

    /**
     * Used to check if a team exists with that uuid
     *
     * @param uuid the UUID to check
     * @return If a team exists with that uuid
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Contract(pure = true, value = "null -> false")
    public abstract boolean isTeam(@Nullable UUID uuid);

    /**
     * Used to check if a team exists with that name
     *
     * @param name the name to check
     * @return If a team exists with that name
     */
    @Contract(pure = true, value = "null -> false")
    public abstract boolean isTeam(@Nullable String name);

    /**
     * Used to check if the specified player is in a team
     *
     * @param player The player to check
     * @return If they are in a team
     */
    public abstract boolean isInTeam(OfflinePlayer player);

    /**
     * Used to get the uuid of the team that the specified player is in
     *
     * @param player the player to check for
     * @return The team uuid
     */
    public abstract UUID getTeamUUID(OfflinePlayer player);

    /**
     * Used to get the team uuid from the team name
     *
     * @param name The name of the team
     * @return The UUID of the specified team
     */
    public abstract UUID getTeamUUID(String name);

    /**
     * Used to load the stored values into the storage manager
     */
    public abstract void loadTeams();

    /**
     * Thread-safe check if team is loaded
     */
    public boolean isLoaded(UUID teamUUID) {
        if (teamUUID == null) {
            return false;
        }
        synchronized (loadedTeams) {
            return loadedTeams.containsKey(teamUUID);
        }
    }

    /**
     * Called when a new team is registered, this can be used to register it in any
     * full team trackers. The team file will be fully prepared with the members
     * within the team
     *
     * @param team   The new team
     * @param player The player that created the team
     */
    protected abstract void registerNewTeam(Team team, Player player);

    /**
     * Used to disband a team with proper cleanup
     *
     * @param team The team that is being disbanded
     */
    public void disbandTeam(Team team) {
        if (team == null) {
            return;
        }

        synchronized (loadedTeams) {
            loadedTeams.remove(team.getID());
        }

        // if a team is being disbanded due to invalid team loading, the file should not
        // be deleted to preserve data
        if (team.getName() != null) {
            try {
                deleteTeamStorage(team);
            } catch (Exception e) {
                Main.plugin.getLogger().severe("Failed to delete team storage for " + team.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Used when a team is disbanded, can be used to remove it from any team
     * trackers
     *
     * @param team The team that is being disbanded
     */
    protected abstract void deleteTeamStorage(Team team);

    /**
     * Called when a team changes its name as this will affect the getTeam(String
     * teamName) method
     *
     * @param team    The team
     * @param newName The name the team has changed to
     */
    public abstract void teamNameChange(Team team, String newName);

    /**
     * Called when a player joins a team, this can be used to track the players
     * location
     *
     * @param team   The team that the player has joined
     * @param player The player that has joined the team
     */
    public abstract void playerJoinTeam(Team team, TeamPlayer player);

    /**
     * Called when a player leaves a team
     *
     * @param team   The team that the player has left
     * @param player The player that has left the team
     */
    public abstract void playerLeaveTeam(Team team, TeamPlayer player);

    /**
     * Called when a team needs a storage manager to manage all information, this is
     * called for preexisting teams
     *
     * @param team The team instance
     * @return The created team storage
     */
    public abstract TeamStorage createTeamStorage(Team team);

    /**
     * Called when a new team is made
     *
     * @param team The team
     * @return The created team storage
     */
    public abstract TeamStorage createNewTeamStorage(Team team);

    /**
     * This method is used to sort all the teams into an array ranking from highest
     * score to lowest
     *
     * @return the array of teams in order of their rank
     */
    public abstract String[] sortTeamsByScore();

    /**
     * This method is used to sort all the team names into an array ranking from
     * highest to lowest
     *
     * @return The sorted array
     */
    public abstract String[] sortTeamsByBalance();

    /**
     * Used to sort all members from largest to smallest by number of members
     *
     * @return the sorted array
     */
    public abstract String[] sortTeamsByMembers();

    /**
     * Used to reset the score of all teams
     */
    public abstract void purgeTeamScore();

    /**
     * Used to reset the balance of all teams
     */
    public abstract void purgeTeamMoney();

    /**
     * @return The stored hologram details
     */
    public abstract List<String> getHoloDetails();

    /**
     * Used to store and save the updated hologram details
     *
     * @param details the details to save
     */
    public abstract void setHoloDetails(List<String> details);

    public abstract void addChestClaim(Team team, Location loc);

    public abstract void removeChestclaim(Location loc);

    /**
     * Can be called by a config option if the server is having difficulties. Do not
     * call from anywhere else as it may cause problems depending on the storage
     * type
     */
    public abstract void rebuildLookups();

    /**
     * Enhanced disable method with proper cleanup
     * This can be overridden if any code needs to be run when onDisable is called
     */
    public void disable() {
        Main.plugin.getLogger().info("TeamManager shutting down...");
        
        // Clear loaded teams safely
        synchronized (loadedTeams) {
            loadedTeams.clear();
        }
        
        Main.plugin.getLogger().info("TeamManager shutdown complete.");
    }
}
