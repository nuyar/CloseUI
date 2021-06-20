package kr.nuyar.www.closeui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloseUI extends JavaPlugin implements Listener {
    public static CloseUI plugin;

    private Inventory closingInventory;
    private Map<String, Boolean> setting;
    List<String> whitelist;

    @Override
    public void onEnable() {
        CloseUI.plugin = this;

        this.closingInventory = Bukkit.createInventory(null, 9, ChatColor.RED + "이 블럭/엔티티를 우클릭하지 말아주세요.");
        this.setting = new HashMap<>();

        this.getConfig().addDefault("whitelist", new ArrayList<String>());
        this.whitelist = this.getConfig().getStringList("whitelist");
        this.saveConfig();


        this.getCommand("closeui").setExecutor(this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSettingEntity(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        if (!(this.setting.containsKey(p.getName()) && this.setting.get(p.getName())))
            return;
        e.setCancelled(true);

        String entity = e.getRightClicked().getUniqueId().toString();

        String world = p.getWorld().getName();
        ConfigurationSection entitiesSection = this.getConfig().getConfigurationSection("entities");

        if (entitiesSection == null)
            entitiesSection = this.getConfig().createSection("entities");
        List<String> entities = entitiesSection.getStringList(world);
        if (entities == null)
            entities = new ArrayList<>();

        if (entities.contains(entity)) {
            entities.remove(entity);
            p.sendMessage(String.format("removed entity %s in %s", entity, world));
        } else {
            entities.add(entity);
            p.sendMessage(String.format("added entity %s in %s", entity, world));
        }

        entitiesSection.set(world, entities);
        this.saveConfig();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSettingBlock(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!(this.setting.containsKey(p.getName()) && this.setting.get(p.getName())))
            return;
        e.setCancelled(true);

        Block block = e.getClickedBlock();

        String world = p.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        String location = String.format("%d %d %d", x, y, z);
        ConfigurationSection blocksSection = this.getConfig().getConfigurationSection("blocks");

        if (blocksSection == null)
            blocksSection = this.getConfig().createSection("blocks");
        List<String> blocks = blocksSection.getStringList(world);
        if (blocks == null)
            blocks = new ArrayList<>();

        if (blocks.contains(location)) {
            blocks.remove(location);
            p.sendMessage(String.format("removed block %s in %s", location, world));
        } else {
            blocks.add(location);
            p.sendMessage(String.format("added block %s in %s", location, world));
        }

        blocksSection.set(world, blocks);
        this.saveConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClickBlock(PlayerInteractEvent e) {
        if(e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player p = e.getPlayer();
        Block block = e.getClickedBlock();

        String world = p.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        String location = String.format("%d %d %d", x, y, z);
        ConfigurationSection blocksSection = this.getConfig().getConfigurationSection("blocks");

        if (blocksSection == null)
            return;
        List<String> blocks = blocksSection.getStringList(world);
        if (blocks == null)
            return;

        if (!blocks.contains(location))
            return;

        InventoryView inv = p.getOpenInventory();
        if (this.whitelist.contains(ChatColor.stripColor(inv.getTitle())))
            return;

        p.openInventory(closingInventory);
        p.closeInventory();
        this.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            p.openInventory(closingInventory);
            p.closeInventory();
        }, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClickEntity(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        String entity = e.getRightClicked().getUniqueId().toString();

        String world = p.getWorld().getName();
        ConfigurationSection entitiesSection = this.getConfig().getConfigurationSection("entities");

        if (entitiesSection == null)
            return;
        List<String> entities = entitiesSection.getStringList(world);
        if (entities == null)
            return;

        if (!entities.contains(entity))
            return;

        InventoryView inv = p.getOpenInventory();
        if (this.whitelist.contains(ChatColor.stripColor(inv.getTitle())))
            return;

        p.openInventory(closingInventory);
        p.closeInventory();
        this.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            p.openInventory(closingInventory);
            p.closeInventory();
        }, 1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp())
            return true;
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (this.setting.containsKey(p.getName()) && this.setting.get(p.getName())) {
            this.setting.remove(p.getName());
            p.sendMessage("설정 모드를 종료합니다.");
        } else {
            this.setting.put(p.getName(), true);
            p.sendMessage("엔티티 혹은 블럭을 우클릭해주세요.");
        }

        return true;
    }
}
