package dev.braggestbacon97.windynamics.world.block;

import com.mojang.serialization.MapCodec;
import dev.braggestbacon97.windynamics.world.WindManager;
import dev.braggestbacon97.windynamics.world.WindSample;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WindSensorBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final int UPDATE_RATE_TICKS = 20;
    private static final float MAX_WIND_FOR_FULL_SIGNAL = 3.5f;
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 14.0, 13.0);
    private static final MapCodec<WindSensorBlock> CODEC = simpleCodec(WindSensorBlock::new);

    public WindSensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<WindSensorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }

        WindSample sample = WindManager.sample(serverLevel, pos);
        Direction outputSide = strongestIncomingSide(sample);

        if (side != outputSide) {
            return 0;
        }

        return Mth.clamp(Mth.floor(sample.speed() / MAX_WIND_FOR_FULL_SIGNAL * 15.0f), 0, 15);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, UPDATE_RATE_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, UPDATE_RATE_TICKS);
        level.updateNeighborsAt(pos, this);
    }

    private static Direction strongestIncomingSide(WindSample sample) {
        float x = -sample.direction().x;
        float z = -sample.direction().y;

        Direction best = Direction.NORTH;
        float bestDot = -Float.MAX_VALUE;

        float north = z;
        if (north > bestDot) { bestDot = north; best = Direction.NORTH; }
        float south = -z;
        if (south > bestDot) { bestDot = south; best = Direction.SOUTH; }
        float east = x;
        if (east > bestDot) { bestDot = east; best = Direction.EAST; }
        float west = -x;
        if (west > bestDot) { bestDot = west; best = Direction.WEST; }

        return best;
    }
}
