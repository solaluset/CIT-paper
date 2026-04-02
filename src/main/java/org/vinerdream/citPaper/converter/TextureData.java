package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.vinerdream.citPaper.utils.CollectionUtils.popValue;

@Setter
@Getter
public class TextureData {
    private @Nullable String model;
    private @Nullable String texture;
    private @Nullable String overlay;

    public TextureData(String model, String texture) {
        this(model, texture, null);
    }

    public TextureData(@Nullable String model, @Nullable String texture, @Nullable String overlay) {
        this.model = model;
        this.texture = texture;
        this.overlay = overlay;
    }

    public boolean isEmpty() {
        return model == null && texture == null && overlay == null;
    }

    public TextureData[] getAll() {
        return new TextureData[]{this};
    }

    public static TextureData fromMap(Map<String, String> map, String key) {
        final TextureData result;
        if (key == null) {
            result = new TextureData(popValue(map, null, "model"), popValue(map, null, "texture"), popValue(map, null, "texture.overlay"));
        } else {
            result = new TextureData(
                    popValue(map, null, "model." + key),
                    popValue(map, null, "texture." + key),
                    popValue(map, null, "texture." + key + "_overlay")
            );
        }
        if (result.isEmpty()) return null;
        return result;
    }
}
