package vn.bloodharvest;

import org.bukkit.plugin.java.JavaPlugin;
import vn.bloodharvest.bloodmoon.BloodMoonManager;
import vn.bloodharvest.boss.BossManager;
import vn.bloodharvest.command.BHCommand;
import vn.bloodharvest.enchant.PseudoEnchant;
import vn.bloodharvest.farming.CustomItems;
import vn.bloodharvest.season.SeasonManager;
import vn.bloodharvest.tpa.TpaCommand;
import vn.bloodharvest.tpa.TpaManager;

public final class BloodHarvestPlugin extends JavaPlugin {

    private static BloodHarvestPlugin instance;
    private SeasonManager seasonManager;
    private BloodMoonManager bloodMoonManager;
    private BossManager bossManager;
    private CustomItems customItems;
    private PseudoEnchant pseudoEnchant;
    private TpaManager tpaManager;

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
        tpaManager = new TpaManager(this);

        // Events
        getServer().getPluginManager().registerEvents(seasonManager, this);
        getServer().getPluginManager().registerEvents(bloodMoonManager, this);
        getServer().getPluginManager().registerEvents(bossManager, this);
        getServer().getPluginManager().registerEvents(customItems, this);
        getServer().getPluginManager().registerEvents(pseudoEnchant, this);
        getServer().getPluginManager().registerEvents(tpaManager, this);

        // Command (log canh bao neu lenh khong co trong mo ta plugin)
        BHCommand bh = new BHCommand(this);
        if (getCommand("bh") != null) {
            getCommand("bh").setExecutor(bh);
            getCommand("bh").setTabCompleter(bh);
        } else {
            getLogger().warning("Khong tim thay lenh /bh trong plugin.yml!");
        }
        TpaCommand tpa = new TpaCommand(this, tpaManager);
        for (String c : new String[]{"tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel", "tptoggle"}) {
            if (getCommand(c) != null) {
                getCommand(c).setExecutor(tpa);
                getCommand(c).setTabCompleter(tpa);
            } else {
                getLogger().warning("Khong tim thay lenh /" + c + " trong plugin.yml!");
            }
        }

        // Recipes
        customItems.registerRecipes();

        // Tasks (rat nhe: task cham, khong tick per-tick)
        seasonManager.startTask();
        bloodMoonManager.startTask();
        bossManager.startTask();
        tpaManager.startTask();

        getLogger().info("BloodHarvest v1.1 enabled! 4 mùa + BloodMoon + 3 boss + 4 elite + TPA sẵn sàng.");
    }

    @Override
    public void onDisable() {
        if (seasonManager != null) seasonManager.stopTask();
        if (bloodMoonManager != null) bloodMoonManager.stopTask();
        if (bossManager != null) bossManager.stopTask();
        if (tpaManager != null) tpaManager.stopTask();
        getLogger().info("BloodHarvest v1.1 disabled.");
    }

    public SeasonManager getSeasonManager() { return seasonManager; }
    public BloodMoonManager getBloodMoonManager() { return bloodMoonManager; }
    public BossManager getBossManager() { return bossManager; }
    public CustomItems getCustomItems() { return customItems; }
    public PseudoEnchant getPseudoEnchant() { return pseudoEnchant; }
    public TpaManager getTpaManager() { return tpaManager; }
}
