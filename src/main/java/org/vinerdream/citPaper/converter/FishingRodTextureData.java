package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class FishingRodTextureData extends TextureData {
    private TextureData cast;

    public FishingRodTextureData(String model, String texture, TextureData cast) {
        super(model, texture);
        this.cast = cast;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && cast == null;
    }

    @Override
    public TextureData[] getAll() {
        return new TextureData[]{this, cast};
    }

    public static FishingRodTextureData fromMap(Map<String, String> map, TextureData mainData) {
        String model = null;
        String texture = null;

        TextureData data = TextureData.fromMap(map, "fishing_rod");
        if (data != null) {
            model = data.getModel();
            texture = data.getTexture();
        }

        final FishingRodTextureData result = new FishingRodTextureData(
                model,
                texture,
                TextureData.fromMap(map, "fishing_rod_cast")
        );
        if (result.isEmpty()) return null;
        result.fillFrom(mainData);
        return result;
    }
}
