package me.badbones69.crazycrates.api.objects;

import me.badbones69.crazycrates.Methods;
import me.badbones69.crazycrates.api.CrazyCrates;
import me.badbones69.crazycrates.api.FileManager;
import me.badbones69.crazycrates.api.enums.CrateType;
import me.badbones69.crazycrates.controllers.Preview;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Crate {
	
	private String name;
	private ItemStack key;
	private ItemStack keyNoNBT;
	private ItemStack adminKey;
	private Integer maxPage = 1;
	private String previewName;
	private boolean previewToggle;
	private boolean boarderToggle;
	private ItemBuilder boarderItem;
	private CrateType crateType;
	private FileConfiguration file;
	private ArrayList<Prize> prizes;
	private String crateInventoryName;
	private boolean giveNewPlayerKeys;
	private int previewChestlines;
	private Integer newPlayerKeys;
	private ArrayList<ItemStack> preview;
	private ArrayList<Tier> tiers;
	private FileManager fileManager = FileManager.getInstance();
	private CrazyCrates cc = CrazyCrates.getInstance();
	
	/**
	 *
	 * @param name The name of the crate.
	 * @param crateType The crate type of the crate.
	 * @param key The key as an item stack.
	 * @param prizes The prizes that can be won.
	 * @param file The crate file.
	 */
	public Crate(String name, String previewName, CrateType crateType, ItemStack key, ArrayList<Prize> prizes, FileConfiguration file, Integer newPlayerKeys, ArrayList<Tier> tiers) {
		ItemBuilder itemBuilder = ItemBuilder.convertItemStack(key);
		this.keyNoNBT = itemBuilder.build();
		this.key = itemBuilder.setCrateName(name).build();
		this.adminKey = itemBuilder
		.addLore("")
		.addLore("&7&l(&6&l!&7&l) Left click for Physical Key")
		.addLore("&7&l(&6&l!&7&l) Right click for Virtual Key")
		.setCrateName(name).build();
		this.file = file;
		this.name = name;
		this.tiers = tiers != null ? tiers : new ArrayList<>();
		this.prizes = prizes;
		this.crateType = crateType;
		this.preview = loadPreview();
		setPreviewChestlines(file != null ? file.getInt("Crate.Preview.ChestLines", 6) : 6);
		this.previewToggle = file != null && (!file.contains("Crate.Preview.Toggle") || file.getBoolean("Crate.Preview.Toggle"));
		this.boarderToggle = file != null && file.getBoolean("Crate.Preview.Glass.Toggle");
		this.previewName = Methods.color(previewName);
		this.newPlayerKeys = newPlayerKeys;
		this.giveNewPlayerKeys = newPlayerKeys > 0;
		for(int amount = preview.size(); amount > getMaxSlots() - (boarderToggle ? 18 : 9); amount -= getMaxSlots() - (boarderToggle ? 18 : 9), maxPage++) ;
		this.crateInventoryName = file != null ? Methods.color(file.getString("Crate.CrateName")) : "";
		this.boarderItem = file != null && file.contains("Crate.Preview.Glass.Item") ? new ItemBuilder().setMaterial(file.getString("Crate.Preview.Glass.Item")).setName(" ") : new ItemBuilder().setMaterial(Material.AIR);
	}
	
	/**
	 * Set the preview lines for a Crate.
	 * @param amount The amount of lines the preview has.
	 */
	public void setPreviewChestlines(int amount) {
		int finalAmount;
		if(amount < 3) {
			finalAmount = 3;
		}else finalAmount = Math.min(amount, 6);
		this.previewChestlines = finalAmount;
	}
	
	/**
	 * Get the amount of lines the preview will show.
	 * @return The amount of lines it is set to show.
	 */
	public int getPreviewChestLines() {
		return this.previewChestlines;
	}
	
	/**
	 * Get the max amount of slots in the preview.
	 * @return The max number of slots in the preview.
	 */
	public int getMaxSlots() {
		return this.previewChestlines * 9;
	}
	
	/**
	 * Check to see if a player can win a prize from a crate.
	 * @param player The player you are checking.
	 * @return True if they can win at least 1 prize and false if they can't win any.
	 */
	public boolean canWinPrizes(Player player) {
		return pickPrize(player) != null;
	}
	
	/**
	 * Picks a random prize based on BlackList Permissions and the Chance System.
	 * @param player The player that will be winning the prize.
	 * @return The winning prize.
	 */
	public Prize pickPrize(Player player) {
		ArrayList<Prize> prizes = new ArrayList<>();
		ArrayList<Prize> useablePrizes = new ArrayList<>();
		// ================= Blacklist Check ================= //
		if(player.isOp()) {
			useablePrizes.addAll(getPrizes());
		}else {
			for(Prize prize : getPrizes()) {
				if(prize.hasBlacklistPermission(player)) {
					if(!prize.hasAltPrize()) {
						continue;
					}
				}
				useablePrizes.add(prize);
			}
		}
		// ================= Chance Check ================= //
		for(int stop = 0; prizes.size() == 0 && stop <= 2000; stop++) {
			for(Prize prize : useablePrizes) {
				int max = prize.getMaxRange();
				int chance = prize.getChance();
				int num;
				for(int counter = 1; counter <= 1; counter++) {
					num = 1 + new Random().nextInt(max);
					if(num >= 1 && num <= chance) {
						prizes.add(prize);
					}
				}
			}
		}
		try {
			return prizes.get(new Random().nextInt(prizes.size()));
		}catch(IllegalArgumentException e) {
			System.out.println("[CrazyCrates] failed to find prize from the " + name + " crate for player " + player.getName() + ".");
			return null;
		}
	}
	
	/**
	 * Picks a random prize based on BlackList Permissions and the Chance System. Only used in the Cosmic Crate Type since it is the only one with tiers.
	 * @param player The player that will be winning the prize.
	 * @param tier The tier you wish the prize to be from.
	 * @return The winning prize based on the crate's tiers.
	 */
	public Prize pickPrize(Player player, Tier tier) {
		ArrayList<Prize> prizes = new ArrayList<>();
		ArrayList<Prize> useablePrizes = new ArrayList<>();
		// ================= Blacklist Check ================= //
		if(player.isOp()) {
			for(Prize prize : getPrizes()) {
				if(prize.getTiers().contains(tier)) {
					useablePrizes.add(prize);
				}
			}
		}else {
			for(Prize prize : getPrizes()) {
				if(prize.hasBlacklistPermission(player)) {
					if(!prize.hasAltPrize()) {
						continue;
					}
				}
				if(prize.getTiers().contains(tier)) {
					useablePrizes.add(prize);
				}
			}
		}
		// ================= Chance Check ================= //
		for(int stop = 0; prizes.size() == 0 && stop <= 2000; stop++) {
			for(Prize prize : useablePrizes) {
				int max = prize.getMaxRange();
				int chance = prize.getChance();
				int num;
				for(int counter = 1; counter <= 1; counter++) {
					num = 1 + new Random().nextInt(max);
					if(num >= 1 && num <= chance) {
						prizes.add(prize);
					}
				}
			}
		}
		return prizes.get(new Random().nextInt(prizes.size()));
	}
	
	/**
	 * Picks a random prize based on BlackList Permissions and the Chance System. Spawns the display item at the location.
	 * @param player The player that will be winning the prize.
	 * @param location The location the firework will spawn at.
	 * @return The winning prize.
	 */
	public Prize pickPrize(Player player, Location location) {
		Prize prize = pickPrize(player);
		if(prize.useFireworks()) {
			Methods.fireWork(location);
		}
		return prize;
	}
	
	/**
	 *
	 * @return name The name of the crate.
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 *
	 * @return The name of the crate's preview page.
	 */
	public String getPreviewName() {
		return previewName;
	}
	
	/**
	 * Get if the preview is toggled on.
	 * @return True if preview is on and false if not.
	 */
	public boolean isPreviewEnabled() {
		return previewToggle;
	}
	
	/**
	 * Get if the preview has an item boarder.
	 * @return True if it does and false if not.
	 */
	public boolean isItemBoarderEnabled() {
		return boarderToggle;
	}
	
	/**
	 * Get the item that shows as the preview boarder if enabled.
	 * @return The ItemBuilder for the boarder item.
	 */
	public ItemBuilder getBoarderItem() {
		return boarderItem;
	}
	
	/**
	 * Get the name of the inventory the crate will have.
	 * @return The name of the inventory for GUI based crate types.
	 */
	public String getCrateInventoryName() {
		return this.crateInventoryName;
	}
	
	/**
	 * Gets the inventory of a preview of prizes for the crate.
	 * @return The preview as an Inventory object.
	 */
	public Inventory getPreview(Player player) {
		Inventory inv = Bukkit.createInventory(null, getMaxSlots(), previewName);
		setDefaultItems(inv, player);
		for(ItemStack item : getPageItems(Preview.getPage(player))) {
			int nextSlot = inv.firstEmpty();
			if(nextSlot >= 0) {
				inv.setItem(nextSlot, item);
			}else {
				break;
			}
		}
		return inv;
	}
	
	/**
	 * Gets the inventory of a preview of prizes for the crate.
	 * @return The preview as an Inventory object.
	 */
	public Inventory getPreview(Player player, int page) {
		Inventory inv = Bukkit.createInventory(null, getMaxSlots(), previewName);
		setDefaultItems(inv, player);
		for(ItemStack item : getPageItems(page)) {
			int nextSlot = inv.firstEmpty();
			if(nextSlot >= 0) {
				inv.setItem(nextSlot, item);
			}else {
				break;
			}
		}
		return inv;
	}
	
	/**
	 * Gets all the preview items.
	 * @return A list of all the preview items.
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<ItemStack> getPreviewItems() {
		return (ArrayList<ItemStack>) preview.clone();
	}
	
	/**
	 *
	 * @return The crate type of the crate.
	 */
	public CrateType getCrateType() {
		return this.crateType;
	}
	
	/**
	 *
	 * @return The key as an item stack.
	 */
	public ItemStack getKey() {
		return this.key.clone();
	}
	
	/**
	 *
	 * @param amount The amount of keys you want.
	 * @return The key as an item stack.
	 */
	public ItemStack getKey(int amount) {
		ItemStack key = this.key.clone();
		key.setAmount(amount);
		return key;
	}
	
	/**
	 *
	 * @return The key as an item stack with no nbt tags.
	 */
	public ItemStack getKeyNoNBT() {
		return this.keyNoNBT.clone();
	}
	
	/**
	 *
	 * @param amount The amount of keys you want.
	 * @return The key as an item stack with no nbt tags.
	 */
	public ItemStack getKeyNoNBT(int amount) {
		ItemStack key = this.keyNoNBT.clone();
		key.setAmount(amount);
		return key;
	}
	
	/**
	 * Get the key that shows in the /cc admin menu.
	 * @return The itemstack of the key shown in the /cc admin menu.
	 */
	public ItemStack getAdminKey() {
		return adminKey;
	}
	
	/**
	 *
	 * @return The crates file.
	 */
	public FileConfiguration getFile() {
		return this.file;
	}
	
	/**
	 *
	 * @return The prizes in the crate.
	 */
	public ArrayList<Prize> getPrizes() {
		return this.prizes;
	}
	
	/**
	 *
	 * @param name Name of the prize you want.
	 * @return The prize you asked for.
	 */
	public Prize getPrize(String name) {
		for(Prize prize : prizes) {
			if(prize.getName().equalsIgnoreCase(name)) {
				return prize;
			}
		}
		return null;
	}
	
	/**
	 *
	 * @return True if new players get keys and false if they do not.
	 */
	public boolean doNewPlayersGetKeys() {
		return giveNewPlayerKeys;
	}
	
	/**
	 *
	 * @return The number of keys new players get.
	 */
	public Integer getNewPlayerKeys() {
		return newPlayerKeys;
	}
	
	/**
	 * Add a new editor item to a prize in the Crate.
	 * @param prize The prize the item is being added to.
	 * @param item The ItemStack that is being added.
	 */
	public void addEditorItem(String prize, ItemStack item) {
		ArrayList<ItemStack> items = new ArrayList<>();
		items.add(item);
		String path = "Crate.Prizes." + prize;
		if(file.contains(path + ".Editor-Items")) {
			for(Object list : file.getList(path + ".Editor-Items")) {
				items.add((ItemStack) list);
			}
		}
		if(!file.contains(path + ".DisplayName")) file.set(path + ".DisplayName", "&7Auto Generated Prize #&6" + prize);
		if(!file.contains(path + ".DisplayItem")) file.set(path + ".DisplayItem", "STAINED_GLASS_PANE:14");
		if(!file.contains(path + ".DisplayAmount")) file.set(path + ".DisplayAmount", 1);
		if(!file.contains(path + ".Lore")) file.set(path + ".Lore", new ArrayList<>());
		if(!file.contains(path + ".MaxRange")) file.set(path + ".MaxRange", 100);
		if(!file.contains(path + ".Chance")) file.set(path + ".Chance", 50);
		if(!file.contains(path + ".Firework")) file.set(path + ".Firework", false);
		if(!file.contains(path + ".Glowing")) file.set(path + ".Glowing", false);
		if(!file.contains(path + ".Player")) file.set(path + ".Player", "");
		if(!file.contains(path + ".Unbreakable")) file.set(path + ".Unbreakable", false);
		if(!file.contains(path + ".Items")) file.set(path + ".Items", new ArrayList<>());
		file.set(path + ".Editor-Items", items);
		if(!file.contains(path + ".Commands")) file.set(path + ".Commands", new ArrayList<>());
		if(!file.contains(path + ".Messages")) file.set(path + ".Messages", new ArrayList<>());
		if(!file.contains(path + ".BlackListed-Permissions")) file.set(path + ".BlackListed-Permissions", new ArrayList<>());
		if(!file.contains(path + ".Alternative-Prize.Toggle")) file.set(path + ".Alternative-Prize.Toggle", false);
		if(!file.contains(path + ".Alternative-Prize.Commands")) file.set(path + ".Alternative-Prize.Commands", new ArrayList<>());
		if(!file.contains(path + ".Alternative-Prize.Items")) file.set(path + ".Alternative-Prize.Items", new ArrayList<>());
		if(!file.contains(path + ".Alternative-Prize.Messages")) file.set(path + ".Alternative-Prize.Messages", new ArrayList<>());
		fileManager.saveFile(fileManager.getFile(name));
	}
	
	/**
	 *
	 * @return The max page for the preview.
	 */
	public int getMaxPage() {
		return maxPage;
	}
	
	/**
	 *
	 * @return A list of the tiers for the crate. Will be empty if there are none.
	 */
	public ArrayList<Tier> getTiers() {
		return tiers;
	}
	
	/**
	 * Loads all the preview items and puts them into a list.
	 * @return A list of all the preview items that were created.
	 */
	private ArrayList<ItemStack> loadPreview() {
		ArrayList<ItemStack> items = new ArrayList<>();
		for(Prize prize : getPrizes()) {
			items.add(prize.getDisplayItem());
		}
		return items;
	}
	
	private List<ItemStack> getPageItems(Integer page) {
		List<ItemStack> list = preview;
		List<ItemStack> items = new ArrayList<>();
		if(page <= 0) page = 1;
		int max = boarderToggle ? getMaxSlots() - 18 : getMaxSlots() - 9;
		int index = page * max - max;
		int endIndex = index >= list.size() ? list.size() - 1 : index + max;
		for(; index < endIndex; index++) {
			if(index < list.size()) items.add(list.get(index));
		}
		for(; items.isEmpty(); page--) {
			if(page <= 0) break;
			index = page * max - max;
			endIndex = index >= list.size() ? list.size() - 1 : index + max;
			for(; index < endIndex; index++) {
				if(index < list.size()) items.add(list.get(index));
			}
		}
		return items;
	}
	
	public int getAbsoluteItemPosition(int baseSlot) {
		return baseSlot + (previewChestlines - 1) * 9;
	}
	
	private void setDefaultItems(Inventory inv, Player player) {
		if(boarderToggle) {
			List<Integer> borderItems = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8);
			for(int i : borderItems) {//Top Boarder slots
				inv.setItem(i, boarderItem.build());
			}
			for(int i = 0; i < borderItems.size(); i++) {
				borderItems.set(i, getAbsoluteItemPosition(borderItems.get(i)));
			}
			for(int i : borderItems) {//Bottom Boarder slots
				inv.setItem(i, boarderItem.build());
			}
		}
		int page = Preview.getPage(player);
		if(Preview.playerInMenu(player)) {
			inv.setItem(getAbsoluteItemPosition(4), Preview.getMenuButton());
		}
		if(page == 1) {
			if(boarderToggle) {
				inv.setItem(getAbsoluteItemPosition(3), boarderItem.build());
			}
		}else {
			inv.setItem(getAbsoluteItemPosition(3), Preview.getBackButton(player));
		}
		if(page == maxPage) {
			if(boarderToggle) {
				inv.setItem(getAbsoluteItemPosition(5), boarderItem.build());
			}
		}else {
			inv.setItem(getAbsoluteItemPosition(5), Preview.getNextButton(player));
		}
	}
	
}