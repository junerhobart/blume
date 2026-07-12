package io.blume.qol.anvil;

import io.blume.qol.QolConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;

public final class AnvilListener implements Listener {

    private final QolConfig config;

    public AnvilListener(@NotNull QolConfig config) {
        this.config = config;
    }

    // LOWEST so dedicated anvil plugins can override our repair-cost cap.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView() instanceof AnvilView view)) {
            return;
        }
        view.setMaximumRepairCost(config.maxRepairCost());
    }
}
