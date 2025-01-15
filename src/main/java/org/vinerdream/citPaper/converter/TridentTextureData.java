package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TridentTextureData extends TextureData {
    private TextureData inHand;
    private TextureData throwing;

    public TridentTextureData(String model, String texture, TextureData inHand, TextureData throwing) {
        super(model, texture);
        this.inHand = inHand;
        this.throwing = throwing;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && inHand == null && throwing == null;
    }

    @Override
    public TextureData[] getAll() {
        return new TextureData[]{this, inHand, throwing};
    }

    public static TridentTextureData fromMap(Map<String, String> map, TextureData mainData) {
        String model = null;
        String texture = null;

        TextureData data = TextureData.fromMap(map, "trident");
        if (data != null) {
            model = data.getModel();
            texture = data.getTexture();
        }

        final TridentTextureData result = new TridentTextureData(
                model,
                texture,
                TextureData.fromMap(map, "trident_in_hand"),
                TextureData.fromMap(map, "trident_throwing")
        );
        if (result.isEmpty()) return null;
        if (mainData != null) {
            if (result.getModel() == null) {
                result.setModel(mainData.getModel());
            }
            if (result.getTexture() == null) {
                result.setTexture(mainData.getTexture());
            }
        }
        return result;
    }
}
