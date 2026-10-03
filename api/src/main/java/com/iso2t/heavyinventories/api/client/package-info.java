/**
 * Physical-client-only integration contracts. Register through a separate client entry class.
 * Weight queries read synchronized data; they cannot change server gameplay state.
 * Render contexts and drawing services are valid only during the current HUD extraction callback.
 */
package com.iso2t.heavyinventories.api.client;
