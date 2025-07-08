package com.booksaw.betterTeams.score;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.customEvents.post.PostPurgeEvent;
import com.booksaw.betterTeams.score.ScoreChange.ChangeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import me.clip.placeholderapi.PlaceholderAPI;

public class ScoreManagement implements Listener {

	private final List<Date> purges;
	private int nextPurge;
	private boolean run;
	private BukkitTask scheduledTask; // Dodajemy referencję do task dla proper cleanup

	public ScoreManagement() {
		purges = new ArrayList<>();
		nextPurge = -1;

		if (Main.plugin.getConfig().getBoolean("enableAutoPurge")) {
			Main.plugin.getConfig().getStringList("autoPurge").forEach(str -> {
				String[] split = str.split(":");
				if (split.length < 2) {
					Main.plugin.getLogger().severe("The autopurge value " + str
							+ " is not of the correct format, it should be of the format 'dateofMonth:hour', if your format looks like this, make sure you have surrounded the message in single quotes (ie - '1:6')");
				} else {
					try {
						Date temp = new Date(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
						purges.add(temp);
						if (nextPurge == -1 && temp.isAfterNow()) {
							nextPurge = purges.indexOf(temp);
						}
					} catch (NumberFormatException e) {
						Main.plugin.getLogger().severe("Invalid number format in autopurge value: " + str);
					}
				}
			});
		}
		run = true;

		if (nextPurge == -1) {
			nextPurge = 0;
		}

		// if there are actually purges
		if (!purges.isEmpty()) {
			scheduleAutoPurge();
		}
	}

	/**
	 * Scheduluje auto-purge używając nowoczesnego Bukkit Scheduler API
	 */
	private void scheduleAutoPurge() {
		// Sprawdź czy plugin jest bezpieczny do schedulowania
		if (!Main.isPluginSafe()) {
			return;
		}

		// Używamy runTaskTimer() zamiast deprecated scheduleSyncRepeatingTask()
		scheduledTask = new BukkitRunnable() {
			@Override
			public void run() {
				// Safety check - jeśli plugin został wyłączony, zatrzymaj task
				if (!Main.isPluginSafe()) {
					cancel();
					return;
				}
				
				handleAutoPurgeCheck();
			}
		}.runTaskTimer(Main.plugin, 0L, 20 * 60L); // Co minutę (20 ticks * 60 = 1200 ticks)
	}

	/**
	 * Obsługuje sprawdzanie auto-purge w nowoczesny sposób
	 */
	private void handleAutoPurgeCheck() {
		if (purges.isEmpty() || nextPurge >= purges.size()) {
			return;
		}

		Date currentPurge = purges.get(nextPurge);
		if (currentPurge.isNow()) {
			if (run) {
				return; // Już wykonywane
			}

			run = true;
			
			// Wykonaj purge asynchronicznie aby nie blokować main thread
			Bukkit.getScheduler().runTask(Main.plugin, () -> {
				if (Main.isPluginSafe()) {
					Team.getTeamManager().purgeTeams(true, true);
				}
			});
			
			// Przejdź do następnego purge
			if (nextPurge + 1 < purges.size()) {
				nextPurge++;
			} else {
				nextPurge = 0;
			}
			return;
		}
		
		// Clean pass - resetuj tracker
		run = false;
	}

	/**
	 * Zatrzymuje scheduled task (powinno być wywołane w onDisable)
	 */
	public void shutdown() {
		if (scheduledTask != null && !scheduledTask.isCancelled()) {
			scheduledTask.cancel();
			scheduledTask = null;
		}
	}

	@EventHandler
	public void onPurge(PostPurgeEvent e) {
		if (!Main.isPluginSafe()) {
			return;
		}
		
		// Bezpieczne wykonanie purge commands
		Bukkit.getScheduler().runTask(Main.plugin, () -> {
			Main.plugin.getConfig().getStringList("purgeCommands").forEach(cmd -> {
				try {
					if (Main.placeholderAPI) {
						cmd = PlaceholderAPI.setPlaceholders(null, cmd);
					}
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
				} catch (Exception ex) {
					Main.plugin.getLogger().warning("Failed to execute purge command: " + cmd + " - " + ex.getMessage());
				}
			});
		});
	}

	@EventHandler
	public void onKill(PlayerDeathEvent e) {
		Player killed = e.getEntity();
		
		// Score decreases for killed player
		Team killedTeam = Team.getTeam(killed);
		if (killedTeam != null) {
			death(killed, killedTeam);
		}

		// Check if there's a killer
		if (e.getEntity().getKiller() == null) {
			return;
		}

		Player killer = e.getEntity().getKiller();
		Team killerTeam = Team.getTeam(killer);
		if (killerTeam == null) {
			return;
		}
		
		kill(killer, killed, killerTeam, killedTeam);
	}

	public void kill(Player source, Player target, Team sourceTeam, Team targetTeam) {
		int scoreForKill;

		if (ScoreChange.isSpam(ChangeType.KILL, source, target)) {
			scoreForKill = Main.plugin.getConfig().getInt("events.kill.spam");
		} else {
			new ScoreChange(ChangeType.KILL, source, target);
			scoreForKill = Main.plugin.getConfig().getInt("events.kill.score");
		}

		// Friendly fire penalty vs normal kill reward
		if (sourceTeam.equals(targetTeam)) {
			sourceTeam.setScore(sourceTeam.getScore() - scoreForKill);
		} else {
			sourceTeam.setScore(sourceTeam.getScore() + scoreForKill);
		}
	}

	public void death(Player source, Team sourceTeam) {
		int scoreForDeath;

		if (ScoreChange.isSpam(ChangeType.DEATH, source)) {
			scoreForDeath = Main.plugin.getConfig().getInt("events.death.spam");
		} else {
			new ScoreChange(ChangeType.DEATH, source);
			scoreForDeath = Main.plugin.getConfig().getInt("events.death.score");
		}

		sourceTeam.setScore(sourceTeam.getScore() + scoreForDeath);
	}

	/**
	 * Inner class representing a date/time for auto-purge scheduling
	 */
	private static class Date {
		private final int date, hours;

		public Date(int date, int hours) {
			this.date = date;
			this.hours = hours;
		}

		/**
		 * Checks if this date is before the given date
		 * @param date the date to check against
		 * @return true if this date is before the given date
		 */
		public boolean isBefore(Date date) {
			if (date.date < this.date) {
				return false;
			} else if (date.date > this.date) {
				return true;
			} else {
				return date.hours > hours;
			}
		}

		/**
		 * Checks if the current time matches this date
		 * @return true if it's time for this scheduled event
		 */
		public boolean isNow() {
			LocalDateTime now = LocalDateTime.now();
			return date == now.getDayOfMonth() && now.getHour() == hours;
		}

		/**
		 * Checks if this date is after the current time
		 * @return true if this date is in the future
		 */
		public boolean isAfterNow() {
			LocalDateTime now = LocalDateTime.now();
			Date nowDate = new Date(now.getDayOfMonth(), now.getHour());
			return nowDate.isBefore(this);
		}
	}
}
