package com.wasteofplastic.acidisland;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.acidisland.nms.NMSAbstraction;


public class DeleteIsland extends BukkitRunnable {
    private AcidIsland plugin;
    private Location l;
    private int counter;
    private int px;
    private int pz;
    private int slice;
    private static NMSAbstraction nms = null;


    /**
     * Class dedicated to deleting islands
     * @param plugin
     * @param loc
     */
    public DeleteIsland(AcidIsland plugin, Location loc) {
	this.plugin = plugin;
	this.l = loc;
	this.counter = 255;
	this.px = l.getBlockX();
	this.pz = l.getBlockZ();
	try {
	    this.slice = checkVersion();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Checks what version the server is running and picks the appropriate NMS handler, or fallback
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private int checkVersion() throws ClassNotFoundException, IllegalArgumentException,
    SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException,
    NoSuchMethodException {
	int slice = 255;
	String serverPackageName = plugin.getServer().getClass().getPackage().getName();
	String pluginPackageName = plugin.getClass().getPackage().getName();
	String version = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
	Class<?> clazz;
	try {
	    //plugin.getLogger().info("Trying " + pluginPackageName + "." + version + ".NMSHandler");
	    clazz = Class.forName(pluginPackageName + "." + version + ".NMSHandler");
	} catch (Exception e) {
	    plugin.getLogger().info("No NMS Handler found, falling back to slow island delete.");
	    clazz = Class.forName(pluginPackageName + ".fallback.NMSHandler");
	    slice = 51;
	}
	//plugin.getLogger().info(serverPackageName);
	//plugin.getLogger().info(pluginPackageName);
	// Check if we have a NMSAbstraction implementing class at that location.
	if (NMSAbstraction.class.isAssignableFrom(clazz)) {
	    nms = (NMSAbstraction) clazz.getConstructor().newInstance();
	} else {
	    throw new IllegalStateException("Class " + clazz.getName() + " does not implement NMSAbstraction");
	}
	return slice;
    }

    @Override
    public void run() {
	//plugin.getLogger().info("DEBUG: removeIsland at location " + l.toString());
	removeSlice(counter, (counter-slice));
	counter = counter - slice;
	if (counter <=0)
	    this.cancel();
    }

    static class Pair {
	private final int left;
	private final int right;
	public Pair(int left, int right) {
	    this.left = left;
	    this.right = right;
	}
	public int getLeft() { return left; }
	public int getRight() { return right; }

	@Override
	public boolean equals(Object o) {
	    if (o == null) return false;
	    if (!(o instanceof Pair)) return false;
	    Pair pairo = (Pair) o;
	    return (this.left == pairo.getLeft()) && (this.right == pairo.getRight());
	}
    }

    void removeSlice(int top, int bottom) {
	List<Pair> chunks = new ArrayList<Pair>();	
	if (bottom <0)
	    bottom = 0;
	plugin.unregisterEvents();
	// Cut island in slices
	for (int y = top; y >= bottom; y--) {
	    for (int x = Settings.island_protectionRange / 2 * -1; x <= Settings.island_protectionRange / 2; x++) {
		for (int z = Settings.island_protectionRange / 2 * -1; z <= Settings.island_protectionRange / 2; z++) {
		    final Block b = new Location(l.getWorld(), px + x, y, pz + z).getBlock();
		    final Pair chunkCoords = new Pair(b.getChunk().getX(),b.getChunk().getZ());
		    if (!chunks.contains(chunkCoords)) {
			chunks.add(chunkCoords);
		    }
		    final Material bt = b.getType();
		    // Grab anything out of containers (do that it is
		    // destroyed)
		    switch (bt) {
		    case CHEST:
			//getLogger().info("DEBUG: Chest");
		    case TRAPPED_CHEST:
			//getLogger().info("DEBUG: Trapped Chest");
			final Chest c = (Chest) b.getState();
			final ItemStack[] items = new ItemStack[c.getInventory().getContents().length];
			c.getInventory().setContents(items);
			b.setType(Material.AIR);
			break;
		    case FURNACE:
			final Furnace f = (Furnace) b.getState();
			final ItemStack[] i2 = new ItemStack[f.getInventory().getContents().length];
			f.getInventory().setContents(i2);
			b.setType(Material.AIR);
			break;
		    case DISPENSER:
			final Dispenser d = (Dispenser) b.getState();
			final ItemStack[] i3 = new ItemStack[d.getInventory().getContents().length];
			d.getInventory().setContents(i3);
			b.setType(Material.AIR);
			break;
		    case HOPPER:
			final Hopper h = (Hopper) b.getState();
			final ItemStack[] i4 = new ItemStack[h.getInventory().getContents().length];
			h.getInventory().setContents(i4);
			b.setType(Material.AIR);
			break;
		    case SIGN_POST:
		    case WALL_SIGN:
		    case SIGN:
			//getLogger().info("DEBUG: Sign");
			b.setType(Material.AIR);
			break;
		    default:
			break;
		    }
		    // Split depending on below or above water line
		    if (y < Settings.sea_level) {
			if (!bt.equals(Material.STATIONARY_WATER))
			    nms.setBlockSuperFast(b, Material.STATIONARY_WATER);
			//b.setType(Material.STATIONARY_WATER);
		    } else {
			if (!bt.equals(Material.AIR))
			    nms.setBlockSuperFast(b, Material.AIR);
			//b.setType(Material.AIR);
		    }

		}
	    }
	}	    
	//l.getWorld().refreshChunk(l.getChunk().getX(), l.getChunk().getZ());
	plugin.restartEvents();
	// Refresh chunks that have been affected
	//plugin.getLogger().info("DEBUG: " + chunks.size() + " chunks need refreshing!");
	for (Pair p: chunks) {
	    l.getWorld().refreshChunk(p.getLeft(), p.getRight());
	    //plugin.getLogger().info("DEBUG: refreshing " + p.getLeft() + "," + p.getRight());
	}
    }
}
