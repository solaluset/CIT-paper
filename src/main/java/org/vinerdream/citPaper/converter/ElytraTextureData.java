package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ElytraTextureData extends TextureData {
    private TextureData broken;

    public ElytraTextureData(String model, String texture, TextureData broken) {
        super(model, texture);
        this.broken = broken;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && broken == null;
    }

    @Override
    public TextureData[] getAll() {
        return new TextureData[]{this, broken};
    }

    public static ElytraTextureData fromMap(Map<String, String> map, TextureData mainData) {
        String model = null;
        String texture = null;

        TextureData data = TextureData.fromMap(map, "elytra");
        if (data != null) {
            model = data.getModel();
            texture = data.getTexture();
        }

        final ElytraTextureData result = new ElytraTextureData(
                model,
                texture,
                TextureData.fromMap(map, "broken_elytra")
        );
        if (result.isEmpty()) return null;
        result.fillFrom(mainData);
        return result;
    }
}
