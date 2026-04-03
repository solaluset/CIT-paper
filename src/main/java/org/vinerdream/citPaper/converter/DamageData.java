package org.vinerdream.citPaper.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;

import static org.vinerdream.citPaper.utils.CollectionUtils.popValue;

public class DamageData implements Comparable<DamageData> {
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

    public static DamageData fromMap(Map<String, String> map) {
        final String data = popValue(map, null, "damage", "components.damage");
        if (data == null) return null;
        return new DamageData(new Range(data), Integer.parseInt(popValue(map, "0", "damageMask")));
    }

    @Override
    public int compareTo(@NotNull DamageData other) {
        return Comparator.comparing((DamageData d) -> d.range).compare(this, other);
    }
}
