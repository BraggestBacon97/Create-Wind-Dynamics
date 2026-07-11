package dev.braggestbacon97.windynamics.world;

public record WindCell(
        float baseAngleDeg,
        float baseSpeed,
        float gustPhase,
        float gustStrength,
        float turbulence
) {}
