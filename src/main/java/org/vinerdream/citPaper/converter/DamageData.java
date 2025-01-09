package org.vinerdream.citPaper.converter;

import lombok.Getter;

import java.util.List;

public class DamageData {
    private final List<Range> ranges;
    @Getter
    private final int mask;

    public DamageData(List<Range> ranges, int mask) {
        this.ranges = ranges;
        this.mask = mask;
    }

    public boolean check(int damage, int maxDurability) {
        damage ^= mask;

        for (Range range : ranges) {
            if (range.check(damage, maxDurability)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return Range.serialize(ranges);
    }

    public static DamageData fromString(String data, String mask) {
        return new DamageData(Range.deserialize(data), Integer.parseInt(mask));
    }
}
