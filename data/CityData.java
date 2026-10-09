package data;

import algorithms.BitmaskDP.Resource;
import java.util.List;

/**
 * Sample smart-city datasets used by the SmartCityX demonstrations.
 * Everything is plain Java (lists, arrays, strings): no database, no files.
 */
public final class CityData {

    private CityData() {
    }

    // -------------------------------------------------------------------------
    // Citizen reports  (KMP, Z-Function, Aho-Corasick, Levenshtein vocabulary)
    // -------------------------------------------------------------------------
    public static final List<String> CITIZEN_REPORTS = List.of(
            "Water pipeline leakage reported near Central Park",
            "Traffic congestion near City Center",
            "Streetlight failure reported in Ward 12",
            "Garbage collection delayed in Sector 5",
            "Fire emergency reported near industrial area",
            "Broken water pipeline near Main Road",
            "Ambulance required near City Hospital",
            "Flood warning issued near River Road",
            "Traffic signal malfunction near Central Junction",
            "Gas leak reported near industrial zone");

    // -------------------------------------------------------------------------
    // Emergency reports  (Aho-Corasick)
    // -------------------------------------------------------------------------
    public static final List<String> EMERGENCY_REPORTS = List.of(
            "Major accident reported near hospital. Ambulance required immediately.",
            "Gas leak detected in Sector 9, fire brigade alerted.",
            "Building collapse after minor earthquake near Old Market.",
            "Flood water entering homes near River Road, ambulance on standby.",
            "Streetlight repair pending in Ward 4.");

    public static final List<String> EMERGENCY_KEYWORDS = List.of(
            "fire", "flood", "accident", "ambulance", "gas leak", "earthquake", "collapse");

    // -------------------------------------------------------------------------
    // Public-service requests  (Levenshtein query correction)
    // -------------------------------------------------------------------------
    public static final List<String> SERVICE_REQUESTS = List.of(
            "pipeline leakage",
            "water supply interruption",
            "streetlight repair",
            "garbage collection",
            "traffic signal repair",
            "road damage",
            "drainage blockage",
            "power outage",
            "tree fallen on road",
            "noise complaint");

    /** Misspelled queries typed into the citizen helpdesk. */
    public static final List<String> MISSPELLED_QUERIES = List.of(
            "pipline leakge",
            "garbge colection",
            "trafic signl repair",
            "powr outag",
            "streetlite repair");

    // -------------------------------------------------------------------------
    // Infrastructure identifiers  (Rabin-Karp)
    // -------------------------------------------------------------------------
    public static final List<String> INFRASTRUCTURE_IDS = List.of(
            "WTR-1024",  // water main
            "WTR-2034",  // reservoir valve
            "TRF-3011",  // traffic signal controller
            "ENR-5012",  // energy substation
            "BRG-4015"); // bridge

    /** A maintenance log that mentions identifiers several times. */
    public static final String INFRASTRUCTURE_LOG =
            "08:00 WTR-1024 valve inspected; "
            + "09:15 TRF-3011 signal timing reset; "
            + "10:40 WTR-2034 pressure low; "
            + "11:05 WTR-1024 leak sealed; "
            + "13:30 ENR-5012 transformer serviced; "
            + "15:20 BRG-4015 load test passed; "
            + "17:45 TRF-3011 camera replaced";

    // -------------------------------------------------------------------------
    // Infrastructure documents  (Suffix Array, LCP / Kasai)
    // -------------------------------------------------------------------------
    public static final List<String> INFRASTRUCTURE_DOCUMENTS = List.of(
            "Water pipeline maintenance required near Central Park",
            "Water pipeline maintenance completed near Central Park",
            "Traffic signal maintenance required near City Center",
            "Streetlight inspection completed in Ward 12 by Energy Department",
            "Bridge BRG-4015 structural inspection scheduled near River Road",
            "Power substation ENR-5012 maintenance required after storm");

    /** Short pair used to explain common-substring analysis. */
    public static final String DOCUMENT_A = "Water pipeline maintenance required";
    public static final String DOCUMENT_B = "Water pipeline maintenance completed";

    // -------------------------------------------------------------------------
    // City resources  (Bitmask DP)
    //   cost     - deployment cost (lakh rupees)
    //   value    - benefit points if dispatched first (slot 0)
    //   urgency  - benefit points lost for every slot of delay
    //   requires - resources that must be dispatched BEFORE this one
    // -------------------------------------------------------------------------
    public static final List<Resource> CITY_RESOURCES = List.of(
            new Resource("Water Tank", 3, 30, 5),
            new Resource("Ambulance", 4, 60, 15),
            new Resource("Fire Truck", 5, 70, 10, 0), // needs Water Tank (index 0)
            new Resource("Generator", 2, 25, 5));

    public static final int RESOURCE_BUDGET = 10;

    // -------------------------------------------------------------------------
    // Analytics pipeline  (Matrix-Chain Multiplication)
    // Each operation is a matrix {rows, cols}; columns of one operation must
    // equal the rows of the next.
    // -------------------------------------------------------------------------
    public static final String[] ANALYTICS_OPERATIONS = {
        "Data Filtering", "Data Transformation", "Document Indexing", "Similarity Analysis"
    };

    public static final int[][] ANALYTICS_SHAPES = {
        {40, 20}, {20, 30}, {30, 10}, {10, 30}
    };
}
