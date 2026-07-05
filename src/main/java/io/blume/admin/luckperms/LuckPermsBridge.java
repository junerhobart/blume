package io.blume.admin.luckperms;

import io.blume.BlumePlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.InheritanceNode;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

final class LuckPermsBridge implements VerifiedGroupPromoter {

    private final BlumePlugin plugin;
    private final UserManager userManager;

    LuckPermsBridge(@NotNull BlumePlugin plugin) {
        this.plugin = plugin;
        LuckPerms luckPerms = LuckPermsProvider.get();
        userManager = luckPerms.getUserManager();
    }

    @Override
    public boolean isActive() {
        return userManager != null;
    }

    @Override
    public void promoteToVerified(@NotNull UUID uuid, @NotNull String group) {
        if (userManager == null || group.isBlank()) {
            return;
        }

        try {
            userManager.modifyUser(uuid, user -> {
                user.data().add(InheritanceNode.builder(group).build());
                user.setPrimaryGroup(group);
            });
        } catch (Exception e) {
            plugin.getLogger().log(
                Level.WARNING,
                "Failed to set LuckPerms primary group '" + group + "' for " + uuid,
                e
            );
        }
    }
}
