package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

import static org.vinerdream.citPaper.utils.MapUtils.popValue;

@Setter
@Getter
public class TextureData {
    private String model;
    private String texture;

    public TextureData(String model, String texture) {
        this.model = model;
        this.texture = texture;
    }

    public boolean isEmpty() {
        return model == null && texture == null;
    }

    public TextureData[] getAll() {
        return new TextureData[]{this};
    }

    public static TextureData fromMap(Map<String, String> map, String key) {
        final TextureData result;
        if (key == null) {
            result = new TextureData(popValue(map, null, "model"), popValue(map, null, "texture"));
        } else {
            result = new TextureData(
                    popValue(map, null, "model." + key),
                    popValue(map, null, "texture." + key)
            );
        }
        if (result.isEmpty()) return null;
        return result;
    }
}
