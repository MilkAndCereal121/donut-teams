package net.luxcube.minecraft.adapter;

import net.luxcube.minecraft.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static net.luxcube.minecraft.util.Colors.translateHex;

public class ItemStackAdapter {


  public static ItemStack toItemStack(@NotNull ConfigurationSection section) {

    Material material = Material.getMaterial(
            section.getString("material", "STONE")
    );

    if (material == null) {
      material = Material.STONE;
    }

    ItemBuilder itemBuilder = new ItemBuilder(material);

    if (section.contains("name")) {
      itemBuilder.name(translateHex(section.getString("name")));
    }

    if (section.contains("lore")) {
      itemBuilder.lore(translateHex(section.getStringList("lore")));
    }

    if (section.contains("custom-model-data")) {
      itemBuilder.customModelData(section.getInt("custom-model-data"));
    }

    return itemBuilder.result();

  }

}
