package org.vinerdream.citPaper.converter;

import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.vinerdream.citPaper.utils.CollectionUtils.popValue;

public class EnchantmentsData implements Comparable<EnchantmentsData> {
    private final @Nullable List<String> enchantments;
    private final @Nullable Range enchantmentLevels;

    public EnchantmentsData(@Nullable List<String> enchantments, @Nullable Range enchantmentLevels) {
        this.enchantments = enchantments;
        this.enchantmentLevels = enchantmentLevels;
    }

    public boolean check(Map<Enchantment, Integer> itemEnchantments) {
        Set<Map.Entry<Enchantment, Integer>> entries = itemEnchantments.entrySet();
        if (enchantments != null) {
            entries = entries.stream().filter(entry -> enchantments.contains(entry.getKey().getKey().toString())).collect(Collectors.toSet());
            if (entries.size() != enchantments.size()) {
                return false;
            }
        }
        if (enchantmentLevels != null) {
            return entries.stream().anyMatch(entry -> enchantmentLevels.check(entry.getValue(), entry.getKey().getMaxLevel()));
        }
        return true;
    }

    public static EnchantmentsData fromMap(Map<String, String> map) {
        final String enchantments = popValue(map, null, "enchantments");
        final String levels = popValue(map, null, "enchantmentLevels");
        if (enchantments == null && levels == null) return null;

        return new EnchantmentsData(
                enchantments != null ? Arrays.stream(enchantments.split(" "))
                        .map(enchantment -> enchantment.contains(":") ? enchantment : "minecraft:" + enchantment)
                        .toList() : null,
                levels != null ? new Range(levels) : null
        );
    }

    public void toMap(Map<String, String> map) {
        if (enchantments != null) {
            map.put("enchantments", String.join(" ", enchantments));
        }
        if (enchantmentLevels != null) {
            map.put("enchantmentLevels", enchantmentLevels.toString());
        }
    }

    @Override
    public int compareTo(@NotNull EnchantmentsData other) {
        if (this.enchantmentLevels != null) {
            if (other.enchantmentLevels == null) {
                return 1;
            }
        } else if (other.enchantmentLevels != null) {
            return -1;
        }
        return Integer.compare(
                this.enchantments != null ? this.enchantments.size() : 0,
                other.enchantments != null ? other.enchantments.size() : 0
        );
    }
}
