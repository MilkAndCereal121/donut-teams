package net.luxcube.minecraft.view;

import me.saiintbrisson.minecraft.View;
import me.saiintbrisson.minecraft.ViewContext;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.service.TeamService;
import net.luxcube.minecraft.util.ViewUtils;
import net.luxcube.minecraft.vo.TeamsVO;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.luxcube.minecraft.util.Colors.translateHex;

public class ConfirmTeamDisbandView extends View {

  private final TeamsPlugin teamsPlugin;
  private final TeamsVO.InventoryInfo inventoryInfo;
  private final TeamsVO teamsVO;

  public ConfirmTeamDisbandView(@NotNull TeamsPlugin teamsPlugin, @NotNull TeamsVO.InventoryInfo inventoryInfo) {
    super(inventoryInfo.getRows(), translateHex(inventoryInfo.getTitle()));
    this.teamsPlugin = teamsPlugin;
    this.teamsVO = teamsPlugin.getTeamsVO();
    this.inventoryInfo = inventoryInfo;
    ViewUtils.cancelAllDefaultActions(this);

    setupSlots();
  }

  private void setupSlots() {
    TeamsVO.IconInfo iconInfo = inventoryInfo.getIcons().get("decline");
    slot(iconInfo.getSlot(), iconInfo.getItemStack())
            .closeOnClick();

    iconInfo = inventoryInfo.getIcons().get("accept");
    slot(iconInfo.getSlot(), iconInfo.getItemStack())
            .onClick(click -> handleConfirmDelete(click))
            .closeOnClick();
  }

  @Override
  protected void onRender(@NotNull ViewContext context) {
    context.set("opened_at", System.currentTimeMillis());
  }

  @Nullable
  private TeamEntity getTeamEntity(@NotNull ViewContext viewContext) {
    return viewContext.get("team_entity");
  }

  @NotNull
  private long getOpenedAt(@NotNull ViewContext viewContext) {
    return viewContext.get("opened_at", System::currentTimeMillis);
  }

  private boolean isExpired(@NotNull ViewContext viewContext) {
    long openedAt = getOpenedAt(viewContext);
    return System.currentTimeMillis() - openedAt > 1000;
  }

  private void handleConfirmDelete(@NotNull ViewContext viewContext) {
    TeamEntity teamEntity = getTeamEntity(viewContext);
    if (teamEntity == null) {
      return;
    }

    Player player = viewContext.getPlayer();
    if (!isExpired(viewContext)) {
      String message = teamsVO.getMessage("wait-to-confirm");

      player.sendMessage(message);
      return;
    }

    TeamService teamService = teamsPlugin.getTeamService();
    for (TeamMember teamMember : teamEntity.getMembers()) {
      teamService.remove(teamMember);
      teamsPlugin.getSqlStorage().removeUser(teamMember);
    }

    teamService.remove(teamEntity);
    teamsPlugin.getSqlStorage().disband(teamEntity);

    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    String message = teamsVO.getMessage("disband-team");

    player.sendMessage(message);
    player.sendActionBar(message);
  }


}
