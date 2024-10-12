package net.luxcube.minecraft.util;

import org.bukkit.Location;

public class Blocks {

  public static long asLong(int x, int y, int z) {
    return ((long) x & 67108863L) << 38 | (long) y & 4095L | ((long) z & 67108863L) << 12;
  }

  public static Location fill(long packed) {
    int x = (int) (packed >> 38),
      y = (int) (packed << 52 >> 52),
      z = (int) (packed << 26 >> 38);

    return new Location(null, x, y, z);
  }

}
