package io.blume.ecology.husbandry;

import io.blume.ecology.EcologyConfig;
import io.blume.ecology.EcologyKeys;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Applies husbandry yield bonuses and penalties based on pasture welfare.
 *
 * <p>Hooks into shear, egg laying, breeding, player kills, and sheep wool regrowth.
 * Milk is intentionally left unchanged. Standing on a bad floor forces the action to
 * use the {@code poor} tier for that action only.</p>
 */
public final class WelfareYieldListener implements Listener {

    private static final int VANILLA_BREED_COOLDOWN = 6000;
    private static final int VANILLA_BABY_AGE = -24000;
    private static final long VANILLA_REGROWTH_MS = 30000L;

    private final PastureScoreService pastureService;
    private final EcologyConfig config;
    private final EcologyKeys keys;
    private final Random random = new Random();

    public WelfareYieldListener(
        @NotNull PastureScoreService pastureService,
        @NotNull EcologyConfig config,
        @NotNull EcologyKeys keys
    ) {
        this.pastureService = pastureService;
        this.config = config;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShear(@NotNull PlayerShearEntityEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        PastureScoreService.PastureScore score = pastureService.score(sheep);
        PastureScoreService.Tier tier = effectiveTier(sheep, score);
        modifyShearDrops(event.getDrops(), tier, score);
        sheep.getPersistentDataContainer().set(keys.shearedAt(), PersistentDataType.LONG, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEggLay(@NotNull EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof Chicken)) {
            return;
        }
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() != Material.EGG) {
            return;
        }
        Chicken chicken = (Chicken) event.getEntity();
        PastureScoreService.PastureScore score = pastureService.score(chicken);
        PastureScoreService.Tier tier = effectiveTier(chicken, score);
        double sunlightMultiplier = sunlightYieldMultiplier(score, tier);
        double cancelChance = bonusDouble(tier, "egg-lay-cancel-chance", 0.0);
        if (random.nextDouble() < cancelChance) {
            event.setCancelled(true);
            return;
        }
        boolean guaranteed = bonusBool(tier, "egg-bonus-guaranteed", false);
        double bonusChance = bonusDouble(tier, "egg-bonus-chance", 0.0);
        if (guaranteed || random.nextDouble() < bonusChance) {
            item.setAmount(item.getAmount() + 1);
        }
        if (sunlightMultiplier > 1.0 && random.nextDouble() < config.husbandrySunlightYieldBonus()) {
            item.setAmount(item.getAmount() + 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreed(@NotNull EntityBreedEvent event) {
        LivingEntity father = event.getFather();
        LivingEntity mother = event.getMother();
        if (!PastureScoreService.isHusbandryAnimal(father.getType())
            || !PastureScoreService.isHusbandryAnimal(mother.getType())) {
            return;
        }
        LivingEntity baby = event.getEntity();
        PastureScoreService.Tier tier = effectiveTier(mother, pastureService.score(mother));
        applyBabyGrowth(baby, tier);
        maybeSpawnTwin(baby, tier);
        applyLoveCooldown(father, mother, tier);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(@NotNull EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }
        EntityType type = event.getEntityType();
        if (type != EntityType.COW && type != EntityType.PIG) {
            return;
        }
        PastureScoreService.PastureScore score = pastureService.score(event.getEntity());
        PastureScoreService.Tier tier = effectiveTier(event.getEntity(), score);
        applyKillDrops(event, type, tier, score);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSheepEatGrass(@NotNull EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        if (event.getBlock().getType() != Material.GRASS_BLOCK || event.getTo() != Material.DIRT) {
            return;
        }
        if (!sheep.isSheared()) {
            return;
        }
        PastureScoreService.Tier tier = effectiveTier(sheep, pastureService.score(sheep));
        long delay = (long) (VANILLA_REGROWTH_MS * bonusDouble(tier, "wool-regrow-multiplier", 1.0));
        long shearedAt = sheep.getPersistentDataContainer().getOrDefault(
            keys.shearedAt(), PersistentDataType.LONG, 0L);
        if (System.currentTimeMillis() - shearedAt < delay) {
            event.setCancelled(true);
        }
    }

    private @NotNull PastureScoreService.Tier effectiveTier(
        @NotNull LivingEntity entity,
        @NotNull PastureScoreService.PastureScore score
    ) {
        if (config.isHusbandryStandingBadForcesPoor() && pastureService.isBadStandingBlock(entity.getLocation())) {
            return PastureScoreService.Tier.POOR;
        }
        PastureScoreService.Tier tier = score.tier();
        if (config.isHusbandryStandingNoSkyPenalty() && !pastureService.hasSkyAccess(entity.getLocation())) {
            tier = PastureScoreService.Tier.down(tier);
        }
        return tier;
    }

    private double sunlightYieldMultiplier(
        @NotNull PastureScoreService.PastureScore score,
        @NotNull PastureScoreService.Tier tier
    ) {
        if (!score.wellSunlit()) {
            return 1.0;
        }
        if (tier != PastureScoreService.Tier.EXCELLENT && tier != PastureScoreService.Tier.GOOD) {
            return 1.0;
        }
        return 1.0 + config.husbandrySunlightYieldBonus();
    }

    private void modifyShearDrops(
        @NotNull List<ItemStack> drops,
        @NotNull PastureScoreService.Tier tier,
        @NotNull PastureScoreService.PastureScore score
    ) {
        double multiplier = bonusDouble(tier, "wool-drop-multiplier", 1.0) * sunlightYieldMultiplier(score, tier);
        int bonusMin = bonusInt(tier, "wool-bonus-min", 0);
        double bonusChance = bonusDouble(tier, "wool-bonus-chance", 0.0);
        int penaltyMin = bonusInt(tier, "wool-penalty-min", 0);
        for (ItemStack drop : drops) {
            if (!Tag.WOOL.isTagged(drop.getType())) {
                continue;
            }
            int amount = drop.getAmount();
            int adjusted = Math.max(1, (int) Math.round(amount * multiplier) + bonusMin - penaltyMin);
            if (random.nextDouble() < bonusChance) {
                adjusted++;
            }
            drop.setAmount(adjusted);
        }
    }

    private void applyBabyGrowth(@NotNull LivingEntity baby, @NotNull PastureScoreService.Tier tier) {
        if (!(baby instanceof Ageable ageable)) {
            return;
        }
        double multiplier = bonusDouble(tier, "baby-growth-multiplier", 1.0);
        if (multiplier == 1.0 || ageable.getAge() >= 0) {
            return;
        }
        ageable.setAge((int) (VANILLA_BABY_AGE / multiplier));
    }

    private void maybeSpawnTwin(@NotNull LivingEntity baby, @NotNull PastureScoreService.Tier tier) {
        double chance = bonusDouble(tier, "breed-twin-chance", 0.0);
        if (chance <= 0.0 || random.nextDouble() >= chance) {
            return;
        }
        World world = baby.getWorld();
        Entity twin = world.spawnEntity(baby.getLocation(), baby.getType());
        if (twin instanceof Ageable ageable) {
            ageable.setBaby();
            if (baby instanceof Ageable source) {
                ageable.setAge(source.getAge());
            }
        }
    }

    private void applyLoveCooldown(
        @NotNull LivingEntity father,
        @NotNull LivingEntity mother,
        @NotNull PastureScoreService.Tier tier
    ) {
        double multiplier = bonusDouble(tier, "love-mode-multiplier", 1.0);
        if (multiplier == 1.0) {
            return;
        }
        int cooldown = (int) (VANILLA_BREED_COOLDOWN * multiplier);
        setBreedCooldown(father, cooldown);
        setBreedCooldown(mother, cooldown);
    }

    // For adult animals a positive age is the remaining breed cooldown in
    // ticks (vanilla sets 6000 after breeding), so setAge is the real API.
    private void setBreedCooldown(@NotNull LivingEntity entity, int cooldown) {
        if (entity instanceof Ageable ageable && ageable.isAdult()) {
            ageable.setAge(cooldown);
        }
    }

    private void applyKillDrops(
        @NotNull EntityDeathEvent event,
        @NotNull EntityType type,
        @NotNull PastureScoreService.Tier tier,
        @NotNull PastureScoreService.PastureScore score
    ) {
        Material meat = meatType(type, event.getEntity().getFireTicks() > 0);
        if (meat == null) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        double bonusRoll = bonusDouble(tier, "kill-bonus-roll", 0.0) * sunlightYieldMultiplier(score, tier);
        double bonusChance = bonusDouble(tier, "kill-bonus-chance", 0.0);
        if (sunlightYieldMultiplier(score, tier) > 1.0) {
            bonusChance = Math.min(1.0, bonusChance + config.husbandrySunlightYieldBonus());
        }
        int penaltyRoll = bonusInt(tier, "kill-penalty-roll", 0);
        if (bonusRoll > 0.0) {
            int rolls = (int) bonusRoll;
            for (int i = 0; i < rolls; i++) {
                drops.add(new ItemStack(meat, 1));
            }
        } else if (bonusChance > 0.0 && random.nextDouble() < bonusChance) {
            drops.add(new ItemStack(meat, 1));
        } else if (penaltyRoll > 0) {
            removeOneMatching(drops, meat);
        }
    }

    private @Nullable Material meatType(@NotNull EntityType type, boolean cooked) {
        return switch (type) {
            case COW, MOOSHROOM -> cooked ? Material.COOKED_BEEF : Material.BEEF;
            case PIG -> cooked ? Material.COOKED_PORKCHOP : Material.PORKCHOP;
            default -> null;
        };
    }

    private void removeOneMatching(@NotNull List<ItemStack> drops, @NotNull Material meat) {
        for (ItemStack drop : drops) {
            if (drop.getType() == meat && drop.getAmount() > 1) {
                drop.setAmount(drop.getAmount() - 1);
                return;
            }
        }
        for (ItemStack drop : drops) {
            if (drop.getAmount() > 1) {
                drop.setAmount(drop.getAmount() - 1);
                return;
            }
        }
    }

    private double bonusDouble(@NotNull PastureScoreService.Tier tier, @NotNull String key, double def) {
        Map<String, Object> map = config.husbandryBonuses().get(tier.name().toLowerCase());
        if (map != null) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        }
        return defaultBonusDouble(tier, key, def);
    }

    private boolean bonusBool(@NotNull PastureScoreService.Tier tier, @NotNull String key, boolean def) {
        Map<String, Object> map = config.husbandryBonuses().get(tier.name().toLowerCase());
        if (map != null) {
            Object value = map.get(key);
            if (value instanceof Boolean flag) {
                return flag;
            }
        }
        return defaultBonusBool(tier, key, def);
    }

    private int bonusInt(@NotNull PastureScoreService.Tier tier, @NotNull String key, int def) {
        Map<String, Object> map = config.husbandryBonuses().get(tier.name().toLowerCase());
        if (map != null) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
        }
        return defaultBonusInt(tier, key, def);
    }

    private double defaultBonusDouble(@NotNull PastureScoreService.Tier tier, @NotNull String key, double def) {
        return switch (tier) {
            case EXCELLENT -> switch (key) {
                case "wool-drop-multiplier" -> 2.0;
                case "breed-twin-chance" -> 0.25;
                case "baby-growth-multiplier" -> 2.0;
                case "wool-regrow-multiplier" -> 0.5;
                case "love-mode-multiplier" -> 0.5;
                case "kill-bonus-roll" -> 1.0;
                default -> def;
            };
            case GOOD -> switch (key) {
                case "wool-drop-multiplier" -> 1.5;
                case "egg-bonus-chance" -> 0.50;
                case "breed-twin-chance" -> 0.15;
                case "baby-growth-multiplier" -> 1.5;
                case "kill-bonus-chance" -> 0.50;
                default -> def;
            };
            case FAIR -> switch (key) {
                case "egg-bonus-chance" -> 0.25;
                case "wool-bonus-chance" -> 0.25;
                case "baby-growth-multiplier" -> 1.25;
                default -> def;
            };
            case POOR -> switch (key) {
                case "egg-lay-cancel-chance" -> 0.50;
                case "wool-penalty-min" -> 1;
                case "wool-regrow-multiplier" -> 1.5;
                case "baby-growth-multiplier" -> 0.75;
                case "love-mode-multiplier" -> 1.5;
                case "kill-penalty-roll" -> 1;
                default -> def;
            };
        };
    }

    private boolean defaultBonusBool(@NotNull PastureScoreService.Tier tier, @NotNull String key, boolean def) {
        return switch (tier) {
            case EXCELLENT -> switch (key) {
                case "egg-bonus-guaranteed" -> true;
                default -> def;
            };
            default -> def;
        };
    }

    private int defaultBonusInt(@NotNull PastureScoreService.Tier tier, @NotNull String key, int def) {
        return switch (tier) {
            case POOR -> switch (key) {
                case "wool-penalty-min" -> 1;
                case "kill-penalty-roll" -> 1;
                default -> def;
            };
            default -> def;
        };
    }

}
