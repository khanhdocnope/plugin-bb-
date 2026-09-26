package vn.bloodharvest.enchant;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.bloodharvest.BloodHarvestPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MVP Pseudo-enchant (fake, khong NMS):
 * - nguyetam I-III: hut % sat thuong thanh mau
 * - boithu I-III: dap lua tang ti le x2 + tu trong lai
 */
public class PseudoEnchant implements Listener {

    private final BloodHarvestPlugin plugin;
    private final NamespacedKey kNguyet = new NamespacedKey("bloodharvest", "nguyetam");
    private final NamespacedKey kBoi = new NamespacedKey("bloodharvest", "boithu");
    // Luu bang PDC rieng cua plugin de tranh trung key
    private NamespacedKey pNguyet;
    private NamespacedKey pBoi;
    private final Random random = new Random();

    public PseudoEnchant(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
        this.pNguyet = new NamespacedKey(plugin, "nguyetam");
        this.pBoi = new NamespacedKey(plugin, "boithu");
    }

    public int getNguyetAm(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return 0;
        Integer v = it.getItemMeta().getPersistentDataContainer().get(pNguyet, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public int getBoiThu(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return 0;
        Integer v = it.getItemMeta().getPersistentDataContainer().get(pBoi, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public boolean applyNguyetAm(Player p, int level) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) return false;
        if (!isWeapon(hand)) return false;
        level = Math.max(1, Math.min(3, level));
        ItemMeta m = hand.getItemMeta();
        m.getPersistentDataContainer().set(pNguyet, PersistentDataType.INTEGER, level);
        List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l.contains("Nguyet Am"));
        lore.add("§dNguyet Am " + toRoman(level) + " §7- hut " + (level * 5 + 1) + "% mau");
        m.setLore(lore);
        hand.setItemMeta(m);
        return true;
    }

    public boolean applyBoiThu(Player p, int level) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) return false;
        if (!isTool(hand)) return false;
        level = Math.max(1, Math.min(3, level));
        ItemMeta m = hand.getItemMeta();
        m.getPersistentDataContainer().set(pBoi, PersistentDataType.INTEGER, level);
        List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l.contains("Boi Thu"));
        lore.add("§aBoi Thu " + toRoman(level) + " §7- " + (20 + level * 15) + "% x2 nong san");
        m.setLore(lore);
        hand.setItemMeta(m);
        return true;
    }

    private boolean isWeapon(ItemStack it) {
        String n = it.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("MACE");
    }

    private boolean isTool(ItemStack it) {
        String n = it.getType().name();
        return n.endsWith("_HOE") || n.endsWith("_AXE") || n.endsWith("_SWORD") || n.endsWith("_SHOVEL");
    }

    private String toRoman(int n) {
        return n == 3 ? "III" : n == 2 ? "II" : "I";
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        int lv = getNguyetAm(p.getInventory().getItemInMainHand());
        if (lv <= 0) return;
        double pct = 0.06 + (lv - 1) * 0.04 + (lv == 3 ? 0.03 : 0); // 6/10/15%
        // BloodMoon +50% hieu qua
        if (plugin.getBloodMoonManager().isActive()) pct *= 1.5;
        double heal = e.getFinalDamage() * pct;
        if (heal < 0.5) return;
        double nhp = Math.min(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(),
                p.getHealth() + heal);
        p.setHealth(nhp);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Material t = b.getType();
        boolean crop = t == Material.WHEAT || t == Material.CARROTS || t == Material.POTATOES
                || t == Material.BEETROOTS || t == Material.NETHER_WART || t == Material.SWEET_BERRY_BUSH;
        if (!crop) return;
        // Chi khi chin
        if (b.getBlockData() instanceof Ageable age) {
            if (age.getAge() < age.getMaximumAge()) return;
        }
        Player p = e.getPlayer();
        int lv = getBoiThu(p.getInventory().getItemInMainHand());
        if (lv <= 0) return;
        double chance = 0.20 + (lv - 1) * 0.15; // 20/35/50%
        // Mua THU +10%
        try {
            if (plugin.getSeasonManager().currentSeason().name().equals("THU")) chance += 0.10;
        } catch (Exception ignored) {}
        if (random.nextDouble() < chance) {
            // x2: them drop giong crop
            for (ItemStack d : b.getDrops(p.getInventory().getItemInMainHand())) {
                b.getWorld().dropItemNaturally(b.getLocation(), d);
            }
        }
        // Tu trong lai: dat lai tuoi 0 sau 1 tick ( tranh dup voi event )
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
