package com.likanug.dual.playerEngine;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

public class JabPlayerPlan extends DefaultPlayerPlan {
    public JabPlayerPlan(App app) {
        super(app);
    }

    public void execute(PlayerActor player, AbstractInputDevice input) {
        super.execute(player, input);
        // Release the AI trigger while the shared reserve is empty so recovery can create a new press edge.
        input.operateShotButton(player.getShortbowAmmo().canFire());
    }

}
