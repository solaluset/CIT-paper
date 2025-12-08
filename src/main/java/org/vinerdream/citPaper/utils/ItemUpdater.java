package org.vinerdream.citPaper.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.config.Mode;
import org.vinerdream.citPaper.converter.OraxenData;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;
import org.vinerdream.citPaper.converter.TextureType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.vinerdream.citPaper.utils.CollectionUtils.mergeMaps;
import static org.vinerdream.citPaper.utils.ItemUtils.*;
import static org.vinerdream.citPaper.utils.PdcUtils.*;

public class ItemUpdater {
    private final CITPaper plugin;
    private final NamespacedKey originalDataKey;
    private final NamespacedKey originalModelKey;
    private final NamespacedKey originalArmorModelKey;
    private final NamespacedKey originalCustomModelDataKey;
    private final NamespacedKey originalTrimKey;
    private final NamespacedKey originalTypeKey;

    private final static Method getCustomName;
    private final static Method getLore;
    private final static String EMPTY_MODEL = "EMPTY";

    static {
        Method method = null;

        try {
            method = ItemMeta.class.getMethod("customName");
        } catch (NoSuchMethodException e) {
            try {
                method = ItemMeta.class.getMethod("getDisplayName");
            } catch (NoSuchMethodException ignored) {}
        }
        getCustomName = method;

        try {
            method = ItemMeta.class.getMethod("lore");
        } catch (NoSuchMethodException e) {
            try {
                method = ItemMeta.class.getMethod("getLore");
            } catch (NoSuchMethodException ignored) {}
        }
        getLore = method;
    }

    public ItemUpdater(CITPaper plugin) {
        this.plugin = plugin;
        this.originalDataKey = new NamespacedKey(plugin, "original-data");
        this.originalModelKey = new NamespacedKey(plugin, "model");
        this.originalArmorModelKey = new NamespacedKey(plugin, "armor-model");
        this.originalCustomModelDataKey = new NamespacedKey(plugin, "custom-model-data");
        this.originalTrimKey = new NamespacedKey(plugin, "trim");
        this.originalTypeKey = new NamespacedKey(plugin, "type");
    }

    public @Nullable ItemStack updateItem(ItemStack item, int damage, Map<Enchantment, Integer> enchantments) {
        return updateItem(item, getItemName(item), damage, enchantments);
    }

    public @Nullable ItemStack updateItem(ItemStack item) {
        return updateItem(item, 0, null);
    }

    public @Nullable ItemStack updateItem(ItemStack item, Map<Enchantment, Integer> enchantments) {
        return updateItem(item, 0, enchantments);
    }

    public @Nullable ItemStack updateItem(ItemStack item, int damage) {
        return updateItem(item, damage, null);
    }

    public @Nullable ItemStack updateItem(ItemStack item, String name) {
        return updateItem(item, name, 0, null);
    }

    public @Nullable ItemStack updateItem(ItemStack item, String name, int damage, Map<Enchantment, Integer> enchantments) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        try {
            return updateMeta(meta, item.getType(), name, damage, enchantments);
        } finally {
            item.setItemMeta(meta);
        }
    }

    public @Nullable ItemStack updateMeta(BookMeta meta) {
        return updateMeta(meta, Material.WRITTEN_BOOK, getItemName(meta), 0, null);
    }

    @SuppressWarnings("removal")
    public @Nullable ItemStack updateMeta(ItemMeta meta, Material itemType, String name, int damage, Map<Enchantment, Integer> enchantments) {
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final boolean legacy = pdc.has(originalDataKey) && Objects.requireNonNull(
                pdc.get(originalDataKey, PersistentDataType.TAG_CONTAINER)
        ).getKeys().isEmpty();

        final Material type;
        ItemStack oraxenResult = null;
        {
            final String originalType = getNestedKey(
                    pdc,
                    PersistentDataType.STRING,
                    originalDataKey,
                    originalTypeKey
            );
            if (originalType != null) {
                type = getMaterial(originalType);
            } else {
                type = itemType;
            }
            assert type != null;
        }

        boolean appliedItem = false;
        boolean appliedArmor = false;
        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (data.getType() == TextureType.ITEM) {
                if (appliedItem) continue;
            } else if (appliedArmor) continue;
            if (plugin.getMode() == Mode.ORAXEN && data.getType() == TextureType.ITEM && data.getOraxenData() == null) {
                continue;
            }

            if (data.getItems().stream().noneMatch(itemKey -> type.getKey().toString().equals(itemKey))) {
                continue;
            }
            if (data.getNamePattern() != null && !data.getNamePattern().matcher(name).find()) {
                continue;
            }
            final List<String> lore = getItemLore(meta);
            if (!data.getLoreData().entrySet().stream().allMatch(entry -> {
                if (entry.getKey() == null) {
                    return lore.stream().anyMatch(line -> entry.getValue().matcher(line).find());
                }
                return lore.size() > entry.getKey() && entry.getValue().matcher(lore.get(entry.getKey())).find();
            })) {
                continue;
            }
            if (data.getCustomModelData() != -1 && (!meta.hasCustomModelData() || meta.getCustomModelData() != data.getCustomModelData())) {
                continue;
            }
            if (data.getDamage() != null && meta instanceof Damageable damageable) {
                if (!data.getDamage().check(damageable.getDamage() + damage, type.getMaxDurability())) {
                    continue;
                }
            }
            if (data.getEnchantments() != null && !data.getEnchantments().check(mergeMaps(meta.getEnchants(), enchantments))) {
                continue;
            }
            if (data.getPotion() != null) {
                if (!(meta instanceof PotionMeta potionMeta)) {
                    continue;
                } else {
                    final PotionType potionType = potionMeta.getBasePotionType();
                    if (potionType == null || !data.getPotion().equals(potionType.getKey().toString())) {
                        continue;
                    }
                }
            }

            if (data.getType() == TextureType.ITEM) {
                if (getNestedKey(
                        pdc,
                        PersistentDataType.STRING,
                        originalDataKey,
                        originalModelKey
                ) == null) {
                    final NamespacedKey model = getItemModel(meta);
                    setNestedKey(
                            pdc,
                            PersistentDataType.STRING,
                            model != null && !legacy ? model.toString() : EMPTY_MODEL,
                            originalDataKey,
                            originalModelKey
                    );
                    if (plugin.getMode() == Mode.ORAXEN) {
                        setNestedKey(
                                pdc,
                                PersistentDataType.INTEGER,
                                meta.hasCustomModelData() ? meta.getCustomModelData() : -1,
                                originalDataKey,
                                originalCustomModelDataKey
                        );
                    }
                }
                if (plugin.getMode() == Mode.ORAXEN) {
                    oraxenResult = oraxenItemCopy(plugin, meta, type);
                    assert data.getOraxenData() != null;
                    applyOraxenData(meta, data.getOraxenData());
                } else {
                    meta.setItemModel(data.getKey());
                }
                appliedItem = true;
            }
            if (data.getArmorData() != null && !appliedArmor) {
                if (getNestedKey(
                        pdc,
                        PersistentDataType.STRING,
                        originalDataKey,
                        originalArmorModelKey
                ) == null) {
                    setNestedKey(
                            pdc,
                            PersistentDataType.STRING,
                            EQUIPPABLE_EXISTS && meta.getEquippable().getModel() != null && !legacy ? meta.getEquippable().getModel().toString() : EMPTY_MODEL,
                            originalDataKey,
                            originalArmorModelKey
                    );
                    if (plugin.getMode() == Mode.ORAXEN && meta instanceof ArmorMeta armorMeta) {
                        setNestedKey(
                                pdc,
                                PersistentDataType.STRING,
                                armorMeta.getTrim() != null ? armorMeta.getTrim().getPattern().getKey().toString() : EMPTY_MODEL,
                                originalDataKey,
                                originalTrimKey
                        );
                        setNestedKey(
                                pdc,
                                PersistentDataType.STRING,
                                type.getKey().toString(),
                                originalDataKey,
                                originalTypeKey
                        );
                    }
                }
                final NamespacedKey armorModel = NamespacedKey.fromString(data.getArmorData().getModel());
                assert armorModel != null;
                if (!armorModel.getNamespace().equals("oraxen")) {
                    setArmorTexture(meta, type.getKey().getKey(), armorModel);
                } else if (meta instanceof ArmorMeta armorMeta) {
                    oraxenResult = oraxenItemCopy(plugin, meta, type);
                    applyOraxenTrim(armorMeta, armorModel);
                }
                appliedArmor = true;
            }
        }

        if (!appliedItem) {
            final String saved = getNestedKey(pdc, PersistentDataType.STRING, originalDataKey, originalModelKey);
            if (saved != null) {
                if (ITEM_MODEL_EXISTS) {
                    if (saved.equals(EMPTY_MODEL)) {
                        meta.setItemModel(null);
                    } else {
                        meta.setItemModel(NamespacedKey.fromString(saved));
                    }
                }
                removeNestedKey(pdc, originalDataKey, originalModelKey);
            }
            final Integer savedCustomModelData = getNestedKey(
                    pdc,
                    PersistentDataType.INTEGER,
                    originalDataKey,
                    originalCustomModelDataKey
            );
            if (savedCustomModelData != null) {
                meta.setCustomModelData(savedCustomModelData != -1 ? savedCustomModelData : null);
                removeNestedKey(pdc, originalDataKey, originalCustomModelDataKey);
            }
        }

        if (!appliedArmor) {
            final String saved = getNestedKey(pdc, PersistentDataType.STRING, originalDataKey, originalArmorModelKey);
            if (saved != null) {
                if (EQUIPPABLE_EXISTS) {
                    if (saved.equals(EMPTY_MODEL)) {
                        setArmorTexture(meta, null, null);
                    } else {
                        setArmorTexture(meta, type.getKey().getKey(), NamespacedKey.fromString(saved));
                    }
                }
                removeNestedKey(pdc, originalDataKey, originalArmorModelKey);
            }

            final String originalType = getNestedKey(pdc, PersistentDataType.STRING, originalDataKey, originalTypeKey);
            if (originalType != null) {
                oraxenResult = new ItemStack(Objects.requireNonNull(getMaterial(originalType)));
                removeNestedKey(pdc, originalDataKey, originalTypeKey);
            }
            final String originalTrim =  getNestedKey(pdc, PersistentDataType.STRING, originalDataKey, originalTrimKey);
            if (originalTrim != null && meta instanceof ArmorMeta armorMeta) {
                applyOraxenTrim(armorMeta, originalTrim.equals(EMPTY_MODEL) ? null : NamespacedKey.fromString(originalTrim));
                meta.removeItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                removeNestedKey(pdc, originalDataKey, originalTrimKey);
            }
        }

        if (oraxenResult != null) {
            cleanModifiers(itemType, oraxenResult.getType(), meta);
            oraxenResult.setItemMeta(meta);
        }
        return oraxenResult;
    }

    private void setArmorTexture(ItemMeta meta, String itemName, NamespacedKey texture) {
        if (texture == null || itemName == null) {
            meta.setEquippable(null);
            return;
        }
        EquippableComponent equippable = meta.getEquippable();
        if (isHelmet(itemName)) {
            equippable.setSlot(EquipmentSlot.HEAD);
        } else if (isChestplate(itemName) || isElytra(itemName)) {
            equippable.setSlot(EquipmentSlot.CHEST);
        } else if (isLeggings(itemName)) {
            equippable.setSlot(EquipmentSlot.LEGS);
        } else if (isBoots(itemName)) {
            equippable.setSlot(EquipmentSlot.FEET);
        } else return;
        equippable.setModel(texture);
        meta.setEquippable(equippable);
    }

    private void applyOraxenData(@NotNull ItemMeta meta, @NotNull OraxenData data) {
        if (data.getItemModel() != null) {
            meta.setItemModel(data.getItemModel());
        }
        if (data.getCustomModelData() != null) {
            meta.setCustomModelData(data.getCustomModelData());
        }
    }

    @SuppressWarnings("deprecation")
    private void applyOraxenTrim(@NotNull ArmorMeta meta, NamespacedKey trim) {
        if (trim == null) {
            meta.setTrim(null);
            return;
        }
        final TrimMaterial trimMaterial;
        if (meta.getTrim() != null) {
            trimMaterial = meta.getTrim().getMaterial();
        } else {
            trimMaterial = Registry.TRIM_MATERIAL.get(Objects.requireNonNull(
                    NamespacedKey.fromString(plugin.getConfig().getString("oraxen.defaultTrimMaterial", "minecraft:amethyst"))
            ));
        }
        meta.setTrim(new ArmorTrim(trimMaterial, Registry.TRIM_PATTERN.get(trim)));
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
    }

    private static String getItemName(ItemStack item) {
        if (item == null) return "";
        return getItemName(item.getItemMeta());
    }

    private static @NotNull String getItemName(ItemMeta meta) {
        if (meta == null) {
            return "";
        }

        final Object result;
        try {
            result = getCustomName.invoke(meta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        if (result == null) {
            if (meta instanceof BookMeta bookMeta) {
                final String title = bookMeta.getTitle();
                if (title != null) {
                    return title;
                }
            }
            return "";
        }

        if (ReflectionUtils.isComponentClassPresent()) {
            return ComponentUtils.componentToString(result);
        }

        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private static @NotNull List<String> getItemLore(ItemMeta meta) {
        if (meta == null) {
            return List.of();
        }

        final Object result;
        try {
            result = getLore.invoke(meta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        if (result == null) return List.of();

        final List<Object> loreList = (List<Object>) result;

        if (ReflectionUtils.isComponentClassPresent()) {
            return loreList.stream().map(ComponentUtils::componentToString).map(s -> s == null ? "" : s).toList();
        }

        return loreList.stream().map(o -> o == null ? "" : o).map(Object::toString).toList();
    }
}
