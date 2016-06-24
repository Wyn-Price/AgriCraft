package com.infinityraider.agricraft.utility;

import com.infinityraider.agricraft.items.ItemNugget;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class OreDictHelper {

    private static final Map<String, Block> oreBlocks = new HashMap<>();
    private static final Map<String, Integer> oreBlockMeta = new HashMap<>();

    public static Block getOreBlockForName(String name) {
        return oreBlocks.get(name);
    }

    public static int getOreMetaForName(String name) {
        return oreBlockMeta.get(name);
    }

    //checks if an itemstack has this ore dictionary entry
    public static boolean hasOreId(ItemStack stack, String tag) {
        if(stack==null || stack.getItem()==null) {
            return false;
        }
        int[] ids = OreDictionary.getOreIDs(stack);
        for(int id:ids) {
            if(OreDictionary.getOreName(id).equals(tag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOreId(Block block, String tag) {
        return block != null && hasOreId(new ItemStack(block), tag);
    }

    //checks if two blocks have the same ore dictionary entry
    public static boolean isSameOre(Block block1, int meta1, Block block2, int meta2) {
        if(block1==block2 && meta1==meta2) {
            return true;
        }
        if(block1==null || block2==null) {
            return false;
        }
        int[] ids1 = OreDictionary.getOreIDs(new ItemStack(block1, 1, meta1));
        int[] ids2 = OreDictionary.getOreIDs(new ItemStack(block2, 1, meta2));
        for (int id1:ids1) {
            for (int id2:ids2) {
                if (id1==id2) {
                    return true;
                }
            }
        }
        return false;
    }

    //finds the ingot for a nugget ore dictionary entry
    public static ItemStack getIngot(String ingotName) {
        ItemStack ingot = null;
        List<ItemStack> entries = OreDictionary.getOres(ingotName);
        if (entries.size() > 0 && entries.get(0).getItem() != null) {
            ingot = entries.get(0);
        }
        return ingot;
    }

    private static void getOreBlock(ItemNugget.NuggetType type) {
        for (ItemStack itemStack : OreDictionary.getOres(type.ore)) {
            if (itemStack.getItem() instanceof ItemBlock) {
                ItemBlock block = (ItemBlock) itemStack.getItem();
                oreBlocks.put(type.name(), block.block);
                oreBlockMeta.put(type.name(), itemStack.getItemDamage());
                break;
            }
        }
    }

    public static ArrayList<ItemStack> getFruitsFromOreDict(ItemStack seed) {
        return getFruitsFromOreDict(seed, true);
    }

    public static ArrayList<ItemStack> getFruitsFromOreDict(ItemStack seed, boolean sameMod) {
        String seedModId = IOHelper.getModId(seed);
        ArrayList<ItemStack> fruits = new ArrayList<>();

        for(int id:OreDictionary.getOreIDs(seed)) {
            if(OreDictionary.getOreName(id).substring(0,4).equalsIgnoreCase("seed")) {
                String name = OreDictionary.getOreName(id).substring(4);
                List<ItemStack> fromOredict = OreDictionary.getOres("crop"+name);
                for(ItemStack stack:fromOredict) {
                    if(stack==null || stack.getItem()==null) {
                        continue;
                    }
                    String stackModId = IOHelper.getModId(stack);
                    if((!sameMod) || stackModId.equals(seedModId)) {
                        fruits.add(stack);
                    }
                }
            }
        }

        return fruits;
    }
}
