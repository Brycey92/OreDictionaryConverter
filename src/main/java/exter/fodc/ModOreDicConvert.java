package exter.fodc;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.Logger;

import exter.fodc.block.BlockAutomaticOreConverter;
import exter.fodc.block.BlockOreConversionTable;
import exter.fodc.item.ItemOreConverter;
import exter.fodc.network.MessageODC;
import exter.fodc.proxy.CommonODCProxy;
import exter.fodc.registry.OreNameRegistry;
import exter.fodc.tileentity.TileEntityAutomaticOreConverter;

@Mod(
    modid = ModOreDicConvert.MODID,
    name = ModOreDicConvert.MODNAME,
    version = ModOreDicConvert.MODVERSION,
    dependencies = "required-after:forge@[13.19.1.2189,)"
    )
public class ModOreDicConvert
{
  public static final String MODID = "fodc";
  public static final String MODNAME = "Ore Dictionary Converter";
  public static final String MODVERSION = "1.10.0";

  public static ItemOreConverter item_oreconverter = null;
  @Instance("fodc")
  public static ModOreDicConvert instance;

  // Says where the client and server 'proxy' code is loaded.
  @SidedProxy(clientSide = "exter.fodc.proxy.ClientODCProxy", serverSide = "exter.fodc.proxy.CommonODCProxy")
  public static CommonODCProxy proxy;
  public static BlockOreConversionTable block_oreconvtable;
  public static BlockAutomaticOreConverter block_oreautoconv;
    
  public static Logger log;
  
  public static boolean log_orenames;

  public static SimpleNetworkWrapper network_channel;
    

  @EventHandler
  public void preInit(FMLPreInitializationEvent event)
  {
    log = event.getModLog();
    Configuration config = new Configuration(event.getSuggestedConfigurationFile());
    config.load();
    log_orenames = config.getBoolean("log_names", Configuration.CATEGORY_GENERAL, false, "Log registered ore names.");
    OreNameRegistry.preInit(config);
    config.save();
    
    
    NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);

    block_oreconvtable = new BlockOreConversionTable();
    block_oreautoconv = new BlockAutomaticOreConverter();
    item_oreconverter = new ItemOreConverter();

    GameRegistry.register(item_oreconverter);

    GameRegistry.register(block_oreconvtable);
    GameRegistry.register(block_oreautoconv);
    GameRegistry.register(new ItemBlock(block_oreconvtable).setRegistryName(block_oreconvtable.getRegistryName()));
    GameRegistry.register(new ItemBlock(block_oreautoconv).setRegistryName(block_oreautoconv.getRegistryName()));

    
    
    network_channel = NetworkRegistry.INSTANCE.newSimpleChannel("EXTER.FODC");
    network_channel.registerMessage(MessageODC.Handler.class, MessageODC.class, 0, Side.SERVER);
    network_channel.registerMessage(MessageODC.Handler.class, MessageODC.class, 0, Side.CLIENT);
  }

  @EventHandler
  public void init(FMLInitializationEvent event)
  {
    GameRegistry.registerTileEntity(TileEntityAutomaticOreConverter.class, "AutoOreConverter");
    proxy.init();

    ItemStack iron_stack = new ItemStack(Items.IRON_INGOT);
    ItemStack redstone_stack = new ItemStack(Items.REDSTONE);
    ItemStack workbench_stack = new ItemStack(Blocks.CRAFTING_TABLE);
    ItemStack wood_stack = new ItemStack(Blocks.PLANKS,1,OreDictionary.WILDCARD_VALUE);
    ItemStack cobble_stack = new ItemStack(Blocks.COBBLESTONE,1,OreDictionary.WILDCARD_VALUE);
    ItemStack oreconverter_stack = new ItemStack(item_oreconverter);
    
    GameRegistry.addRecipe(
        oreconverter_stack,
        "I",
        "C",
        "B",
        'I', iron_stack,
        'C', cobble_stack,
        'B', workbench_stack);
    GameRegistry.addRecipe(
        new ItemStack(block_oreconvtable),
        "O",
        "W",
        'O', oreconverter_stack,
        'W', wood_stack);
    GameRegistry.addRecipe(
        new ItemStack(block_oreautoconv),
        "IOI",
        "CRC",
        "ICI",
        'I', iron_stack,
        'O', oreconverter_stack,
        'R', redstone_stack,
        'C', cobble_stack);
  }


  @EventHandler
  public void postInit(FMLPostInitializationEvent event)
  {
    //log.setParent(FMLLog.getLogger());
    String[] ore_names = OreDictionary.getOreNames();
    for (String name : ore_names)
    {
      if(name == null)
      {
        log.warn("null name in Ore Dictionary.");
        continue;
      }
      OreNameRegistry.registerOreName(name);
    }
    MinecraftForge.EVENT_BUS.register(this);
  }

  @EventHandler
  public void remap(FMLMissingMappingsEvent event)
  {
    for(MissingMapping map:event.get())
    {
      String name = map.resourceLocation.getResourcePath();
      if(name.equals("oreconverter") && map.type == GameRegistry.Type.ITEM)
      {
        map.remap(item_oreconverter);
      } else if(name.equals("oreconvtable"))
      {
        switch(map.type)
        {
          case BLOCK:
            map.remap(block_oreconvtable);
            break;
          case ITEM:
            map.remap(ItemBlock.getItemFromBlock(block_oreconvtable));
            break;
          default:
            break;
        }
      } else if(name.equals("oreautoconverter"))
      {
        switch(map.type)
        {
          case BLOCK:
            map.remap(block_oreautoconv);
            break;
          case ITEM:
            map.remap(ItemBlock.getItemFromBlock(block_oreautoconv));
            break;
          default:
            break;
        }
      } else
      {
        map.warn();
      }
    }
  }

  @SubscribeEvent
  public void onOreDictionaryRegister(OreDictionary.OreRegisterEvent event)
  {
    OreNameRegistry.registerOreName(event.getName());
  }
}
