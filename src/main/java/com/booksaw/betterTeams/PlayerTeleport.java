package com.booksaw.betterTeams;

import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * A class to handle a teleport with a delay
 *
 * @author booksaw
 */
public class PlayerTeleport {

	final String reference;
	private final Player player;
	private final Location location;
	private final Location playerLoc;

	/**
	 * This will start the cooldown (there is no delay)
	 *
	 * @param player    The player to teleport
	 * @param location  the location to teleport them to
	 * @param reference the reference for the message that should be sent when the
	 *                  player is teleported
	 * @throws Exception Well, isn't this a generic one?
	 */
	public PlayerTeleport(Player player, Location location, String reference) throws Exception {
		// FIX: Initialize all final fields at the top of the constructor
		// to ensure they are assigned a value in all possible code paths.
		this.player = player;
		this.location = location;
		this.reference = reference;
		this.playerLoc = player.getLocation();

		if (location == null || location.getWorld() == null) {
			throw new Exception("Location or world is null");
		}

		// Do not start the teleport process if the plugin is disabled.
		if (!Main.plugin.isEnabled()) {
			return;
		}

		if (player.hasPermission("betterteams.warmup.bypass")) {
			Bukkit.getScheduler().runTask(Main.plugin, this::runTp);
			return;
		}

		int wait = Main.plugin.getConfig().getInt("tpDelay");
		if (wait <= 0) {
			Bukkit.getScheduler().runTask(Main.plugin, this::runTp);
			return;
		}

		// sending the wait message
		MessageManager.sendMessage(player, "teleport.wait", wait);

		Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
			// Final check to ensure the plugin was not disabled during the warmup period.
			if (!Main.plugin.isEnabled()) {
				return;
			}

			if (canTp()) {
				try {
					runTp();
				} catch (Exception e) {
					// This should not happen with the previous null checks, but as a safeguard.
					e.printStackTrace();
				}
			} else {
				cancel();
			}
		}, 20L * wait);

	}

	public void runTp() {
		if (location == null || location.getWorld() == null) {
			throw new NullPointerException("Location = " + location + " world is = " + ((location != null) ? location.getWorld() : "BLANK"));
		}

		PlayerTeleportEvent event = new PlayerTeleportEvent(player, player.getLocation(), location);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		player.teleport(location);
		MessageManager.sendMessage(player, reference);
	}

	public boolean canTp() {

		if (!Main.plugin.getConfig().getBoolean("noMove")) {
			return true;
		}

		return player.isOnline() && playerLoc.getWorld() != null && player.getLocation().getWorld() != null &&
				playerLoc.getWorld().equals(player.getLocation().getWorld()) &&
				playerLoc.distance(player.getLocation()) <= Math.abs(Main.plugin.getConfig().getInt("maxMove"));
	}

	public void cancel() {
		MessageManager.sendMessage(player, "teleport.fail");
	}

}
