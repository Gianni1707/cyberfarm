package com.cyberfarm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * The formed reactor's and turbine's skin: a strip of 16-pixel tiles (texgen.py, machine_shell), one on every block of a facet, so
 * it is as sharp as the machines around. Row 0 is the tower's bottom, the turbine's front; column 0 and the last are the facet's edges.
 */
final class ShellTiles {
    private static final int BODY = 0, BODY_2 = 1, BAND = 2, BASE = 3, RIM = 4, TRIMMED = 5;
    private static final float TILES = 11.0F;
    /** A white texel, for flat colours. */
    static final float WHITE_U = 10.5F / TILES, WHITE_V = 0.5F;

    private ShellTiles() {}

    /** The tile for this place: a plinth, a rim, a band of light every third row, riveted plates between; steel trims on the edges. */
    private static int tile(int row, int rows, int column, int columns) {
        int tile;
        if (row == 0) {
            tile = BASE;
        } else if (row == rows - 1) {
            tile = RIM;
        } else if (row % 3 == 0 && row != rows - 2) {
            tile = BAND;
        } else {
            tile = (row + column) % 2 != 0 ? BODY_2 : BODY;
        }
        return column == 0 || column == columns - 1 ? tile + TRIMMED : tile;
    }

    /** Whether this place has anything in the glow texture. */
    static boolean glows(int row, int rows, int column, int columns) {
        int tile = tile(row, rows, column, columns) % TRIMMED;
        return tile == BAND || tile == RIM;
    }

    /** The u at one side of a place, {@code side} 0 at the column's first side, 1 at its other; the last column is mirrored so its trim is outside. */
    static float u(int row, int rows, int column, int columns, int side) {
        boolean mirrored = column == columns - 1 && columns > 1;
        return (tile(row, rows, column, columns) + (mirrored ? 1 - side : side)) / TILES;
    }

    /** The u across a plain riveted plate, for a flat face laid out on a grid. */
    static float plateU(int column, int row, float across) {
        return (((column + row) & 1) + across) / TILES;
    }

    /**
     * A plate standing out of a face: centred on (x, y, z), facing (nx, ny, nz), its texture's up along (ax, ay, az), both unit and
     * square to each other; its edges take the texture's outermost row, so they look like its frame.
     */
    static void raisedPlate(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float nx, float ny, float nz, float ax, float ay, float az,
            float half, float height, int light) {
        float bx = ny * az - nz * ay, by = nz * ax - nx * az, bz = nx * ay - ny * ax;
        float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (float[] c : corners) {
            corner(pose, buffer, x + (bx * c[0] + ax * c[1]) * half + nx * height, y + (by * c[0] + ay * c[1]) * half + ny * height,
                    z + (bz * c[0] + az * c[1]) * half + nz * height, c[2], c[3], nx, ny, nz, light);
        }
        for (int i = 0; i < 4; i++) {
            float[] p = corners[i];
            float[] q = corners[(i + 1) % 4];
            float sx = (bx * (p[0] + q[0]) + ax * (p[1] + q[1])) / 2.0F, sy = (by * (p[0] + q[0]) + ay * (p[1] + q[1])) / 2.0F, sz = (bz * (p[0] + q[0]) + az * (p[1] + q[1])) / 2.0F;
            for (float[] step : new float[][] {{p[0], p[1], 0.0F}, {q[0], q[1], 0.0F}, {q[0], q[1], height}, {p[0], p[1], height}}) {
                corner(pose, buffer, x + (bx * step[0] + ax * step[1]) * half + nx * step[2], y + (by * step[0] + ay * step[1]) * half + ny * step[2],
                        z + (bz * step[0] + az * step[1]) * half + nz * step[2], 0.5F, 0.02F, sx, sy, sz, light);
            }
        }
    }

    private static void corner(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light) {
        buffer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** How many tiles fit across a facet this wide. */
    static int columns(float width) {
        return Math.max(1, Math.round(width));
    }
}
