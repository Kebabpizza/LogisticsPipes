/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.proxy.buildcraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.main.ActionDisableLogistics;
import logisticspipes.main.LogisticsTriggerProvider;
import logisticspipes.main.TriggerSupplierFailed;
import logisticspipes.nei.NEILogisticsPipesConfig;
import logisticspipes.pipes.PipeItemsApiaristAnalyser;
import logisticspipes.pipes.PipeItemsApiaristSink;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsBuilderSupplierLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogisticsMk2;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.PipeItemsLiquidSupplier;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsProviderLogisticsMk2;
import logisticspipes.pipes.PipeItemsRemoteOrdererLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.PipeLogisticsChassiMk1;
import logisticspipes.pipes.PipeLogisticsChassiMk2;
import logisticspipes.pipes.PipeLogisticsChassiMk3;
import logisticspipes.pipes.PipeLogisticsChassiMk4;
import logisticspipes.pipes.PipeLogisticsChassiMk5;
import logisticspipes.routing.RoutedEntityItem;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.IInventory;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraftforge.client.MinecraftForgeClient;
import buildcraft.api.gates.Action;
import buildcraft.api.gates.ActionManager;
import buildcraft.api.gates.Trigger;
import buildcraft.api.transport.IPipedItem;
import buildcraft.core.EntityPassiveItem;
import buildcraft.core.utils.Localization;
import buildcraft.core.utils.Utils;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.EntityData;
import buildcraft.transport.ItemPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.TransportProxyClient;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.registry.LanguageRegistry;

public class BuildCraftProxy {

	/** Support for teleport pipes **/
	public static boolean teleportPipeDetected = false;
	public static Class<? extends Pipe> PipeItemTeleport;
	public static Method teleportPipeMethod;

	public static List<Item> pipelist = new ArrayList<Item>();
	
	public static Trigger LogisticsFailedTrigger;
	
	public static Action LogisticsDisableAction;
	
	public boolean checkPipesConnections(TileEntity tile1, TileEntity tile2) {
		return Utils.checkPipesConnections(tile1, tile2);
	}

	public void dropItems(World world, IInventory inventory, int x, int y, int z) {
		Utils.dropItems(world, inventory, x, y, z);
	}

	public void dropItems(World world, ItemStack stack, int x, int y, int z) {
		Utils.dropItems(world, stack, x, y, z);
	}

	public IRoutedItem GetOrCreateRoutedItem(World worldObj, EntityData itemData) {
		if (!isRoutedItem(itemData.item)){
			RoutedEntityItem newItem = new RoutedEntityItem(worldObj, itemData.item);
			itemData.item = newItem;
			return newItem;
		}
		return (IRoutedItem) itemData.item; 
	}
	
	public boolean isRoutedItem(IPipedItem item) {
		return (item instanceof RoutedEntityItem);
	}
	
	public IRoutedItem GetRoutedItem(IPipedItem item) {
		return (IRoutedItem) item;
	}
	
	public IRoutedItem CreateRoutedItem(World worldObj, IPipedItem item) {
		RoutedEntityItem newItem = new RoutedEntityItem(worldObj, item);
		return newItem;
	}

	public IRoutedItem CreateRoutedItem(ItemStack payload, World worldObj) {
		EntityPassiveItem entityItem = new EntityPassiveItem(worldObj, 0, 0, 0, payload);
		return CreateRoutedItem(worldObj, entityItem);
	}

	public void registerTeleportPipes() {
		try {
			PipeItemTeleport = (Class<? extends Pipe>) Class.forName("buildcraft.additionalpipes.pipes.PipeItemTeleport");
			//PipeItemTeleport = (Class<? extends Pipe>) Class.forName("net.minecraft.src.buildcraft.additionalpipes.pipes.PipeItemTeleport");
			teleportPipeMethod = PipeItemTeleport.getMethod("getConnectedPipes", boolean.class);
			teleportPipeDetected = true;
			ModLoader.getLogger().fine("Additional pipes detected, adding compatibility");

		} catch (Exception e) {
			try {
				//PipeItemTeleport = (Class<? extends Pipe>) Class.forName("buildcraft.additionalpipes.pipes.PipeItemTeleport");
				PipeItemTeleport = (Class<? extends Pipe>) Class.forName("net.minecraft.src.buildcraft.additionalpipes.pipes.PipeItemTeleport");
				teleportPipeMethod = PipeItemTeleport.getMethod("getConnectedPipes", boolean.class);
				teleportPipeDetected = true;
				ModLoader.getLogger().fine("Additional pipes detected, adding compatibility");

			} catch (Exception e1) {
				ModLoader.getLogger().fine("Additional pipes not detected: " + e1.getMessage());
			}
		}
	}

	public void registerTrigger() {
		LogisticsFailedTrigger = new TriggerSupplierFailed(700);
		ActionManager.registerTriggerProvider(new LogisticsTriggerProvider());
		LogisticsDisableAction = new ActionDisableLogistics(700);
	}

	public void registerLocalization() {
		Localization.addLocalization("/lang/logisticspipes/", "en_US");
	}

	public void registerPipes(Side side) {
		LogisticsPipes.LogisticsBasicPipe = createPipe(Configs.LOGISTICSPIPE_BASIC_ID, PipeItemsBasicLogistics.class, "Basic Logistics Pipe", side);
		LogisticsPipes.LogisticsRequestPipe = createPipe(Configs.LOGISTICSPIPE_REQUEST_ID, PipeItemsRequestLogistics.class, "Request Logistics Pipe", side);
		LogisticsPipes.LogisticsProviderPipe = createPipe(Configs.LOGISTICSPIPE_PROVIDER_ID, PipeItemsProviderLogistics.class, "Provider Logistics Pipe", side);
		LogisticsPipes.LogisticsCraftingPipe = createPipe(Configs.LOGISTICSPIPE_CRAFTING_ID, PipeItemsCraftingLogistics.class, "Crafting Logistics Pipe", side);
		LogisticsPipes.LogisticsSatellitePipe = createPipe(Configs.LOGISTICSPIPE_SATELLITE_ID, PipeItemsSatelliteLogistics.class, "Satellite Logistics Pipe", side);
		LogisticsPipes.LogisticsSupplierPipe = createPipe(Configs.LOGISTICSPIPE_SUPPLIER_ID, PipeItemsSupplierLogistics.class, "Supplier Logistics Pipe", side);
		LogisticsPipes.LogisticsChassiPipe1 = createPipe(Configs.LOGISTICSPIPE_CHASSI1_ID, PipeLogisticsChassiMk1.class, "Logistics Chassi Mk1", side);
		LogisticsPipes.LogisticsChassiPipe2 = createPipe(Configs.LOGISTICSPIPE_CHASSI2_ID, PipeLogisticsChassiMk2.class, "Logistics Chassi Mk2", side);
		LogisticsPipes.LogisticsChassiPipe3 = createPipe(Configs.LOGISTICSPIPE_CHASSI3_ID, PipeLogisticsChassiMk3.class, "Logistics Chassi Mk3", side);
		LogisticsPipes.LogisticsChassiPipe4 = createPipe(Configs.LOGISTICSPIPE_CHASSI4_ID, PipeLogisticsChassiMk4.class, "Logistics Chassi Mk4", side);
		LogisticsPipes.LogisticsChassiPipe5 = createPipe(Configs.LOGISTICSPIPE_CHASSI5_ID, PipeLogisticsChassiMk5.class, "Logistics Chassi Mk5", side);
		LogisticsPipes.LogisticsCraftingPipeMK2 = createPipe(Configs.LOGISTICSPIPE_CRAFTING_MK2_ID, PipeItemsCraftingLogisticsMk2.class, "Crafting Logistics Pipe MK2", side);
		LogisticsPipes.LogisticsRequestPipeMK2 = createPipe(Configs.LOGISTICSPIPE_REQUEST_MK2_ID, PipeItemsRequestLogisticsMk2.class, "Request Logistics Pipe MK2", side);
		LogisticsPipes.LogisticsRemoteOrdererPipe = createPipe(Configs.LOGISTICSPIPE_REMOTE_ORDERER_ID, PipeItemsRemoteOrdererLogistics.class, "Remote Orderer Pipe", side);
		LogisticsPipes.LogisticsProviderPipeMK2 = createPipe(Configs.LOGISTICSPIPE_PROVIDER_MK2_ID, PipeItemsProviderLogisticsMk2.class, "Provider Logistics Pipe MK2", side);
		LogisticsPipes.LogisticsApiaristAnalyserPipe = createPipe(Configs.LOGISTICSPIPE_APIARIST_ANALYSER_ID, PipeItemsApiaristAnalyser.class, "Apiarist Logistics Analyser Pipe", side);
		LogisticsPipes.LogisticsApiaristSinkPipe = createPipe(Configs.LOGISTICSPIPE_APIARIST_SINK_ID, PipeItemsApiaristSink.class, "Apiarist Logistics Analyser Pipe", side);
		LogisticsPipes.LogisticsInvSysCon = createPipe(Configs.LOGISTICSPIPE_INVSYSCON_ID, PipeItemsInvSysConnector.class, "Logistics Inventory System Connector", side);
	
		LogisticsPipes.LogisticsBuilderSupplierPipe = createPipe(Configs.LOGISTICSPIPE_BUILDERSUPPLIER_ID, PipeItemsBuilderSupplierLogistics.class, "Builder Supplier Logistics Pipe", side);
		LogisticsPipes.LogisticsLiquidSupplierPipe = createPipe(Configs.LOGISTICSPIPE_LIQUIDSUPPLIER_ID, PipeItemsLiquidSupplier.class, "Liquid Supplier Logistics Pipe", side);
	}

	
	protected Item createPipe(int defaultID, Class <? extends Pipe> clas, String descr, Side side) {
		ItemPipe res =  BlockGenericPipe.registerPipe (defaultID, clas);
		res.setItemName(clas.getSimpleName());
		
		if(side.isClient()) {
			LanguageRegistry.addName(res, descr);
			MinecraftForgeClient.registerItemRenderer(res.shiftedIndex, TransportProxyClient.pipeItemRenderer);
		}
		if(defaultID != Configs.LOGISTICSPIPE_BASIC_ID) {
			registerShapelessResetRecipe(res,0,LogisticsPipes.LogisticsBasicPipe,0);
		}
		pipelist.add(res);
		return res;
	}
	
	protected void registerShapelessResetRecipe(Item fromItem, int fromData, Item toItem, int toData) {
		for(int j=1;j < 10; j++) {
			Object[] obj = new Object[j];
			for(int k=0;k<j;k++) {
				obj[k] = new ItemStack(fromItem, 1, toData);
			}
			CraftingManager.getInstance().addShapelessRecipe(new ItemStack(toItem, j, fromData), obj);
		}
	}
}
