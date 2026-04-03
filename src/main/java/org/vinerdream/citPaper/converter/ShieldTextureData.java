package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ShieldTextureData extends TextureData {
    private TextureData blocking;

    public ShieldTextureData(String model, String texture, TextureData blocking) {
        super(model, texture);
        this.blocking = blocking;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && blocking == null;
    }

    @Override
    public TextureData[] getAll() {
        return new TextureData[]{this, blocking};
    }

    public static ShieldTextureData fromMap(Map<String, String> map, TextureData mainData) {
        String model = null;
        String texture = null;

        TextureData data = TextureData.fromMap(map, "shield");
        if (data != null) {
            model = data.getModel();
            texture = data.getTexture();
        }

        final ShieldTextureData result = new ShieldTextureData(
                model,
                texture,
                TextureData.fromMap(map, "shield_blocking")
        );
        if (result.isEmpty()) return null;
        result.fillFrom(mainData);
        return result;
    }
}
