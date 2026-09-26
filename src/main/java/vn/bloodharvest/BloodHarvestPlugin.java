package vn.bloodharvest;

import org.bukkit.plugin.java.JavaPlugin;
import vn.bloodharvest.bloodmoon.BloodMoonManager;
import vn.bloodharvest.boss.BossManager;
import vn.bloodharvest.command.BHCommand;
import vn.bloodharvest.enchant.PseudoEnchant;
import vn.bloodharvest.farming.CustomItems;
import vn.bloodharvest.season.SeasonManager;

public final class BloodHarvestPlugin extends JavaPlugin {

    private static BloodHarvestPlugin instance;
    private SeasonManager seasonManager;
    private BloodMoonManager bloodMoonManager;
    private BossManager bossManager;
    private CustomItems customItems;
    private PseudoEnchant pseudoEnchant;

    public static BloodHarvestPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        customItems = new CustomItems(this);
        pseudoEnchant = new PseudoEnchant(this);
        seasonManager = new SeasonManager(this);
        bloodMoonManager = new BloodMoonManager(this);
        bossManager = new BossManager(this);

        // Events
        getServer().getPluginManager().registerEvents(seasonManager, this);
        getServer().getPluginManager().registerEvents(bloodMoonManager, this);
        getServer().getPluginManager().registerEvents(bossManager, this);
        getServer().getPluginManager().registerEvents(customItems, this);
        getServer().getPluginManager().registerEvents(pseudoEnchant, this);

        // Command
        BHCommand bh = new BHCommand(this);
        if (getCommand("bh") != null) {
            getCommand("bh").setExecutor(bh);
            getCommand("bh").setTabCompleter(bh);
        }

        // Recipes
        customItems.registerRecipes();

        // Tasks (rat nhe: 2 task, khong tick per-tick)
        seasonManager.startTask();
        bloodMoonManager.startTask();
        bossManager.startTask();

        getLogger().info("BloodHarvest MVP enabled! Mua primavera + BloodMoon + Boss san sang.");
    }

    @Override
    public void onDisable() {
        if (seasonManager != null) seasonManager.stopTask();
        if (bloodMoonManager != null) bloodMoonManager.stopTask();
        if (bossManager != null) bossManager.stopTask();
        getLogger().info("BloodHarvest MVP disabled.");
    }

    public SeasonManager getSeasonManager() { return seasonManager; }
    public BloodMoonManager getBloodMoonManager() { return bloodMoonManager; }
    public BossManager getBossManager() { return bossManager; }
    public CustomItems getCustomItems() { return customItems; }
    public PseudoEnchant getPseudoEnchant() { return pseudoEnchant; }
}
