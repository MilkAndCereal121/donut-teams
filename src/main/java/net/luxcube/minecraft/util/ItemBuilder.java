package net.luxcube.minecraft.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemBuilder {

  private final ItemStack itemStack;
  private final ItemMeta itemMeta;

  public ItemBuilder(@NotNull Material material) {
    this.itemStack = new ItemStack(material);
    this.itemMeta = itemStack.getItemMeta();
  }

  public ItemBuilder(@NotNull ItemStack itemStack) {
    this.itemStack = itemStack;
    this.itemMeta = itemStack.getItemMeta();
  }

  public ItemBuilder name(@NotNull String name) {
    this.itemMeta.setDisplayName(name);
    return this;
  }

  public ItemBuilder amount(int amount) {
    this.itemStack.setAmount(amount);
    return this;
  }

  public ItemBuilder skull(@NotNull Player player) {
    SkullMeta skullMeta = (SkullMeta) itemMeta;
    skullMeta.setOwningPlayer(player);
    return this;
  }

  public ItemBuilder skull(@NotNull String playerName) {
    SkullMeta skullMeta = (SkullMeta) itemMeta;
    skullMeta.setOwner(playerName);
    return this;
  }

  public ItemBuilder glow() {
    itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    return this;
  }

  public ItemBuilder lore(@NotNull List<String> lore) {
    this.itemMeta.setLore(lore);
    return this;
  }

  public ItemBuilder addLore(@NotNull List<String> lores) {
    List<String> currentLores = this.itemMeta.getLore();
    if (currentLores == null) {
      currentLores = new ArrayList<>();
    }
    currentLores.addAll(lores);
    this.itemMeta.setLore(currentLores);
    return this;
  }

  public ItemBuilder addLore(@NotNull String... lines) {
    List<String> currentLores = this.itemMeta.getLore();
    if (currentLores == null) {
      currentLores = new ArrayList<>();
    }
    for (String line : lines) {
      currentLores.add(line);
    }
    this.itemMeta.setLore(currentLores);
    return this;
  }

  public ItemBuilder lore(@NotNull String... lines) {
    List<String> lore = itemMeta.getLore();
    if (lore == null) {
      lore = new ArrayList<>();
    }
    for (String line : lines) {
      lore.add(line);
    }
    itemMeta.setLore(lore);
    return this;
  }

  public ItemBuilder color(@NotNull Color color) {
    LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemMeta;
    leatherArmorMeta.setColor(color);
    return this;
  }

  public ItemBuilder itemFlags(@NotNull ItemFlag... itemFlags) {
    this.itemMeta.addItemFlags(itemFlags);
    return this;
  }

  public ItemBuilder placeholders(@NotNull Map<String, String> placeholders) {
    if (itemMeta.hasDisplayName()) {
      String name = itemMeta.getDisplayName();
      name = replacePlaceholders(name, placeholders);
      itemMeta.setDisplayName(name);
    }

    if (itemMeta.hasLore()) {
      List<String> lore = itemMeta.getLore();
      lore.replaceAll(line -> replacePlaceholders(line, placeholders));
      itemMeta.setLore(lore);
    }

    return this;
  }

  public ItemBuilder customModelData(int customModelData) {
    this.itemMeta.setCustomModelData(customModelData);
    return this;
  }

  private String replacePlaceholders(@NotNull String text, @NotNull Map<String, String> placeholders) {
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      text = text.replace(entry.getKey(), entry.getValue());
    }
    return text;
  }

  public ItemBuilder effect(@NotNull PotionEffect potionEffect) {
    PotionMeta potionMeta = (PotionMeta) itemMeta;
    potionMeta.addCustomEffect(potionEffect, true);
    return this;
  }

  public ItemBuilder potionColor(@NotNull Color color) {
    PotionMeta potionMeta = (PotionMeta) itemMeta;
    potionMeta.setColor(color);
    return this;
  }

  public ItemStack result() {
    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

}
