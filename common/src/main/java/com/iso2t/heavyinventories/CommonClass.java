package com.iso2t.heavyinventories;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ConfigOptions;

public class CommonClass {

    public static void init() {
        PlayerHolder.setWeightStarting(ConfigOptions.PLAYER_STARTING_WEIGHT);
    }

}