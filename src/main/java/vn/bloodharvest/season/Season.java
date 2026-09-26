package vn.bloodharvest.season;

public enum Season {
    XUAN("Xuan", "§a§lMua Xuan §7- Lua/Mia x1.3"),
    HA("Ha", "§6§lMua Ha §7- Nang han, Ember Pepper"),
    THU("Thu", "§e§lMua Thu §7- Bi ngo/Dua x1.5"),
    DONG("Dong", "§b§lMua Dong §7- Dong bang, coi chung Chill");

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
