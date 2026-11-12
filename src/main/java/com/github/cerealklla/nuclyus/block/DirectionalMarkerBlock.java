package com.github.cerealklla.nuclyus.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A generic, reusable "which way does this point" marker -- a thin (snow-layer-depth), mostly
 * transparent, fully non-collidable decal showing a visible arrow in whatever direction its own
 * {@code FACING} points. Built to replace ad-hoc markers across the suite that borrowed a real
 * vanilla block (a door, in TinyWyrlds' original Connector Point) purely for its {@code FACING}
 * property, with no actual visual cue which way it pointed -- see TinyWyrlds'
 * {@code wyrldprint.connector.ConnectorArrowBlock} (now a thin subclass of this) for the real
 * incident that prompted replacing it, and this class for the generalization: "I would like to be
 * able to put it on walls too."
 *
 * <p>Placeable on the floor, a wall, or the ceiling -- same {@link AttachFace}+{@code FACING}
 * combination vanilla's own {@code LeverBlock}/{@code ButtonBlock} already use (this class extends
 * the same {@link FaceAttachedHorizontalDirectionalBlock} they do), so the placement/rotation UX is
 * identical and already proven elsewhere in this suite ({@code TaggedLeverBlock}). One deliberate
 * difference from vanilla's own default placement logic: {@link #getStateForPlacement} always
 * points the arrow back at whoever placed it (explicit spec, confirmed correct for the floor case
 * before this generalization: "pointing toward the player's face when placed") for every attach
 * face, not just walls -- vanilla's own default only does that for walls, and sets a floor/ceiling
 * placement's facing to match the placer's own look direction instead, which isn't what's wanted
 * here. A plain right-click ({@link #useWithoutItem}) cycles {@code FACING} clockwise regardless of
 * which face it's mounted on.
 *
 * <p>{@link #canSurvive} is overridden to never require a supporting block -- unlike a real lever,
 * this is meant to be placeable freely (e.g. in a void crafting area), matching the original
 * TinyWyrlds block's own unrestricted placement.
 *
 * <p>This class registers no block of its own -- it's a reusable base a consuming mod subclasses
 * and registers under its own id(s)/texture(s)/variant data (e.g. TinyWyrlds' own
 * {@code ConnectorPointSize}-keyed small/medium/large registrations), adding whatever domain
 * behavior it needs (glass fill, a particle effect, extra codec state) via the usual
 * {@code setPlacedBy}/{@code affectNeighborsAfterRemoval}/{@code animateTick} overrides, calling
 * {@code super(...)} first.
 */
public class DirectionalMarkerBlock extends FaceAttachedHorizontalDirectionalBlock {

    private static final double THICKNESS = 2.0;

    private final MapCodec<DirectionalMarkerBlock> codec;

    public DirectionalMarkerBlock(Properties properties) {
        super(properties);
        this.codec = simpleCodec(DirectionalMarkerBlock::new);
        registerDefaultState(defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState state;
            if (direction.getAxis() == Direction.Axis.Y) {
                state = defaultBlockState()
                        .setValue(FACE, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
                        .setValue(FACING, context.getHorizontalDirection().getOpposite());
            } else {
                state = defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, direction.getOpposite());
            }
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
        }
        return InteractionResult.SUCCESS;
    }

    /** Snow-layer-thin in every mode: a flat slab resting on the ground for FLOOR, hanging from
     *  the ceiling for CEILING, or flush against whichever wall face for WALL -- never a full
     *  cube, same intent as the original's {@code noOcclusion()}.
     *
     *  <p>Real bug caught before ever being live-tested, same mistake class as the original
     *  TinyWyrlds arrow's first (screenshot-rejected) attempt: the WALL case here used to hug the
     *  wrong side for every direction. {@link #getStateForPlacement} sets {@code FACING =
     *  direction.getOpposite()} where {@code direction} is where the player was looking (i.e.
     *  toward the clicked wall), so the real backing wall sits on the {@code FACING.getOpposite()}
     *  side, not the {@code FACING} side -- confirmed against vanilla's own {@code LeverBlock}
     *  (its base shape, {@code Block.boxZ(6,8,10,16)}, hugs z=16/south for its default
     *  {@code FACING=NORTH} state, i.e. the side *opposite* facing, exactly this rule). Each case
     *  below now hugs the side opposite its own {@code FACING}, protruding toward {@code FACING}
     *  itself, matching that. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> thinBox(0, 0, 0, 16, THICKNESS, 16);
            case CEILING -> thinBox(0, 16 - THICKNESS, 0, 16, 16, 16);
            case WALL -> switch (state.getValue(FACING)) {
                case NORTH -> thinBox(0, 0, 16 - THICKNESS, 16, 16, 16);
                case SOUTH -> thinBox(0, 0, 0, 16, 16, THICKNESS);
                case EAST -> thinBox(0, 0, 0, THICKNESS, 16, 16);
                case WEST -> thinBox(16 - THICKNESS, 0, 0, 16, 16, 16);
                default -> thinBox(0, 0, 0, 16, 16, THICKNESS);
            };
        };
    }

    private static VoxelShape thinBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    /** Explicit spec: "the arrow itself should have no collision," regardless of which surface
     *  it's mounted on -- a player walks straight through/over it. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
