package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;

import java.util.List;

import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;

/** Provides immutable arena geometry, spawn positions, and the obstacles used by the simulation. */
public final class ArenaLayout {

    private static final ArenaLayout OPEN = new ArenaLayout("Open", List.of());
    private static final ArenaLayout CENTRAL_COVER = new ArenaLayout(
            "Central cover",
            List.of(new ArenaRect(INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_HEIGHT * 0.5F, 280.0F, 80.0F)));

    private final String displayName;
    private final List<ArenaRect> obstacles;

    private ArenaLayout(String displayName, List<ArenaRect> obstacles) {
        this.displayName = displayName;
        this.obstacles = List.copyOf(obstacles);
    }

    public static ArenaLayout open() {
        return OPEN;
    }

    public static ArenaLayout centralCover() {
        return CENTRAL_COVER;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<ArenaRect> getObstacles() {
        return obstacles;
    }

    public boolean blocksCircle(float x, float y, float radius) {
        return obstacles.stream().anyMatch(rect -> rect.containsCircle(x, y, radius));
    }

    /** Pushes a player to the nearest safe side when movement enters a cover rectangle. */
    public void resolvePlayer(PlayerActor player) {
        final float radius = player.getHalfBodySize();
        for (ArenaRect obstacle : obstacles) {
            if (!obstacle.containsCircle(player.getxPosition(), player.getyPosition(), radius)) continue;

            final float pushLeft = player.getxPosition() - (obstacle.left() - radius);
            final float pushRight = (obstacle.right() + radius) - player.getxPosition();
            final float pushTop = player.getyPosition() - (obstacle.top() - radius);
            final float pushBottom = (obstacle.bottom() + radius) - player.getyPosition();
            final float smallestPush = Math.min(Math.min(pushLeft, pushRight), Math.min(pushTop, pushBottom));
            if (smallestPush == pushLeft) {
                player.setxPosition(obstacle.left() - radius);
                player.setxVelocity(-Math.abs(player.getxVelocity()));
            } else if (smallestPush == pushRight) {
                player.setxPosition(obstacle.right() + radius);
                player.setxVelocity(Math.abs(player.getxVelocity()));
            } else if (smallestPush == pushTop) {
                player.setyPosition(obstacle.top() - radius);
                player.setyVelocity(-Math.abs(player.getyVelocity()));
            } else {
                player.setyPosition(obstacle.bottom() + radius);
                player.setyVelocity(Math.abs(player.getyVelocity()));
            }
        }
    }

    /** Renders cover geometry in the same fixed coordinate space as players and arrows. */
    public void display(App app) {
        if (obstacles.isEmpty()) return;
        app.pushStyle();
        app.rectMode(processing.core.PConstants.CENTER);
        app.fill(64, 176);
        app.stroke(0, 128);
        for (ArenaRect obstacle : obstacles) {
            app.rect(obstacle.centerX(), obstacle.centerY(), obstacle.width(), obstacle.height());
        }
        app.popStyle();
    }
}
