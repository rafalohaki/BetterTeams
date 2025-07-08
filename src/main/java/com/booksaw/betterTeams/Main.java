/*
 * Main.java
 * Fixed: ensure all scoreboard operations run on the main thread and robustly handle plugin reloads.
 */
package com.booksaw.betterTeams;

import java.io.File;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import com.booksaw.betterTeams.commands.HelpCommand;
import com.booksaw.betterTeams.commands.ParentCommand;
import com.booksaw.betterTeams.commands.PermissionParentCommand;
import com.booksaw.betterTeams.commands.team.AllyChatCommand;
import com.booksaw.betterTeams.commands.team.AllyCommand;
import com.booksaw.betterTeams.commands.team.AnchorCommand;
import com.booksaw.betterTeams.commands.team.BalCommand;
import com.booksaw.betterTeams.commands.team.BaltopCommand;
import com.booksaw.betterTeams.commands.team.BanCommand;
import com.booksaw.betterTeams.commands.team.ChatCommand;
import com.booksaw.betterTeams.commands.team.ColorCommand;
import com.booksaw.betterTeams.commands.team.CreateCommand;
import com.booksaw.betterTeams.commands.team.DelHome;
import com.booksaw.betterTeams.commands.team.DelwarpCommand;
import com.booksaw.betterTeams.commands.team.DemoteCommand;
import com.booksaw.betterTeams.commands.team.DepositCommand;
import com.booksaw.betterTeams.commands.team.DescriptionCommand;
import com.booksaw.betterTeams.commands.team.DisbandCommand;
import com.booksaw.betterTeams.commands.team.EchestCommand;
import com.booksaw.betterTeams.commands.team.HomeCommand;
import com.booksaw.betterTeams.commands.team.InfoCommand;
import com.booksaw.betterTeams.commands.team.InviteCommand;
import com.booksaw.betterTeams.commands.team.JoinCommand;
import com.booksaw.betterTeams.commands.team.KickCommand;
import com.booksaw.betterTeams.commands.team.LeaveCommand;
import com.booksaw.betterTeams.commands.team.ListCommand;
import com.booksaw.betterTeams.commands.team.NameCommand;
import com.booksaw.betterTeams.commands.team.NeutralCommand;
import com.booksaw.betterTeams.commands.team.OpenCommand;
import com.booksaw.betterTeams.commands.team.PromoteCommand;
import com.booksaw.betterTeams.commands.team.PvpCommand;
import com.booksaw.betterTeams.commands.team.RankCommand;
import com.booksaw.betterTeams.commands.team.RankupCommand;
import com.booksaw.betterTeams.commands.team.SetAnchorCommand;
import com.booksaw.betterTeams.commands.team.SetOwnerCommand;
import com.booksaw.betterTeams.commands.team.SetWarpCommand;
import com.booksaw.betterTeams.commands.team.SethomeCommand;
import com.booksaw.betterTeams.commands.team.TagCommand;
import com.booksaw.betterTeams.commands.team.TitleCommand;
import com.booksaw.betterTeams.commands.team.TopCommand;
import com.booksaw.betterTeams.commands.team.UnbanCommand;
import com.booksaw.betterTeams.commands.team.WarpCommand;
import com.booksaw.betterTeams.commands.team.WarpsCommand;
import com.booksaw.betterTeams.commands.team.WithdrawCommand;
import com.booksaw.betterTeams.commands.team.chest.ChestCheckCommand;
import com.booksaw.betterTeams.commands.team.chest.ChestClaimCommand;
import com.booksaw.betterTeams.commands.team.chest.ChestRemoveCommand;
import com.booksaw.betterTeams.commands.team.chest.ChestRemoveallCommand;
import com.booksaw.betterTeams.commands.teama.AllyTeama;
import com.booksaw.betterTeams.commands.teama.AnchorTeama;
import com.booksaw.betterTeams.commands.teama.ChatSpyTeama;
import com.booksaw.betterTeams.commands.teama.ColorTeama;
import com.booksaw.betterTeams.commands.teama.CreateHoloTeama;
import com.booksaw.betterTeams.commands.teama.CreateTeama;
import com.booksaw.betterTeams.commands.teama.DelwarpTeama;
import com.booksaw.betterTeams.commands.teama.DemoteTeama;
import com.booksaw.betterTeams.commands.teama.DescriptionTeama;
import com.booksaw.betterTeams.commands.teama.DisbandTeama;
import com.booksaw.betterTeams.commands.teama.EchestTeama;
import com.booksaw.betterTeams.commands.teama.HomeTeama;
import com.booksaw.betterTeams.commands.teama.ImportmessagesTeama;
import com.booksaw.betterTeams.commands.teama.InviteTeama;
import com.booksaw.betterTeams.commands.teama.JoinTeama;
import com.booksaw.betterTeams.commands.teama.LeaveTeama;
import com.booksaw.betterTeams.commands.teama.NameTeama;
import com.booksaw.betterTeams.commands.teama.NeutralTeama;
import com.booksaw.betterTeams.commands.teama.OpenTeama;
import com.booksaw.betterTeams.commands.teama.PromoteTeama;
import com.booksaw.betterTeams.commands.teama.PurgeTeama;
import com.booksaw.betterTeams.commands.teama.ReloadTeama;
import com.booksaw.betterTeams.commands.teama.RemoveHoloTeama;
import com.booksaw.betterTeams.commands.teama.SetAnchorTeama;
import com.booksaw.betterTeams.commands.teama.SetOwnerTeama;
import com.booksaw.betterTeams.commands.teama.SetrankTeama;
import com.booksaw.betterTeams.commands.teama.SetwarpTeama;
import com.booksaw.betterTeams.commands.teama.TagTeama;
import com.booksaw.betterTeams.commands.teama.TeleportTeama;
import com.booksaw.betterTeams.commands.teama.TitleTeama;
import com.booksaw.betterTeams.commands.teama.VersionTeama;
import com.booksaw.betterTeams.commands.teama.WarpTeama;
import com.booksaw.betterTeams.commands.teama.chest.ChestClaimTeama;
import com.booksaw.betterTeams.commands.teama.chest.ChestDisableClaims;
import com.booksaw.betterTeams.commands.teama.chest.ChestEnableClaims;
import com.booksaw.betterTeams.commands.teama.chest.ChestRemoveTeama;
import com.booksaw.betterTeams.commands.teama.chest.ChestRemoveallTeama;
import com.booksaw.betterTeams.commands.teama.money.AddMoney;
import com.booksaw.betterTeams.commands.teama.money.RemoveMoney;
import com.booksaw.betterTeams.commands.teama.money.SetMoney;
import com.booksaw.betterTeams.commands.teama.score.AddScore;
import com.booksaw.betterTeams.commands.teama.score.RemoveScore;
import com.booksaw.betterTeams.commands.teama.score.SetScore;
import com.booksaw.betterTeams.cooldown.CooldownManager;
import com.booksaw.betterTeams.cost.CostManager;
import com.booksaw.betterTeams.events.AllyManagement;
import com.booksaw.betterTeams.events.ChatManagement;
import com.booksaw.betterTeams.events.ChestManagement;
import com.booksaw.betterTeams.events.DamageManagement;
import com.booksaw.betterTeams.events.HomeAnchorManagement;
import com.booksaw.betterTeams.events.InventoryManagement;
import com.booksaw.betterTeams.events.MCTeamManagement;
import com.booksaw.betterTeams.events.MCTeamManagement.BelowNameType;
import com.booksaw.betterTeams.events.MessagesManagement;
import com.booksaw.betterTeams.events.RankupEvents;
import com.booksaw.betterTeams.integrations.UltimateClaimsManager;
import com.booksaw.betterTeams.integrations.WorldGuardManagerV7;
import com.booksaw.betterTeams.integrations.ZKothManager;
import com.booksaw.betterTeams.integrations.hologram.DHHologramManager;
import com.booksaw.betterTeams.integrations.hologram.HDHologramManager;
import com.booksaw.betterTeams.integrations.hologram.HologramManager;
import com.booksaw.betterTeams.integrations.placeholder.TeamPlaceholders;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.score.ScoreManagement;
import com.booksaw.betterTeams.team.storage.StorageType;
import com.booksaw.betterTeams.team.storage.convert.Converter;
import com.booksaw.betterTeams.team.storage.storageManager.YamlStorageManager;
import com.booksaw.betterTeams.util.WebhookHandler;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

/**
 * Main class of the plugin, extends JavaPlugin
 *
 * @author booksaw
 */
public class Main extends JavaPlugin {

	public static Main plugin;
	public static Economy econ = null;
	public static Permission perms = null;
	public static boolean placeholderAPI = false;
	public boolean useHolograms = false;
	public MCTeamManagement teamManagement;
	public ChatManagement chatManagement;
	public WorldGuardManagerV7 wgManagement;
	@Getter
	private PermissionParentCommand teamCommand;

	@Getter
	private BooksawCommand teamBooksawCommand;

	@Getter
	private TeamPlaceholders teamPlaceholders;

	/**
	 * If the ultimateClaims expansion has been enabled
	 */
	@Getter
	private boolean ultimateClaimsEnabled = false;

	private Metrics metrics = null;

	/**
	 * This is used to store the config file in which the the teams data is stored
	 */
	FileConfiguration teams;
	private DamageManagement damageManagement;

	private ConfigManager configManager;

	private BukkitAudiences adventure;

	public boolean isAdventure() {
		return adventure != null;
	}

	private boolean closeAdventure = true;

	/**
	 * A centralized helper method to safely check if the plugin is active.
	 * This should be used before any scheduler calls or Bukkit API interactions
	 * that rely on the plugin being enabled.
	 * @return true if the plugin is not null and is enabled.
	 */
	public static boolean isPluginSafe() {
		return plugin != null && plugin.isEnabled();
	}

	@Override
	public void onLoad() {
		plugin = this;
		configManager = new ConfigManager("config", true);

		Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if (worldGuardPlugin != null && configManager.config.getBoolean("worldGuard.enabled")) {
			
			try {
				String version = worldGuardPlugin.getPluginMeta().getVersion();
				char majorVersion = version.charAt(0);
				
				if (majorVersion == '7') {
					wgManagement = new WorldGuardManagerV7();
					getLogger().info("WorldGuard " + version + " integration enabled");
				} else {
					getLogger().warning("Unsupported WorldGuard version: " + version
						+ ". Currently supported: 7.x.x");
				}
				
			} catch (Exception e) {
				getLogger().warning("Failed to detect WorldGuard version: " + e.getMessage());
			}
		}
	}


	@Override
	public void onEnable() {
		// Set the static instance to this instance of the plugin.
		plugin = this;

		setupMetrics();

		String language = getConfig().getString("language");
		MessageManager.setLanguage(language);
		if (Objects.requireNonNull(language).equals("en") || language.isEmpty()) {
			MessageManager.setLanguage("messages");
		}

		try {
			if (adventure == null) {
				adventure = BukkitAudiences.create(this);
			}
		} catch (Exception ignored) {
		}

		MessageManager.setupMessageSender(adventure);

		loadCustomConfigs();

		setupStorage();

		ChatManagement.enable();

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
				&& Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("PlaceholderAPI")).isEnabled()) {
			placeholderAPI = true;
			teamPlaceholders = new TeamPlaceholders(this);
			teamPlaceholders.register();
		}

		if (Bukkit.getPluginManager().getPlugin("UltimateClaims") != null
				&& Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UltimateClaims")).isEnabled()
				&& getConfig().getBoolean("ultimateClaims.enabled")) {
			ultimateClaimsEnabled = true;
			new UltimateClaimsManager();
		}

		useHolograms = setupHolograms();

		if (!setupEconomy() || !getConfig().getBoolean("useVault")) {
			econ = null;
		}

		if (!setupPermissions()) {
			perms = null;
		}

		setupCommands();
		setupListeners();
	}

	@Override
	public void onDisable() {

		for (Entry<Player, Team> temp : InventoryManagement.adminViewers.entrySet()) {
			temp.getKey().closeInventory();
			temp.getValue().saveEchest();
		}

		if (useHolograms && HologramManager.holoManager != null) {
			HologramManager.holoManager.disable();
		}

		if (teamManagement != null) {
			teamManagement.removeAll(false);
		}

		// Cancel all tasks associated with this plugin to prevent them from running after disable.
		Bukkit.getScheduler().cancelTasks(this);

		Team.disable();

		MessageManager.dumpMessages();
		MessageManager.dumpMessageSender();

		if (closeAdventure && adventure != null) {
			adventure.close();
			adventure = null;
		}

		closeAdventure = true;

		// Nullify the static reference. This is CRITICAL for the isPluginSafe() check
		// to work correctly after a reload from tools like PlugMan.
		plugin = null;
	}

	public void loadCustomConfigs() {

		File f = MessageManager.getFile();
		String language = MessageManager.getLanguage();
		try {
			if (!f.exists()) {
				saveResource(language + ".yml", false);
			}
		} catch (Exception e) {
			if (isPluginSafe()) {
				getLogger().warning("Could not load selected language: " + language
						+ " go to https://github.com/booksaw/BetterTeams/wiki/Language to view a list of supported languages");
				getLogger().warning("Reverting to english so the plugin can still function");
			}
			MessageManager.setLanguage("messages");
			loadCustomConfigs();
			return;
		}

		ConfigManager messagesConfigManager = new ConfigManager(language, true);

		MessageManager.addMessages(messagesConfigManager);

		if (!language.equals("messages")) {
			messagesConfigManager = new ConfigManager("messages", true);
			MessageManager.addBackupMessages(messagesConfigManager.config);
		}

		if (getConfig().getBoolean("disableCombat")) {
			if (damageManagement == null) {
				damageManagement = new DamageManagement();
				getServer().getPluginManager().registerEvents(damageManagement, this);
			}

		} else {
			if (damageManagement != null) {
				if (isPluginSafe()) getLogger().log(Level.WARNING, "Restart server for damage changes to apply");
			}
		}

		// loading the fully custom help message option
		HelpCommand.setupHelp();
	}

	private boolean setupHolograms() {
		boolean hdHolos = Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays");
		if (hdHolos) {
			new HDHologramManager();
		}
		boolean dhHolos = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
		if (!hdHolos && dhHolos) {
			new DHHologramManager();
		}
		return hdHolos || dhHolos;
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return true;
	}

	private boolean setupPermissions() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		if (rsp == null) {
			return false;
		}
		perms = rsp.getProvider();
		return perms != null;
	}

	public void reload() {
		closeAdventure = false;
		onDisable();
		teamManagement = null;
		reloadConfig();
		configManager = new ConfigManager("config", true);
		ChatManagement.enable();
		damageManagement = null;
		onEnable();
	}

	public void setupCommands() {
		teamCommand = new PermissionParentCommand(new CostManager("team"), new CooldownManager("team"), "team");
		teamCommand.addSubCommands(new CreateCommand(teamCommand), new LeaveCommand(), new DisbandCommand(),
				new DescriptionCommand(), new InviteCommand(), new JoinCommand(), new NameCommand(), new OpenCommand(),
				new InfoCommand(teamCommand), new KickCommand(), new PromoteCommand(), new DemoteCommand(),
				new HomeCommand(), new SethomeCommand(), new BanCommand(), new UnbanCommand(),
				new ChatCommand(teamCommand), new ColorCommand(), new TitleCommand(), new TopCommand(),
				new BaltopCommand(), new RankCommand(), new DelHome(), new AllyCommand(), new NeutralCommand(),
				new AllyChatCommand(teamCommand), new ListCommand(), new WarpCommand(), new SetWarpCommand(),
				new DelwarpCommand(), new WarpsCommand(), new EchestCommand(), new RankupCommand(), new TagCommand());

		if (getConfig().getBoolean("anchor.enable")) {
			teamCommand.addSubCommands(new AnchorCommand(), new SetAnchorCommand());
		}

		if (getConfig().getBoolean("disableCombat")) {
			teamCommand.addSubCommand(new PvpCommand());
		}
		if (getConfig().getBoolean("singleOwner")) {
			teamCommand.addSubCommand(new SetOwnerCommand());
		}

		ParentCommand chest = new PermissionParentCommand("chest");
		chest.addSubCommands(new ChestClaimCommand(), new ChestRemoveCommand(), new ChestRemoveallCommand(), new ChestCheckCommand());
		teamCommand.addSubCommand(chest);

		teamBooksawCommand = new BooksawCommand("team", teamCommand, "betterteams.standard", "All commands for teams",
				getConfig().getStringList("command.team"));

		ParentCommand teamaCommand = new ParentCommand("teamadmin");
		teamaCommand.addSubCommands(new ReloadTeama(), new ChatSpyTeama(), new TitleTeama(),
				new VersionTeama("version"), new VersionTeama("debug"), new HomeTeama(), new NameTeama(),
				new DescriptionTeama(), new OpenTeama(), new InviteTeama(), new CreateTeama(), new JoinTeama(),
				new LeaveTeama(), new PromoteTeama(), new DemoteTeama(), new WarpTeama(), new SetwarpTeama(),
				new DelwarpTeama(), new PurgeTeama(), new DisbandTeama(), new ColorTeama(), new EchestTeama(),
				new SetrankTeama(teamaCommand), new TagTeama(), new TeleportTeama(teamaCommand), new AllyTeama(),
				new NeutralTeama(), new ImportmessagesTeama());

		if (getConfig().getBoolean("anchor.enable")) {
			teamaCommand.addSubCommands(new AnchorTeama(), new SetAnchorTeama());
		}

		if (getConfig().getBoolean("singleOwner")) {
			teamaCommand.addSubCommand(new SetOwnerTeama());
		}

		ParentCommand teamaScoreCommand = new ParentCommand("score");
		teamaScoreCommand.addSubCommands(new AddScore(), new SetScore(), new RemoveScore());
		teamaCommand.addSubCommand(teamaScoreCommand);

		ParentCommand teamaMoneyCommand = new ParentCommand("money");
		teamaMoneyCommand.addSubCommands(new AddMoney(), new SetMoney(), new RemoveMoney());
		teamaCommand.addSubCommand(teamaMoneyCommand);

		ParentCommand teamaChestCommand = new ParentCommand("chest");
		teamaChestCommand.addSubCommands(new ChestClaimTeama(), new ChestRemoveTeama(), new ChestRemoveallTeama(),
				new ChestEnableClaims(), new ChestDisableClaims());
		teamaCommand.addSubCommand(teamaChestCommand);

		if (useHolograms) {
			ParentCommand teamaHoloCommand = new ParentCommand("holo");
			teamaHoloCommand.addSubCommands(new CreateHoloTeama(), new RemoveHoloTeama());
			teamaCommand.addSubCommand(teamaHoloCommand);
		}

		if (econ != null) {
			teamCommand.addSubCommands(new DepositCommand(teamCommand), new BalCommand(),
					new WithdrawCommand(teamCommand));
		}

		new BooksawCommand("teamadmin", teamaCommand, "betterteams.admin", "All admin commands for teams",
				getConfig().getStringList("command.teama"));
	}

	public void setupListeners() {
		getLogger().info("Display team name config value: " + getConfig().getString("displayTeamName"));
		BelowNameType type = BelowNameType.getType(Objects.requireNonNull(getConfig().getString("displayTeamName")));
		getLogger().info("Loading below name. Type: " + type);
		if (getConfig().getBoolean("useTeams")) {
			if (teamManagement == null) {
				teamManagement = new MCTeamManagement(type);
				// Guard the scheduler call, although it's generally safe in onEnable
				if (isPluginSafe()) {
					Bukkit.getScheduler().runTask(this, () -> teamManagement.displayBelowNameForAll());
				}
				getServer().getPluginManager().registerEvents(teamManagement, this);
				getLogger().info("teamManagement declared: " + teamManagement);
			}
		} else {
			getLogger().info("Not loading management");
			if (teamManagement != null) {
				getLogger().log(Level.WARNING, "Restart server for minecraft team changes to apply");
			}
		}

		if (getServer().getPluginManager().isPluginEnabled("zKoth")) {
			getLogger().info("Found plugin zKoth, adding plugin integration");
			getServer().getPluginManager().registerEvents(new ZKothManager(), this);
		}

		getServer().getPluginManager().registerEvents((chatManagement = new ChatManagement()), this);
		getServer().getPluginManager().registerEvents(new ScoreManagement(), this);
		getServer().getPluginManager().registerEvents(new AllyManagement(), this);
		getServer().getPluginManager().registerEvents(new MessagesManagement(), this);

		if (getConfig().getBoolean("hookSupport")) {
			getServer().getPluginManager().registerEvents(new WebhookHandler(), this);
		}

		if (getConfig().getBoolean("checkUpdates")) {
			getServer().getPluginManager().registerEvents(new UpdateChecker(this), this);
		}

		if (teamCommand != null && teamCommand.isEnabled("chest")) {
			getServer().getPluginManager().registerEvents(new ChestManagement(), this);
		}

		getServer().getPluginManager().registerEvents(new InventoryManagement(), this);
		getServer().getPluginManager().registerEvents(new RankupEvents(), this);
		if (getConfig().getBoolean("anchor.enable")) {
			HomeAnchorManagement homeAnchorListener = new HomeAnchorManagement(this);
			homeAnchorListener.registerEvent();
		}
	}

	public void setupMetrics() {
		if (metrics == null) {
			int pluginId = 7855;
			metrics = new Metrics(this, pluginId);
			metrics.addCustomChart(new SimplePie("language", () -> getConfig().getString("language")));
			metrics.addCustomChart(new SimplePie("storage_type", () -> getConfig().getString("storageType")));
		}
	}

	@Override
	public @NotNull FileConfiguration getConfig() {
		return configManager.config;
	}

	public void setupStorage() {
		File f = new File("plugins/BetterTeams/" + YamlStorageManager.TEAMLISTSTORAGELOC + ".yml");

		if (!f.exists()) {
			saveResource("teams.yml", false);
		}

		YamlConfiguration teamStorage = YamlConfiguration.loadConfiguration(f);
		StorageType from = StorageType.getStorageType(teamStorage.getString("storageType", "FLATFILE"));
		StorageType to = StorageType.getStorageType(getConfig().getString("storageType", ""));

		if (from != to) {
			Converter converter = Converter.getConverter(from, to);

			if (converter == null) {
				if(isPluginSafe()) getLogger().info("Cannot convert to the selected storage type (" + to.toString()
						+ "), continuing with preexisting one (" + from.toString() + ")");
				to = from;
			} else {
				converter.convertStorage();
			}
		}

		Team.setupTeamManager(to);
		Team.getTeamManager().loadTeams();
	}
}