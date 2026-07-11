package dev.braggestbacon97.windynamics.world.block;

import com.mojang.serialization.MapCodec;
import dev.braggestbacon97.windynamics.registry.ModBlockEntities;
import dev.braggestbacon97.windynamics.world.block.entity.WindSailHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WindSailHubBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final MapCodec<WindSailHubBlock> CODEC = simpleCodec(WindSailHubBlock::new);

    public WindSailHubBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<WindSailHubBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindSailHubBlockEntity(pos, state);
    }

    private static <T extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<T> createTickerHelper(
            BlockEntityType<T> type,
            BlockEntityType<A> expectedType,
            BlockEntityTicker<A> ticker) {
        return type == expectedType ? (BlockEntityTicker<T>) ticker : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WIND_SAIL_HUB.get(), WindSailHubBlockEntity::tick);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof WindSailHubBlockEntity hub) {
            hub.incrementSails(player.isShiftKeyDown() ? -1 : 1);
        }
        return InteractionResult.CONSUME;
    }
}
