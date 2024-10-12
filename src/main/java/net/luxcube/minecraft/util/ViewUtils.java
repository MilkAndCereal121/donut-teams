package net.luxcube.minecraft.util;

import me.saiintbrisson.minecraft.AbstractView;
import me.saiintbrisson.minecraft.VirtualView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ViewUtils {

    public static void cancelAllDefaultActions(@NotNull AbstractView view) {
        view.setCancelOnClick(true);
        view.setCancelOnClone(true);
        view.setCancelOnDrag(true);
        view.setCancelOnDrop(true);
        view.setCancelOnMoveOut(true);
        view.setCancelOnPickup(true);
        view.setCancelOnShiftClick(true);
    }

    public static void fillBorder(@NotNull VirtualView view, @NotNull ItemStack edge) {
        int sizeInventory = view.getSize();

        for (int i = 0; i < 9; i++) {
            view.slot(i).withItem(edge);
        }

        for (int i = sizeInventory - 9; i < sizeInventory; i++) {
            view.slot(i).withItem(edge);
        }

        for (int i = 0; i < (sizeInventory / 9) - 1; i++) {
            view.slot(i * 9).withItem(edge);
            view.slot(i * 9 + 8).withItem(edge);
        }
    }

    public static int getRollbackSlot(@NotNull VirtualView view) {
        return view.getSize() - 5;
    }

}
