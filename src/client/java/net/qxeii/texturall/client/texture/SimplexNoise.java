package net.qxeii.texturall.client.texture;

public final class SimplexNoise {
    private static final int[][] GRADIENTS = {
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
        {1, 0}, {-1, 0}, {1, 0}, {-1, 0},
        {0, 1}, {0, -1}, {0, 1}, {0, -1}
    };

    private final short[] perm = new short[512];

    public SimplexNoise(long seed) {
        short[] source = new short[256];
        for (short i = 0; i < source.length; i++) {
            source[i] = i;
        }

        long state = seed;
        for (int i = source.length - 1; i >= 0; i--) {
            state = next(state);
            int index = (int) ((state + 31) % (i + 1));
            if (index < 0) {
                index += i + 1;
            }

            perm[i] = source[index];
            perm[i + 256] = perm[i];
            source[index] = source[i];
        }
    }

    public double sample(double x, double y) {
        double skew = (x + y) * 0.3660254037844386;
        int i = fastFloor(x + skew);
        int j = fastFloor(y + skew);
        double unskew = (i + j) * 0.21132486540518713;
        double x0 = x - (i - unskew);
        double y0 = y - (j - unskew);

        int i1;
        int j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }

        double x1 = x0 - i1 + 0.21132486540518713;
        double y1 = y0 - j1 + 0.21132486540518713;
        double x2 = x0 - 1.0 + 0.42264973081037427;
        double y2 = y0 - 1.0 + 0.42264973081037427;

        int ii = i & 255;
        int jj = j & 255;
        double n0 = corner(ii, jj, x0, y0);
        double n1 = corner(ii + i1, jj + j1, x1, y1);
        double n2 = corner(ii + 1, jj + 1, x2, y2);
        return 70.0 * (n0 + n1 + n2);
    }

    private double corner(int i, int j, double x, double y) {
        double t = 0.5 - x * x - y * y;
        if (t <= 0.0) {
            return 0.0;
        }

        int[] gradient = GRADIENTS[perm[i + perm[j]] % GRADIENTS.length];
        t *= t;
        return t * t * (gradient[0] * x + gradient[1] * y);
    }

    private static int fastFloor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    private static long next(long state) {
        return state * 6364136223846793005L + 1442695040888963407L;
    }
}
