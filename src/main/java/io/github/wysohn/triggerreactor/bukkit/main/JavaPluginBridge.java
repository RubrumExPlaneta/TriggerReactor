/*******************************************************************************
 *     Copyright (C) 2018 wysohn
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package io.github.wysohn.triggerreactor.bukkit.main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.script.ScriptException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import io.github.wysohn.triggerreactor.bukkit.bridge.BukkitCommandSender;
import io.github.wysohn.triggerreactor.bukkit.bridge.BukkitInventory;
import io.github.wysohn.triggerreactor.bukkit.bridge.player.BukkitPlayer;
import io.github.wysohn.triggerreactor.bukkit.manager.AreaSelectionManager;
import io.github.wysohn.triggerreactor.bukkit.manager.ExecutorManager;
import io.github.wysohn.triggerreactor.bukkit.manager.PermissionManager;
import io.github.wysohn.triggerreactor.bukkit.manager.PlaceholderManager;
import io.github.wysohn.triggerreactor.bukkit.manager.PlayerLocationManager;
import io.github.wysohn.triggerreactor.bukkit.manager.ScriptEditManager;
import io.github.wysohn.triggerreactor.bukkit.manager.VariableManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.AreaTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.ClickTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.CommandTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.CustomTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.InventoryTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.NamedTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.RepeatingTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.WalkTriggerManager;
import io.github.wysohn.triggerreactor.bukkit.manager.trigger.share.api.APISupport;
import io.github.wysohn.triggerreactor.bukkit.tools.BukkitUtil;
import io.github.wysohn.triggerreactor.bukkit.tools.DelegatedPlayer;
import io.github.wysohn.triggerreactor.core.bridge.ICommandSender;
import io.github.wysohn.triggerreactor.core.bridge.IInventory;
import io.github.wysohn.triggerreactor.core.bridge.IItemStack;
import io.github.wysohn.triggerreactor.core.bridge.event.IEvent;
import io.github.wysohn.triggerreactor.core.bridge.player.IPlayer;
import io.github.wysohn.triggerreactor.core.main.TriggerReactor;
import io.github.wysohn.triggerreactor.core.manager.AbstractAreaSelectionManager;
import io.github.wysohn.triggerreactor.core.manager.AbstractExecutorManager;
import io.github.wysohn.triggerreactor.core.manager.AbstractPermissionManager;
import io.github.wysohn.triggerreactor.core.manager.AbstractPlaceholderManager;
import io.github.wysohn.triggerreactor.core.manager.AbstractPlayerLocationManager;
import io.github.wysohn.triggerreactor.core.manager.AbstractScriptEditManager;
import io.github.wysohn.triggerreactor.core.manager.AbstractVariableManager;
import io.github.wysohn.triggerreactor.core.manager.Manager;
import io.github.wysohn.triggerreactor.core.manager.location.SimpleLocation;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractAreaTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractCommandTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractCustomTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractInventoryTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractInventoryTriggerManager.InventoryTrigger;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractLocationBasedTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractNamedTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractRepeatingTriggerManager;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractTriggerManager.Trigger;
import io.github.wysohn.triggerreactor.core.manager.trigger.share.api.AbstractAPISupport;
import io.github.wysohn.triggerreactor.core.script.interpreter.Interpreter;
import io.github.wysohn.triggerreactor.core.script.interpreter.Interpreter.ProcessInterrupter;
import io.github.wysohn.triggerreactor.core.script.parser.Node;
import io.github.wysohn.triggerreactor.tools.mysql.MiniConnectionPoolManager;

public class JavaPluginBridge extends TriggerReactor implements Plugin{
    private Map<String, AbstractAPISupport> sharedVars = new HashMap<>();

    private io.github.wysohn.triggerreactor.bukkit.main.TriggerReactor bukkitPlugin;

    private BungeeCordHelper bungeeHelper;
    private Lag tpsHelper;
    private MysqlSupport mysqlHelper;

    private AbstractExecutorManager executorManager;
    private AbstractPlaceholderManager placeholderManager;
    private AbstractVariableManager variableManager;
    private AbstractScriptEditManager scriptEditManager;
    private AbstractPlayerLocationManager locationManager;
    private AbstractPermissionManager permissionManager;
    private AbstractAreaSelectionManager selectionManager;

    private AbstractLocationBasedTriggerManager<AbstractLocationBasedTriggerManager.ClickTrigger> clickManager;
    private AbstractLocationBasedTriggerManager<AbstractLocationBasedTriggerManager.WalkTrigger> walkManager;
    private AbstractCommandTriggerManager cmdManager;
    private AbstractInventoryTriggerManager invManager;
    private AbstractAreaTriggerManager areaManager;
    private AbstractCustomTriggerManager customManager;
    private AbstractRepeatingTriggerManager repeatManager;

    private AbstractNamedTriggerManager namedTriggerManager;

    @Override
    public AbstractExecutorManager getExecutorManager() {
        return executorManager;
    }

    @Override
    public AbstractPlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    @Override
    public AbstractVariableManager getVariableManager() {
        return variableManager;
    }

    @Override
    public AbstractScriptEditManager getScriptEditManager() {
        return scriptEditManager;
    }

    @Override
    public AbstractPlayerLocationManager getLocationManager() {
        return locationManager;
    }

    @Override
    public AbstractPermissionManager getPermissionManager() {
        return permissionManager;
    }

    @Override
    public AbstractAreaSelectionManager getSelectionManager() {
        return selectionManager;
    }

    @Override
    public AbstractLocationBasedTriggerManager<AbstractLocationBasedTriggerManager.ClickTrigger> getClickManager() {
        return clickManager;
    }

    @Override
    public AbstractLocationBasedTriggerManager<AbstractLocationBasedTriggerManager.WalkTrigger> getWalkManager() {
        return walkManager;
    }

    @Override
    public AbstractCommandTriggerManager getCmdManager() {
        return cmdManager;
    }

    @Override
    public AbstractInventoryTriggerManager getInvManager() {
        return invManager;
    }

    @Override
    public AbstractAreaTriggerManager getAreaManager() {
        return areaManager;
    }

    @Override
    public AbstractCustomTriggerManager getCustomManager() {
        return customManager;
    }

    @Override
    public AbstractRepeatingTriggerManager getRepeatManager() {
        return repeatManager;
    }

    @Override
    public AbstractNamedTriggerManager getNamedTriggerManager() {
        return namedTriggerManager;
    }

    public BungeeCordHelper getBungeeHelper() {
        return bungeeHelper;
    }

    public Lag getTpsHelper() {
        return tpsHelper;
    }

    public MysqlSupport getMysqlHelper() {
        return mysqlHelper;
    }

    private Thread bungeeConnectionThread;

    public void onEnable(io.github.wysohn.triggerreactor.bukkit.main.TriggerReactor plugin){
        Thread.currentThread().setContextClassLoader(plugin.getClass().getClassLoader());

        this.bukkitPlugin = plugin;

        for(Entry<String, Class<? extends AbstractAPISupport>> entry : APISupport.getSharedVars().entrySet()){
            AbstractAPISupport.addSharedVar(sharedVars, entry.getKey(), entry.getValue());
        }

        try {
            executorManager = new ExecutorManager(this);
        } catch (ScriptException | IOException e) {
            initFailed(e);
            return;
        }

        try {
            placeholderManager = new PlaceholderManager(this);
        } catch (ScriptException | IOException e) {
            initFailed(e);
            return;
        }

        try {
            variableManager = new VariableManager(this);
        } catch (IOException | InvalidConfigurationException e) {
            initFailed(e);
            return;
        }

        scriptEditManager = new ScriptEditManager(this);
        locationManager = new PlayerLocationManager(this);
        permissionManager = new PermissionManager(this);
        selectionManager = new AreaSelectionManager(this);

        clickManager = new ClickTriggerManager(this);
        walkManager = new WalkTriggerManager(this);
        cmdManager = new CommandTriggerManager(this);
        invManager = new InventoryTriggerManager(this);
        areaManager = new AreaTriggerManager(this);
        customManager = new CustomTriggerManager(this);
        repeatManager = new RepeatingTriggerManager(this);

        namedTriggerManager = new NamedTriggerManager(this);

        for(Manager manager : Manager.getManagers()) {
            manager.reload();
        }

        bungeeHelper = new BungeeCordHelper();
        bungeeConnectionThread = new Thread(bungeeHelper);
        bungeeConnectionThread.setPriority(Thread.MIN_PRIORITY);
        bungeeConnectionThread.start();

        tpsHelper = new Lag();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(bukkitPlugin, tpsHelper, 100L, 1L);

        FileConfiguration config = plugin.getConfig();
        if(config.getBoolean("Mysql.Enable", false)) {
            try {
                plugin.getLogger().info("Initializing Mysql support...");
                mysqlHelper = new MysqlSupport(config.getString("Mysql.Address"),
                        config.getString("Mysql.DbName"),
                        "data",
                        config.getString("Mysql.UserName"),
                        config.getString("Mysql.Password"));
                plugin.getLogger().info(mysqlHelper.toString());
                plugin.getLogger().info("Done!");
            } catch (SQLException e) {
                e.printStackTrace();
                plugin.getLogger().warning("Failed to initialize Mysql. Check for the error above.");
            }
        } else {
            String path = "Mysql.Enable";
            if(!config.isSet(path))
                config.set(path, false);
            path = "Mysql.Address";
            if(!config.isSet(path))
                config.set(path, "127.0.0.1:3306");
            path = "Mysql.DbName";
            if(!config.isSet(path))
                config.set(path, "TriggerReactor");
            path = "Mysql.UserName";
            if(!config.isSet(path))
                config.set(path, "root");
            path = "Mysql.Password";
            if(!config.isSet(path))
                config.set(path, "1234");

            plugin.saveConfig();
        }
    }

    private void initFailed(Exception e) {
        e.printStackTrace();
        getLogger().severe("Initialization failed!");
        getLogger().severe(e.getMessage());
        disablePlugin();
    }

    public void onDisable(JavaPlugin plugin){
        getLogger().info("Finalizing the scheduled script executions...");
        cachedThreadPool.shutdown();
        bungeeConnectionThread.interrupt();
        getLogger().info("Shut down complete!");
    }

    @Override
    protected void sendCommandDesc(ICommandSender sender, String command, String desc){
        sender.sendMessage(ChatColor.AQUA+command+" "+ChatColor.DARK_GRAY+"- "+ChatColor.GRAY+desc);
    }

    @Override
    protected void sendDetails(ICommandSender sender, String detail){
        detail = ChatColor.translateAlternateColorCodes('&', detail);
        sender.sendMessage("  "+ChatColor.GRAY+detail);
    }

    @Override
    protected String getPluginDescription() {
        return bukkitPlugin.getDescription().getFullName();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void showGlowStones(ICommandSender sender, Set<Entry<SimpleLocation, Trigger>> set) {
        for (Entry<SimpleLocation, Trigger> entry : set) {
            SimpleLocation sloc = entry.getKey();
            Player player = sender.get();
            player.sendBlockChange(
                    new Location(Bukkit.getWorld(sloc.getWorld()), sloc.getX(), sloc.getY(), sloc.getZ()),
                    Material.GLOWSTONE, (byte) 0);
        }
    }

    @Override
    public void registerEvents(Manager manager) {
        if(manager instanceof Listener)
            Bukkit.getPluginManager().registerEvents((Listener) manager, this.bukkitPlugin);
    }

    @Override
    public File getDataFolder() {
        return bukkitPlugin.getDataFolder();
    }

    @Override
    public Logger getLogger() {
        return bukkitPlugin.getLogger();
    }

    @Override
    public boolean isEnabled() {
        return bukkitPlugin.isEnabled();
    }

    @Override
    public <T> T getMain() {
        return (T) bukkitPlugin;
    }

    @Override
    public boolean isConfigSet(String key) {
        return bukkitPlugin.getConfig().isSet(key);
    }

    @Override
    public void setConfig(String key, Object value) {
        bukkitPlugin.getConfig().set(key, value);
    }

    @Override
    public Object getConfig(String key) {
        return bukkitPlugin.getConfig().get(key);
    }

    @Override
    public <T> T getConfig(String key, T def) {
        return (T) bukkitPlugin.getConfig().get(key, def);
    }

    @Override
    public void saveConfig() {
        bukkitPlugin.saveConfig();
    }

    @Override
    public void reloadConfig() {
        bukkitPlugin.reloadConfig();
    }

    @Override
    public void runTask(Runnable runnable) {
        Bukkit.getScheduler().runTask(bukkitPlugin, runnable);
    }

    @Override
    public void saveAsynchronously(Manager manager) {
        bukkitPlugin.saveAsynchronously(manager);
    }

    @Override
    public void handleException(Object context, Throwable e) {
        e.printStackTrace();
        if(context instanceof PlayerEvent){
            Player player = ((PlayerEvent) context).getPlayer();
            runTask(new Runnable(){
                @Override
                public void run() {
                    Throwable ex = e;
                    player.sendMessage(ChatColor.RED+"Could not execute this trigger.");
                    while(ex != null){
                        player.sendMessage(ChatColor.RED+" >> Caused by:");
                        player.sendMessage(ChatColor.RED+ex.getMessage());
                        ex = ex.getCause();
                    }
                    player.sendMessage(ChatColor.RED+"If you are administrator, see console for details.");
                }
            });
        }
    }

    @Override
    public void handleException(ICommandSender sender, Throwable e) {
        e.printStackTrace();
        runTask(new Runnable(){
            @Override
            public void run() {
                Throwable ex = e;
                sender.sendMessage(ChatColor.RED+"Could not execute this trigger.");
                while(ex != null){
                    sender.sendMessage(ChatColor.RED+" >> Caused by:");
                    sender.sendMessage(ChatColor.RED+ex.getMessage());
                    ex = ex.getCause();
                }
                sender.sendMessage(ChatColor.RED+"If you are administrator, see console for details.");
            }
        });
    }

    @Override
    public ProcessInterrupter createInterrupter(Object e, Interpreter interpreter, Map<UUID, Long> cooldowns) {
        return new ProcessInterrupter(){
            @Override
            public boolean onNodeProcess(Node node) {
                if(interpreter.isCooldown() && e instanceof PlayerEvent){
                    Player player = ((PlayerEvent) e).getPlayer();
                    UUID uuid = player.getUniqueId();
                    cooldowns.put(uuid, interpreter.getCooldownEnd());
                }
                return false;
            }

            @Override
            public boolean onCommand(Object context, String command, Object[] args) {
                if("CALL".equals(command)){
                    if(args.length < 1)
                        throw new RuntimeException("Need parameter [String] or [String, boolean]");

                    if(args[0] instanceof String){
                        Trigger trigger = getNamedTriggerManager().getTriggerForName((String) args[0]);
                        if(trigger == null)
                            throw new RuntimeException("No trigger found for Named Trigger "+args[0]);

                        if(args.length > 1 && args[1] instanceof Boolean){
                            trigger.setSync((boolean) args[1]);
                        } else {
                            trigger.setSync(true);
                        }

                        if(trigger.isSync()){
                            trigger.activate(e, interpreter.getVars());
                        }else{//use snapshot to avoid concurrent modification
                            trigger.activate(e, new HashMap<>(interpreter.getVars()));
                        }

                        return true;
                    } else {
                        throw new RuntimeException("Parameter type not match; it should be a String."
                                + " Make sure to put double quotes, if you provided String literal.");
                    }
                } else if("CANCELEVENT".equals(command)) {
                    if(!interpreter.isSync())
                        throw new RuntimeException("CANCELEVENT is illegal in async mode!");

                    if(context instanceof Cancellable){
                        ((Cancellable) context).setCancelled(true);
                        return true;
                    } else {
                        throw new RuntimeException(context+" is not a Cancellable event!");
                    }
                }

                return false;
            }

        };
    }

    @Override
    public ProcessInterrupter createInterrupterForInv(Object e, Interpreter interpreter, Map<UUID, Long> cooldowns,
            Map<IInventory, InventoryTrigger> inventoryMap) {
        return new ProcessInterrupter() {
            @Override
            public boolean onNodeProcess(Node node) {
                if (interpreter.isCooldown()) {
                    if(e instanceof InventoryInteractEvent){
                        HumanEntity he = ((InventoryInteractEvent) e).getWhoClicked();
                        if(he instanceof Player){
                            Player player = (Player) he;
                            UUID uuid = player.getUniqueId();
                            cooldowns.put(uuid, interpreter.getCooldownEnd());
                        }
                    }
                    return false;
                }

                //safety feature to stop all trigger immediately if executing on 'open' or 'click'
                //  is still running after the inventory is closed.
                if(e instanceof InventoryOpenEvent
                        || e instanceof InventoryClickEvent){
                    Inventory inv = ((InventoryEvent) e).getInventory();

                    //it's not GUI so stop execution
                    if(!inventoryMap.containsKey(new BukkitInventory(inv)))
                        return true;
                }

                return false;
            }

            @Override
            public boolean onCommand(Object context, String command, Object[] args) {
                if("CALL".equals(command)){
                    if(args.length < 1)
                        throw new RuntimeException("Need parameter [String] or [String, boolean]");

                    if(args[0] instanceof String){
                        Trigger trigger = getNamedTriggerManager().getTriggerForName((String) args[0]);
                        if(trigger == null)
                            throw new RuntimeException("No trigger found for Named Trigger "+args[0]);

                        if(args.length > 1 && args[1] instanceof Boolean){
                            trigger.setSync((boolean) args[1]);
                        } else {
                            trigger.setSync(true);
                        }

                        if(trigger.isSync()){
                            trigger.activate(e, interpreter.getVars());
                        }else{//use snapshot to avoid concurrent modification
                            trigger.activate(e, new HashMap<>(interpreter.getVars()));
                        }

                        return true;
                    } else {
                        throw new RuntimeException("Parameter type not match; it should be a String."
                                + " Make sure to put double quotes, if you provided String literal.");
                    }
                } else if("CANCELEVENT".equals(command)) {
                    if(!interpreter.isSync())
                        throw new RuntimeException("CANCELEVENT is illegal in async mode!");

                    if(context instanceof Cancellable){
                        ((Cancellable) context).setCancelled(true);
                        return true;
                    } else {
                        throw new RuntimeException(context+" is not a Cancellable event!");
                    }
                }

                return false;
            }

        };
    }

    @Override
    public UUID extractUUIDFromContext(Object e) {
        if(e instanceof PlayerEvent){
            Player player = ((PlayerEvent) e).getPlayer();
            return player.getUniqueId();
        }else if(e instanceof InventoryInteractEvent){
            return ((InventoryInteractEvent) e).getWhoClicked().getUniqueId();
        }

        return null;
    }

    public class BungeeCordHelper implements PluginMessageListener, Runnable{
        private final String CHANNEL = "BungeeCord";

        private final String SUB_SERVERLIST = "ServerList";
        private final String SUB_USERCOUNT = "UserCount";

        private final Map<String, Integer> playerCounts = new ConcurrentHashMap<>();

        /**
         * constructor should only be called from onEnable()
         */
        private BungeeCordHelper() {
            Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(bukkitPlugin, CHANNEL);
            Bukkit.getServer().getMessenger().registerIncomingPluginChannel(bukkitPlugin, CHANNEL, this);
        }

        @Override
        public void onPluginMessageReceived(String channel, Player player, byte[] message) {
            if (!channel.equals(CHANNEL)) {
                return;
            }

            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if (subchannel.equals(SUB_SERVERLIST)) {
                String[] serverList = in.readUTF().split(", ");
                Set<String> serverListSet = Sets.newHashSet(serverList);

                for(String server : serverListSet){
                    if(!playerCounts.containsKey(server))
                        playerCounts.put(server, -1);
                }

                Set<String> deleteServer = new HashSet<>();
                for(Entry<String, Integer> entry : playerCounts.entrySet()){
                    if(!serverListSet.contains(entry.getKey()))
                        deleteServer.add(entry.getKey());
                }

                for(String delete : deleteServer){
                    playerCounts.remove(delete);
                }
            } else if(subchannel.equals(SUB_USERCOUNT)){
                String server = in.readUTF(); // Name of server, as given in the arguments
                int playercount = in.readInt();

                playerCounts.put(server, playercount);
            }
        }

        public void sendToServer(Player player, String serverName){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(bukkitPlugin, CHANNEL, out.toByteArray());
        }

        public String[] getServerNames(){
            String[] servers = playerCounts.keySet().toArray(new String[playerCounts.size()]);
            return servers;
        }

        public int getPlayerCount(String serverName){
            return playerCounts.getOrDefault(serverName, -1);
        }

        @Override
        public void run(){
            while(!Thread.interrupted()){
                Player player = Iterables.getFirst(BukkitUtil.getOnlinePlayers(), null);
                if(player == null)
                    return;

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(SUB_SERVERLIST);
                out.writeUTF("GetServers");
                player.sendPluginMessage(bukkitPlugin, SUB_SERVERLIST, out.toByteArray());

                if(!playerCounts.isEmpty()){
                    for(Entry<String, Integer> entry : playerCounts.entrySet()){
                        ByteArrayDataOutput out2 = ByteStreams.newDataOutput();
                        out2.writeUTF(SUB_USERCOUNT);
                        out2.writeUTF("PlayerCount");
                        out2.writeUTF(entry.getKey());
                        player.sendPluginMessage(bukkitPlugin, SUB_USERCOUNT, out2.toByteArray());
                    }
                }

                try {
                    Thread.sleep(5 * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //https://bukkit.org/threads/get-server-tps.143410/
    public class Lag implements Runnable {
        public int TICK_COUNT = 0;
        public long[] TICKS = new long[600];

        public double getTPS() {
            return getTPS(100);
        }

        public double getTPS(int ticks) {
            if (TICK_COUNT < ticks) {
                return 20.0D;
            }
            int target = (TICK_COUNT - 1 - ticks) % TICKS.length;
            long elapsed = System.currentTimeMillis() - TICKS[target];

            return ticks / (elapsed / 1000.0D);
        }

        public long getElapsed(int tickID) {
            if (TICK_COUNT - tickID >= TICKS.length) {
            }

            long time = TICKS[(tickID % TICKS.length)];
            return System.currentTimeMillis() - time;
        }

        @Override
        public void run() {
            TICKS[(TICK_COUNT % TICKS.length)] = System.currentTimeMillis();

            TICK_COUNT += 1;
        }
    }

    public class MysqlSupport{
        private final String KEY = "dbkey";
        private final String VALUE = "dbval";

        private final MysqlConnectionPoolDataSource ds;
        private final MiniConnectionPoolManager pool;

        private String dbName;
        private String tablename;

        private String address;

        private MysqlSupport(String address, String dbName, String tablename, String userName, String password) throws SQLException {
            this.dbName = dbName;
            this.tablename = tablename;
            this.address = address;

            ds = new MysqlConnectionPoolDataSource();
            ds.setURL("jdbc:mysql://" + address + "/" + dbName);
            ds.setUser(userName);
            ds.setPassword(password);
            ds.setCharacterEncoding("UTF-8");
            ds.setUseUnicode(true);
            ds.setAutoReconnectForPools(true);
            ds.setAutoReconnect(true);
            ds.setAutoReconnectForConnectionPools(true);

            ds.setCachePreparedStatements(true);
            ds.setCachePrepStmts(true);

            pool = new MiniConnectionPoolManager(ds, 2);

            Connection conn = createConnection();
            initTable(conn);
            conn.close();
        }

        private Connection createConnection() {
            Connection conn = null;

            try {
                conn = pool.getConnection();
            } catch (SQLException e) {
                // e.printStackTrace();
            } finally {
                if (conn == null)
                    conn = pool.getValidConnection();
            }

            return conn;
        }

        private final String CREATETABLEQUARY = "" + "CREATE TABLE IF NOT EXISTS %s (" + "" + KEY
                + " CHAR(128) PRIMARY KEY," + "" + VALUE + " MEDIUMBLOB" + ")";

        private void initTable(Connection conn) throws SQLException {
            PreparedStatement pstmt = conn.prepareStatement(String.format(CREATETABLEQUARY, tablename));
            pstmt.executeUpdate();
            pstmt.close();
        }

        public Object get(String key) throws SQLException {
            Object out = null;

            try (Connection conn = createConnection();
                    PreparedStatement pstmt = conn.prepareStatement("SELECT " + VALUE + " FROM " + tablename + " WHERE " + KEY + " = ?");) {
                pstmt.setString(1, key);
                ResultSet rs = pstmt.executeQuery();

                if(!rs.next())
                    return null;
                InputStream is = rs.getBinaryStream(VALUE);

                try(ObjectInputStream ois = new ObjectInputStream(is)){
                    out = ois.readObject();
                } catch (IOException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                    return null;
                }
            }

            return out;
        }

        public void set(String key, Serializable value) throws SQLException {
            try (Connection conn = createConnection();
                    PreparedStatement pstmt = conn.prepareStatement("REPLACE INTO " + tablename + " VALUES (?, ?)");){


                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);) {
                    oos.writeObject(value);

                    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

                    pstmt.setString(1, key);
                    pstmt.setBinaryStream(2, bais);

                    pstmt.executeUpdate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public String toString() {
            return "Mysql Connection("+address+") to [dbName=" + dbName + ", tablename=" + tablename + "]";
        }


    }

    @Override
    public <T> Future<T> callSyncMethod(Callable<T> call) {
        return Bukkit.getScheduler().callSyncMethod(bukkitPlugin, call);
    }

    @Override
    public void disablePlugin() {
        Bukkit.getPluginManager().disablePlugin(bukkitPlugin);
    }

    @Override
    public void callEvent(IEvent event) {
        Bukkit.getPluginManager().callEvent(event.get());
    }

    @Override
    protected IPlayer getPlayer(String string) {
        Player player = Bukkit.getPlayer(string);
        if(player != null)
            return new BukkitPlayer(player);
        else
            return null;
    }

    @Override
    protected Object createEmptyPlayerEvent(ICommandSender sender) {
        Object unwrapped = sender.get();

        if(unwrapped instanceof Player) {
            return new PlayerEvent((Player) unwrapped){
                @Override
                public HandlerList getHandlers() {
                    return null;
                }};
        }else if(unwrapped instanceof ConsoleCommandSender) {
            return new PlayerEvent(new DelegatedPlayer((ConsoleCommandSender) unwrapped)){
                @Override
                public HandlerList getHandlers() {
                    return null;
                }};
        }else{
            throw new RuntimeException("Cannot create empty PlayerEvent for "+sender);
        }
    }

    @Override
    protected void setItemTitle(IItemStack iS, String title) {
        ItemStack IS = iS.get();
        ItemMeta IM = IS.getItemMeta();
        IM.setDisplayName(title);
        IS.setItemMeta(IM);
    }

    @Override
    protected void addItemLore(IItemStack iS, String lore) {
        ItemStack IS = iS.get();

        ItemMeta IM = IS.getItemMeta();
        List<String> lores = IM.hasLore() ? IM.getLore() : new ArrayList<>();
        lores.add(lore);
        IM.setLore(lores);
        IS.setItemMeta(IM);
    }

    @Override
    protected boolean setLore(IItemStack iS, int index, String lore) {
        ItemStack IS = iS.get();

        ItemMeta IM = IS.getItemMeta();
        List<String> lores = IM.hasLore() ? IM.getLore() : new ArrayList<>();
        if(lore == null || index < 0 || index > lores.size() - 1)
            return false;

        lores.set(index, lore);
        IM.setLore(lores);
        IS.setItemMeta(IM);

        return true;
    }

    @Override
    protected boolean removeLore(IItemStack iS, int index) {
        ItemStack IS = iS.get();

        ItemMeta IM = IS.getItemMeta();
        List<String> lores = IM.getLore();
        if(lores == null || index < 0 || index > lores.size() - 1)
            return false;

        lores.remove(index);
        IM.setLore(lores);
        IS.setItemMeta(IM);

        return true;
    }

    @Override
    public boolean isServerThread() {
        boolean result = false;

        synchronized(this){
            result = Bukkit.isPrimaryThread();
        }

        return result;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return bukkitPlugin.onTabComplete(sender, command, alias, args);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return super.onCommand(new BukkitCommandSender(sender), command.getName(), args);
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return bukkitPlugin.getDescription();
    }

    @Override
    public FileConfiguration getConfig() {
        return bukkitPlugin.getConfig();
    }

    @Override
    public InputStream getResource(String filename) {
        return bukkitPlugin.getResource(filename);
    }

    @Override
    public void saveDefaultConfig() {
        bukkitPlugin.saveDefaultConfig();
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        bukkitPlugin.saveResource(resourcePath, replace);
    }

    @Override
    public PluginLoader getPluginLoader() {
        return bukkitPlugin.getPluginLoader();
    }

    @Override
    public Server getServer() {
        return bukkitPlugin.getServer();
    }

    @Override
    public void onDisable() {
        bukkitPlugin.onDisable();
    }

    @Override
    public void onLoad() {
        bukkitPlugin.onLoad();
    }

    @Override
    public void onEnable() {
        bukkitPlugin.onEnable();
    }

    @Override
    public boolean isNaggable() {
        return bukkitPlugin.isNaggable();
    }

    @Override
    public void setNaggable(boolean canNag) {
        bukkitPlugin.setNaggable(canNag);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return bukkitPlugin.getDefaultWorldGenerator(worldName, id);
    }

    @Override
    public String getName() {
        return bukkitPlugin.getName();
    }

    @Override
    public Map<String, AbstractAPISupport> getSharedVars() {
        return sharedVars;
    }
}
