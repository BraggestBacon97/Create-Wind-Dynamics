package dev.braggestbacon97.windynamics.world;

import net.minecraft.world.phys.Vec2;

public record WindSample(
        Vec2 direction,
        float speed,
        float pressure,
        float turbulence,
        WeatherState weather
) {
    public static final WindSample ZERO = new WindSample(new Vec2(0.0f, 1.0f), 0.0f, 1.0f, 0.0f, WeatherState.CLEAR);

    public boolean isCalm() {
        return speed <= 0.001f;
    }
}
