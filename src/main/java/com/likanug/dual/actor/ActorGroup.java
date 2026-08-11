package com.likanug.dual.actor;

import com.likanug.dual.actor.arrow.AbstractArrowActor;
import com.likanug.dual.actor.player.AbstractPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;

import java.util.ArrayList;

public class ActorGroup {
    private ActorGroup enemyGroup;

    private AbstractPlayerActor player;
    private final ArrayList<AbstractArrowActor> arrowList = new ArrayList<>();
    private final ArrayList<AbstractArrowActor> removingArrowList = new ArrayList<>();

    public void update() {
        player.update();

        if (!removingArrowList.isEmpty()) {
            arrowList.removeAll(removingArrowList);
            removingArrowList.clear();
        }

        for (AbstractArrowActor eachArrow : arrowList) {
            eachArrow.update();
        }
    }

    public void act() {
        player.act();
        for (AbstractArrowActor eachArrow : arrowList) {
            eachArrow.act();
        }
    }

    public void setPlayer(PlayerActor newPlayer) {
        player = newPlayer;
        newPlayer.group = this;
    }

    public void addArrow(AbstractArrowActor newArrow) {
        arrowList.add(newArrow);
        newArrow.group = this;
    }

    public void displayPlayer() {
        player.display();
    }

    public void displayArrows() {
        for (AbstractArrowActor eachArrow : arrowList) {
            // Collision marks arrows before hit-stop; hiding marked arrows keeps the frozen impact frame truthful.
            if (removingArrowList.contains(eachArrow)) continue;
            eachArrow.display();
        }
    }

    public ActorGroup getEnemyGroup() {
        return enemyGroup;
    }

    public void setEnemyGroup(ActorGroup enemyGroup) {
        this.enemyGroup = enemyGroup;
    }

    public AbstractPlayerActor getPlayer() {
        return player;
    }

    public void setPlayer(AbstractPlayerActor player) {
        this.player = player;
        player.group = this;
    }

    public ArrayList<AbstractArrowActor> getArrowList() {
        return arrowList;
    }

    public ArrayList<AbstractArrowActor> getRemovingArrowList() {
        return removingArrowList;
    }

    /** Removes every arrow and pending removal marker so the next round starts with a clean arena. */
    public void clearArrows() {
        arrowList.clear();
        removingArrowList.clear();
    }
}
