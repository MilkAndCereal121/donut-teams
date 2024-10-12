package net.luxcube.minecraft.entity.permission;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

@RequiredArgsConstructor
@Getter
public enum TeamPermission {
  DELETE(0x1, false),
  INVITE(0x2, false),
  HOME(0x4, true),
  SET_HOME(0x8, false);

  private final int bitmask;
  private final boolean defaultValue;

  public static EnumSet<TeamPermission> toEnumSet(int bitmask) {
    EnumSet<TeamPermission> permissions = EnumSet.noneOf(TeamPermission.class);
    for (TeamPermission permission : values()) {
      if ((bitmask & permission.bitmask) != 0) {
        permissions.add(permission);
      }
    }
    return permissions;
  }

  public static int toBitmask(@NotNull Collection<TeamPermission> permissions) {
    int bitmask = 0;
    for (TeamPermission permission : permissions) {
      bitmask |= permission.bitmask;
    }
    return bitmask;
  }

  public static List<TeamPermission> fromBitmask(int bitmask) {
    return toEnumSet(bitmask).stream().toList();
  }

  public boolean hasPermission(int bitmask) {
    return (bitmask & this.bitmask) != 0;
  }
}
