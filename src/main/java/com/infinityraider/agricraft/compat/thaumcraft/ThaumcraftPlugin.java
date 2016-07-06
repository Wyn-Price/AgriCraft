package com.infinityraider.agricraft.compat.thaumcraft;

import com.agricraft.agricore.core.AgriCore;
import com.infinityraider.agricraft.api.v1.AgriPlugin;
import com.infinityraider.agricraft.api.v1.IAgriPlugin;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import com.infinityraider.agricraft.init.AgriBlocks;
import com.infinityraider.agricraft.reference.Constants;
import net.minecraft.item.ItemStack;

@AgriPlugin
public class ThaumcraftPlugin implements IAgriPlugin {

	@Override
	public boolean isEnabled() {
		return true;
	}
	
    @Override
    public void init() {
		
		AgriCore.getLogger("AgriCraft-Plugins").info("Plugins are working! Reported from plugin {0}!", this.getClass().getCanonicalName());
		
		// Fix Golems
        FMLInterModComms.sendMessage(
				"Thaumcraft",
				"harvestClickableCrop",
				new ItemStack(AgriBlocks.CROP, 1, Constants.MATURE)
		);

    }

}
