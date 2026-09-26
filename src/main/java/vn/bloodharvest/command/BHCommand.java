package vn.bloodharvest.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
 * BloodHarvest v1.0: /bh mua | lich | bloodmoon | boss | give | enchant | help
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
            case "lich", "calendar" -> {
                var sm = plugin.getSeasonManager();
                sender.sendMessage(Component.text("§6§lLich BloodHarvest"));
                sender.sendMessage(Component.text("§7Mua: " + String.join(" -> ", sm.getOrder())));
                sender.sendMessage(sm.statusLine());
                if (!Bukkit.getWorlds().isEmpty()) {
                    World w = Bukkit.getWorlds().get(0);
                    long nights = plugin.getBloodMoonManager().nightsToNext(w);
                    sender.sendMessage(Component.text(nights == 0 ? "§cDem nay la Blood Moon!" : "§7Blood Moon sau " + nights + " dem."));
                }
                sender.sendMessage(Component.text("§7Boss song: " + plugin.getBossManager().aliveBosses() + " | Elite: " + plugin.getBossManager().aliveElites()));
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
                    sender.sendMessage(Component.text("§e/bh boss <dalang|kysi|huyetmau|yeti|hoayeu|bunhin|tinhlinh|list>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("list")) {
                    sender.sendMessage(Component.text("§7Boss: dalang, kysi, huyetmau | Elite: yeti, hoayeu, bunhin, tinhlinh"));
                    sender.sendMessage(Component.text("§7Dang song: boss=" + plugin.getBossManager().aliveBosses() + " elite=" + plugin.getBossManager().aliveElites()));
                    return true;
                }
                Location loc = p.getLocation().add(p.getLocation().getDirection().multiply(3));
                loc.setY(p.getWorld().getHighestBlockYAt(loc).getY() + 1);
                boolean ok = false;
                String name = args[1].toLowerCase();
                switch (name) {
                    case "dalang" -> ok = plugin.getBossManager().spawnDaLang(loc);
                    case "kysi" -> ok = plugin.getBossManager().spawnKySi(loc);
                    case "huyetmau" -> ok = plugin.getBossManager().spawnHuyetMau(loc);
                    case "yeti" -> ok = plugin.getBossManager().spawnYeti(loc);
                    case "hoayeu" -> ok = plugin.getBossManager().spawnHoaYeu(loc);
                    case "bunhin" -> ok = plugin.getBossManager().spawnBuNhin(loc);
                    case "tinhlinh" -> ok = plugin.getBossManager().spawnTinhLinh(loc);
                    default -> sender.sendMessage(Component.text("§cKhong biet boss/elite."));
                }
                if (name.equals("dalang") || name.equals("kysi") || name.equals("huyetmau")) {
                    sender.sendMessage(Component.text(ok ? "§aDa goi boss " + name + "!" : "§cDa co boss roi (gioi han 1)."));
                } else {
                    sender.sendMessage(Component.text("§aDa goi elite " + name + "!"));
                }
                return true;
            }
            case "give" -> {
                if (!(sender instanceof Player p)) return true;
                if (!sender.hasPermission("bloodharvest.admin")) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§e/bh give <shard|frost|traam|ember|moonroot|banh|lau|nguyetam|boithu|suonghan|huyetcuong|dongam|nguyetgiap> [sl]"));
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
                    case "moonroot" -> p.getInventory().addItem(plugin.getCustomItems().createMoonroot(amt));
                    case "banh" -> p.getInventory().addItem(plugin.getCustomItems().createBanhBiThu(amt));
                    case "lau" -> p.getInventory().addItem(plugin.getCustomItems().createLauNamNguyet(amt));
                    case "nguyetam", "boithu", "suonghan", "huyetcuong", "dongam", "nguyetgiap" ->
                            p.getInventory().addItem(plugin.getCustomItems().createEnchantBook(args[1].toLowerCase(), 1));
                    default -> sender.sendMessage(Component.text("§cKhong biet item."));
                }
                sender.sendMessage(Component.text("§aDa give " + args[1] + " x" + amt));
                return true;
            }
            case "enchant" -> {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§e/bh enchant <nguyetam|boithu|suonghan|huyetcuong|dongam|nguyetgiap> [lv]"));
                    return true;
                }
                int lv = 1;
                if (args.length >= 3) {
                    try { lv = Math.max(1, Math.min(3, Integer.parseInt(args[2]))); } catch (Exception ignored) {}
                }
                boolean ok = false;
                switch (args[1].toLowerCase()) {
                    case "nguyetam" -> ok = plugin.getPseudoEnchant().applyNguyetAm(p, lv);
                    case "boithu" -> ok = plugin.getPseudoEnchant().applyBoiThu(p, lv);
                    case "suonghan" -> ok = plugin.getPseudoEnchant().applySuongHan(p, lv);
                    case "huyetcuong" -> ok = plugin.getPseudoEnchant().applyHuyetCuong(p, lv);
                    case "dongam" -> ok = plugin.getPseudoEnchant().applyDongAm(p);
                    case "nguyetgiap" -> ok = plugin.getPseudoEnchant().applyNguyetGiap(p, lv);
                    default -> sender.sendMessage(Component.text("§cKhong biet enchant."));
                }
                sender.sendMessage(Component.text(ok ? "§aEnchant thanh cong!" : "§cCam dung loai do tren tay chinh (vu khi/nong cu/giap)."));
                return true;
            }
            default -> {
                help(sender);
                return true;
            }
        }
    }

    private void help(CommandSender s) {
        s.sendMessage(Component.text("§6§lBloodHarvest v1.0"));
        s.sendMessage(Component.text("§e/bh mua §7- xem mua | §e/bh lich §7- lich + Blood Moon"));
        s.sendMessage(Component.text("§e/bh bloodmoon §7- check | §e/bh boss list §7- xem boss"));
        s.sendMessage(Component.text("§e/bh boss <dalang|kysi|huyetmau|...> §7(admin)"));
        s.sendMessage(Component.text("§e/bh give <shard|moonroot|banh|lau|...> §7(admin)"));
        s.sendMessage(Component.text("§e/bh enchant <nguyetam|boithu|suonghan|huyetcuong|dongam|nguyetgiap>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("mua", "lich", "bloodmoon", "boss", "give", "enchant", "help"), args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "mua": return filter(Arrays.asList("set", "next"), args[1]);
                case "bloodmoon": return filter(Arrays.asList("status", "start", "end"), args[1]);
                case "boss": return filter(Arrays.asList("list", "dalang", "kysi", "huyetmau", "yeti", "hoayeu", "bunhin", "tinhlinh"), args[1]);
                case "give": return filter(Arrays.asList("shard", "frost", "traam", "ember", "moonroot", "banh", "lau", "nguyetam", "boithu", "suonghan", "huyetcuong", "dongam", "nguyetgiap"), args[1]);
                case "enchant": return filter(Arrays.asList("nguyetam", "boithu", "suonghan", "huyetcuong", "dongam", "nguyetgiap"), args[1]);
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
