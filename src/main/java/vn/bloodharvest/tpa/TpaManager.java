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
                        String name = to != null ? to.getName() : "nguoi choi";
                        from.sendMessage(Component.text("§cYeu cau TPA toi " + name + " da het han."));
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
            from.sendMessage(Component.text("§cBan khong the TPA toi chinh minh."));
            return;
        }
        if (toggledOff.contains(to.getUniqueId())) {
            from.sendMessage(Component.text("§c" + to.getName() + " dang tat yeu cau dich chuyen."));
            return;
        }
        long now = System.currentTimeMillis();
        long cd = cooldownSeconds() * 1000L;
        Long last = cooldown.get(from.getUniqueId());
        if (last != null && now - last < cd && !from.hasPermission("bloodharvest.admin")) {
            long left = (cd - (now - last) + 999) / 1000;
            from.sendMessage(Component.text("§cCho " + left + "s nua moi gui tiep duoc."));
            return;
        }
        cooldown.put(from.getUniqueId(), now);
        incoming.put(to.getUniqueId(), new Request(from.getUniqueId(), to.getUniqueId(), here, now));

        if (here) {
            from.sendMessage(Component.text("§aDa gui yeu cau §e" + to.getName() + " §aden cho ban. Het han sau " + expireSeconds() + "s."));
            to.sendMessage(Component.text("§e" + from.getName() + " §7muon ban dich chuyen den cho ho."));
        } else {
            from.sendMessage(Component.text("§aDa gui yeu cau dich chuyen toi §e" + to.getName() + "§a. Het han sau " + expireSeconds() + "s."));
            to.sendMessage(Component.text("§e" + from.getName() + " §7muon dich chuyen toi cho ban."));
        }
        Component buttons = Component.text("§a[Chap nhan] ")
                .clickEvent(ClickEvent.runCommand("/tpaccept " + from.getName()))
                .append(Component.text("§c[Tu choi]").clickEvent(ClickEvent.runCommand("/tpdeny " + from.getName())));
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
            target.sendMessage(Component.text("§cKhong co yeu cau TPA nao" + (nameOrNull != null ? " tu " + nameOrNull : "") + "."));
            return;
        }
        if (r.expired(System.currentTimeMillis(), expireSeconds() * 1000L)) {
            incoming.remove(target.getUniqueId());
            target.sendMessage(Component.text("§cYeu cau da het han."));
            return;
        }
        Player from = Bukkit.getPlayer(r.from());
        if (from == null || !from.isOnline()) {
            incoming.remove(target.getUniqueId());
            target.sendMessage(Component.text("§cNguoi gui da offline."));
            return;
        }
        incoming.remove(target.getUniqueId());

        // Ai la nguoi di chuyen? /tpa: from -> target. /tpahere: target -> from.
        Player mover = r.here() ? target : from;
        Player dest = r.here() ? from : target;
        if (!mover.isOnline() || !dest.isOnline()) {
            target.sendMessage(Component.text("§cMot ben da offline."));
            return;
        }
        int warmup = mover.hasPermission("bloodharvest.admin") ? 0 : warmupSeconds();
        Location fromLoc = mover.getLocation().clone();
        Location toLoc = dest.getLocation().clone();
        from.sendMessage(Component.text("§a" + target.getName() + " da chap nhan! Dich chuyen sau " + warmup + "s (dung yen)."));
        target.sendMessage(Component.text("§aDa chap nhan. Dich chuyen sau " + warmup + "s."));
        if (warmup <= 0) {
            mover.teleport(toLoc);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!mover.isOnline() || !dest.isOnline()) return;
            if (mover.getLocation().getWorld() != fromLoc.getWorld()
                    || mover.getLocation().distanceSquared(fromLoc) > 4.0) {
                mover.sendMessage(Component.text("§cBan da di chuyen! TPA bi huy."));
                from.sendMessage(Component.text("§cTPA bi huy vi doi phuong di chuyen."));
                return;
            }
            mover.teleport(dest.getLocation());
            mover.sendMessage(Component.text("§aDa dich chuyen!"));
        }, warmup * 20L);
    }

    public void deny(Player target, String nameOrNull) {
        Request r = resolve(target, nameOrNull);
        if (r == null) {
            target.sendMessage(Component.text("§cKhong co yeu cau TPA nao de tu choi."));
            return;
        }
        incoming.remove(target.getUniqueId());
        target.sendMessage(Component.text("§cDa tu choi yeu cau."));
        Player from = Bukkit.getPlayer(r.from());
        if (from != null && from.isOnline()) {
            from.sendMessage(Component.text("§c" + target.getName() + " da tu choi yeu cau dich chuyen."));
        }
    }

    public void cancel(Player from) {
        Request found = null;
        for (Request r : incoming.values()) {
            if (r.from().equals(from.getUniqueId())) { found = r; break; }
        }
        if (found == null) {
            from.sendMessage(Component.text("§cBan khong co yeu cau nao dang gui."));
            return;
        }
        incoming.remove(found.to());
        from.sendMessage(Component.text("§eDa huy yeu cau TPA."));
        Player to = Bukkit.getPlayer(found.to());
        if (to != null && to.isOnline()) {
            to.sendMessage(Component.text("§e" + from.getName() + " da huy yeu cau dich chuyen."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        incoming.remove(id);
        incoming.values().removeIf(r -> r.from().equals(id));
    }
}
