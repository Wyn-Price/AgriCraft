package com.infinityraider.agricraft.tiles;

import com.infinityraider.agricraft.api.v1.crop.IAdditionalCropData;
import com.infinityraider.agricraft.farming.PlantStats;
import com.infinityraider.agricraft.blocks.BlockCrop;
import com.infinityraider.agricraft.farming.mutation.CrossOverResult;
import com.infinityraider.agricraft.farming.mutation.MutationEngine;
import com.infinityraider.agricraft.config.AgriCraftConfig;
import com.infinityraider.agricraft.reference.Constants;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import com.agricraft.agricore.core.AgriCore;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import com.infinityraider.agricraft.api.v1.plant.IAgriPlant;
import com.infinityraider.agricraft.api.v1.stat.IAgriStat;
import com.infinityraider.agricraft.api.v1.crop.IAgriCrop;
import com.infinityraider.agricraft.api.v1.fertilizer.IAgriFertilizer;
import com.infinityraider.agricraft.apiimpl.v1.PlantRegistry;
import com.infinityraider.agricraft.init.AgriBlocks;
import com.agricraft.agricore.util.MathHelper;
import com.infinityraider.agricraft.utility.WorldHelper;
import com.infinityraider.agricraft.api.v1.misc.IAgriDebuggable;
import com.infinityraider.agricraft.apiimpl.v1.StatRegistry;
import com.infinityraider.agricraft.reference.AgriNBT;
import com.infinityraider.agricraft.reference.AgriProperties;

public class TileEntityCrop extends TileEntityBase implements IAgriCrop, IAgriDebuggable {

	public static final String NAME = "crops";

	private boolean crossCrop = false;
	private boolean weed = false;

	private @Nonnull
	IAgriStat stats = new PlantStats();
	private IAgriPlant plant;
	private IAdditionalCropData data;

	private final MutationEngine mutationEngine;

	public TileEntityCrop() {
		this.mutationEngine = new MutationEngine(this);
	}

	@Override
	public boolean isRotatable() {
		return false;
	}

	// =========================================================================
	// IPlantProvider Methods
	// =========================================================================
	@Override
	public boolean hasPlant() {
		return this.plant != null;
	}

	@Override
	public IAgriPlant getPlant() {
		return this.plant;
	}

	// =========================================================================
	// IStatProvider Methods
	// =========================================================================
	@Override
	public boolean hasStat() {
		return true;//this.stats != null;
	}

	@Nonnull
	@Override
	public IAgriStat getStat() {
		return this.stats;
	}

	// =========================================================================
	// Misc.
	// =========================================================================
	@Override
	public boolean isCrossCrop() {
		return crossCrop;
	}

	@Override
	public void setCrossCrop(boolean status) {
		if (status != this.crossCrop) {
			this.crossCrop = status;
			if (!worldObj.isRemote && crossCrop) {
				SoundType type = Blocks.PLANKS.getSoundType();
				worldObj.playSound(null, (double) ((float) xCoord() + 0.5F), (double) ((float) yCoord() + 0.5F), (double) ((float) zCoord() + 0.5F), type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
			}
			this.markDirty();
		}
	}

	/*
	 * get growthrate
	 */
	public int getGrowthRate() {
		// TODO: update!
		return AgriCraftConfig.weedGrowthRate;
	}

	// =========================================================================
	// Growthstage
	// =========================================================================
	@Override
	public int getGrowthStage() {
		return this.getBlockMetadata();
	}

	@Override
	public void setGrowthStage(int stage) {
		if (this.hasPlant() || this.hasWeed()) {
			stage = MathHelper.inRange(stage, 0, Constants.MATURE);
			IBlockState state = worldObj.getBlockState(pos);
			state = state.withProperty(AgriProperties.GROWTHSTAGE, stage);
			this.worldObj.setBlockState(pos, state, 3);
			this.stats = this.stats.withMeta(stage);
			this.markForUpdate();
		}
	}

	// =========================================================================
	// IPlantAcceptor Methods
	// =========================================================================
	@Override
	public boolean acceptsPlant(IAgriPlant plant) {
		return plant != null && !this.hasPlant() && !this.hasWeed() && !this.isCrossCrop();
	}

	@Override
	public boolean setPlant(IAgriPlant plant) {
		if ((!this.crossCrop) && (!this.hasPlant()) && (plant != null)) {
			this.plant = plant;
			plant.onSeedPlanted(worldObj, pos);
			IAdditionalCropData new_data = plant.getInitialCropData(worldObj, getPos(), this);
			if (new_data != null) {
				this.data = new_data;
			}
			this.markForUpdate();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public IAgriPlant removePlant() {
		IAgriPlant oldPlant = getPlant();
		this.setGrowthStage(0);
		this.plant = null;
		if (oldPlant != null) {
			oldPlant.onPlantRemoved(worldObj, pos);
		}
		this.data = null;
		return oldPlant;
	}

	// =========================================================================
	// IStatAcceptor Methods
	// =========================================================================
	@Override
	public boolean acceptsStat(IAgriStat stat) {
		return true;
	}

	@Override
	public boolean setStat(IAgriStat stat) {
		if (stat != null) {
			this.stats = stat;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public IAgriStat removeStat() {
		IAgriStat old = this.stats;
		this.stats = new PlantStats();
		return old;
	}

	/*
	 * check if the crop is fertile
	 */
	@Override
	public boolean isFertile() {
		return this.weed || worldObj.isAirBlock(this.getPos().add(0, 1, 0)) && plant.getGrowthRequirement().canGrow(this.worldObj, pos);
	}

	/*
	 * gets the height of the crop
	 */
	@SideOnly(Side.CLIENT)
	public float getCropHeight() {
		return hasPlant() ? plant.getHeight(getBlockMetadata()) : Constants.UNIT * 13;
	}

	/**
	 * check the block if the plant is mature
	 */
	@Override
	public boolean isMature() {
		return getGrowthStage() >= Constants.MATURE;
	}

	// =========================================================================
	// IWeedable Methods
	// =========================================================================
	@Override
	public double getWeedSpawnChance() {
		if (this.hasPlant()) {
			return AgriCraftConfig.weedsWipePlants ? ((double) (10 - stats.getStrength())) / 10 : 0;
		} else {
			return this.weed ? 0 : 1;
		}
	}

	@Override
	public boolean hasWeed() {
		return weed;
	}

	@Override
	public boolean spawnWeed(Random rand) {
		if (!weed && rand.nextDouble() < getWeedSpawnChance()) {
			this.crossCrop = false;
			this.weed = true;
			this.removePlant();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean spreadWeed(Random rand) {
		List<IAgriCrop> neighbours = this.getNeighbours();
		for (IAgriCrop crop : neighbours) {
			if (crop != null && crop.spawnWeed(rand)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean clearWeed() {
		if (this.hasWeed()) {
			this.setGrowthStage(0);
			this.weed = false;
			return true;
		} else {
			return false;
		}
	}

	// =========================================================================
	// IFertilizable Methods
	// =========================================================================
	@Override
	public boolean acceptsFertilizer(IAgriFertilizer fertilizer) {
		if (this.crossCrop) {
			return AgriCraftConfig.bonemealMutation && fertilizer.canTriggerMutation();
		}
		if (this.hasWeed()) {
			return true;
		}
		if (this.hasPlant()) {
			return fertilizer.isFertilizerAllowed(plant.getTier());
		}
		return false;
	}

	//when fertilizer is applied
	@Override
	public boolean onApplyFertilizer(IAgriFertilizer fertilizer, Random rand) {
		if (this.getGrowthStage() < Constants.MATURE && (this.hasPlant() || this.hasWeed())) {
			((BlockCrop) AgriBlocks.CROP).grow(getWorld(), rand, getPos(), getWorld().getBlockState(getPos()));
			return true;
		} else if (fertilizer.canTriggerMutation() && this.isCrossCrop() && AgriCraftConfig.bonemealMutation) {
			this.crossOver();
			return true;
		}
		return false;
	}

	// =========================================================================
	// IHarvestable methods.
	// =========================================================================
	@Override
	public boolean canHarvest() {
		return hasPlant() && isMature();
	}

	@Override
	public boolean harvest(@Nullable EntityPlayer player) {
		// TODO: Correct horrible hack.
		// This is a terrible, aweful method of doing this.
		return ((BlockCrop) getWorld().getBlockState(pos).getBlock()).harvest(getWorld(), getPos(), getWorld().getBlockState(getPos()), player, this);
	}

	@Override
	public List<ItemStack> getFruits() {
		return plant.getFruitsOnHarvest(stats.getGain(), worldObj.rand);
	}

	// =========================================================================
	// Other
	// =========================================================================
	@Override
	public TileEntity getTileEntity() {
		return this;
	}

	@Override
	public IAdditionalCropData getAdditionalCropData() {
		return this.data;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.hasPlant()) {
			plant.onValidate(worldObj, pos, this);
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (this.hasPlant()) {
			plant.onInvalidate(worldObj, pos, this);
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (this.hasPlant()) {
			plant.onChunkUnload(worldObj, pos, this);
		}
	}

	// =========================================================================
	// NBT
	// =========================================================================
	//this saves the data on the tile entity
	@Override
	public void writeTileNBT(NBTTagCompound tag) {
		this.stats.writeToNBT(tag);
		tag.setBoolean(AgriNBT.CROSS_CROP, crossCrop);
		tag.setBoolean(AgriNBT.WEED, weed);
		if (plant != null) {
			tag.setString(AgriNBT.SEED, plant.getId());
		}
		if (getAdditionalCropData() != null) {
			tag.setTag(AgriNBT.INVENTORY, getAdditionalCropData().writeToNBT());
		}
		//AgriCore.getLogger("Plant-Tag").debug("Write Tag: {0}", tag);
	}

	//this loads the saved data for the tile entity
	@Override
	public void readTileNBT(NBTTagCompound tag) {
		this.stats = StatRegistry.getInstance().getValue(tag);
		this.crossCrop = tag.getBoolean(AgriNBT.CROSS_CROP);
		this.weed = tag.getBoolean(AgriNBT.WEED);
		this.plant = PlantRegistry.getInstance().getPlant(tag.getString(AgriNBT.SEED));
		if (tag.hasKey(AgriNBT.INVENTORY) && this.plant != null) {
			this.data = plant.readCropDataFromNBT(tag.getCompoundTag(AgriNBT.INVENTORY));
		}
		//AgriCore.getLogger("Plant-Tag").debug("Read Tag: {0}", tag);
	}

	/**
	 * Apply a GROWTH increment
	 */
	public void applyGrowthTick() {
		int meta = getGrowthStage();
		if (hasPlant()) {
			plant.onAllowedGrowthTick(worldObj, pos, meta);
		}
		IBlockState state = getWorld().getBlockState(getPos());
		if (hasWeed() || !plant.isMature(getWorld(), pos, state)) {
			setGrowthStage(meta + 1);
			/* TODO: Annouce Growth Tick Via API! */
		}
	}

	/**
	 * the code that makes the crop cross with neighboring crops
	 */
	public void crossOver() {
		mutationEngine.executeCrossOver();
	}

	/*
	 * Called by the mutation engine to apply the result of a cross over
	 */
	public void applyCrossOverResult(CrossOverResult result) {
		crossCrop = false;
		this.setSeed(result.toStack());
	}

	/**
	 * @return a list with all neighbours of type <code>TileEntityCrop</code> in
	 * the NORTH, SOUTH, EAST and WEST DIRECTION
	 */
	@Override
	public List<IAgriCrop> getNeighbours() {
		return WorldHelper.getTileNeighbors(worldObj, pos, IAgriCrop.class);
	}

	/**
	 * @return a list with only mature neighbours of type
	 * <code>TileEntityCrop</code>
	 */
	@Override
	public List<IAgriCrop> getMatureNeighbours() {
		List<IAgriCrop> neighbours = getNeighbours();
		neighbours.removeIf((p) -> {
			return !(p.hasPlant() && p.isMature());
		});
		return neighbours;
	}

	@Override
	public void addDebugInfo(List<String> list) {
		list.add("CROP:");
		if (this.crossCrop) {
			list.add(" - This is a crosscrop");
		} else if (this.hasPlant()) {
			list.add(" - This crop has a plant");
			list.add(" - Plant: " + this.plant.getPlantName());
			list.add(" - Id: " + this.plant.getId());
			list.add(" - Texture: " + this.plant.getPrimaryPlantTexture(this.getGrowthStage()).toString());
			list.add(" - Tier: " + plant.getTier());
			list.add(" - Stage: " + this.getGrowthStage());
			list.add(" - Meta: " + this.getBlockMetadata());
			list.add(" - Growth: " + this.stats.getGrowth());
			list.add(" - Gain: " + this.stats.getGain());
			list.add(" - Strength: " + this.stats.getStrength());
			list.add(" - Fertile: " + this.isFertile());
			list.add(" - Mature: " + this.isMature());
		} else if (this.weed) {
			list.add(" - This crop has weeds");
			list.add(" - Meta: " + this.getBlockMetadata());
		} else {
			list.add(" - This crop has no plant");
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("unchecked")
	public void addDisplayInfo(List information) {
		if (this.hasPlant()) {
			//Add the SEED name.
			information.add(AgriCore.getTranslator().translate("agricraft_tooltip.seed") + ": " + this.plant.getSeedName());
			//Add the GROWTH.
			if (this.isMature()) {
				information.add(AgriCore.getTranslator().translate("agricraft_tooltip.growthStage") + ": " + AgriCore.getTranslator().translate("agricraft_tooltip.mature"));
			} else {
				information.add(AgriCore.getTranslator().translate("agricraft_tooltip.growthStage") + ": " + ((int) (100.0 * this.getBlockMetadata() / Constants.MATURE) + "%"));
			}
			//Add the ANALYZED data.
			if (this.stats.isAnalyzed()) {
				this.stats.addStats(information);
			} else {
				information.add(AgriCore.getTranslator().translate("agricraft_tooltip.analyzed"));
			}
			//Add the fertility information.
			information.add(AgriCore.getTranslator().translate(this.isFertile() ? "agricraft_tooltip.fertile" : "agricraft_tooltip.notFertile"));
		} else if (this.hasWeed()) {
			information.add(AgriCore.getTranslator().translate("agricraft_tooltip.weeds"));
		} else {
			information.add(AgriCore.getTranslator().translate("agricraft_tooltip.empty"));
		}
	}
}
