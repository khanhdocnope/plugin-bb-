package vn.bloodharvest.bloodmoon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import vn.bloodharvest.BloodHarvestPlugin;

import java.time.Duration;
import java.util.Random;

/**
 * MVP BloodMoon: cu N dem 1 lan, buff mob + rot Blood Shard.
 * Nhe: 1 task 100 ticks, chi xu ly khi dang dem.
 */
public class BloodMoonManager implements Listener {

    private final BloodHarvestPlugin plugin;
    private final Random random = new Random();
    private BukkitTask task;
    private boolean active = false;
    private boolean forced = false;
    private boolean forcedOff = false;
    private long lastAnnouncedDay = -1;
    private NamespacedKey buffKey;

    public BloodMoonManager(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
        this.buffKey = new NamespacedKey(plugin, "bloodmoon_buff");
    }

    public int everyN() {
        return Math.max(1, plugin.getConfig().getInt("bloodmoon.every-n-nights", 3));
    }

    public boolean isActive() { return active; }

    public long currentDay(World w) {
        return w.getFullTime() / 24000L;
    }

    public boolean isNight(World w) {
        long t = w.getTime();
        return t >= 13000 && t <= 23000;
    }

    public boolean shouldBeBloodMoon(World w) {
        if (forced) return true;
        if (forcedOff) return false;
        long day = currentDay(w);
        return isNight(w) && (day % everyN() == everyN() - 1);
    }

    public long nightsToNext(World w) {
        long day = currentDay(w);
        long r = day % everyN();
        long target = (everyN() - 1 - r + everyN()) % everyN();
        // Neu dang dem bloodmoon thi = 0
        if (shouldBeBloodMoon(w)) return 0;
        return target;
    }

    public void startTask() {
        stopTask();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getWorlds().isEmpty()) return;
            World w = Bukkit.getWorlds().get(0);
            boolean should = shouldBeBloodMoon(w);
            if (should && !active) {
                active = true;
                lastAnnouncedDay = currentDay(w);
                announceStart();
            } else if (!should && active) {
                active = false;
                forced = false;
                forcedOff = false;
                Bukkit.broadcast(Component.text("§7Binh minh len... Blood Moon ket thuc."));
            }
        }, 40L, 100L);
    }

    public void stopTask() {
        if (task != null) { task.cancel(); task = null; }
        active = false;
    }

    private void announceStart() {
        Bukkit.broadcast(Component.text("§c§l☾ BLOOD MOON! §7Quai manh hon, can than!"));
        Title title = Title.title(
                Component.text("§c§lBLOOD MOON"),
                Component.text("§7Quai +HP +DMG - Rot Blood Shard"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(800)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }
    }

    public void forceStart(Player by) {
        World w = by != null ? by.getWorld() : Bukkit.getWorlds().get(0);
        w.setTime(14000);
        forced = true;
        forcedOff = false;
    }

    public void forceEnd() {
        forced = false;
        forcedOff = true;
        active = false;
        Bukkit.broadcast(Component.text("§7Blood Moon bi buoc ket thuc (admin)."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!active) return;
        if (!(e.getEntity() instanceof Monster)) return;
        // Chi buff tu nhien, khong buff boss/elite da danh dau
        LivingEntity le = e.getEntity();
        if (le.getPersistentDataContainer().has(new NamespacedKey(plugin, "boss"), PersistentDataType.STRING)) return;
        if (le.getPersistentDataContainer().has(new NamespacedKey(plugin, "elite"), PersistentDataType.STRING)) return;

        double hpMult = plugin.getConfig().getDouble("bloodmoon.buff-hp-multiplier", 1.6);
        try {
            if (le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                double base = le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                le.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(base * hpMult);
                le.setHealth(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }
        } catch (Exception ignored) {}
        le.getPersistentDataContainer().set(buffKey, PersistentDataType.BYTE, (byte) 1);
        le.setCustomNameVisible(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!active) return;
        if (!(e.getDamager() instanceof LivingEntity le)) return;
        if (!le.getPersistentDataContainer().has(buffKey, PersistentDataType.BYTE)) return;
        double dmgMult = plugin.getConfig().getDouble("bloodmoon.buff-damage-multiplier", 1.3);
        e.setDamage(e.getDamage() * dmgMult);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!active) return;
        if (!(e.getEntity() instanceof Monster)) return;
        if (e.getEntity().getKiller() == null) return;
        double chance = plugin.getConfig().getDouble("bloodmoon.shard-drop-chance", 0.35);
        if (random.nextDouble() < chance) {
            e.getDrops().add(plugin.getCustomItems().createBloodShard(1));
        }
    }
}
