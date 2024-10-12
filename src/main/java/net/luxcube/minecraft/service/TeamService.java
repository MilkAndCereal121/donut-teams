package net.luxcube.minecraft.service;

import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

public class TeamService {

  private final Map<UUID, TeamMember> teamMembers = new Hashtable<>();
  private final Map<String, TeamEntity> teamEntities = new Hashtable<>();

  @Nullable
  public TeamMember getTeamMember(@NotNull UUID uniqueId) {
    return teamMembers.get(uniqueId);
  }

  @Nullable
  public TeamEntity getTeamEntity(@NotNull String teamName) {
    return teamEntities.get(teamName);
  }

  public void put(@NotNull TeamMember teamMember) {
    teamMembers.put(teamMember.getUniqueId(), teamMember);
  }

  public void put(@NotNull TeamEntity teamEntity) {
    teamEntities.put(teamEntity.getName(), teamEntity);
  }

  public void remove(@NotNull TeamMember teamMember) {
    teamMembers.remove(teamMember.getUniqueId());
  }

  public void remove(@NotNull UUID uniqueId) {
    teamMembers.remove(uniqueId);
  }

  public void remove(@NotNull TeamEntity teamEntity) {
    teamEntities.remove(teamEntity.getName());
  }

  public boolean contains(@NotNull UUID uniqueId) {
    return teamMembers.containsKey(uniqueId);
  }

  public boolean contains(@NotNull String teamName) {
    return teamEntities.containsKey(teamName);
  }

  public Collection<TeamEntity> getAllTeams() {
    return teamEntities.values();
  }

  public Collection<TeamMember> getAllMembers() {
    return teamMembers.values();
  }
}
