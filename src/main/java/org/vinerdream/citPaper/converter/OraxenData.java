package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.vinerdream.citPaper.utils.CollectionUtils.popValue;

@Getter
public class OraxenData {
    private final String id;
    @Setter
    private @Nullable Integer customModelData;
    @Setter
    private @Nullable NamespacedKey itemModel;

    public OraxenData(String id, @Nullable Integer customModelData, @Nullable NamespacedKey itemModel) {
        this.id = id;
        this.customModelData = customModelData;
        this.itemModel = itemModel;
    }

    public void toMap(Map<String, String> map) {
        map.put("oraxen_id", id);
        if (customModelData != null) {
            map.put("oraxen_custom_model_data", customModelData.toString());
        }
        if (itemModel != null) {
            map.put("oraxen_item_model", itemModel.toString());
        }
    }

    public static OraxenData fromMap(Map<String, String> map) {
        final String id = popValue(map, null, "oraxen_id");
        if (id == null) return null;
        final String customModelData = popValue(map, null, "oraxen_custom_model_data");
        final String itemModel = popValue(map, null, "oraxen_item_model");
        return new OraxenData(
                id,
                customModelData == null ? null : Integer.parseInt(customModelData),
                itemModel == null ? null : NamespacedKey.fromString(itemModel)
        );
    }
}
