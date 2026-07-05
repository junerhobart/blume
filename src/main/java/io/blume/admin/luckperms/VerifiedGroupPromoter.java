package io.blume.admin.luckperms;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface VerifiedGroupPromoter {

    boolean isActive();

    void promoteToVerified(@NotNull UUID uuid, @NotNull String group);
}
