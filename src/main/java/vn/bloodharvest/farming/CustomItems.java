package vn.bloodharvest.farming;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.bloodharvest.BloodHarvestPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * MVP Farming: 4 item custom bang vanilla + PDC, khong can resource pack.
 * - Blood Shard (tien boss)
 * - Frostberry (DONG)
 * - Ember Pepper (HA)
 * - Tra Am (chong Chill)
 */
public class CustomItems implements Listener {

    private final BloodHarvestPlugin plugin;
    private final NamespacedKey itemKey;

    public CustomItems(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "bh_item");
    }

    public NamespacedKey key() { return itemKey; }

    public boolean isCustom(ItemStack it, String id) {
        if (it == null || !it.hasItemMeta()) return false;
        String v = it.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return id.equals(v);
    }

    private ItemStack base(Material mat, String name, List<String> lore, String id) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        m.setLore(lore);
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    public ItemStack createBloodShard(int amount) {
        ItemStack it = base(Material.AMETHYST_SHARD, "§c§lBlood Shard",
                Arrays.asList("§7Tien Blood Moon", "§7Doi enchant / goi boss"), "blood_shard");
        it.setAmount(Math.max(1, amount));
        return it;
    }

    public ItemStack createFrostberry(int amount) {
        ItemStack it = base(Material.SWEET_BERRIES, "§b§lFrostberry",
                Arrays.asList("§7Moc mua §bDong", "§7An: hoi 2 dui + chong lanh 30s"), "frostberry");
        it.setAmount(Math.max(1, amount));
        return it;
    }

    public ItemStack createEmberPepper(int amount) {
        ItemStack it = base(Material.CARROT, "§6§lEmber Pepper",
                Arrays.asList("§7Moc mua §6Ha", "§7An: nhanh nhe 20s + no bung"), "ember_pepper");
        it.setAmount(Math.max(1, amount));
        return it;
    }

    public ItemStack createTraAm(int amount) {
        ItemStack it = base(Material.HONEY_BOTTLE, "§e§lTra Am",
                Arrays.asList("§7Giai Chill mua Dong", "§7Uong: khang lanh 3 phut"), "tra_am");
        it.setAmount(Math.max(1, amount));
        return it;
    }

    public ItemStack createEnchantBook(String type, int level) {
        String name = type.equals("boithu") ? "§a§lSach Boi Thu " + level : "§d§lSach Nguyet Am " + level;
        ItemStack it = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList("§7Cam sach + chuot phai vao vu khi/nong cu", "§7/goi: /bh enchant <nguyetam|boithu>", "§8" + type + ":" + level));
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "enchant_" + type);
        // Luu level vao PDC rieng de lenh doc
        m.getPersistentDataContainer().set(new NamespacedKey(plugin, "enchant_level"), PersistentDataType.INTEGER, level);
        it.setItemMeta(m);
        return it;
    }

    public int bookLevel(ItemStack book) {
        if (book == null || !book.hasItemMeta()) return 1;
        Integer v = book.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "enchant_level"), PersistentDataType.INTEGER);
        return v == null ? 1 : Math.max(1, Math.min(3, v));
    }

    public void registerRecipes() {
        try {
            // Tra Am: honey bottle + sweet berries (dai dien Frostberry) - shapeless de de craft
            NamespacedKey k1 = new NamespacedKey(plugin, "tra_am_recipe");
            ShapelessRecipe r1 = new ShapelessRecipe(k1, createTraAm(1));
            r1.addIngredient(Material.HONEY_BOTTLE);
            r1.addIngredient(Material.SWEET_BERRIES);
            plugin.getServer().addRecipe(r1);

            // Sach Nguyet Am: book + 4 blood shard
            NamespacedKey k2 = new NamespacedKey(plugin, "nguyetam_book");
            ShapedRecipe r2 = new ShapedRecipe(k2, createEnchantBook("nguyetam", 1));
            r2.shape("SSS", "SBS", "SSS");
            r2.setIngredient('S', Material.AMETHYST_SHARD);
            r2.setIngredient('B', Material.BOOK);
            plugin.getServer().addRecipe(r2);
        } catch (Exception e) {
            plugin.getLogger().warning("Khong dang ky duoc recipe: " + e.getMessage());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack it = e.getItem();
        if (isCustom(it, "frostberry")) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
        } else if (isCustom(it, "ember_pepper")) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0));
        } else if (isCustom(it, "tra_am")) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0, false, false, true));
            e.getPlayer().removePotionEffect(PotionEffectType.HUNGER);
            e.getPlayer().removePotionEffect(PotionEffectType.SLOW);
        }
        // De vanilla xu ly food/honey binh thuong (bao hoa, hoi mau)
    }

    // An PDC enchant khoi hien thi lung tung
    @SuppressWarnings("unused")
    private void hideEnchants(ItemMeta m) {
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
}
