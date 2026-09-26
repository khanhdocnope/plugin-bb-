package vn.bloodharvest.boss;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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
import vn.bloodharvest.season.Season;

import java.util.*;

/**
 * BloodHarvest v1.0 Boss + Elite:
 * Boss Blood Moon (gioi han 1 con): dalang (Ravager), kysi (Skeleton cuoi ngua), huyetmau (CaveSpider)
 * Elite theo mua: yeti-DONG (Stray), hoayeu-HA (Husk), bunhin-THU (IronGolem), tinhlinh-XUAN (Allay)
 * Nhe: 1 task skill 12s + 1 task elite tu nhien 5p.
 */
public class BossManager implements Listener {

    private final BloodHarvestPlugin plugin;
    private final NamespacedKey bossKey;
    private final NamespacedKey eliteKey;
    private final Set<UUID> bosses = new HashSet<>();
    private final Set<UUID> elites = new HashSet<>();
    private BukkitTask skillTask;
    private BukkitTask eliteTask;
    private final Random random = new Random();

    public BossManager(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "boss");
        this.eliteKey = new NamespacedKey(plugin, "elite");
    }

    public NamespacedKey bossKey() { return bossKey; }
    public NamespacedKey eliteKey() { return eliteKey; }

    public int aliveBosses() {
        bosses.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            return e == null || e.isDead();
        });
        return bosses.size();
    }

    public int aliveElites() {
        elites.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            return e == null || e.isDead();
        });
        return elites.size();
    }

    public void startTask() {
        stopTask();
        skillTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSkills, 240L, 240L);
        eliteTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickNaturalElite, 6000L, 6000L);
    }

    public void stopTask() {
        if (skillTask != null) { skillTask.cancel(); skillTask = null; }
        if (eliteTask != null) { eliteTask.cancel(); eliteTask = null; }
    }

    private String bossType(LivingEntity le) {
        return le.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
    }

    private String eliteType(LivingEntity le) {
        return le.getPersistentDataContainer().get(eliteKey, PersistentDataType.STRING);
    }

    private void tickSkills() {
        for (UUID id : new ArrayList<>(bosses)) {
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof LivingEntity le) || le.isDead()) continue;
            String t = bossType(le);
            if (t == null) continue;
            switch (t) {
                case "dalang" -> {
                    if (!(le instanceof Ravager rav)) continue;
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
                    }
                }
                case "kysi" -> {
                    Player target = nearestPlayer(le);
                    if (target == null) continue;
                    // Dash ve phia muc tieu + ban 3 mui ten
                    org.bukkit.util.Vector dir = target.getLocation().toVector().subtract(le.getLocation().toVector()).normalize();
                    le.setVelocity(dir.multiply(1.4).setY(0.3));
                    for (int i = 0; i < 3; i++) {
                        Arrow a = le.launchProjectile(Arrow.class, dir.clone().multiply(2).setY(0.1 * (i - 1)));
                        a.setDamage(4.0);
                    }
                }
                case "huyetmau" -> {
                    List<Entity> nearby = le.getNearbyEntities(14, 6, 14);
                    long babies = nearby.stream().filter(x -> x instanceof CaveSpider).count();
                    if (babies < 4 && random.nextDouble() < 0.8) {
                        for (int i = 0; i < 2 && babies + i < 4; i++) {
                            le.getWorld().spawnEntity(le.getLocation().add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1), EntityType.CAVE_SPIDER);
                        }
                        Bukkit.broadcast(Component.text("§cHuyet Mau sinh dan nhen con!"));
                    }
                    for (Entity n : nearby) {
                        if (n instanceof Player p) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0, false, false, true));
                        }
                    }
                }
            }
        }
        for (UUID id : new ArrayList<>(elites)) {
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof LivingEntity le) || le.isDead()) continue;
            String t = eliteType(le);
            if (t == null) continue;
            switch (t) {
                case "yeti" -> {
                    for (Entity n : le.getNearbyEntities(8, 4, 8)) {
                        if (n instanceof Player p) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0, false, false, true));
                        }
                    }
                }
                case "hoayeu" -> {
                    for (Entity n : le.getNearbyEntities(7, 4, 7)) {
                        if (n instanceof Player p) {
                            p.setFireTicks(Math.max(p.getFireTicks(), 40));
                        }
                    }
                }
                case "bunhin" -> {
                    // Bao ve farm: danh quai + hoi mau nhe cho player
                    for (Entity n : le.getNearbyEntities(10, 5, 10)) {
                        if (n instanceof Monster m) {
                            m.damage(4.0, le);
                        } else if (n instanceof Player p) {
                            if (p.getHealth() < p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - 1) {
                                p.setHealth(Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), p.getHealth() + 1.0));
                            }
                        }
                    }
                }
                case "tinhlinh" -> {
                    for (Entity n : le.getNearbyEntities(8, 4, 8)) {
                        if (n instanceof Player p) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false, true));
                        }
                    }
                }
            }
        }
    }

    private void tickNaturalElite() {
        if (!plugin.getConfig().getBoolean("boss.natural-elite", true)) return;
        if (Bukkit.getOnlinePlayers().isEmpty() || Bukkit.getWorlds().isEmpty()) return;
        if (random.nextDouble() > plugin.getConfig().getDouble("boss.natural-elite-chance", 0.3)) return;
        int maxTotal = Bukkit.getOnlinePlayers().size() * plugin.getConfig().getInt("limits.max-elite-per-player", 2);
        if (aliveElites() >= maxTotal) return;
        Player p = new ArrayList<>(Bukkit.getOnlinePlayers()).get(random.nextInt(Bukkit.getOnlinePlayers().size()));
        Season s;
        try { s = plugin.getSeasonManager().currentSeason(); } catch (Exception ex) { s = Season.XUAN; }
        Location loc = p.getLocation().add(random.nextInt(11) - 5, 0, random.nextInt(11) - 5);
        loc.setY(p.getWorld().getHighestBlockYAt(loc) + 1);
        switch (s) {
            case HA -> spawnHoaYeu(loc);
            case THU -> spawnBuNhin(loc);
            case DONG -> spawnYeti(loc);
            default -> spawnTinhLinh(loc);
        }
    }

    /** Blood Moon tu goi 1 trong 3 boss gan player ngau nhien */
    public boolean trySpawnNaturalBoss(World w) {
        if (aliveBosses() >= plugin.getConfig().getInt("boss.max-alive", 1)) return false;
        if (w.getPlayers().isEmpty()) return false;
        Player p = w.getPlayers().get(random.nextInt(w.getPlayers().size()));
        Location loc = p.getLocation().add(random.nextInt(21) - 10, 0, random.nextInt(21) - 10);
        loc.setY(w.getHighestBlockYAt(loc).getY() + 1);
        int pick = random.nextInt(3);
        if (pick == 0) return spawnDaLang(loc);
        if (pick == 1) return spawnKySi(loc);
        return spawnHuyetMau(loc);
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

    private void setup(LivingEntity le, String type, String name, double hp, double dmg, boolean boss) {
        le.setCustomName(name);
        le.setCustomNameVisible(true);
        le.setRemoveWhenFarAway(false);
        if (le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            le.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            le.setHealth(hp);
        }
        if (dmg > 0 && le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(dmg);
        }
        le.getPersistentDataContainer().set(boss ? bossKey : eliteKey, PersistentDataType.STRING, type);
        (boss ? bosses : elites).add(le.getUniqueId());
        if (boss) {
            int mins = plugin.getConfig().getInt("boss.despawn-minutes", 10);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!le.isDead()) {
                    le.remove();
                    bosses.remove(le.getUniqueId());
                }
            }, mins * 60L * 20L);
        }
    }

    public boolean spawnDaLang(Location loc) {
        if (aliveBosses() >= plugin.getConfig().getInt("boss.max-alive", 1)) return false;
        Ravager rav = (Ravager) loc.getWorld().spawnEntity(loc, EntityType.RAVAGER);
        setup(rav, "dalang", "§c§lDa Lang Huyet Nguyet §7[ Boss ]",
                plugin.getConfig().getDouble("boss.dalang-hp", 300.0),
                plugin.getConfig().getDouble("boss.dalang-damage", 12.0), true);
        Bukkit.broadcast(Component.text("§c§lBOSS §fDa Lang Huyet Nguyet xuat hien! (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")"));
        return true;
    }

    public boolean spawnKySi(Location loc) {
        if (aliveBosses() >= plugin.getConfig().getInt("boss.max-alive", 1)) return false;
        SkeletonHorse horse = (SkeletonHorse) loc.getWorld().spawnEntity(loc, EntityType.SKELETON_HORSE);
        horse.setTamed(true);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        Skeleton sk = (Skeleton) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
        sk.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        sk.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        sk.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
        horse.addPassenger(sk);
        setup(sk, "kysi", "§8§lKy Si Khong Dau §7[ Boss ]",
                plugin.getConfig().getDouble("boss.kysi-hp", 260.0),
                plugin.getConfig().getDouble("boss.kysi-damage", 10.0), true);
        // Ngua chet thi boss mat cho tru -> danh dau ngua de xoa kem
        horse.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "kysi_horse");
        Bukkit.broadcast(Component.text("§c§lBOSS §fKy Si Khong Dau xuat hien! (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")"));
        return true;
    }

    public boolean spawnHuyetMau(Location loc) {
        if (aliveBosses() >= plugin.getConfig().getInt("boss.max-alive", 1)) return false;
        CaveSpider sp = (CaveSpider) loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
        setup(sp, "huyetmau", "§4§lHuyet Mau §7[ Boss ]",
                plugin.getConfig().getDouble("boss.huyetmau-hp", 220.0),
                plugin.getConfig().getDouble("boss.huyetmau-damage", 9.0), true);
        sp.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false, false));
        Bukkit.broadcast(Component.text("§c§lBOSS §fHuyet Mau xuat hien! (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")"));
        return true;
    }

    public boolean spawnYeti(Location loc) {
        Stray s = (Stray) loc.getWorld().spawnEntity(loc, EntityType.STRAY);
        s.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        setup(s, "yeti", "§b§lYeti Bang §7[ Elite Dong ]",
                plugin.getConfig().getDouble("boss.yeti-hp", 120.0),
                plugin.getConfig().getDouble("boss.yeti-damage", 7.0), false);
        return true;
    }

    public boolean spawnHoaYeu(Location loc) {
        Husk h = (Husk) loc.getWorld().spawnEntity(loc, EntityType.HUSK);
        h.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, false));
        h.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        setup(h, "hoayeu", "§6§lHoa Yeu Sa Mac §7[ Elite Ha ]",
                plugin.getConfig().getDouble("boss.hoayeu-hp", 90.0),
                plugin.getConfig().getDouble("boss.hoayeu-damage", 6.0), false);
        return true;
    }

    public boolean spawnBuNhin(Location loc) {
        IronGolem g = (IronGolem) loc.getWorld().spawnEntity(loc, EntityType.IRON_GOLEM);
        g.setPlayerCreated(false);
        setup(g, "bunhin", "§e§lBu Nhin Song §7[ Bao ve farm ]",
                plugin.getConfig().getDouble("boss.bunhin-hp", 150.0),
                plugin.getConfig().getDouble("boss.bunhin-damage", 8.0), false);
        return true;
    }

    public boolean spawnTinhLinh(Location loc) {
        Allay a = (Allay) loc.getWorld().spawnEntity(loc, EntityType.ALLAY);
        setup(a, "tinhlinh", "§a§lTinh Linh Hoa §7[ Elite Xuan ]",
                plugin.getConfig().getDouble("boss.tinhlinh-hp", 40.0), 0, false);
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
            e.setDroppedExp(120);
            e.getDrops().add(plugin.getCustomItems().createBloodShard(8));
            e.getDrops().add(plugin.getCustomItems().createEnchantBook("nguyetam", 1 + random.nextInt(2)));
            e.getDrops().add(plugin.getCustomItems().createLauNamNguyet(1));
            Bukkit.broadcast(Component.text("§aBoss Da Lang bi ha! Rot Shard + Nguyet Am."));
        } else if ("kysi".equals(b)) {
            bosses.remove(le.getUniqueId());
            e.getDrops().clear();
            e.setDroppedExp(120);
            e.getDrops().add(plugin.getCustomItems().createBloodShard(8));
            e.getDrops().add(plugin.getCustomItems().createEnchantBook("huyetcuong", 1 + random.nextInt(2)));
            // Xoa ngua kem
            if (le.getVehicle() instanceof SkeletonHorse horse) horse.remove();
            Bukkit.broadcast(Component.text("§aKy Si Khong Dau bi ha! Rot Shard + Huyet Cuong."));
        } else if ("huyetmau".equals(b)) {
            bosses.remove(le.getUniqueId());
            e.getDrops().clear();
            e.setDroppedExp(120);
            e.getDrops().add(plugin.getCustomItems().createBloodShard(6));
            e.getDrops().add(plugin.getCustomItems().createMoonroot(3));
            e.getDrops().add(plugin.getCustomItems().createEnchantBook("boithu", 1 + random.nextInt(2)));
            Bukkit.broadcast(Component.text("§aHuyet Mau bi ha! Rot Shard + Moonroot + Boi Thu."));
        } else if ("yeti".equals(el)) {
            elites.remove(le.getUniqueId());
            e.getDrops().add(plugin.getCustomItems().createFrostberry(3));
            e.getDrops().add(plugin.getCustomItems().createTraAm(1));
            if (random.nextDouble() < 0.3) e.getDrops().add(plugin.getCustomItems().createEnchantBook("suonghan", 1));
        } else if ("hoayeu".equals(el)) {
            elites.remove(le.getUniqueId());
            e.getDrops().add(plugin.getCustomItems().createEmberPepper(3));
            e.getDrops().add(new ItemStack(Material.BLAZE_POWDER, 2));
        } else if ("bunhin".equals(el)) {
            elites.remove(le.getUniqueId());
            e.getDrops().add(plugin.getCustomItems().createBanhBiThu(2));
            if (random.nextDouble() < 0.3) e.getDrops().add(plugin.getCustomItems().createEnchantBook("dongam", 1));
        } else if ("tinhlinh".equals(el)) {
            elites.remove(le.getUniqueId());
            e.getDrops().add(new ItemStack(Material.POPPY, 3));
            e.getDrops().add(new ItemStack(Material.DANDELION, 2));
            if (random.nextDouble() < 0.3) e.getDrops().add(plugin.getCustomItems().createEnchantBook("nguyetgiap", 1));
        }
    }
}
