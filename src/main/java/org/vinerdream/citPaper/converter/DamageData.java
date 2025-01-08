package org.vinerdream.citPaper.converter;

public class DamageData {
    private final int minimum;
    private final int maximum;
    private final boolean isPercents;

    public DamageData(int minimum, int maximum, boolean isPercents) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.isPercents = isPercents;
    }

    public boolean check(int currentDurability, int maxDurability) {
        if (isPercents) {
            currentDurability = (int) ((double) currentDurability / maxDurability * 100);
        }
        return currentDurability >= minimum && currentDurability <= maximum;
    }

    public String toString() {
        String result = minimum + "-" + maximum;
        if (isPercents) result += "%";
        return result;
    }

    public static DamageData fromString(String data) {
        final int min, max;
        final boolean isPercents;
        if (data.endsWith("%")) {
            isPercents = true;
            data = data.substring(0, data.length() - 1);
        } else {
            isPercents = false;
        }
        if (data.contains("-")) {
            String[] parts = data.split("-");
            min = Integer.parseInt(parts[0]);
            if (parts.length >= 2) {
                max = Integer.parseInt(parts[1]);
            } else {
                max = Integer.MAX_VALUE;
            }
        } else {
            min = max = Integer.parseInt(data);
        }
        return new DamageData(min, max, isPercents);
    }
}
