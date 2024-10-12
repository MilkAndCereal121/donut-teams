package net.luxcube.minecraft.view;

import lombok.val;
import me.saiintbrisson.minecraft.PaginatedView;
import me.saiintbrisson.minecraft.PaginatedViewSlotContext;
import me.saiintbrisson.minecraft.ViewContext;
import me.saiintbrisson.minecraft.ViewItem;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.util.ItemBuilder;
import net.luxcube.minecraft.util.ViewUtils;
import net.luxcube.minecraft.view.sort.SortType;
import net.luxcube.minecraft.vo.TeamsVO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static net.luxcube.minecraft.util.Colors.translateHex;

public class TeamInfoView extends PaginatedView<TeamMember> {

  private final TeamsPlugin teamsPlugin;
  private final TeamsVO.InventoryInfo inventoryInfo;
  private final TeamsVO teamsVO;

  public TeamInfoView(@NotNull TeamsPlugin teamsPlugin, @NotNull TeamsVO.InventoryInfo inventoryInfo) {
    super(inventoryInfo.getRows(), translateHex(inventoryInfo.getTitle()));

    this.teamsPlugin = teamsPlugin;
    this.teamsVO = teamsPlugin.getTeamsVO();
    this.inventoryInfo = inventoryInfo;
    ViewUtils.cancelAllDefaultActions(this);

    setLayout(
      inventoryInfo.getLayout()
    );

    setNextPageItem((paginatedContext, item) -> {
      item.withItem(inventoryInfo.getIcons().get("next").getItemStack());
      item.onClick(click -> {
        if (!paginatedContext.hasNextPage()) {
          return;
        }

        paginatedContext.switchToNextPage();
      });
    });

    setPreviousPageItem((paginatedContext, item) -> {
      item.withItem(inventoryInfo.getIcons().get("back").getItemStack());
      item.onClick(click -> {
        if (!paginatedContext.hasPreviousPage()) {
          return;
        }

        paginatedContext.switchToPreviousPage();
      });
    });

    TeamsVO.IconInfo iconInfo = inventoryInfo.getIcons().get("search");
    slot(iconInfo.getSlot(), iconInfo.getItemStack())
      .onClick(this::handleSearchClick)
      .closeOnClick();

    iconInfo = inventoryInfo.getIcons().get("sort-join-date");
    slot(iconInfo.getSlot())
      .onUpdate(update -> update.setItem(buildSortIcon(update)))
      .onRender(render -> render.setItem(buildSortIcon(render)))
      .onClick(this::handleSortClick);

    iconInfo = inventoryInfo.getIcons().get("team");
    slot(iconInfo.getSlot())
      .onUpdate(update -> update.setItem(buildTeamIcon(update)))
      .onRender(render -> render.setItem(buildTeamIcon(render)))
      .onClick(this::handleRefreshClick);

    iconInfo = inventoryInfo.getIcons().get("team-home");
    slot(iconInfo.getSlot(), iconInfo.getItemStack())
      .onClick(this::handleTeamHomeClick)
      .closeOnClick();

    iconInfo = inventoryInfo.getIcons().get("pvp-on");
    slot(iconInfo.getSlot())
      .onUpdate(update -> update.setItem(buildPvpIcon(update)))
      .onRender(render -> render.setItem(buildPvpIcon(render)))
      .onClick(this::handlePvpClick);

    iconInfo = inventoryInfo.getIcons().get("invite");
    for (int slot : (List<Integer>) inventoryInfo.getExtra().get("teammate-slots")) {
      slot(slot, iconInfo.getItemStack())
        .onClick(click -> {
          Player player = click.getPlayer();

          player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

          String message = teamsVO.getMessage("team-invite-usage");

          player.sendMessage(message);
          player.sendActionBar(message);
        }).closeOnClick();
    }
  }

  @Override
  protected void onItemRender(
    @NotNull PaginatedViewSlotContext<TeamMember> paginatedViewSlotContext,
    @NotNull ViewItem viewItem,
    @NotNull TeamMember teamMember
  ) {
    viewItem.withItem(buildTeammateIcon(teamMember));
    viewItem.onClick(click -> {
      Player player = click.getPlayer();
      if (player.getUniqueId().compareTo(teamMember.getUniqueId()) == 0) {
        String message = teamsVO.getMessage("cannot-modify-leader");

        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        return;
      }

      player.closeInventory();
      player.sendMessage("§cFeature still in development.");
    });
  }

  @Override
  protected void onRender(@NotNull ViewContext context) {
    val paginated = context.paginated();
    TeamEntity teamEntity = getTeamEntity(context);

    List<TeamMember> teamMembers = teamEntity.getMembers();

    String filteredName = getFilteredName(context);
    if (filteredName != null) {
      paginated.setSource(
        teamMembers.stream()
          .filter(teamMember -> teamMember.getName().toLowerCase().startsWith(filteredName.toLowerCase()))
          .toList()
      );
    } else {
      SortType sortType = getSortType(context);
      paginated.setSource(sortType.sort(teamsPlugin, teamMembers));
    }
  }

  @NotNull
  private SortType getSortType(@NotNull ViewContext viewContext) {
    return viewContext.get("sort_type", () -> SortType.JOIN_DATE);
  }

  @NotNull
  private TeamEntity getTeamEntity(@NotNull ViewContext viewContext) {
    return viewContext.get("team_entity");
  }

  @Nullable
  private String getFilteredName(@NotNull ViewContext viewContext) {
    return viewContext.get("filtered_name");
  }

  @NotNull
  private ItemStack buildSortIcon(@NotNull ViewContext viewContext) {
    SortType sortType = getSortType(viewContext);

    return switch (sortType) {
      case JOIN_DATE -> inventoryInfo.getIcons().get("sort-join-date").getItemStack();
      case ALPHABETICALLY -> inventoryInfo.getIcons().get("sort-alphabetically").getItemStack();
      case PERMISSIONS -> inventoryInfo.getIcons().get("sort-permissions").getItemStack();
      case MONEY -> inventoryInfo.getIcons().get("sort-money").getItemStack();
    };
  }

  @NotNull
  private ItemStack buildTeamIcon(@NotNull ViewContext viewContext) {
    TeamEntity teamEntity = getTeamEntity(viewContext);
    TeamsVO.IconInfo iconInfo = inventoryInfo.getIcons().get("team");

    return new ItemBuilder(iconInfo.getItemStack().clone())
      .itemFlags(
        ItemFlag.HIDE_ATTRIBUTES
      ).placeholders(Map.of("%name%", teamEntity.getName()))
      .result();
  }

  @NotNull
  private ItemStack buildPvpIcon(@NotNull ViewContext viewContext) {
    TeamEntity teamEntity = getTeamEntity(viewContext);
    TeamsVO.IconInfo iconInfo = inventoryInfo.getIcons().get("pvp-" + (teamEntity.isFriendlyFire() ? "on" : "off"));

    return new ItemBuilder(iconInfo.getItemStack().clone())
      .itemFlags(
        ItemFlag.HIDE_ATTRIBUTES,
        ItemFlag.HIDE_ENCHANTS,
        ItemFlag.HIDE_ATTRIBUTES
      )
      .result();
  }

  @NotNull
  private ItemStack buildTeammateIcon(@NotNull TeamMember teamMember) {
    return new ItemBuilder(Material.PLAYER_HEAD)
      .skull(teamMember.getName())
      .name(translateHex("#34eb9b" + teamMember.getName()))
      .lore("§fClick to edit")
      .result();
  }

  private void handleSearchClick(@NotNull ViewContext viewContext) {
    Player player = viewContext.getPlayer();

    viewContext.close();

    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    String message = teamsVO.getMessage("search-name");

    player.sendMessage(message);
    player.sendActionBar(message);

    player.setMetadata("team_searching", new FixedMetadataValue(teamsPlugin, true));
  }

  private void handleTeamHomeClick(@NotNull ViewContext viewContext) {
    Player player = viewContext.getPlayer();

    viewContext.close();

    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    player.performCommand("team home");
  }

  private void handlePvpClick(@NotNull ViewContext viewContext) {
    TeamEntity teamEntity = getTeamEntity(viewContext);

    Player player = viewContext.getPlayer();
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

    teamEntity.setFriendlyFire(!teamEntity.isFriendlyFire());
//    viewContext.close();

    viewContext.update();
  }

  private void handleRefreshClick(ViewContext viewContext) {
    Player player = viewContext.getPlayer();
//    viewContext.close();

    viewContext.update();
  }

  private void handleSortClick(ViewContext context) {
    SortType sortType = getSortType(context),
      next = switch (sortType) {
        case JOIN_DATE -> SortType.ALPHABETICALLY;
        case ALPHABETICALLY -> SortType.PERMISSIONS;
        case PERMISSIONS -> SortType.MONEY;
        case MONEY -> SortType.JOIN_DATE;
      };

    Player player = context.getPlayer();
    context.set("sort_type", next);

    context.close();

    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

//    context.update();
    teamsPlugin.getViewFrame()
      .open(
        TeamInfoView.class,
        player,
        context.getData()
      );
  }
}
