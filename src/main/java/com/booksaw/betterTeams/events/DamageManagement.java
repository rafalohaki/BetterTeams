package com.booksaw.betterTeams.events;

import java.util.Collection;
import java.util.Objects;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public class DamageManagement implements Listener {

	private final boolean disablePotions;
	private final boolean disableSelf;

	// Cache the PotionEffectType lookups for performance, using modern NamespacedKey access.
	private static final PotionEffectType BAD_OMEN = PotionEffectType.getByKey(NamespacedKey.minecraft("bad_omen"));
	private static final PotionEffectType BLINDNESS = PotionEffectType.getByKey(NamespacedKey.minecraft("blindness"));
	private static final PotionEffectType NAUSEA = PotionEffectType.getByKey(NamespacedKey.minecraft("nausea")); // Formerly CONFUSION
	private static final PotionEffectType INSTANT_DAMAGE = PotionEffectType.getByKey(NamespacedKey.minecraft("instant_damage")); // Formerly HARM
	private static final PotionEffectType HUNGER = PotionEffectType.getByKey(NamespacedKey.minecraft("hunger"));
	private static final PotionEffectType MINING_FATIGUE = PotionEffectType.getByKey(NamespacedKey.minecraft("mining_fatigue")); // Formerly SLOW_DIGGING
	private static final PotionEffectType UNLUCK = PotionEffectType.getByKey(NamespacedKey.minecraft("unluck"));
	private static final PotionEffectType WEAKNESS = PotionEffectType.getByKey(NamespacedKey.minecraft("weakness"));
	private static final PotionEffectType POISON = PotionEffectType.getByKey(NamespacedKey.minecraft("poison"));

	public DamageManagement() {
		disablePotions = Main.plugin.getConfig().getBoolean("disablePotions");
		disableSelf = Main.plugin.getConfig().getBoolean("playerDamageSelf");
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onDamage(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player)) return;

		Team temp = Team.getTeam((Player) e.getEntity());
		if (temp == null) return;

		try {
			if (e.getDamager() instanceof Player) {
				if (!Objects.requireNonNull(Team.getTeam((Player) e.getDamager())).canDamage(temp, (Player) e.getDamager())) {
					e.setCancelled(true);
				}
			} else if (e.getDamager() instanceof Projectile && !(e.getDamager() instanceof ThrownPotion)) {
				Projectile arrow = (Projectile) e.getDamager();
				ProjectileSource source = arrow.getShooter();
				if (source instanceof Player && !Objects.requireNonNull(Team.getTeam((Player) source)).canDamage(temp, (Player) source)) {
					if (disableSelf && source == e.getEntity()) return;
					e.setCancelled(true);
				}
			} else if (e.getDamager() instanceof ThrownPotion && disablePotions) {
				ThrownPotion arrow = (ThrownPotion) e.getDamager();
				ProjectileSource source = arrow.getShooter();
				if (source instanceof Player && !Objects.requireNonNull(Team.getTeam((Player) source)).canDamage(temp, (Player) source)) {
					e.setCancelled(true);
				}
			} else if (e.getDamager() instanceof TNTPrimed) {
				TNTPrimed explosive = (TNTPrimed) e.getDamager();
				Entity source = explosive.getSource();
				if (source instanceof Player && !Objects.requireNonNull(Team.getTeam((Player) source)).canDamage(temp, (Player) source)) {
					if (disableSelf && source == e.getEntity()) return;
					e.setCancelled(true);
				}
			}
		} catch (NullPointerException ignored) {}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPotion(PotionSplashEvent e) {
		if (!(e.getEntity().getShooter() instanceof Player) || !disablePotions) return;

		Player thrower = (Player) e.getEntity().getShooter();
		Team team = Team.getTeam(thrower);
		if (team == null) return;

		Collection<PotionEffect> effects = e.getPotion().getEffects();
		boolean cancel = false;

		// Check if any of the potion's effects are considered harmful.
		for (PotionEffect effect : effects) {
			PotionEffectType type = effect.getType();
			if (type.equals(BAD_OMEN) || type.equals(BLINDNESS) || type.equals(NAUSEA) || type.equals(INSTANT_DAMAGE)
					|| type.equals(HUNGER) || type.equals(MINING_FATIGUE) || type.equals(UNLUCK)
					|| type.equals(WEAKNESS) || type.equals(POISON)) {
				cancel = true;
				break; // Found a harmful effect, no need to check further.
			}
		}

		if (cancel) {
			for (LivingEntity entity : e.getAffectedEntities()) {
				try {
					if (entity instanceof Player && !Objects.requireNonNull(Team.getTeam((Player) entity)).canDamage(team, thrower)) {
						if (disableSelf && entity == thrower) continue;
						e.setIntensity(entity, 0); // Cancel the effect for this friendly player.
					}
				} catch (NullPointerException ignored) {}
			}
		}
	}
}
