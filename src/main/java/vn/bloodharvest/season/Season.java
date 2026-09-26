package vn.bloodharvest.season;

public enum Season {
    XUAN("Xuân", "§a§lMùa Xuân §7- Lúa/Mía x1.3"),
    HA("Hạ", "§6§lMùa Hạ §7- Nắng hạn, Ember Pepper"),
    THU("Thu", "§e§lMùa Thu §7- Bí ngô/Dưa x1.5"),
    DONG("Đông", "§b§lMùa Đông §7- Đóng băng, coi chừng Chill");

    private final String id;
    private final String display;

    Season(String id, String display) {
        this.id = id;
        this.display = display;
    }

    public String id() { return id; }
    public String display() { return display; }

    public static Season fromString(String s) {
        if (s == null) return XUAN;
        for (Season v : values()) {
            if (v.name().equalsIgnoreCase(s) || v.id.equalsIgnoreCase(s)) return v;
        }
        return XUAN;
    }
}
