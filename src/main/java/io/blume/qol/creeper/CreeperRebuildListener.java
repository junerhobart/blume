package io.blume.qol.creeper;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;

public final class CreeperRebuildListener implements Listener {

    private final CreeperRebuildService service;
    private final boolean rebuildWither;

    public CreeperRebuildListener(
        @NotNull CreeperRebuildService service,
        boolean rebuildWither
    ) {
        this.service = service;
        this.rebuildWither = rebuildWither;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!shouldRebuild(event.getEntityType())) {
            return;
        }
        service.rebuild(event.blockList());
        event.blockList().clear();
    }

    private boolean shouldRebuild(EntityType type) {
        return switch (type) {
            case CREEPER -> true;
            case WITHER, WITHER_SKULL -> rebuildWither;
            default -> false;
        };
    }
}
