package sonar.flux.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.core.common.block.SonarBlock;
import sonar.core.common.block.SonarMaterials;
import sonar.core.helpers.SonarHelper;
import sonar.core.network.FlexibleGuiHandler;
import sonar.core.utils.ISpecialTooltip;
import sonar.flux.FluxTranslate;
import sonar.flux.common.item.ItemConfigurator;
import sonar.flux.common.tileentity.TileFlux;

import javax.annotation.Nullable;
import java.util.List;

public abstract class FluxConnection extends SonarBlock implements ITileEntityProvider, ISpecialTooltip {

	public static final PropertyBool CONNECTED = PropertyBool.create("connected");

	public FluxConnection() {
		super(SonarMaterials.machine, false);
		this.hasSpecialRenderer = true;
	}

	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag advanced){
		super.addInformation(stack, world, tooltip, advanced);
	}

	@Override
	public void addSpecialToolTip(ItemStack stack, World world, List<String> list, NBTTagCompound tag) {}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		ItemStack heldItem = hand == null ? ItemStack.EMPTY : player.getHeldItem(hand);
		if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemConfigurator)) {
			if (!world.isRemote) {
				TileEntity target = world.getTileEntity(pos);
				if (target instanceof TileFlux) {
					TileFlux flux = (TileFlux) target;
					if (flux.canAccess(player).canView()) {
						FlexibleGuiHandler.instance().openBasicTile(player, flux, 0);
					} else {
						player.sendMessage(new TextComponentString(SonarHelper.getProfileByUUID(flux.playerUUID.getValue()).getName() + " : " + FluxTranslate.ERROR_NO_PERMISSION.t()));
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase player, ItemStack itemstack) {
		super.onBlockPlacedBy(world, pos, state, player, itemstack);
		TileEntity target = world.getTileEntity(pos);
		if (target instanceof TileFlux) {
			TileFlux flux = (TileFlux) target;
			flux.onBlockPlacedBy(world, pos, state, player, itemstack);
		}
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos){
		super.neighborChanged(state, world, pos, block, fromPos);
		if (!world.isRemote){
			updateRedstonePower(world, pos);
		}
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state){
		super.onBlockAdded(world, pos, state);
		if (!world.isRemote) {
			updateRedstonePower(world, pos);
		}
	}

	private void updateRedstonePower(World world, BlockPos pos){
		TileEntity target = world.getTileEntity(pos);
		if (target instanceof TileFlux) {
			TileFlux flux = (TileFlux) target;
			flux.updateRedstonePower();
		}
	}

	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(CONNECTED, meta == 1);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(CONNECTED) ? 1 : 0;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, CONNECTED);
	}
}
