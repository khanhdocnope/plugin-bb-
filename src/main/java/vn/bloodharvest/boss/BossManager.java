package vn.bloodharvest.boss;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import vn.bloodharvest.BloodHarvestPlugin;

import java.util.*;

/**
 * MVP Boss: 1 boss + 1 elite, skill don gian, gioi han 1 boss song.
 * - Da Lang Huyet Nguyet (Ravager): gam + goi soi
 * - Yeti Bang (Stray): aura slow
 */
public class BossManager implements Listener {

    private final BloodHarvestPlugin plugin;
    private final NamespacedKey bossKey;
    private final NamespacedKey eliteKey;
    private final Set<UUID> bosses = new HashSet<>();
    private final Set<UUID> elites = new HashSet<>();
    private BukkitTask skillTask;
    private final Random random = new Random();

    public BossManager(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "boss");
        this.eliteKey = new NamespacedKey(plugin, "elite");
    }

    public int aliveBosses() {
        bosses.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            return e == null || e.isDead();
        });
        return bosses.size();
    }

    public void startTask() {
        stopTask();
        // Skill moi 12s, rat nhe
        skillTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSkills, 240L, 240L);
    }

    public void stopTask() {
        if (skillTask != null) { skillTask.cancel(); skillTask = null; }
    }

    private void tickSkills() {
        for (UUID id : new ArrayList<>(bosses)) {
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof Ravager rav) || rav.isDead()) continue;
            // Gam: day lui + goi toi da 2 soi
            List<Entity> nearby = rav.getNearbyEntities(12, 6, 12);
            long wolves = nearby.stream().filter(x -> x instanceof Wolf).count();
            for (Entity n : nearby) {
                if (n instanceof Player p) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
                    p.damage(2.0, rav);
                    p.setVelocity(p.getLocation().toVector().subtract(rav.getLocation().toVector()).normalize().multiply(1.2).setY(0.5));
                }
            }
            if (wolves < 2 && random.nextDouble() < 0.7) {
                for (int i = 0; i < 2 - wolves; i++) {
                    Wolf w = (Wolf) rav.getWorld().spawnEntity(rav.getLocation().add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1), EntityType.WOLF);
                    w.setAngry(true);
                    w.setAdult();
                    w.setTarget(nearestPlayer(rav));
                }
                Bukkit.broadcast(Component.text("§cDa Lang gam len va goi dan soi!"));
            }
        }
        for (UUID id : new ArrayList<>(elites)) {
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof Stray stray) || stray.isDead()) continue;
            for (Entity n : stray.getNearbyEntities(8, 4, 8)) {
                if (n instanceof Player p) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0, false, false, true));
                }
            }
        }
    }

    private Player nearestPlayer(LivingEntity from) {
        Player best = null;
        double bd = 1e9;
        for (Player p : from.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(from.getLocation());
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    public boolean spawnDaLang(Location loc) {
        int max = plugin.getConfig().getInt("boss.max-alive", 1);
        if (aliveBosses() >= max) return false;
        Ravager rav = (Ravager) loc.getWorld().spawnEntity(loc, EntityType.RAVAGER);
        rav.setCustomName("§c§lDa Lang Huyet Nguyet §7[ Boss ]");
        rav.setCustomNameVisible(true);
        rav.setRemoveWhenFarAway(false);
        double hp = plugin.getConfig().getDouble("boss.dalang-hp", 300.0);
        double dmg = plugin.getConfig().getDouble("boss.dalang-damage", 12.0);
        if (rav.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            rav.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            rav.setHealth(hp);
        }
        if (rav.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            rav.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(dmg);
        }
        rav.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "dalang");
        bosses.add(rav.getUniqueId());
        // Despawn sau N phut
        int mins = plugin.getConfig().getInt("boss.despawn-minutes", 10);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!rav.isDead()) {
                rav.remove();
                bosses.remove(rav.getUniqueId());
            }
        }, mins * 60L * 20L);
        Bukkit.broadcast(Component.text("§c§lBOSS §fDa Lang Huyet Nguyet xuat hien! Toa do: "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()));
        return true;
    }

    public boolean spawnYeti(Location loc) {
        Stray s = (Stray) loc.getWorld().spawnEntity(loc, EntityType.STRAY);
        s.setCustomName("§b§lYeti Bang §7[ Elite Dong ]");
        s.setCustomNameVisible(true);
        double hp = plugin.getConfig().getDouble("boss.yeti-hp", 120.0);
        double dmg = plugin.getConfig().getDouble("boss.yeti-damage", 7.0);
        if (s.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            s.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            s.setHealth(hp);
        }
        if (s.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            s.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(dmg);
        }
        s.getEquipment().setHelmet(new ItemStack(org.bukkit.Material.IRON_HELMET));
        s.getPersistentDataContainer().set(eliteKey, PersistentDataType.STRING, "yeti");
        elites.add(s.getUniqueId());
        return true;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        String b = le.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        String el = le.getPersistentDataContainer().get(eliteKey, PersistentDataType.STRING);
        if ("dalang".equals(b)) {
            bosses.remove(le.getUniqueId());
            e.getDrops().clear();
            e.getDroppedExp = 120;
            e.getDrops().add(plugin.getCustomItems().createBloodShard(5));
            e.getDrops().add(plugin.getCustomItems().createBloodShard(3));
            // 50% sach enchant
            if (random.nextBoolean()) {
                e.getDrops().add(plugin.getCustomItems().createEnchantBook("nguyetam", 1 + random.nextInt(2)));
            } else {
                e.getDrops().add(plugin.getCustomItems().createEnchantBook("boithu", 1 + random.nextInt(2)));
            }
            Bukkit.broadcast(Component.text("§aBoss Da Lang bi ha! Rot Shard + sach enchant."));
        } else if ("yeti".equals(el)) {
            elites.remove(le.getUniqueId());
            e.getDrops().add(plugin.getCustomItems().createFrostberry(3));
            e.getDrops().add(plugin.getCustomItems().createTraAm(1));
        }
    }
}
