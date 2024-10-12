package net.luxcube.minecraft.vo;

import net.luxcube.minecraft.entity.TeamEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record InviteVO(
  @NotNull UUID inviter,
  @NotNull TeamEntity teamEntity,
  long expiration
) {

  public boolean isExpired() {
    return System.currentTimeMillis() > expiration;
  }

}
