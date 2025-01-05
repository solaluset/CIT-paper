package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BowTextureData extends TextureData {
    private final static String prefix = "bow_";
    private TextureData pulling_0;
    private TextureData pulling_1;
    private TextureData pulling_2;

    public BowTextureData(
            String model, String texture,
            TextureData pulling_0,
            TextureData pulling_1,
            TextureData pulling_2
    ) {
        super(model, texture);
        this.pulling_0 = pulling_0;
        this.pulling_1 = pulling_1;
        this.pulling_2 = pulling_2;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && pulling_0 == null && pulling_1 == null && pulling_2 == null;
    }

    @Override
    public TextureData[] getAll() {
        return new TextureData[]{this, pulling_0, pulling_1, pulling_2};
    }

    public static BowTextureData fromMap(Map<String, String> map, TextureData mainData) {
        String model = null;
        String texture = null;

        TextureData data = TextureData.fromMap(map, prefix + "standby");
        if (data != null) {
            model = data.getModel();
            texture = data.getTexture();
        }

        final BowTextureData result = new BowTextureData(
                model,
                texture,
                TextureData.fromMap(map, prefix + "pulling_0"),
                TextureData.fromMap(map, prefix + "pulling_1"),
                TextureData.fromMap(map, prefix + "pulling_2")
        );
        if (result.isEmpty()) return null;
        if (mainData != null) {
            result.setModel(mainData.getModel());
            result.setTexture(mainData.getTexture());
        }
        return result;
    }
}
