package org.vinerdream.citPaper.converter;

import java.util.Arrays;
import java.util.List;

public class Range {
    private record RangePart(int minimum, int maximum, boolean isPercents) {
        private boolean check(int value, int maxValue) {
            if (isPercents) {
                value = (int) Math.round((double) value / maxValue * 100);
            }
            return value >= minimum && value <= maximum;
        }

        public String toString() {
            String result = minimum + "-" + maximum;
            if (isPercents) {
                return result + "%";
            }
            return result;
        }

        private static RangePart parse(String data) {
            final int min, max;
            boolean isPercents = false;
            if (data.endsWith("%")) {
                isPercents = true;
                data = data.substring(0, data.length() - 1);
            }
            if (data.contains("-")) {
                String[] parts = data.split("-");
                if (parts[0].endsWith("%")) {
                    isPercents = true;
                    parts[0] = parts[0].substring(0, parts[0].length() - 1);
                }
                min = parts[0].isEmpty() ? 0 : Integer.parseInt(parts[0]);
                if (parts.length >= 2) {
                    max = Integer.parseInt(parts[1]);
                } else {
                    max = isPercents ? 100 : Integer.MAX_VALUE;
                }
            } else {
                min = max = Integer.parseInt(data);
            }
            return new RangePart(min, max, isPercents);
        }
    }

    private final List<RangePart> ranges;

    public Range(String data) {
        ranges = Arrays.stream(data.split(" ")).map(RangePart::parse).toList();
    }

    public boolean check(int value, int maxValue) {
        return ranges.stream().anyMatch(part -> part.check(value, maxValue));
    }

    public String toString() {
        return String.join(" ", ranges.stream().map(RangePart::toString).toList());
    }
}
