package com.iso2t.heavyinventories.platform.services;

/**
 * Platform abstraction for opening config screens.
 * Fabric and NeoForge supply client-side implementations.
 */
public interface IConfigScreenHelper {

	/**
	 * Opens the client config screen on the client side.
	 * This should only be called from the client.
	 */
	void openClientConfig ();

	/**
	 * Opens the server config screen on the client side.
	 * This should only be called from the client.
	 */
	void openServerConfig ();

	/**
	 * Opens the common config screen on the client side.
	 * This should only be called from the client.
	 */
	void openCommonConfig ();

	boolean isClientSide ();

	/**
	 * Sends a packet to the client to open the specified config screen.
	 *
	 * @param playerId   the player to send the packet to
	 * @param configType the type of config to open ("client", "server", or "common")
	 */
	void sendOpenConfigPacket (Object playerId, String configType);

	IConfigScreenHelper NO_OP = new IConfigScreenHelper() {
		@Override
		public void openClientConfig () {
		}

		@Override
		public void openServerConfig () {
		}

		@Override
		public void openCommonConfig () {
		}

		@Override
		public boolean isClientSide () {
			return false;
		}

		@Override
		public void sendOpenConfigPacket (Object playerId, String configType) {
		}
	};
}

