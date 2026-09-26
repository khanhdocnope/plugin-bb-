package vn.bloodharvest.season;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import vn.bloodharvest.BloodHarvestPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * BloodHarvest v1.0 Seasons: vong lap 4 mua dua tren ngay ingame, khong can DB.
 * Nhe: 1 task BossBar moi 100 ticks + 1 task Chill moi 200 ticks.
 */
public class SeasonManager implements Listener {

    private final BloodHarvestPlugin plugin;
    private final Random random = new Random();
    private BossBar bar;
    private BukkitTask task;
    private BukkitTask chillTask;
    private int seasonOffset = 0;

    public SeasonManager(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
        this.bar = Bukkit.createBossBar("Mua", BarColor.GREEN, BarStyle.SOLID);
        // Doc start-season de tinh offset
        String start = plugin.getConfig().getString("season.start-season", "XUAN");
        List<String> order = getOrder();
        int idx = order.indexOf(Season.fromString(start).name());
        if (idx < 0) idx = 0;
        this.seasonOffset = idx;
    }

    public List<String> getOrder() {
        List<String> order = plugin.getConfig().getStringList("season.order");
        if (order == null || order.isEmpty()) {
            return Arrays.asList("XUAN", "HA", "THU", "DONG");
        }
        return order;
    }

    public int getDaysPerSeason() {
        return Math.max(1, plugin.getConfig().getInt("season.days-per-season", 8));
    }

    public World mainWorld() {
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    public long totalDays() {
        World w = mainWorld();
        if (w == null) return 0;
        return w.getFullTime() / 24000L;
    }

    public Season currentSeason() {
        List<String> order = getOrder();
        int dps = getDaysPerSeason();
        long idx = (totalDays() / dps + seasonOffset) % order.size();
        return Season.fromString(order.get((int) idx));
    }

    public long dayInSeason() {
        int dps = getDaysPerSeason();
        return (totalDays() % dps) + 1;
    }

    public long daysToNextSeason() {
        int dps = getDaysPerSeason();
        return dps - (totalDays() % dps);
    }

    public void setSeason(Season s) {
        // Don gian: xoay offset sao cho mua hien tai = s
        List<String> order = getOrder();
        int want = order.indexOf(s.name());
        int dps = getDaysPerSeason();
        long curSlot = totalDays() / dps;
        int curIdx = (int) ((curSlot + seasonOffset) % order.size());
        seasonOffset = (seasonOffset + (want - curIdx) + order.size() * 10) % order.size();
    }

    public void nextSeason() {
        Season cur = currentSeason();
        List<String> order = getOrder();
        int i = order.indexOf(cur.name());
        Season next = Season.fromString(order.get((i + 1) % order.size()));
        setSeason(next);
    }

    public void startTask() {
        stopTask();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World w = mainWorld();
            if (w == null) return;
            Season s = currentSeason();
            String blood = plugin.getBloodMoonManager() != null && plugin.getBloodMoonManager().isActive()
                    ? " §c● Blood Moon!" : "";
            String title = s.display() + " §8| §fNgay " + dayInSeason() + "/" + getDaysPerSeason() + blood;
            bar.setTitle(title);
            switch (s) {
                case XUAN -> bar.setColor(BarColor.GREEN);
                case HA -> bar.setColor(BarColor.YELLOW);
                case THU -> bar.setColor(BarColor.PINK);
                case DONG -> bar.setColor(BarColor.BLUE);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
            }
        }, 20L, 100L);

        // Chill mua DONG: moi 10s check 1 lan, rat nhe
        chillTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentSeason() != Season.DONG) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == org.bukkit.GameMode.CREATIVE
                        || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                // Dong Am enchant -> mien Chill
                try {
                    if (plugin.getPseudoEnchant() != null && plugin.getPseudoEnchant().hasDongAmArmor(p)) continue;
                } catch (Exception ignored) {}
                Biome biome = p.getLocation().getBlock().getBiome();
                String bn = biome.name();
                boolean snowy = bn.contains("SNOW") || bn.contains("FROZEN") || bn.contains("ICE");
                if (!snowy) continue;
                // Mien neu co Tra Am (FIRE_RES) hoac gan lua
                if (p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) continue;
                // Gan lua/lava/duoc trong 5 block thi khong chill
                boolean nearHeat = p.getLocation().getBlock().getLightFromBlocks() >= 10;
                if (nearHeat) continue;
                p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0, false, false, true));
                if (random.nextDouble() < 0.4) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 0, false, false, true));
                }
            }
        }, 200L, 200L);
    }

    public void stopTask() {
        if (task != null) { task.cancel(); task = null; }
        if (chillTask != null) { chillTask.cancel(); chillTask = null; }
        if (bar != null) bar.removeAll();
    }

    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) {
        Season s = currentSeason();
        double mult = plugin.getConfig().getDouble("season.growth-multiplier." + s.name(), 1.0);
        if (mult >= 1.0) return; // XUAN/THU/HA khong can cham, chi DONG cham
        // DONG 0.4 => 60% huy growth
        if (random.nextDouble() > mult) {
            e.setCancelled(true);
        }
    }

    public Component statusLine() {
        Season s = currentSeason();
        return Component.text("Mua hien tai: " + s.name()
                + " (ngay " + dayInSeason() + "/" + getDaysPerSeason() + ")");
    }
}
