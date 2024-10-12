package net.luxcube.minecraft.entity.member;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.luxcube.minecraft.entity.permission.TeamPermission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class TeamMember {

  private final UUID uniqueId;

  private final String name, teamName;

  private final Instant joinedAt;

  private final List<TeamPermission> teamPermissions;

  @Nullable
  public Player getBukkitPlayer() {
    return Bukkit.getPlayer(uniqueId);
  }

  @NotNull
  public OfflinePlayer getBukkitOfflinePlayer() {
    return Bukkit.getOfflinePlayer(uniqueId);
  }

  public boolean hasPermission(@NotNull TeamPermission teamPermission) {
    return teamPermissions.contains(teamPermission);
  }

  public boolean hasPermission(@NotNull List<TeamPermission> teamPermissions) {
    return this.teamPermissions.containsAll(teamPermissions);
  }

  public boolean hasPermission(@NotNull TeamPermission... teamPermissions) {
    return hasPermission(List.of(teamPermissions));
  }

  public int getPermissionPriority() {
    return teamPermissions.stream()
            .mapToInt(TeamPermission::getBitmask)
            .max()
            .orElse(0);
  }



}
