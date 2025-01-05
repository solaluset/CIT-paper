package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

import static org.vinerdream.citPaper.utils.MapUtils.popValue;

public class TextureData {
    @Getter
    @Setter
    private String model;
    @Getter
    private final String texture;

    public TextureData(String model, String texture) {
        this.model = model;
        this.texture = texture;
    }

    public boolean isEmpty() {
        return model == null && texture == null;
    }

    public static TextureData fromMap(Map<String, String> map, String key) {
        final TextureData result;
        if (key == null) {
            result = new TextureData(popValue(map, "model", null), popValue(map, "texture", null));
        } else {
            result = new TextureData(
                    popValue(map, "model." + key, null),
                    popValue(map, "texture." + key, null)
            );
        }
        if (result.isEmpty()) return null;
        return result;
    }
}
