// Adapted from Vane (MIT) - https://github.com/oddlama/vane
// Copyright (c) 2020 oddlama

package io.blume.qol.doubledoors;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.jetbrains.annotations.Nullable;

public final class SingleDoor {

    private final Block lowerBlock;
    private Door lower;
    private Door upper;

    private SingleDoor(Block lowerBlock) {
        this.lowerBlock = lowerBlock;
        this.lower = asDoorState(lowerBlock);
        this.upper = asDoorState(lowerBlock.getRelative(BlockFace.UP));
    }

    @Nullable
    public static SingleDoor createFromBlock(Block originBlock) {
        if (!validateSingleDoor(originBlock)) {
            return null;
        }
        return new SingleDoor(getLower(originBlock));
    }

    private static Block getLower(Block originBlock) {
        Bisected.Half half = asDoorState(originBlock).getHalf();
        return half == Bisected.Half.TOP
            ? originBlock.getRelative(BlockFace.DOWN)
            : originBlock;
    }

    private static boolean validateSingleDoor(Block originBlock) {
        if (!isDoor(originBlock)) {
            return false;
        }
        Block otherHalf = otherVerticalHalf(originBlock);
        if (!isDoor(otherHalf)) {
            return false;
        }
        return doorVerticalHalvesMatch(originBlock, otherHalf);
    }

    private static boolean isDoor(Block block) {
        return block.getBlockData() instanceof Door;
    }

    private static Block otherVerticalHalf(Block block) {
        Door door = asDoorState(block);
        return door.getHalf() == Bisected.Half.TOP
            ? block.getRelative(BlockFace.DOWN)
            : block.getRelative(BlockFace.UP);
    }

    private static boolean doorVerticalHalvesMatch(Block originBlock, Block otherBlock) {
        Bisected.Half expected = opposite(asDoorState(originBlock).getHalf());
        return asDoorState(otherBlock).getHalf() == expected;
    }

    private static Bisected.Half opposite(Bisected.Half half) {
        return half == Bisected.Half.TOP ? Bisected.Half.BOTTOM : Bisected.Half.TOP;
    }

    @Nullable
    private static Door asDoorState(Block block) {
        return asDoorState(block.getBlockData());
    }

    @Nullable
    private static Door asDoorState(BlockData blockData) {
        return blockData instanceof Door door ? door : null;
    }

    public boolean updateCachedState() {
        lower = asDoorState(lowerBlock);
        upper = asDoorState(otherVerticalHalf(lowerBlock));
        return lower != null && upper != null;
    }

    public void setOpen(boolean open) {
        Door data = asDoorState(lowerBlock);
        if (data == null) {
            return;
        }
        data.setOpen(open);
        lowerBlock.setBlockData(data);
    }

    @Nullable
    public SingleDoor getSecondDoor() {
        SingleDoor normalDoor = findOtherDoor(false);
        SingleDoor hackedDoor = findOtherDoor(true);
        return prioritize(normalDoor, hackedDoor);
    }

    @Nullable
    private SingleDoor prioritize(SingleDoor normalDoor, SingleDoor hackedDoor) {
        if (normalDoor == null) {
            return hackedDoor;
        }
        if (hackedDoor == null) {
            return normalDoor;
        }
        return lower.isOpen() ? hackedDoor : normalDoor;
    }

    @Nullable
    private SingleDoor findOtherDoor(boolean hacked) {
        BlockFace otherDoorDirection = otherDoorDirection(lower, hacked);
        Block potentialOtherDoor = lowerBlock.getRelative(otherDoorDirection);
        Door potentialState = asDoorState(potentialOtherDoor);
        if (potentialState == null) {
            return null;
        }
        if (lowerBlock.getType() != potentialOtherDoor.getType()) {
            return null;
        }
        if (potentialState.getHalf() != lower.getHalf()) {
            return null;
        }
        if (potentialState.isOpen() != lower.isOpen()) {
            return null;
        }

        BlockFace otherPointing = otherDoorDirection(potentialState, hacked);
        Block shouldBeUs = potentialOtherDoor.getRelative(otherPointing);
        boolean isUs = shouldBeUs.getX() == lowerBlock.getX()
            && shouldBeUs.getY() == lowerBlock.getY()
            && shouldBeUs.getZ() == lowerBlock.getZ();
        return isUs ? createFromBlock(potentialOtherDoor) : null;
    }

    private BlockFace otherDoorDirection(Door ourDoor, boolean hacked) {
        if (hacked) {
            return ourDoor.getFacing();
        }
        return ourDoor.getHinge() == Door.Hinge.LEFT
            ? rotateCw(ourDoor.getFacing())
            : rotateCcw(ourDoor.getFacing());
    }

    private static BlockFace rotateCw(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> throw new IllegalArgumentException("Not a horizontal face: " + face);
        };
    }

    private static BlockFace rotateCcw(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case EAST -> BlockFace.NORTH;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> throw new IllegalArgumentException("Not a horizontal face: " + face);
        };
    }

    public boolean isOpen() {
        return lower.isOpen();
    }
}
