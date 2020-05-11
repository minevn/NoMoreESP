package tw.mics.spigot.plugin.nomoreesp;

import org.bukkit.plugin.java.JavaPlugin;

import net.minefs.MineStrike.Main;

public class NoMoreESP extends JavaPlugin {
	private static NoMoreESP INSTANCE;
	public static EntityHider hider;
	private Main ms;

	@Override
	public void onEnable() {
		ms = (Main) getServer().getPluginManager().getPlugin("MineStrike");
		INSTANCE = this;
		Config.load();

		hider = new EntityHider(this);
		if (Config.SEND_FAKE_HEALTH.getBoolean()) {
			new HealthHider(this);
		}
	}

	public void log(String str, Object... args) {
		String message = String.format(str, args);
		getLogger().info(message);
	}

	public static NoMoreESP getInstance() {
		return INSTANCE;
	}

	public Main getMSInstance() {
		return ms;
	}
}
