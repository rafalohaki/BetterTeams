package com.booksaw.betterTeams.customEvents;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import lombok.Getter;

/**
 * Used to track the details of a below name change event
 * Unlike most other events in BetterTeams, this one cannot be cancelled!
 *
 * @author booksaw
 */
@Getter
public class BelowNameChangeEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final ChangeType type;

	public BelowNameChangeEvent(Player player, ChangeType type) {
		// This event is synchronous, as established previously.
		super(false);

		// 🧩 1. Event Thread Guard (Dev Mode Only)
		// Ensures this synchronous event is not accidentally fired from an async task.
		if (!Bukkit.isPrimaryThread()) {
			throw new IllegalStateException("BelowNameChangeEvent must only be called on the main thread.");
		}

		this.player = player;
		this.type = type;
	}

	@SuppressWarnings("unused")
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public enum ChangeType {
		ADD, REMOVE
	}
}
