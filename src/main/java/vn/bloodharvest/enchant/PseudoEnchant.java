package vn.bloodharvest.enchant;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.bloodharvest.BloodHarvestPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BloodHarvest v1.0 Pseudo-enchant (fake, khong NMS) - 6 loai:
 * - nguyetam I-III (vu khi): hut % sat thuong -> mau, Blood Moon x1.5
 * - suonghan I-II (vu khi): % lam cham muc tieu 2s
 * - huyetcuong I-III (vu khi): +dmg khi thap mau / Blood Moon
 * - boithu I-III (nong cu): % x2 nong san + tu trong lai, THU +10%
 * - dongam I (giap): mien Chill mua Dong
 * - nguyetgiap I-II (giap): giam dmg ban dem
 */
public class PseudoEnchant implements Listener {

    private final BloodHarvestPlugin plugin;
    private final NamespacedKey pNguyet;
    private final NamespacedKey pBoi;
    private final NamespacedKey pSuong;
    private final NamespacedKey pCuong;
    private final NamespacedKey pAm;
    private final NamespacedKey pGiap;
    private final Random random = new Random();

    public PseudoEnchant(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
        this.pNguyet = new NamespacedKey(plugin, "nguyetam");
        this.pBoi = new NamespacedKey(plugin, "boithu");
        this.pSuong = new NamespacedKey(plugin, "suonghan");
        this.pCuong = new NamespacedKey(plugin, "huyetcuong");
        this.pAm = new NamespacedKey(plugin, "dongam");
        this.pGiap = new NamespacedKey(plugin, "nguyetgiap");
    }

    public int getLevel(ItemStack it, NamespacedKey k) {
        if (it == null || !it.hasItemMeta()) return 0;
        Integer v = it.getItemMeta().getPersistentDataContainer().get(k, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public int getNguyetAm(ItemStack it) { return getLevel(it, pNguyet); }
    public int getBoiThu(ItemStack it) { return getLevel(it, pBoi); }
    public int getSuongHan(ItemStack it) { return getLevel(it, pSuong); }
    public int getHuyetCuong(ItemStack it) { return getLevel(it, pCuong); }

    public int armorLevel(Player p, NamespacedKey k) {
        PlayerInventory inv = p.getInventory();
        int best = getLevel(inv.getItemInMainHand(), k);
        for (ItemStack a : inv.getArmorContents()) {
            best = Math.max(best, getLevel(a, k));
        }
        return best;
    }

    /** Dong Am: co tren bat ky manh giap/do cam tay -> mien Chill */
    public boolean hasDongAmArmor(Player p) {
        return armorLevel(p, pAm) > 0;
    }

    private boolean putLore(ItemStack hand, NamespacedKey k, int level, String line) {
        if (hand.getType() == Material.AIR) return false;
        level = Math.max(1, level);
        ItemMeta m = hand.getItemMeta();
        if (m == null) return false;
        m.getPersistentDataContainer().set(k, PersistentDataType.INTEGER, level);
        List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        String shortKey = k.getKey();
        lore.removeIf(l -> l.contains(shortKeyToName(shortKey)));
        lore.add(line);
        m.setLore(lore);
        hand.setItemMeta(m);
        return true;
    }

    private String shortKeyToName(String k) {
        return switch (k) {
            case "nguyetam" -> "Nguyệt Ẩm";
            case "boithu" -> "Bội Thu";
            case "suonghan" -> "Sương Hàn";
            case "huyetcuong" -> "Huyết Cuồng";
            case "dongam" -> "Đông Ấm";
            case "nguyetgiap" -> "Nguyệt Giáp";
            default -> k;
        };
    }

    public boolean applyNguyetAm(Player p, int level) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isWeapon(hand)) return false;
        level = clamp(level, 3);
        return putLore(hand, pNguyet, level, "§dNguyệt Ẩm " + toRoman(level) + " §7- hút " + (level * 5 + 1) + "% máu");
    }

    public boolean applyBoiThu(Player p, int level) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isTool(hand)) return false;
        level = clamp(level, 3);
        return putLore(hand, pBoi, level, "§aBội Thu " + toRoman(level) + " §7- " + (20 + level * 15) + "% x2 nông sản");
    }

    public boolean applySuongHan(Player p, int level) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isWeapon(hand)) return false;
        level = clamp(level, 2);
        return putLore(hand, pSuong, level, "§bSương Hàn " + toRoman(level) + " §7- " + (level * 10) + "% làm chậm 2s");
    }

    public boolean applyHuyetCuong(Player p, int level) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isWeapon(hand)) return false;
        level = clamp(level, 3);
        return putLore(hand, pCuong, level, "§cHuyết Cuồng " + toRoman(level) + " §7- mạnh khi yếu máu/đêm Huyết Nguyệt");
    }

    public boolean applyDongAm(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isArmor(hand) && !isTool(hand) && !isWeapon(hand)) return false;
        return putLore(hand, pAm, 1, "§eĐông Ấm §7- miễn Chill mùa Đông");
    }

    public boolean applyNguyetGiap(Player p, int level) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isArmor(hand)) return false;
        level = clamp(level, 2);
        return putLore(hand, pGiap, level, "§9Nguyệt Giáp " + toRoman(level) + " §7- giảm " + (level * 8) + "% sát thương ban đêm");
    }

    private int clamp(int lv, int max) { return Math.max(1, Math.min(max, lv)); }

    private boolean isWeapon(ItemStack it) {
        if (it == null) return false;
        String n = it.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("MACE");
    }

    private boolean isTool(ItemStack it) {
        if (it == null) return false;
        String n = it.getType().name();
        return n.endsWith("_HOE") || n.endsWith("_AXE") || n.endsWith("_SWORD") || n.endsWith("_SHOVEL");
    }

    private boolean isArmor(ItemStack it) {
        if (it == null) return false;
        String n = it.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || n.equals("TURTLE_HELMET");
    }

    private String toRoman(int n) {
        return n >= 3 ? "III" : n == 2 ? "II" : "I";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        ItemStack hand = p.getInventory().getItemInMainHand();

        // Huyet Cuong: +dmg khi mau thap hoac Blood Moon
        int cuong = getHuyetCuong(hand);
        if (cuong > 0) {
            double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            boolean low = p.getHealth() < maxHp * 0.4;
            boolean blood = plugin.getBloodMoonManager() != null && plugin.getBloodMoonManager().isActive();
            if (low || blood) {
                double bonus = 0.08 * cuong + (blood ? 0.09 : 0) + (low ? 0.06 : 0);
                e.setDamage(e.getDamage() * (1.0 + bonus));
            }
        }

        // Suong Han: slow muc tieu
        int suong = getSuongHan(hand);
        if (suong > 0 && random.nextDouble() < suong * 0.10 + 0.05) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, suong - 1, false, false, true));
        }

        // Nguyet Am: hut mau theo final damage (uoc luong sau buff cuong)
        int lv = getNguyetAm(hand);
        if (lv > 0) {
            double pct = 0.06 + (lv - 1) * 0.04 + (lv == 3 ? 0.03 : 0);
            if (plugin.getBloodMoonManager() != null && plugin.getBloodMoonManager().isActive()) pct *= 1.5;
            double heal = e.getFinalDamage() * pct;
            if (heal >= 0.5) {
                double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                p.setHealth(Math.min(maxHp, p.getHealth() + heal));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDefend(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        int giap = armorLevel(p, pGiap);
        if (giap <= 0) return;
        long time = p.getWorld().getTime();
        boolean night = time >= 13000 && time <= 23000;
        if (!night && (plugin.getBloodMoonManager() == null || !plugin.getBloodMoonManager().isActive())) return;
        e.setDamage(e.getDamage() * (1.0 - giap * 0.08));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Material t = b.getType();
        boolean crop = t == Material.WHEAT || t == Material.CARROTS || t == Material.POTATOES
                || t == Material.BEETROOTS || t == Material.NETHER_WART || t == Material.SWEET_BERRY_BUSH;
        if (!crop) return;
        if (b.getBlockData() instanceof Ageable age) {
            if (age.getAge() < age.getMaximumAge()) return;
        }
        Player p = e.getPlayer();
        int lv = getBoiThu(p.getInventory().getItemInMainHand());
        if (lv <= 0) return;
        double chance = 0.20 + (lv - 1) * 0.15;
        try {
            if (plugin.getSeasonManager().currentSeason().name().equals("THU")) chance += 0.10;
        } catch (Exception ignored) {}
        if (random.nextDouble() < chance) {
            for (ItemStack d : b.getDrops(p.getInventory().getItemInMainHand())) {
                b.getWorld().dropItemNaturally(b.getLocation(), d);
            }
        }
        if (t == Material.WHEAT || t == Material.CARROTS || t == Material.POTATOES || t == Material.BEETROOTS) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (b.getType() == Material.AIR) {
                    b.setType(t);
                    if (b.getBlockData() instanceof Ageable a2) {
                        a2.setAge(0);
                        b.setBlockData(a2);
                    }
                }
            });
        }
    }
}
