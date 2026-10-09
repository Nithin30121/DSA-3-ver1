package algorithms;

/**
 * Matrix-Chain Multiplication (MCM)
 * =================================
 *
 * Use in SmartCityX: the analytics pipeline
 *   Data Filtering -> Data Transformation -> Document Indexing -> Similarity Analysis
 * is modelled as a chain of matrices A1 A2 A3 A4. Multiplying a (p x q)
 * matrix by a (q x r) matrix costs p*q*r scalar multiplications. Every
 * parenthesization gives the same result, but NOT the same cost. MCM finds
 * the cheapest order.
 *
 * dims = [p0, p1, ..., pn]; matrix Ai (1-based) has size dims[i-1] x dims[i].
 * In code the matrices are 0-based: matrix i is dims[i] x dims[i+1].
 *
 * DP state:
 *   cost[i][j] = minimum scalar multiplications to compute Ai .. Aj
 * Base case:
 *   cost[i][i] = 0  (one matrix, nothing to multiply)
 * Recurrence:
 *   cost[i][j] = min over k in [i, j-1] of
 *                cost[i][k] + cost[k+1][j] + dims[i] * dims[k+1] * dims[j+1]
 *   split[i][j] = the k that gave the minimum (the last multiplication)
 *
 * Complexity:
 *   Time  O(n^3): O(n^2) (i, j) pairs, each tries O(n) split points
 *   Space O(n^2): the cost and split tables
 */
public final class MatrixChain {

    /**
     * Largest dimension accepted. One multiplication then costs at most
     * 10^15, so the sums in the cost table stay far below Long.MAX_VALUE.
     */
    public static final int MAX_DIMENSION = 100_000;

    private MatrixChain() {
    }

    private static void checkDims(int[] dims) {
        for (int d : dims) {
            if (d < 1 || d > MAX_DIMENSION) {
                throw new IllegalArgumentException(
                        "every dimension must be between 1 and " + MAX_DIMENSION + ", but found " + d);
            }
        }
    }

    /** The two DP tables. */
    public static final class Result {
        public final long[][] cost;
        public final int[][] split;

        Result(long[][] cost, int[][] split) {
            this.cost = cost;
            this.split = split;
        }

        public long minimumCost() {
            return cost[0][cost.length - 1];
        }
    }

    /**
     * Turn operation shapes {rows, cols} into a dims array, checking that the
     * output of each operation fits the input of the next.
     */
    public static int[] dimsFromShapes(String[] names, int[][] shapes) {
        if (shapes.length == 0) {
            throw new IllegalArgumentException("at least one operation is required");
        }
        if (names.length != shapes.length) {
            throw new IllegalArgumentException("every operation needs exactly one name and one shape");
        }
        int[] dims = new int[shapes.length + 1];
        dims[0] = shapes[0][0];
        for (int i = 0; i < shapes.length; i++) {
            if (shapes[i].length != 2) {
                throw new IllegalArgumentException("'" + names[i] + "' must have a shape of {rows, cols}");
            }
            if (shapes[i][0] != dims[i]) {
                throw new IllegalArgumentException("'" + names[i - 1] + "' output (" + dims[i]
                        + ") does not match '" + names[i] + "' input (" + shapes[i][0] + ")");
            }
            dims[i + 1] = shapes[i][1];
        }
        return dims;
    }

    /** Fill the cost and split tables. */
    public static Result solve(int[] dims) {
        int n = dims.length - 1; // number of matrices
        if (n < 1) {
            throw new IllegalArgumentException("dims must describe at least one matrix (length >= 2)");
        }
        checkDims(dims);
        long[][] cost = new long[n][n];
        int[][] split = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                split[i][j] = -1;
            }
        }

        for (int length = 2; length <= n; length++) {        // chain length
            for (int i = 0; i + length - 1 < n; i++) {
                int j = i + length - 1;
                cost[i][j] = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {                 // try every split point
                    long candidate = cost[i][k] + cost[k + 1][j]
                            + (long) dims[i] * dims[k + 1] * dims[j + 1];
                    if (candidate < cost[i][j]) {
                        cost[i][j] = candidate;
                        split[i][j] = k;
                    }
                }
            }
        }
        return new Result(cost, split);
    }

    /** Rebuild the best bracket order from the split table. */
    public static String parenthesization(int[][] split, int i, int j, String[] names) {
        if (i == j) {
            return names[i];
        }
        int k = split[i][j];
        return "(" + parenthesization(split, i, k, names) + " x " + parenthesization(split, k + 1, j, names) + ")";
    }

    /** Default names A1, A2, ..., An. */
    public static String[] defaultNames(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = "A" + (i + 1);
        }
        return names;
    }

    public static long minimumCost(int[] dims) {
        return solve(dims).minimumCost();
    }

    /** Cost of the naive order ((A1 A2) A3) ... for comparison. */
    public static long leftToRightCost(int[] dims) {
        checkDims(dims);
        long total = 0;
        for (int k = 1; k < dims.length - 1; k++) {
            total += (long) dims[0] * dims[k] * dims[k + 1];
        }
        return total;
    }
}
