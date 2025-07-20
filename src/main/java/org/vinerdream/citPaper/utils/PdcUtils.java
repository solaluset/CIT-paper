package org.vinerdream.citPaper.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PdcUtils {
    public static <P, C> @Nullable C getNestedKey(@NotNull PersistentDataContainer pdc, @NotNull PersistentDataType<P, C> type, @NotNull NamespacedKey...keys) {
        assert keys.length > 0;
        PersistentDataContainer lastPdc = pdc;
        for (int i = 0; i < keys.length - 1; i++) {
            lastPdc = lastPdc.get(keys[i], PersistentDataType.TAG_CONTAINER);
            if (lastPdc == null) {
                return null;
            }
        }
        return lastPdc.get(keys[keys.length - 1], type);
    }

    public static <P, C> void setNestedKey(@NotNull PersistentDataContainer pdc, @NotNull PersistentDataType<P, C> type, @NotNull C value, @NotNull NamespacedKey ...keys) {
        assert keys.length > 0;
        final List<PersistentDataContainer> pdcList = new ArrayList<>(List.of(pdc));
        for (int i = 0; i < keys.length - 1; i++) {
            if (!pdcList.getLast().has(keys[i])) {
                pdcList.add(pdc.getAdapterContext().newPersistentDataContainer());
            } else {
                pdcList.add(pdcList.getLast().get(keys[i], PersistentDataType.TAG_CONTAINER));
            }
        }
        pdcList.getLast().set(keys[keys.length - 1], type, value);
        for (int i = keys.length - 2; i >= 0; i--) {
            final PersistentDataContainer lastPdc = pdcList.removeLast();
            pdcList.getLast().set(keys[i], PersistentDataType.TAG_CONTAINER, lastPdc);
        }
    }

    public static void removeNestedKey(@NotNull PersistentDataContainer pdc, @NotNull NamespacedKey ...keys) {
        assert keys.length > 0;
        final List<PersistentDataContainer> pdcList = new ArrayList<>(List.of(pdc));
        int lastI = -1;
        for (int i = 0; i < keys.length - 1; i++) {
            if (!pdcList.getLast().has(keys[i])) {
                break;
            }
            pdcList.add(pdcList.getLast().get(keys[i], PersistentDataType.TAG_CONTAINER));
            lastI = i;
        }
        if (lastI == keys.length - 2) {
            pdcList.getLast().remove(keys[keys.length - 1]);
        }
        for (int i = lastI; i >= 0; i--) {
            final PersistentDataContainer lastPdc = pdcList.removeLast();
            if (lastPdc.getKeys().isEmpty()) {
                pdcList.getLast().remove(keys[i]);
            } else {
                pdcList.getLast().set(keys[i], PersistentDataType.TAG_CONTAINER, lastPdc);
            }
        }
    }
}
