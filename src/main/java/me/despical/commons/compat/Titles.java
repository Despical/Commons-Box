/*
 * Commons - Box of common utilities.
 * Copyright (C) 2021 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.commons.compat;

import me.despical.commons.ReflectionUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A reflection API for titles in Minecraft.
 * Fully optimized - Supports 1.8.8 and above.
 * Requires ReflectionUtils.
 * Messages are not colorized by default.
 * <p>
 * Titles are text messages that appear in the
 * middle of the players screen: https://minecraft.gamepedia.com/Commands/title
 * PacketPlayOutTitle: https://wiki.vg/Protocol#Title
 *
 * @author Despical
 * @version 1.2.1
 * @see <a href="https://github.com/CryptoMorin/XSeries">Original Code</a>
 * @see ReflectionUtils
 */
public class Titles {

	/**
	 * Check if the server is running on 1.11 or higher.
	 * Since in 1.11 you can change the timings.
	 */
	private static final boolean SUPPORTED_API = Material.getMaterial("OBSERVER") != null;

	/**
	 * EnumTitleAction
	 * Used for the fade in, stay and fade out feature of titles.
	 */
	private static final Object TIMES, TITLE, SUBTITLE, CLEAR;

	/**
	 * PacketPlayOutTitle Types: TITLE, SUBTITLE, ACTIONBAR, TIMES, CLEAR, RESET;
	 */
	private static final MethodHandle PACKET;

	/**
	 * ChatComponentText JSON message builder.
	 */
	private static final MethodHandle CHAT_COMPONENT_TEXT;

	static {
		MethodHandle packetCtor = null, chatComp = null;

		Object times = null, title = null, subtitle = null, clear = null;

		if (!SUPPORTED_API) {
			Class<?> chatComponentText = ReflectionUtils.getNMSClass("ChatComponentText");
			Class<?> packet = ReflectionUtils.getNMSClass("PacketPlayOutTitle");
			Class<?> titleTypes = packet.getDeclaredClasses()[0];

			for (Object type : titleTypes.getEnumConstants()) {
				switch (type.toString()) {
					case "TIMES":
						times = type;
						break;
					case "TITLE":
						title = type;
						break;
					case "SUBTITLE":
						subtitle = type;
						break;
					case "CLEAR":
						clear = type;
				}
			}

			MethodHandles.Lookup lookup = MethodHandles.lookup();
			try {
				chatComp = lookup.findConstructor(chatComponentText, MethodType.methodType(void.class, String.class));

				packetCtor = lookup.findConstructor(packet, MethodType.methodType(void.class, titleTypes, ReflectionUtils.getNMSClass("IChatBaseComponent"), int.class, int.class, int.class));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		TITLE = title;
		SUBTITLE = subtitle;
		TIMES = times;
		CLEAR = clear;
		PACKET = packetCtor;
		CHAT_COMPONENT_TEXT = chatComp;
	}

	private Titles() {
	}

	/**
	 * Sends a title message with title and subtitle to a player.
	 *
	 * @param player   the player to send the title to.
	 * @param fadeIn   the amount of ticks for title to fade in.
	 * @param stay     the amount of ticks for the title to stay.
	 * @param fadeOut  the amount of ticks for the title to fade out.
	 * @param title    the title message.
	 * @param subtitle the subtitle message.
	 * @see #clearTitle(Player)
	 * @since 1.2.1
	 */
	public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		if (player == null) return;
		if (title == null && subtitle == null) return;

		if (SUPPORTED_API) {
			player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
			return;
		}

		try {
			Object timesPacket = PACKET.invoke(TIMES, CHAT_COMPONENT_TEXT.invoke(title), fadeIn, stay, fadeOut);
			ReflectionUtils.sendPacket(player, timesPacket);

			if (title != null) {
				Object titlePacket = PACKET.invoke(TITLE, CHAT_COMPONENT_TEXT.invoke(title), fadeIn, stay, fadeOut);
				ReflectionUtils.sendPacket(player, titlePacket);
			}

			if (subtitle != null) {
				Object subtitlePacket = PACKET.invoke(SUBTITLE, CHAT_COMPONENT_TEXT.invoke(subtitle), fadeIn, stay, fadeOut);
				ReflectionUtils.sendPacket(player, subtitlePacket);
			}
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}

	/**
	 * Sends a title message with title and subtitle with normal
	 * fade in, stay and fade out time to a player.
	 *
	 * @param player   the player to send the title to.
	 * @param title    the title message.
	 * @param subtitle the subtitle message.
	 * @see #sendTitle(Player, String, String, int, int, int)
	 * @since 1.2.1
	 */
	public static void sendTitle(Player player, String title, String subtitle) {
		sendTitle(player, title, subtitle, 10, 20, 10);
	}

	/**
	 * Clears the title and subtitle message from the player's screen.
	 *
	 * @param player the player to clear the title from.
	 * @since 1.2.1
	 */
	public static void clearTitle(Player player) {
		if (player == null) return;

		if (SUPPORTED_API) {
			player.resetTitle();
			return;
		}

		Object clearPacket;

		try {
			clearPacket = PACKET.invoke(CLEAR, null, -1, -1, -1);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			return;
		}

		ReflectionUtils.sendPacket(player, clearPacket);
	}
}