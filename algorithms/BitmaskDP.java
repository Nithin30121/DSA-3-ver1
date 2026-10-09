package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bitmask DP - Emergency Resource Dispatch
 * ========================================
 *
 * Use in SmartCityX: choose WHICH city resources to send to an incident,
 * and in WHAT ORDER, to get the most benefit within a budget.
 *
 * Problem:
 *   - Each resource has a cost, a benefit value, an urgency and prerequisites.
 *   - One resource leaves the depot per time slot (slot 0, 1, 2, ...).
 *   - A resource sent in slot t gives  value - urgency * t  benefit
 *     (urgent resources lose more benefit when they are sent late).
 *   - A resource can only be sent after its prerequisite resources
 *     (the Fire Truck needs the Water Tank on site first, to refill).
 *   - Total cost of the chosen resources must not exceed the budget.
 *
 * Bitmask representation (4 resources):
 *   bit 0 -> 0001 = Water Tank
 *   bit 1 -> 0010 = Ambulance
 *   bit 2 -> 0100 = Fire Truck
 *   bit 3 -> 1000 = Generator
 *   mask 1101 = {Water Tank, Fire Truck, Generator}
 *
 * DP:
 *   dp[mask] = best total benefit after dispatching exactly the resources in
 *              mask, in the best valid order (UNREACHABLE if impossible).
 *   Key observation: the next slot number is popcount(mask), so the state
 *   only needs the SET of dispatched resources, not their order.
 *
 *   Base      : dp[0] = 0 (nothing sent yet)
 *   Transition: for each resource r not in mask whose prerequisites are all
 *               in mask and with cost[mask | r] <= budget:
 *                 dp[mask | 1<<r] = max(dp[mask | 1<<r],
 *                                       dp[mask] + value[r] - urgency[r] * popcount(mask))
 *   Answer    : the mask with the largest dp value (ties -> cheaper mask)
 *
 * Why DP instead of brute force:
 *   Brute force tries every ordered selection: sum over k of n!/(n-k)!
 *   sequences (65 for n = 4, about 3.5 * 10^12 for n = 15). DP merges all
 *   orders that reach the same set into ONE state: 2^n states x n moves.
 *
 * Complexity:
 *   Time O(2^n * n), Space O(2^n)
 */
public final class BitmaskDP {

    public static final int UNREACHABLE = Integer.MIN_VALUE;
    public static final int MAX_RESOURCES = 20; // 2^20 states is plenty for a demo

    private BitmaskDP() {
    }

    /** One city resource. */
    public static final class Resource {
        public final String name;
        public final int cost;
        public final int value;
        public final int urgency;
        public final int[] requires; // indices of resources needed before this one

        public Resource(String name, int cost, int value, int urgency, int... requires) {
            this.name = name;
            this.cost = cost;
            this.value = value;
            this.urgency = urgency;
            this.requires = requires;
        }
    }

    /** Output of the optimisation, including the full DP tables. */
    public static final class Result {
        public final int bestMask;
        public final List<Integer> order; // dispatch order (resource indices)
        public final int totalCost;
        public final int totalBenefit;
        public final int[] dp;     // dp[mask], UNREACHABLE if impossible
        public final int[] cost;   // cost[mask]
        public final int[] last;   // resource added last on the best path to mask
        public final int transitions;

        Result(int bestMask, List<Integer> order, int[] dp, int[] cost, int[] last, int transitions) {
            this.bestMask = bestMask;
            this.order = order;
            this.totalCost = cost[bestMask];
            this.totalBenefit = dp[bestMask];
            this.dp = dp;
            this.cost = cost;
            this.last = last;
            this.transitions = transitions;
        }

        public boolean reachable(int mask) {
            return dp[mask] != UNREACHABLE;
        }
    }

    /** Number of 1-bits (Brian Kernighan's trick clears the lowest set bit). */
    public static int countBits(int mask) {
        int count = 0;
        while (mask != 0) {
            mask &= mask - 1;
            count++;
        }
        return count;
    }

    /** Binary string with a fixed number of digits, e.g. (5, 4) -> "0101". */
    public static String toBinary(int mask, int width) {
        StringBuilder sb = new StringBuilder();
        for (int bit = width - 1; bit >= 0; bit--) {
            sb.append((mask & (1 << bit)) != 0 ? '1' : '0');
        }
        return sb.toString();
    }

    public static List<String> maskToNames(int mask, List<Resource> resources) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < resources.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                names.add(resources.get(i).name);
            }
        }
        return names;
    }

    /** Benefit of sending resource r when `slot` resources have already left. */
    public static int gain(Resource r, int slot) {
        return r.value - r.urgency * slot;
    }

    public static Result optimize(List<Resource> resources, int budget) {
        int n = resources.size();
        if (n > MAX_RESOURCES) {
            throw new IllegalArgumentException("at most " + MAX_RESOURCES + " resources are supported");
        }
        int size = 1 << n;

        // Prerequisites as bitmasks (and a check that the input makes sense).
        int[] need = new int[n];
        for (int r = 0; r < n; r++) {
            Resource resource = resources.get(r);
            if (resource.cost < 0) {
                throw new IllegalArgumentException("cost of '" + resource.name + "' must not be negative");
            }
            for (int p : resource.requires) {
                if (p < 0 || p >= n || p == r) {
                    throw new IllegalArgumentException("'" + resource.name + "' has an invalid prerequisite: " + p);
                }
                need[r] |= 1 << p;
            }
        }

        // cost[mask] from a smaller mask: remove the lowest set bit.
        int[] cost = new int[size];
        for (int mask = 1; mask < size; mask++) {
            int low = mask & -mask;
            int r = Integer.numberOfTrailingZeros(low);
            cost[mask] = cost[mask ^ low] + resources.get(r).cost;
        }

        int[] dp = new int[size];
        int[] parent = new int[size];
        int[] last = new int[size];
        for (int mask = 0; mask < size; mask++) {
            dp[mask] = UNREACHABLE;
            parent[mask] = -1;
            last[mask] = -1;
        }
        dp[0] = 0;
        int transitions = 0;

        // Increasing numeric order: every subset is finished before any mask
        // that adds one more bit to it.
        for (int mask = 0; mask < size; mask++) {
            if (dp[mask] == UNREACHABLE) {
                continue;
            }
            int slot = countBits(mask);
            for (int r = 0; r < n; r++) {
                int bit = 1 << r;
                if ((mask & bit) != 0) {
                    continue; // already dispatched
                }
                if ((mask & need[r]) != need[r]) {
                    continue; // prerequisite not on site yet
                }
                int next = mask | bit;
                if (cost[next] > budget) {
                    continue; // over budget
                }
                transitions++;
                int candidate = dp[mask] + gain(resources.get(r), slot);
                if (dp[next] == UNREACHABLE || candidate > dp[next]) {
                    dp[next] = candidate;
                    parent[next] = mask;
                    last[next] = r;
                }
            }
        }

        int best = 0;
        for (int mask = 0; mask < size; mask++) {
            if (dp[mask] == UNREACHABLE) {
                continue;
            }
            if (dp[mask] > dp[best] || (dp[mask] == dp[best] && cost[mask] < cost[best])) {
                best = mask;
            }
        }

        // Rebuild the dispatch order by following parent links back to 0.
        List<Integer> order = new ArrayList<>();
        for (int mask = best; mask != 0; mask = parent[mask]) {
            order.add(last[mask]);
        }
        Collections.reverse(order);

        return new Result(best, order, dp, cost, last, transitions);
    }
}
