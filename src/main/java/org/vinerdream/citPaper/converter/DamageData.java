package org.vinerdream.citPaper.converter;

import java.util.Map;

public class DamageData {
    private final Range range;
    private final int mask;

    public DamageData(Range range, int mask) {
        this.range = range;
        this.mask = mask;
    }

    public boolean check(int damage, int maxDurability) {
        return range.check(damage ^ mask, maxDurability);
    }

    public void toMap(Map<String, String> map) {
        map.put("damage", range.toString());
        if (mask != 0) {
            map.put("damageMask", String.valueOf(mask));
        }
    }

    public static DamageData fromString(String data, String mask) {
        return new DamageData(new Range(data), Integer.parseInt(mask));
    }
}
