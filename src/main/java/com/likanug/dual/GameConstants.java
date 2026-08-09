package com.likanug.dual;

public final class GameConstants {

    private GameConstants() {}

    // --- 玩家 ---
    public static final float PLAYER_BODY_SIZE = 32.0F;
    public static final float PLAYER_COLLISION_RADIUS = 16.0F;
    public static final float PLAYER_MAX_VX = 10.0F;
    public static final float PLAYER_MAX_VY = 7.0F;
    public static final float PLAYER_FRICTION = 0.92F;
    public static final float PLAYER_BOUNCE = 0.5F;
    public static final float PLAYER_THRUST_SPEED = 8.0F;
    public static final float DAMAGED_DURATION_SEC = 0.75F;
    public static final int DAMAGED_RING_SIZE = 64;
    public static final float DAMAGED_RING_STROKE = 4.0F;
    public static final int DAMAGED_END_FEEDBACK_FRAMES = 8;

    // --- 短弓箭 ---
    public static final float SHORTBOW_ARROW_HALF_LENGTH = 8.0F;
    public static final float SHORTBOW_ARROW_HALF_BODY = 20.0F;
    public static final float SHORTBOW_TERMINAL_SPEED = 8.0F;
    public static final float SHORTBOW_HEAD_HALF_LENGTH = 8.0F;
    public static final float SHORTBOW_HEAD_HALF_WIDTH = 4.0F;
    public static final float SHORTBOW_FEATHER_HALF_WIDTH = 4.0F;
    public static final float SHORTBOW_FEATHER_LENGTH = 8.0F;
    public static final int SHORTBOW_MAX_AMMO = 3;
    public static final float SHORTBOW_AMMO_RECOVERY_SEC = 1.0F;
    public static final float SHORTBOW_FIRE_INTERVAL_SEC = 0.2F;

    // --- 长弓箭 ---
    public static final float LONGBOW_SPEED = 64.0F;
    public static final float LONGBOW_COMPONENT_INTERVAL = 24.0F;
    public static final int LONGBOW_SHAFT_COUNT = 5;
    public static final float LONGBOW_CHARGE_SEC = 0.5F;
    public static final float LONGBOW_CHARGE_MOVE_RATIO = 0.5F;
    public static final float LONGBOW_AIM_SPEED_RATIO = 0.1F;
    public static final float LONGBOW_AUTO_AIM_RANGE = 520.0F;
    public static final int LONGBOW_RING_SIZE = 80;
    public static final float LONGBOW_RING_STROKE = 5.0F;
    public static final int TACTICAL_OPENING_WINDOW_FRAMES = 90;
    public static final int MATCH_ROUNDS_TO_WIN = 3;

    // --- 竞技场背景 ---
    public static final int ARENA_BACKGROUND_COLOR = 96;
    public static final int ARENA_GRID_COLOR = 112;
    public static final int ARENA_GRID_LINES_PER_AXIS = 6;
    public static final float ARENA_GRID_MAX_ACCELERATION = 0.025F;

    // --- 粒子 ---
    public static final float PARTICLE_FRICTION = 0.98F;
    public static final float PARTICLE_SQUARE_ROT_SPEED = 1.5F;
    public static final float PARTICLE_LINE_LENGTH = 800.0F;

    // --- 游戏事件 ---
    public static final int KILL_PARTICLE_COUNT = 50;
    public static final int KILL_PARTICLE_SIZE = 16;
    public static final int ARROW_BREAK_PARTICLE_COUNT = 10;
    public static final int ARROW_BREAK_PARTICLE_SIZE = 7;
    public static final int INTERCEPT_PARTICLE_COUNT = 12;
    public static final int INTERCEPT_PARTICLE_SIZE = 5;
    public static final int INTERCEPT_RING_SIZE = 48;
    public static final float INTERCEPT_RING_STROKE = 3.0F;
    public static final int SCREEN_SHAKE_ON_KILL = 50;
    public static final int SCREEN_SHAKE_ON_HIT = 10;
}
