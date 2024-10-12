package net.luxcube.minecraft.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.entity.permission.TeamPermission;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
@Setter
public class TeamEntity {

  private final String name;

  private final List<TeamMember> members;

  private final Instant createdAt;

  private boolean friendlyFire;

  private Location home;

  @NotNull
  public TeamMember getLeader() {
    return members.stream()
      .filter(member -> member.hasPermission(TeamPermission.DELETE))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Team has no leader"));
  }

  public boolean hasMember(@NotNull TeamMember teamMember) {
    return members.contains(teamMember);
  }

  public boolean hasMember(@NotNull String name) {
    return members.stream()
      .anyMatch(member -> member.getName().equals(name));
  }

  public boolean hasMember(@NotNull UUID uniqueId) {
    return members.stream()
      .anyMatch(member -> member.getUniqueId().equals(uniqueId));
  }

  public void addMember(@NotNull TeamMember teamMember) {
    members.add(teamMember);
  }

  public void removeMember(@NotNull TeamMember teamMember) {
    members.removeIf(member -> member.getUniqueId().equals(teamMember.getUniqueId()));
  }

  public void removeMember(@NotNull UUID uniqueId) {
    members.removeIf(member -> member.getUniqueId().equals(uniqueId));
  }

  public void removeMember(@NotNull String name) {
    members.removeIf(member -> member.getName().equals(name));
  }

  public boolean isSomeoneOnline(@NotNull Player except) {
    return members.stream()
      .map(TeamMember::getBukkitPlayer)
      .filter(player -> player != null && player != except)
      .anyMatch(Player::isOnline);
  }
}
