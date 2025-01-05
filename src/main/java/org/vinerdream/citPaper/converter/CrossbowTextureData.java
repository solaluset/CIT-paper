package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CrossbowTextureData extends BowTextureData {
    private final static String prefix = "crossbow_";
    private TextureData withArrow;
    private TextureData withFirework;

    public CrossbowTextureData(
            String model, String texture,
            TextureData pulling_0,
            TextureData pulling_1,
            TextureData pulling_2,
            TextureData withArrow,
            TextureData withFirework
    ) {
        super(model, texture, pulling_0, pulling_1, pulling_2);
        this.withArrow = withArrow;
        this.withFirework = withFirework;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && withArrow == null && withFirework == null;
    }

    @Override
    public TextureData[] getAll() {
        return new TextureData[]{this, getPulling_0(), getPulling_1(), getPulling_2(), withArrow, withFirework};
    }

    public static CrossbowTextureData fromMap(Map<String, String> map, TextureData mainData) {
        BowTextureData bowData = BowTextureData.fromMap(map, mainData);
        final CrossbowTextureData result;
        if (bowData != null) {
            result = new CrossbowTextureData(
                    bowData.getModel(),
                    bowData.getTexture(),
                    bowData.getPulling_0(),
                    bowData.getPulling_1(),
                    bowData.getPulling_2(),
                    null,
                    null
            );
        } else {
            result = new CrossbowTextureData(null, null, null, null, null, null, null);
        }
        result.setWithArrow(TextureData.fromMap(map, prefix + "arrow"));
        result.setWithFirework(TextureData.fromMap(map, prefix + "firework"));
        if (result.isEmpty()) return null;
        if (mainData != null) {
            result.setModel(mainData.getModel());
            result.setTexture(mainData.getTexture());
        }
        return result;
    }
}
