package vn.bloodharvest.tpa;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.bloodharvest.BloodHarvestPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 1 executor cho ca cum: /tpa /tpahere /tpaccept /tpdeny /tpcancel /tptoggle
 */
public class TpaCommand implements CommandExecutor, TabCompleter {

    private final BloodHarvestPlugin plugin;
    private final TpaManager manager;

    public TpaCommand(BloodHarvestPlugin plugin, TpaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private Player needPlayer(CommandSender s) {
        if (s instanceof Player p) return p;
        s.sendMessage(Component.text("§cChi player moi dung duoc lenh nay."));
        return null;
    }

    private Player findOnline(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) p = Bukkit.getPlayer(name);
        return (p != null && p.isOnline()) ? p : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String n = cmd.getName().toLowerCase();
        switch (n) {
            case "tpa", "tpahere" -> {
                Player from = needPlayer(sender);
                if (from == null) return true;
                if (args.length < 1) {
                    sender.sendMessage(Component.text("§e/" + label + " <ten>"));
                    return true;
                }
                Player to = findOnline(args[0]);
                if (to == null) {
                    sender.sendMessage(Component.text("§cKhong tim thay " + args[0] + " (offline?)."));
                    return true;
                }
                manager.send(from, to, n.equals("tpahere"));
                return true;
            }
            case "tpaccept", "tpyes" -> {
                Player target = needPlayer(sender);
                if (target == null) return true;
                manager.accept(target, args.length >= 1 ? args[0] : null);
                return true;
            }
            case "tpdeny", "tpno" -> {
                Player target = needPlayer(sender);
                if (target == null) return true;
                manager.deny(target, args.length >= 1 ? args[0] : null);
                return true;
            }
            case "tpcancel" -> {
                Player from = needPlayer(sender);
                if (from == null) return true;
                manager.cancel(from);
                return true;
            }
            case "tptoggle" -> {
                Player p = needPlayer(sender);
                if (p == null) return true;
                boolean off = manager.toggle(p);
                p.sendMessage(Component.text(off ? "§cDa TAT yeu cau dich chuyen." : "§aDa BAT yeu cau dich chuyen."));
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String n = cmd.getName().toLowerCase();
        if ((n.equals("tpa") || n.equals("tpahere") || n.equals("tpaccept") || n.equals("tpdeny")) && args.length == 1) {
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player me && me.getUniqueId().equals(p.getUniqueId())) continue;
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(p.getName());
            }
            return out;
        }
        return new ArrayList<>();
    }
}
