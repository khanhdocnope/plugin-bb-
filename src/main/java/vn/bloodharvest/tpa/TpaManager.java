package vn.bloodharvest.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import vn.bloodharvest.BloodHarvestPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * TPA nhe cho SMP 3-5 nguoi: khong DB, chi Map + 1 task don rac 30s.
 */
public class TpaManager implements Listener {

    public record Request(UUID from, UUID to, boolean here, long atMillis) {
        public boolean expired(long now, long expireMs) {
            return now - atMillis > expireMs;
        }
    }

    private final BloodHarvestPlugin plugin;
    private final Map<UUID, Request> incoming = new HashMap<>(); // target -> request
    private final Map<UUID, Long> cooldown = new HashMap<>(); // sender -> last send
    private final Set<UUID> toggledOff = new HashSet<>();
    private BukkitTask cleanupTask;

    public TpaManager(BloodHarvestPlugin plugin) {
        this.plugin = plugin;
    }

    public int expireSeconds() {
        return Math.max(10, plugin.getConfig().getInt("tpa.expire-seconds", 60));
    }

    public int warmupSeconds() {
        return Math.max(0, Math.min(10, plugin.getConfig().getInt("tpa.warmup-seconds", 3)));
    }

    public int cooldownSeconds() {
        return Math.max(0, plugin.getConfig().getInt("tpa.cooldown-seconds", 10));
    }

    public void startTask() {
        stopTask();
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long exp = expireSeconds() * 1000L;
            incoming.entrySet().removeIf(e -> {
                if (e.getValue().expired(now, exp)) {
                    Player from = Bukkit.getPlayer(e.getValue().from());
                    if (from != null && from.isOnline()) {
                        Player to = Bukkit.getPlayer(e.getKey());
                        String name = to != null ? to.getName() : "người chơi";
                        from.sendMessage(Component.text("§cYêu cầu TPA tới " + name + " đã hết hạn."));
                    }
                    return true;
                }
                return false;
            });
        }, 600L, 600L);
    }

    public void stopTask() {
        if (cleanupTask != null) { cleanupTask.cancel(); cleanupTask = null; }
    }

    public boolean isOff(UUID uuid) { return toggledOff.contains(uuid); }

    public boolean toggle(Player p) {
        if (toggledOff.contains(p.getUniqueId())) {
            toggledOff.remove(p.getUniqueId());
            return false;
        }
        toggledOff.add(p.getUniqueId());
        // Xoa request dang cho toi neu tat
        incoming.remove(p.getUniqueId());
        return true;
    }

    public void send(Player from, Player to, boolean here) {
        if (from.getUniqueId().equals(to.getUniqueId())) {
            from.sendMessage(Component.text("§cBạn không thể TPA tới chính mình."));
            return;
        }
        if (toggledOff.contains(to.getUniqueId())) {
            from.sendMessage(Component.text("§c" + to.getName() + " đang tắt yêu cầu dịch chuyển."));
            return;
        }
        long now = System.currentTimeMillis();
        long cd = cooldownSeconds() * 1000L;
        Long last = cooldown.get(from.getUniqueId());
        if (last != null && now - last < cd && !from.hasPermission("bloodharvest.admin")) {
            long left = (cd - (now - last) + 999) / 1000;
            from.sendMessage(Component.text("§cChờ " + left + "s nữa mới gửi tiếp được."));
            return;
        }
        cooldown.put(from.getUniqueId(), now);
        incoming.put(to.getUniqueId(), new Request(from.getUniqueId(), to.getUniqueId(), here, now));

        if (here) {
            from.sendMessage(Component.text("§aĐã gửi yêu cầu §e" + to.getName() + " §ađến chỗ bạn. Hết hạn sau " + expireSeconds() + "s."));
            to.sendMessage(Component.text("§e" + from.getName() + " §7muốn bạn dịch chuyển đến chỗ họ."));
        } else {
            from.sendMessage(Component.text("§aĐã gửi yêu cầu dịch chuyển tới §e" + to.getName() + "§a. Hết hạn sau " + expireSeconds() + "s."));
            to.sendMessage(Component.text("§e" + from.getName() + " §7muốn dịch chuyển tới chỗ bạn."));
        }
        Component buttons = Component.text("§a[Chấp nhận] ")
                .clickEvent(ClickEvent.runCommand("/tpaccept " + from.getName()))
                .append(Component.text("§c[Từ chối]").clickEvent(ClickEvent.runCommand("/tpdeny " + from.getName())));
        to.sendMessage(buttons);
    }

    private Request resolve(Player target, String nameOrNull) {
        Request r = incoming.get(target.getUniqueId());
        if (r == null) return null;
        if (nameOrNull == null || nameOrNull.isEmpty()) return r;
        Player named = Bukkit.getPlayerExact(nameOrNull);
        if (named == null) named = Bukkit.getPlayer(nameOrNull);
        if (named == null || !named.getUniqueId().equals(r.from())) return null;
        return r;
    }

    public void accept(Player target, String nameOrNull) {
        Request r = resolve(target, nameOrNull);
        if (r == null) {
            target.sendMessage(Component.text("§cKhông có yêu cầu TPA nào" + (nameOrNull != null ? " từ " + nameOrNull : "") + "."));
            return;
        }
        if (r.expired(System.currentTimeMillis(), expireSeconds() * 1000L)) {
            incoming.remove(target.getUniqueId());
            target.sendMessage(Component.text("§cYêu cầu đã hết hạn."));
            return;
        }
        Player from = Bukkit.getPlayer(r.from());
        if (from == null || !from.isOnline()) {
            incoming.remove(target.getUniqueId());
            target.sendMessage(Component.text("§cNgười gửi đã offline."));
            return;
        }
        incoming.remove(target.getUniqueId());

        // Ai la nguoi di chuyen? /tpa: from -> target. /tpahere: target -> from.
        Player mover = r.here() ? target : from;
        Player dest = r.here() ? from : target;
        if (!mover.isOnline() || !dest.isOnline()) {
            target.sendMessage(Component.text("§cMột bên đã offline."));
            return;
        }
        int warmup = mover.hasPermission("bloodharvest.admin") ? 0 : warmupSeconds();
        Location fromLoc = mover.getLocation().clone();
        Location toLoc = dest.getLocation().clone();
        from.sendMessage(Component.text("§a" + target.getName() + " đã chấp nhận! Dịch chuyển sau " + warmup + "s (đứng yên)."));
        target.sendMessage(Component.text("§aĐã chấp nhận. Dịch chuyển sau " + warmup + "s."));
        if (warmup <= 0) {
            mover.teleport(toLoc);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!mover.isOnline() || !dest.isOnline()) return;
            if (mover.getLocation().getWorld() != fromLoc.getWorld()
                    || mover.getLocation().distanceSquared(fromLoc) > 4.0) {
                mover.sendMessage(Component.text("§cBạn đã di chuyển! TPA bị hủy."));
                from.sendMessage(Component.text("§cTPA bị hủy vì đối phương di chuyển."));
                return;
            }
            mover.teleport(dest.getLocation());
            mover.sendMessage(Component.text("§aĐã dịch chuyển!"));
        }, warmup * 20L);
    }

    public void deny(Player target, String nameOrNull) {
        Request r = resolve(target, nameOrNull);
        if (r == null) {
            target.sendMessage(Component.text("§cKhông có yêu cầu TPA nào để từ chối."));
            return;
        }
        incoming.remove(target.getUniqueId());
        target.sendMessage(Component.text("§cĐã từ chối yêu cầu."));
        Player from = Bukkit.getPlayer(r.from());
        if (from != null && from.isOnline()) {
            from.sendMessage(Component.text("§c" + target.getName() + " đã từ chối yêu cầu dịch chuyển."));
        }
    }

    public void cancel(Player from) {
        Request found = null;
        for (Request r : incoming.values()) {
            if (r.from().equals(from.getUniqueId())) { found = r; break; }
        }
        if (found == null) {
            from.sendMessage(Component.text("§cBạn không có yêu cầu nào đang gửi."));
            return;
        }
        incoming.remove(found.to());
        from.sendMessage(Component.text("§eĐã hủy yêu cầu TPA."));
        Player to = Bukkit.getPlayer(found.to());
        if (to != null && to.isOnline()) {
            to.sendMessage(Component.text("§e" + from.getName() + " đã hủy yêu cầu dịch chuyển."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        incoming.remove(id);
        incoming.values().removeIf(r -> r.from().equals(id));
    }
}
