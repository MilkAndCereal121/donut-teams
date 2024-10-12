package net.luxcube.minecraft.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colors {

  private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

  public static List<String> translateHex(@NotNull List<String> colors) {
    return colors.stream()
            .map(Colors::translateHex)
            .toList();
  }

  public static String translateHex(String message) {
    message = message.replaceAll("&#", "#");

    Matcher matcher = HEX_PATTERN.matcher(message);
    while (matcher.find()) {
      String hexCode = message.substring(matcher.start(), matcher.end());
      String replaceSharp = hexCode.replace('#', 'x');

      char[] ch = replaceSharp.toCharArray();
      StringBuilder builder = new StringBuilder("");
      for (char c : ch) {
        builder.append("&" + c);
      }

      message = message.replace(hexCode, builder.toString());
      matcher = HEX_PATTERN.matcher(message);
    }

    return ChatColor.translateAlternateColorCodes('&', message);
  }
}
