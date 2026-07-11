package dev.braggestbacon97.windynamics.world;

import dev.braggestbacon97.windynamics.WindDynamicsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class WindSavedData extends SavedData {
    public static final String ID = WindDynamicsMod.MOD_ID + "_wind_map";

    private long worldSeed;
    private final Map<Long, WindCell> cachedCells = new HashMap<>();

    public WindSavedData() {}

    public static WindSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WindSavedData data = new WindSavedData();
        data.worldSeed = tag.getLong("WorldSeed");
        return data;
    }

    public static WindSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WindSavedData::new, WindSavedData::load, null),
                ID
        );
    }

    public void setWorldSeed(long worldSeed) {
        if (this.worldSeed != worldSeed) {
            this.worldSeed = worldSeed;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("WorldSeed", worldSeed);
        return tag;
    }

    public WindSample sample(ServerLevel level, BlockPos pos) {
        setWorldSeed(level.getSeed());

        WeatherState weather = WeatherState.CLEAR;
        if (level.isThundering()) {
            weather = WeatherState.THUNDER;
        } else if (level.isRaining() || level.isRainingAt(pos.above())) {
            weather = WeatherState.RAIN;
        }

        final int cellSize = 32;
        final int cellX = Math.floorDiv(pos.getX(), cellSize);
        final int cellZ = Math.floorDiv(pos.getZ(), cellSize);

        WindCell c00 = cell(cellX, cellZ);
        WindCell c10 = cell(cellX + 1, cellZ);
        WindCell c01 = cell(cellX, cellZ + 1);
        WindCell c11 = cell(cellX + 1, cellZ + 1);

        float fx = (pos.getX() - cellX * cellSize) / (float) cellSize;
        float fz = (pos.getZ() - cellZ * cellSize) / (float) cellSize;
        fx = smooth(fx);
        fz = smooth(fz);

        float angleA = lerpAngle(c00.baseAngleDeg(), c10.baseAngleDeg(), fx);
        float angleB = lerpAngle(c01.baseAngleDeg(), c11.baseAngleDeg(), fx);
        float angle = lerpAngle(angleA, angleB, fz);

        float speedA = Mth.lerp(fx, c00.baseSpeed(), c10.baseSpeed());
        float speedB = Mth.lerp(fx, c01.baseSpeed(), c11.baseSpeed());
        float speed = Mth.lerp(fz, speedA, speedB);

        float turbulenceA = Mth.lerp(fx, c00.turbulence(), c10.turbulence());
        float turbulenceB = Mth.lerp(fx, c01.turbulence(), c11.turbulence());
        float turbulence = Mth.lerp(fz, turbulenceA, turbulenceB);

        float gustA = Mth.lerp(fx, c00.gustStrength(), c10.gustStrength());
        float gustB = Mth.lerp(fx, c01.gustStrength(), c11.gustStrength());
        float gust = Mth.lerp(fz, gustA, gustB);

        float phaseA = Mth.lerp(fx, c00.gustPhase(), c10.gustPhase());
        float phaseB = Mth.lerp(fx, c01.gustPhase(), c11.gustPhase());
        float phase = Mth.lerp(fz, phaseA, phaseB);

        float weatherSpeedMul;
        float weatherTurbMul;
        float weatherShift;
        switch (weather) {
            case THUNDER -> {
                weatherSpeedMul = 1.45f;
                weatherTurbMul = 1.8f;
                weatherShift = 36.0f;
            }
            case RAIN -> {
                weatherSpeedMul = 1.18f;
                weatherTurbMul = 1.35f;
                weatherShift = 14.0f;
            }
            default -> {
                weatherSpeedMul = 0.92f;
                weatherTurbMul = 0.85f;
                weatherShift = 0.0f;
            }
        }

        float time = (level.getGameTime() + phase) * 0.02f;
        float gustWave = 0.65f + 0.35f * Mth.sin(time);
        float gustWave2 = 0.8f + 0.2f * Mth.sin(time * 0.37f + phase * 0.17f);

        speed = Mth.clamp(speed * weatherSpeedMul * gustWave * (1.0f + gust), 0.0f, 3.5f);
        turbulence = Mth.clamp(turbulence * weatherTurbMul * gustWave2, 0.0f, 1.0f);
        angle = wrapDegrees(angle + weatherShift * turbulence * Mth.sin(time * 0.11f));

        float radians = (float) Math.toRadians(angle);
        float dx = Mth.cos(radians);
        float dz = Mth.sin(radians);

        float pressure = Mth.clamp(1.0f + speed * 0.05f - turbulence * 0.1f, 0.6f, 1.5f);
        return new WindSample(new net.minecraft.world.phys.Vec2(dx, dz), speed, pressure, turbulence, weather);
    }

    private WindCell cell(int cellX, int cellZ) {
        long key = key(cellX, cellZ);
        return cachedCells.computeIfAbsent(key, ignored -> createCell(cellX, cellZ));
    }

    private WindCell createCell(int cellX, int cellZ) {
        long seed = mix(worldSeed, cellX, cellZ);
        float angle = (float) (hash01(seed ^ 0xA53FL) * 360.0);
        float speed = 0.15f + (float) hash01(seed ^ 0xB21DL) * 2.8f;
        float gustPhase = (float) (hash01(seed ^ 0xC77DL) * 6283.1853);
        float gustStrength = (float) hash01(seed ^ 0xD00DL) * 0.45f;
        float turbulence = 0.05f + (float) hash01(seed ^ 0xE11EL) * 0.35f;
        return new WindCell(angle, speed, gustPhase, gustStrength, turbulence);
    }

    private static long key(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private static long mix(long seed, int x, int z) {
        long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }

    private static double hash01(long value) {
        value ^= (value >>> 33);
        value *= 0xff51afd7ed558ccdL;
        value ^= (value >>> 33);
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= (value >>> 33);
        return (value & 0x1fffffffffffffL) / (double) 0x1fffffffffffffL;
    }

    private static float smooth(float v) {
        return v * v * (3.0f - 2.0f * v);
    }

    private static float lerpAngle(float a, float b, float t) {
        float delta = wrapDegrees(b - a);
        return a + delta * t;
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0f;
        if (degrees >= 180.0f) {
            degrees -= 360.0f;
        }
        if (degrees < -180.0f) {
            degrees += 360.0f;
        }
        return degrees;
    }
}
