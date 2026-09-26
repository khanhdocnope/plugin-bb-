package vn.bloodharvest.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.bloodharvest.BloodHarvestPlugin;
import vn.bloodharvest.season.Season;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /bh mua | bloodmoon | boss | give | enchant | help
 */
public class BHCommand implements CommandExecutor, TabCompleter {

    private final BloodHarvestPlugin plugin;

    public BHCommand(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "mua", "season" -> {
                if (args.length == 1) {
                    sender.sendMessage(plugin.getSeasonManager().statusLine());
                    return true;
                }
                if (args[1].equalsIgnoreCase("next") && sender.hasPermission("bloodharvest.admin")) {
                    plugin.getSeasonManager().nextSeason();
                    sender.sendMessage(Component.text("§aDa chuyen sang " + plugin.getSeasonManager().currentSeason().name()));
                    return true;
                }
                if (args[1].equalsIgnoreCase("set") && args.length >= 3 && sender.hasPermission("bloodharvest.admin")) {
                    try {
                        Season s = Season.fromString(args[2]);
                        plugin.getSeasonManager().setSeason(s);
                        sender.sendMessage(Component.text("§aSet mua -> " + s.name()));
                    } catch (Exception ex) {
                        sender.sendMessage(Component.text("§cMua khong hop le: XUAN/HA/THU/DONG"));
                    }
                    return true;
                }
                sender.sendMessage(Component.text("§e/bh mua | /bh mua next | /bh mua set <XUAN/HA/THU/DONG>"));
                return true;
            }
            case "bloodmoon" -> {
                if (args.length == 1) {
                    boolean a = plugin.getBloodMoonManager().isActive();
                    sender.sendMessage(Component.text(a ? "§cDang Blood Moon!" : "§7Khong co Blood Moon."));
                    return true;
                }
                if (!sender.hasPermission("bloodharvest.admin")) {
                    sender.sendMessage(Component.text("§cCan quyen admin."));
                    return true;
                }
                if (args[1].equalsIgnoreCase("start") && sender instanceof Player p) {
                    plugin.getBloodMoonManager().forceStart(p);
                    sender.sendMessage(Component.text("§cDa bat Blood Moon!"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("end")) {
                    plugin.getBloodMoonManager().forceEnd();
                    return true;
                }
                return true;
            }
            case "boss" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(Component.text("§cChi player moi goi boss."));
                    return true;
                }
                if (!sender.hasPermission("bloodharvest.admin")) {
                    sender.sendMessage(Component.text("§cCan quyen admin de test boss."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§e/bh boss <dalang|yeti>"));
                    return true;
                }
                Location loc = p.getLocation().add(p.getLocation().getDirection().multiply(3));
                loc.setY(p.getWorld().getHighestBlockYAt(loc) + 1);
                if (args[1].equalsIgnoreCase("dalang")) {
                    boolean ok = plugin.getBossManager().spawnDaLang(loc);
                    sender.sendMessage(Component.text(ok ? "§aDa goi Da Lang!" : "§cDa co boss roi (gioi han 1)."));
                } else if (args[1].equalsIgnoreCase("yeti")) {
                    plugin.getBossManager().spawnYeti(loc);
                    sender.sendMessage(Component.text("§aDa goi Yeti Bang!"));
                }
                return true;
            }
            case "give" -> {
                if (!(sender instanceof Player p)) return true;
                if (!sender.hasPermission("bloodharvest.admin")) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§e/bh give <shard|frost|traam|ember|nguyetam|boithu> [soluong]"));
                    return true;
                }
                int amt = 1;
                if (args.length >= 3) {
                    try { amt = Math.max(1, Math.min(64, Integer.parseInt(args[2]))); } catch (Exception ignored) {}
                }
                switch (args[1].toLowerCase()) {
                    case "shard" -> p.getInventory().addItem(plugin.getCustomItems().createBloodShard(amt));
                    case "frost" -> p.getInventory().addItem(plugin.getCustomItems().createFrostberry(amt));
                    case "traam" -> p.getInventory().addItem(plugin.getCustomItems().createTraAm(amt));
                    case "ember" -> p.getInventory().addItem(plugin.getCustomItems().createEmberPepper(amt));
                    case "nguyetam" -> p.getInventory().addItem(plugin.getCustomItems().createEnchantBook("nguyetam", 1));
                    case "boithu" -> p.getInventory().addItem(plugin.getCustomItems().createEnchantBook("boithu", 1));
                    default -> sender.sendMessage(Component.text("§cKhong biet item."));
                }
                sender.sendMessage(Component.text("§aDa give " + args[1] + " x" + amt));
                return true;
            }
            case "enchant" -> {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§e/bh enchant <nguyetam|boithu> [1-3] - cam vu khi tren tay"));
                    return true;
                }
                int lv = 1;
                if (args.length >= 3) {
                    try { lv = Math.max(1, Math.min(3, Integer.parseInt(args[2]))); } catch (Exception ignored) {}
                }
                boolean ok;
                if (args[1].equalsIgnoreCase("nguyetam")) {
                    ok = plugin.getPseudoEnchant().applyNguyetAm(p, lv);
                } else if (args[1].equalsIgnoreCase("boithu")) {
                    ok = plugin.getPseudoEnchant().applyBoiThu(p, lv);
                } else return true;
                sender.sendMessage(Component.text(ok ? "§aEnchant thanh cong!" : "§cCam vu khi/nong cu dung tren tay chinh."));
                return true;
            }
            default -> {
                help(sender);
                return true;
            }
        }
    }

    private void help(CommandSender s) {
        s.sendMessage(Component.text("§6§lBloodHarvest MVP"));
        s.sendMessage(Component.text("§e/bh mua §7- xem mua"));
        s.sendMessage(Component.text("§e/bh bloodmoon status §7- check Blood Moon"));
        s.sendMessage(Component.text("§e/bh boss <dalang|yeti> §7(admin)"));
        s.sendMessage(Component.text("§e/bh give <shard|frost|traam|ember> §7(admin)"));
        s.sendMessage(Component.text("§e/bh enchant <nguyetam|boithu> §7- ep enchant vao do dang cam"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("mua", "bloodmoon", "boss", "give", "enchant", "help"), args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "mua": return filter(Arrays.asList("set", "next"), args[1]);
                case "bloodmoon": return filter(Arrays.asList("status", "start", "end"), args[1]);
                case "boss": return filter(Arrays.asList("dalang", "yeti"), args[1]);
                case "give": return filter(Arrays.asList("shard", "frost", "traam", "ember", "nguyetam", "boithu"), args[1]);
                case "enchant": return filter(Arrays.asList("nguyetam", "boithu"), args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mua") && args[1].equalsIgnoreCase("set")) {
            return filter(Arrays.asList("XUAN", "HA", "THU", "DONG"), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> base, String prefix) {
        List<String> out = new ArrayList<>();
        for (String s : base) if (s.toLowerCase().startsWith(prefix.toLowerCase())) out.add(s);
        return out;
    }
}
