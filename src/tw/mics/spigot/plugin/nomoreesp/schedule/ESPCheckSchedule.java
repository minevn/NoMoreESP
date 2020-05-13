package tw.mics.spigot.plugin.nomoreesp.schedule;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_12_R1.AxisAlignedBB;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.WorldServer;
import net.minefs.MineStrike.Main;
import net.minefs.MineStrike.Handler.GameState;
import net.minefs.MineStrike.Handler.GameTeam;
import net.minefs.MineStrike.Modes.Competitive;
import net.minefs.MineStrike.Modes.Game;
import net.minefs.MineStrike.Modes.ZombieEscape;
import tw.mics.spigot.plugin.nomoreesp.Config;
import tw.mics.spigot.plugin.nomoreesp.EntityHider;
import tw.mics.spigot.plugin.nomoreesp.NoMoreESP;

public class ESPCheckSchedule {
	NoMoreESP plugin;
	// Runnable runnable;
	EntityHider hider;
	// int schedule_id;
	boolean running;

	// CONSTANT
	int PLAYER_TRACKING_RANGE = Config.CHECK_DISTANCE.getInt();

	final double DONT_HIDE_RANGE = 1.5;
	final double VECTOR_LENGTH = 0.5;
	final double HIDE_DEGREES = 70;
	long lastWarning = 0;
	private Game game;
	private Player player;
	private Main ms;

	public ESPCheckSchedule(NoMoreESP i, Game game, Player player) {
		plugin = i;
		hider = NoMoreESP.hider;
		this.game = game;
		this.player = player;
		ms = Main.getInstance();
		setupRunnable();
		plugin.getLogger().info("ESP scanner for player " + player.getName() + " in game " + game.getID() + " set");
	}

	private void setupRunnable() {
		running = true;
		new Thread() {
			@Override
			public void run() {
				while (true) {
					long startTime = System.currentTimeMillis();
					synchronized (this) {
						if (!running)
							return;
					}
					try {
						checkHide();
					} catch (Exception e) {
						// plugin.getLogger().warning(e.getMessage());
						// e.printStackTrace();
					}
					long took = System.currentTimeMillis() - startTime;
					long pauseTime = 7 - took;
					if (pauseTime <= 0) {
//						if (System.currentTimeMillis() - lastWarning >= 15000) {
//							plugin.getLogger().warning("Took too long to scan: " + took + "ms");
//							plugin.getLogger().warning("Game " + game.getID() + ", size: " + game.getPlayers().size());
//						}
						continue;
					}
					try {
						Thread.sleep(pauseTime, 812500);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}
			}
		}.start();
		// runnable = new Runnable() {
		// public void run() {
		// checkHide();
		// }
		// };
		// schedule_id =
		// this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin,
		// runnable, 0, 1)
		// .getTaskId();
	}

	protected void checkHide() {
		if (game.getState() == GameState.WAITING)
			return;
//		gameScan(game.getTeamA(), game.getTeamB());
//		gameScan(game.getTeamB(), game.getTeamA());
		playerScan();
	}

	public void playerScan() {
		if (!player.isOnline()) {
			stop();
			return;
		}
		if (player.hasPermission("ms.walldebug"))
			return;
		List<Player> targets = null;
		GameTeam opposite = null;
		if (!game.isZombieGame()) {
			if (ms.getManager().getTeam(game, player) == game.getTeamA())
				opposite = game.getTeamB();
			else
				opposite = game.getTeamA();
			if (opposite == null)
				return;
			targets = opposite.getPlayers();
		} else
			targets = game.getPlayers();
		boolean force = (game.getSpectators().contains(player) && !(game instanceof Competitive))
				|| game.getState() == GameState.END;
		for (Player target : targets)
			if (target != player && target.getWorld() == player.getWorld())
				checkLookable(player, target,
						force && (!game.isZombieGame() || ms.getManager().sameTeam(game, player, target)), game);
	}

	public void stop() {
		running = false;
		plugin.getLogger().info("Stopped ESP scanner for player " + player.getName() + " in game " + game.getID());
	}

	public void gameScan(GameTeam team, GameTeam oppositeTeam) {
		if (team == null || oppositeTeam == null)
			return;
		for (Player player : team.getPlayers()) {
			if (player.hasPermission("ms.walldebug"))
				continue;
			boolean force = (game.getSpectators().contains(player) && !(game instanceof Competitive))
					|| game.getState() == GameState.END || (game.isZombieGame() && game.isRoundEnding());
			for (Player target : oppositeTeam.getPlayers())
				if (target.getWorld() == player.getWorld())
					checkLookable(player, target, force, game);
		}
	}

	private void checkLookable(Player player, Player target, boolean force, Game game) {
		Location loc = player.getEyeLocation().clone().add(0, 1.125, 0); // 1.625 - 0.5
		Location target_loc = target.getEyeLocation().clone().add(0, 0.5, 0); // 1 - 0.5
		double width = 0.48;
		Location targetAA = target.getLocation().clone().add(-width, 0, -width);
		Location targetBB = target.getLocation().clone().add(width, 1.9, width);
		Location targetCC = target.getLocation().clone().add(0, 1.1, 0);
		// int d = (int) player.getLocation().distance(target.getLocation());
		double distance = loc.distance(target_loc);
		if (distance > PLAYER_TRACKING_RANGE)
			return;
		if (distance < 1.5 && player.getGameMode() != GameMode.SPECTATOR && game instanceof ZombieEscape
				&& game.getMain().getManager().sameTeam(game, player, target)) {
			hider.hideEntity(player, target);
			return;
		}
		if (distance < DONT_HIDE_RANGE || force || target.isGlowing()) {
			hider.showEntity(player, target);
			return;
		}

		Vector vector1 = target_loc.clone().subtract(loc).toVector();
		vector1.multiply(1 / vector1.length());

		target_loc.setY(loc.getY());

		Vector A = vector1.clone();
		Vector B = loc.getDirection();
		double degrees = Math.toDegrees(Math.acos(A.dot(B) / (A.length() * B.length())));
		if (degrees > HIDE_DEGREES) {
			hider.hideEntity(player, target);
			return;
		}

		// if (checked && !(game instanceof DefuseGame)) {
		// hider.showEntity(player, target);
		// return;
		// }

		loc.add(vector1.clone().multiply(DONT_HIDE_RANGE / VECTOR_LENGTH));

		vector1.multiply(VECTOR_LENGTH);
		try {
			if (getTargetBlock(lookAt(player.getEyeLocation(), targetAA), target.getLocation(), game) != null
					&& getTargetBlock(lookAt(player.getEyeLocation(), targetBB), target.getLocation(), game) != null
					&& getTargetBlock(lookAt(player.getEyeLocation(), targetCC), target.getLocation(), game) != null) {
				hider.hideEntity(player, target);
				return;
			}
		} catch (Exception e) {
			plugin.getLogger().warning("HIDE ERROR");
			e.printStackTrace();
		}
		hider.showEntity(player, target);
	}

	// public void removeRunnablez() {
	// this.plugin.getServer().getScheduler().cancelTask(schedule_id);
	// }

	public EntityHider getHider() {
		return hider;
	}

	public Location lookAt(Location loc, Location lookat) {
		double dx = lookat.getX() - loc.getX();
		double dy = lookat.getY() - loc.getY();
		double dz = lookat.getZ() - loc.getZ();

		double dxz = Math.sqrt(dx * dx + dz * dz);
		double pitch = Math.atan(dy / dxz);
		double yaw = 0;

		if (dx != 0) {
			if (dx < 0) {
				yaw = 1.5 * Math.PI;
			} else {
				yaw = 0.5 * Math.PI;
			}
			yaw -= Math.atan(dz / dx);
		} else if (dz < 0) {
			yaw = Math.PI;
		}

		loc.setYaw((float) Math.toDegrees(-yaw));
		loc.setPitch((float) Math.toDegrees(-pitch));
		return loc;
	}

	public Block getTargetBlock(Location direction, Location l, Game game) {
		direction = direction.clone();
		double radians = Math.toRadians(direction.getYaw() + 90.0);
		double radians2 = Math.toRadians(direction.getPitch() + 90.0f);
		double n2 = Math.sin(radians2) * Math.cos(radians);
		double cos = Math.cos(radians2);
		double z2 = Math.sin(radians2) * Math.sin(radians);
		double x = direction.getX();
		double y = direction.getY();
		double z = direction.getZ();
		double n = 0.1, d = direction.distance(l);
		while (d - n > 0.1) {
//			AsyncWorld w = AsyncWorld.wrap(direction.getWorld());
			World w = direction.getWorld();
			Block block = w.getBlockAt(direction);
			Material m = block.getType();
			if ((!block.isEmpty() && !block.isLiquid() && hasIntersection(block, direction.toVector())
					&& !m.isTransparent() && m != Material.BARRIER && !m.name().contains("FENCE")
					&& m != Material.TRAP_DOOR && m != Material.IRON_TRAPDOOR)
					|| (m == Material.CROPS && !game.isZombieGame()))
				return block;
			direction.setX(x + n * n2);
			direction.setY(y + n * cos);
			direction.setZ(z + n * z2);
			n += 0.1;
		}
		return null;
	}

	public boolean hasIntersection(Block b, Vector position) {
		BlockPosition pos = new BlockPosition(b.getX(), b.getY(), b.getZ());
		WorldServer world = ((CraftWorld) b.getWorld()).getHandle();
		AxisAlignedBB box = world.getType(pos).d(world, pos);
		if (box == null)
			return false;
		Vector min = new Vector(pos.getX() + box.a, pos.getY() + box.b, pos.getZ() + box.c);
		Vector max = new Vector(pos.getX() + box.d, pos.getY() + box.e, pos.getZ() + box.f);
		if (position.getX() < min.getX() || position.getX() > max.getX())
			return false;
		if (position.getY() < min.getY() || position.getY() > max.getY())
			return false;
		if (position.getZ() < min.getZ() || position.getZ() > max.getZ())
			return false;
		return true;
	}
}
