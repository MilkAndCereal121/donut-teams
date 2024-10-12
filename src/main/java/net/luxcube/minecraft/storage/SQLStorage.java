package net.luxcube.minecraft.storage;

import lombok.experimental.Delegate;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.entity.permission.TeamPermission;
import net.luxcube.minecraft.util.Blocks;
import net.luxcube.minecraft.util.SQLReader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

public class SQLStorage {

  private final TeamsPlugin teamsPlugin;

  @Delegate
  private SQLReader sqlReader;

  private Connection connection;

  public SQLStorage(@NotNull TeamsPlugin teamsPlugin) {
    this.teamsPlugin = teamsPlugin;

    File databaseFile = new File(teamsPlugin.getDataFolder(), "database.db");
    if (!databaseFile.exists()) {
      teamsPlugin.saveResource("database.db", false);
    }

    try {
      Class.forName("org.sqlite.JDBC");
    } catch (@NotNull ClassNotFoundException exception) {
      teamsPlugin.getLogger()
        .severe("Failed to load the SQLite driver: " + exception.getMessage());
    }

    try {
      connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to connect to the database: " + exception.getMessage());
    }

    sqlReader = new SQLReader();
      try {
          sqlReader.loadFromResources();
      } catch (@NotNull IOException e) {
        teamsPlugin.getLogger()
          .severe("Failed to load SQL resources: " + e.getMessage());
      }
  }

  public void setup() {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("create_team_table"))) {
      statement.executeUpdate();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to create the teams table: " + exception.getMessage());
    }

    try (PreparedStatement statement = connection.prepareStatement(getSql("create_member_table"))) {
      statement.executeUpdate();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to create the members table: " + exception.getMessage());
    }
  }

  public void close() {
    if (connection == null) {
      throw new IllegalStateException("The database connection is already closed");
    }

    try {
      connection.close();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to close the database connection: " + exception.getMessage());
    }
  }

  public void createTeam(@NotNull TeamEntity teamEntity) {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("insert_team_into_table"))) {
      statement.setString(1, teamEntity.getName());
      statement.setLong(
        2,
        teamEntity.getCreatedAt()
          .toEpochMilli()
      );
      statement.setBoolean(3, teamEntity.isFriendlyFire());
      statement.executeUpdate();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to create the team: " + exception.getMessage());
    }
  }

  public void disband(@NotNull TeamEntity teamEntity) {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("disband_team_from_table"))) {
      statement.setString(1, teamEntity.getName());
      statement.executeUpdate();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to delete the team: " + exception.getMessage());
    }
  }

  public void addUser(@NotNull TeamMember teamMember) {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("insert_member_into_team"))) {
      statement.setString(
        1,
        teamMember.getUniqueId()
          .toString()
      );

      statement.setString(2, teamMember.getName());
      statement.setString(3, teamMember.getTeamName());

      statement.setLong(
        4,
        teamMember.getJoinedAt()
          .toEpochMilli()
      );
      statement.setInt(
        5,
        TeamPermission.toBitmask(teamMember.getTeamPermissions())
      );
      statement.executeUpdate();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to add the user: " + exception.getMessage());
    }
  }

  public void removeUser(@NotNull TeamMember teamMember) {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("kick_member_by_uuid_and_team"))) {
      statement.setString(
        1,
        teamMember.getUniqueId()
          .toString()
      );
      statement.setString(2, teamMember.getTeamName());
      statement.executeUpdate();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to remove the user: " + exception.getMessage());
    }
  }

  public void updateTeam(@NotNull TeamEntity teamEntity) {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("update_team_info"))) {
      statement.setBoolean(1, teamEntity.isFriendlyFire());

      Location location = teamEntity.getHome();
      if (location != null) {
        statement.setString(
          2,
          location.getWorld()
            .getName()
        );

        statement.setLong(
          3,
          Blocks.asLong(
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
          )
        );

        statement.setFloat(4, location.getYaw());
        statement.setFloat(5, location.getPitch());
      } else {
        statement.setNull(2, java.sql.Types.VARCHAR);
        statement.setNull(3, Types.INTEGER);
        statement.setNull(4, java.sql.Types.FLOAT);
        statement.setNull(5, java.sql.Types.FLOAT);
      }

      statement.setString(6, teamEntity.getName());

      statement.executeUpdate();
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to update the team: " + exception.getMessage());
    }
  }

  @Nullable
  public TeamMember retrieveMember(@NotNull UUID uniqueId) {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("retrieve_member_by_uuid"))) {
      statement.setString(1, uniqueId.toString());

      ResultSet resultSet = statement.executeQuery();
      if (!resultSet.next()) {
        return null;
      }

      return new TeamMember(
        uniqueId,
        resultSet.getString("name"),
        resultSet.getString("team_name"),
        Instant.ofEpochMilli(
          resultSet.getLong("joined_at")
        ),
        TeamPermission.fromBitmask(
          resultSet.getInt("permissions")
        )
      );
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to retrieve the member: " + exception.getMessage());
      exception.printStackTrace();
    }

    return null;
  }

  @Nullable
  public TeamEntity retrieveTeam(@NotNull String teamName) {
    checkArgument(connection != null, "The database connection is not open");

    try (PreparedStatement statement = connection.prepareStatement(getSql("retrieve_team_by_name"))) {
      statement.setString(1, teamName);

      ResultSet resultSet = statement.executeQuery();
      if (!resultSet.next()) {
        return null;
      }

      TeamEntity teamEntity = new TeamEntity(
        teamName,
        new ArrayList<>(),
        Instant.ofEpochMilli(
          resultSet.getLong("created_at")
        )
      );

      teamEntity.setFriendlyFire(
        resultSet.getBoolean("friendly_fire")
      );

      String worldName = resultSet.getString("home_world");
      if (worldName != null) {
        Location home = Blocks.fill(
          resultSet.getLong("home_position")
        );

        home.setWorld(Bukkit.getWorld(worldName));

        home.setYaw(
          resultSet.getFloat("home_yaw")
        );

        home.setPitch(
          resultSet.getFloat("home_pitch")
        );

        teamEntity.setHome(home);
      }

      return teamEntity;
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to retrieve the team: " + exception.getMessage());
      exception.printStackTrace();

    }

    return null;
  }

  @NotNull
  public List<TeamMember> retrieveAllMembers(@NotNull String teamName) {
    checkArgument(connection != null, "The database connection is not open");

    List<TeamMember> teamMembers = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(getSql("retrieve_all_members_by_team"))) {
      statement.setString(1, teamName);

      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        teamMembers.add(new TeamMember(
          UUID.fromString(resultSet.getString("unique_id")),
          resultSet.getString("name"),
          teamName,
          Instant.ofEpochMilli(
            resultSet.getLong("joined_at")
          ),
          TeamPermission.fromBitmask(
            resultSet.getInt("permissions")
          )
        ));
      }
    } catch (@NotNull Exception exception) {
      teamsPlugin.getLogger()
        .severe("Failed to retrieve the members: " + exception.getMessage());
      exception.printStackTrace();

    }

    return teamMembers;
  }
}
