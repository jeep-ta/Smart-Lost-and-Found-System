package com.example.lostandfound;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

// LostAndFoundSystem
// Implements steps:
// 1. Item qualification
// 2. Preprocessing & normalization
// 3. Runtime storage in ArrayList
// 4. Persistent storage via file handling
// 5. Similarity scoring (Jaccard)
// 6. Thresholding & ranking
// 7. Clear result presentation
// (8th step - verification & claim workflow - future implementation)
// Also includes basic record maintenance & logging (step 9).

public class LostAndFoundSystem {
    // Storage files (in working directory)
    private static final String STORAGE_FILE = "items_store.txt";
    private static final String AUDIT_FILE = "audit.log";
    private static final String DELIM = "||"; // simple delimiter
    private static final double MIN_VALUE = 50.0; // minimum value to accept automatically
    private static final double PERISHABLE_MIN_VALUE = 100.0; // perishables with low value rejected
    private static double SIMILARITY_THRESHOLD = 0.25; // adjustable threshold for matches (configurable)

    // Category options
    public static final String[] CATEGORIES = { "Wallet", "Bag", "Electronics", "Documents", "Clothing", "Keys",
            "Others" };

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "of", "in", "on", "at", "to", "for", "with", "and", "or", "is", "are", "was", "were",
            "be", "been",
            "by", "from", "that", "this", "these", "those", "it", "its", "my", "your", "our", "their", "as", "but",
            "not", "so",
            "if", "then", "into", "about", "over", "under", "near", "between", "among", "per", "each", "per"));
    // Acceptable item categories - personal or movable objects
    private static final Set<String> ACCEPTABLE_KEYWORDS = new HashSet<>(Arrays.asList(
            "handbag", "bag", "backpack", "purse", "tote", "satchel", "briefcase",
            "wallet", "purse", "money", "cash", "coins", "creditcard", "card", "id", "identification", "passport",
            "license",
            "phone", "cellphone", "mobile", "smartphone", "tablet", "laptop", "notebook", "computer", "device",
            "book", "textbook", "notebook", "journal", "diary", "magazine", "newspaper", "document", "paper",
            "watch", "ring", "necklace", "bracelet", "earrings", "jewelry", "accessory", "glasses", "sunglasses",
            "keys", "key", "keychain", "remote", "camera", "charger", "cable", "headphones", "earbuds", "airpods",
            "clothing", "shirt", "pants", "jacket", "coat", "hat", "cap", "scarf", "gloves", "belt", "shoes", "boots"));

    // Explicitly excluded categories
    private static final Set<String> EXCLUDED_KEYWORDS = new HashSet<>(Arrays.asList(
            "food", "drink", "beverage", "water", "soda", "coffee", "tea", "juice", "milk", "beer", "wine", "alcohol",
            "sandwich", "burger", "pizza", "snack", "candy", "chocolate", "cookie", "cake", "bread", "fruit",
            "vegetable",
            "tissue", "tissues", "napkin", "napkins", "paper", "toilet", "bottle", "plastic", "container", "cup", "mug",
            "disposable", "trash", "garbage", "waste", "perishable", "expired", "rotten", "moldy", "stale"));

    // Low-value disposable items (regardless of keywords)
    private static final Set<String> DISPOSABLE_KEYWORDS = new HashSet<>(Arrays.asList(
            "tissue", "tissues", "napkin", "napkins", "toilet", "paper", "plastic", "bottle", "cup", "container",
            "disposable", "wrapper", "packaging", "bag", "trash", "garbage"));

    private static List<Item> foundItems = new ArrayList<>();

    // Configuration methods
    public void setSearchThreshold(double threshold) {
        if (threshold >= 0.0 && threshold <= 1.0) {
            SIMILARITY_THRESHOLD = threshold;
        }
    }

    public double getSearchThreshold() {
        return SIMILARITY_THRESHOLD;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        log("SYSTEM", "Starting LostAndFoundSystem");
        loadItemsFromFile();

        boolean running = true;
        while (running) {
            showMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    submitFoundItem(sc);
                    break;
                case "2":
                    submitLostItemAndSearch(sc);
                    break;
                case "3":
                    listFoundItems();
                    break;
                case "4":
                    maintenanceRemoveItem(sc);
                    break;
                case "5":
                    displayQualificationCriteria();
                    break;
                case "6":
                    System.out.println("Exiting system...");
                    log("SYSTEM", "Exiting LostAndFoundSystem");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
        sc.close();
    }

    private static void showMenu() {
        System.out.println("\n--- Lost & Found System ---");
        System.out.println("1. Submit Found Item");
        System.out.println("2. Find Lost Item (Search)");
        System.out.println("3. List Found Items");
        System.out.println("4. Maintenance (Remove item)");
        System.out.println("5. View Qualification Criteria");
        System.out.println("6. Exit");
        System.out.print("Choose an option: ");
    }

    // Item submission & qualification (step 1)
    private static void submitFoundItem(Scanner sc) {
        System.out.println("\nSubmit Found Item (qualification will be performed):");

        System.out.print("Name (e.g., Black Wallet): ");
        String name = sc.nextLine().trim();

        System.out.print("Description (what makes it unique): ");
        String description = sc.nextLine().trim();

        // Category selection
        System.out.println("Category (choose number):");
        for (int i = 0; i < CATEGORIES.length; i++) {
            System.out.printf("  %d. %s\n", i + 1, CATEGORIES[i]);
        }
        System.out.print("Select category: ");
        String catChoice = sc.nextLine().trim();
        String category = "Others";
        try {
            int idx = Integer.parseInt(catChoice) - 1;
            if (idx >= 0 && idx < CATEGORIES.length) {
                category = CATEGORIES[idx];
            }
        } catch (Exception e) {
            System.out.println("Invalid selection, defaulting to 'Others'");
        }

        System.out.print("Location found (e.g., Main Library): ");
        String location = sc.nextLine().trim();

        System.out.print("Estimated value in PHP (numeric, e.g., 1500): ");
        double estimatedValue = parseDoubleInput(sc);

        System.out.print("Is it perishable? (y/n): ");
        boolean perishable = sc.nextLine().trim().toLowerCase().startsWith("y");

        System.out.print("Contact info of reporter (phone or email): ");
        String contact = sc.nextLine().trim();

        LocalDate dateFound = LocalDate.now();

        Item item = new Item(UUID.randomUUID().toString(), name, description, category, dateFound, location, contact,
                estimatedValue, perishable, "ACTIVE", LocalDateTime.now());

        // Qualification check:
        if (!qualifyItem(item)) {
            System.out.println("Item did not meet qualification criteria and was not accepted.");
            log("QUALIFY-REJECT", item.summaryForLog());
            return;
        }

        // Preprocess tokens are computed inside Item constructor helper
        foundItems.add(item);
        saveItemsToFile();
        log("ADD-FOUND", item.summaryForLog());
        System.out.println("Item accepted and saved. ID: " + item.id);
    }

    private static double parseDoubleInput(Scanner sc) {
        double v = 0.0;
        String s = sc.nextLine().trim();
        try {
            v = Double.parseDouble(s);
        } catch (Exception e) {
            System.out.println("Invalid number. Defaulting to 0.");
        }
        return v;
    }

    private static boolean qualifyItem(Item item) {
        System.out.println("=== ITEM QUALIFICATION CRITERIA CHECK ===");

        // 1. Basic required field checks
        if (item.name.isEmpty() || item.description.isEmpty()) {
            System.out.println("[REJECTED] Name and description are required.");
            return false;
        }
        if (item.location.isEmpty()) {
            System.out.println("[REJECTED] Location is required.");
            return false;
        }
        if (item.contact.isEmpty()) {
            System.out.println("[REJECTED] Reporter contact is required.");
            return false;
        }

        // 2. Contact validation
        if (!(item.contact.matches(".*\\\\d.*") || item.contact.contains("@"))) {
            System.out.println("[REJECTED] Contact seems invalid. Provide phone number or email.");
            return false;
        }

        // 3. Location validation - allows free-form entries (e.g., "GEB-205", "STC-3F")
        if (!isValidInstitutionLocation(item.location)) {
            System.out.println("[REJECTED] Location is required and cannot be empty.");
            return false;
        }

        // 4. Exclusion check - food, beverages, disposables
        if (isExcludedItem(item.name, item.description)) {
            System.out.println("[REJECTED] Item is explicitly excluded (food, beverages, or disposable items).");
            System.out.println("   Excluded: Food, drinks, tissues, plastic bottles, disposable containers, etc.");
            return false;
        }

        // 5. Tangible and storable check
        if (!isTangibleAndStorable(item.name, item.description)) {
            System.out.println("[REJECTED] Item must be tangible and capable of being stored.");
            System.out.println("   Digital/virtual items are not acceptable.");
            return false;
        }

        // 6. Personal or movable object check
        if (!isPersonalOrMovableItem(item.name, item.description)) {
            System.out.println("[REJECTED] Only personal or movable objects are acceptable.");
            System.out.println("   Acceptable: handbags, wallets, phones, books, accessories, clothing, etc.");
            return false;
        }

        // 7. Value and importance check
        if (!hasSufficientValue(item.name, item.description, item.estimatedValue)) {
            System.out.printf("[REJECTED] Item lacks sufficient value (%.2f PHP) or importance.\n",
                    item.estimatedValue);
            System.out.printf("   Minimum value threshold: %.2f PHP or must contain valuable keywords.\n", MIN_VALUE);
            System.out.println("   Valuable items: phones, laptops, wallets, keys, jewelry, identification, etc.");
            return false;
        }

        // 8. Perishability check - if marked perishable and low value, reject
        if (item.perishable && item.estimatedValue < PERISHABLE_MIN_VALUE) {
            System.out.printf("[REJECTED] Perishable item with low value (%.2f PHP).\n", item.estimatedValue);
            System.out.printf("   Perishable items must have minimum value of %.2f PHP.\n", PERISHABLE_MIN_VALUE);
            return false;
        }

        System.out.println("[ACCEPTED] Item meets all qualification criteria.");
        System.out.printf("   - Personal/movable object: PASS\n");
        System.out.printf("   - Institution location: PASS\n");
        System.out.printf("   - Tangible and storable: PASS\n");
        System.out.printf("   - Sufficient value/importance: PASS (%.2f PHP)\n", item.estimatedValue);
        System.out.printf("   - Not excluded category: PASS\n");

        return true;
    }

    private static boolean containsKeyword(String text, Set<String> keywords) {
        String lower = text.toLowerCase();
        for (String k : keywords) {
            if (lower.contains(k))
                return true;
        }
        return false;
    }

    /**
     * Validates if the location is non-empty (allows free-form campus locations)
     */
    private static boolean isValidInstitutionLocation(String location) {
        // Allow any non-empty location - users can enter classroom names with building
        // prefixes
        return location != null && !location.trim().isEmpty();
    }

    /**
     * Determines if an item is a personal or movable object based on keywords
     */
    private static boolean isPersonalOrMovableItem(String name, String description) {
        String combinedText = (name + " " + description).toLowerCase();
        return containsKeyword(combinedText, ACCEPTABLE_KEYWORDS);
    }

    /**
     * Checks if an item is explicitly excluded (food, beverages, disposables)
     */
    private static boolean isExcludedItem(String name, String description) {
        String combinedText = (name + " " + description).toLowerCase();
        return containsKeyword(combinedText, EXCLUDED_KEYWORDS) ||
                containsKeyword(combinedText, DISPOSABLE_KEYWORDS);
    }

    /**
     * Validates if an item meets the tangible and storable criteria
     */
    private static boolean isTangibleAndStorable(String name, String description) {
        String combinedText = (name + " " + description).toLowerCase();

        // Items that are clearly not tangible or storable
        Set<String> nonTangibleKeywords = new HashSet<>(Arrays.asList(
                "digital", "virtual", "online", "software", "app", "data", "file", "memory",
                "cloud", "streaming", "download", "electronic", "email", "message"));

        return !containsKeyword(combinedText, nonTangibleKeywords);
    }

    /**
     * Validates if an item has sufficient value or importance to be worth tracking
     */
    private static boolean hasSufficientValue(String name, String description, double estimatedValue) {
        // High-value threshold for automatic acceptance
        if (estimatedValue >= MIN_VALUE)
            return true;

        // Check for valuable keywords that indicate importance regardless of estimated
        // value
        String combinedText = (name + " " + description).toLowerCase();
        Set<String> highValueKeywords = new HashSet<>(Arrays.asList(
                "passport", "license", "id", "identification", "credit", "card", "debit",
                "phone", "cellphone", "laptop", "tablet", "watch", "jewelry", "ring",
                "keys", "key", "wallet", "purse", "handbag", "backpack", "briefcase"));

        return containsKeyword(combinedText, highValueKeywords);
    }

    /**
     * Displays the complete qualification criteria for the Lost and Found system
     */
    private static void displayQualificationCriteria() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("           LOST AND FOUND SYSTEM - QUALIFICATION CRITERIA");
        System.out.println("=".repeat(80));

        System.out.println("\n[ACCEPTABLE ITEMS]:");
        System.out.println("   Only personal or movable objects may be classified as 'lost':");
        System.out.println("   - Handbags, wallets, purses, backpacks, briefcases");
        System.out.println("   - Identification cards, passports, licenses, credit cards");
        System.out.println("   - Mobile phones, tablets, laptops, computers");
        System.out.println("   - Books, textbooks, notebooks, documents");
        System.out.println("   - Accessories: watches, jewelry, glasses, keys");
        System.out.println("   - Clothing items: shirts, pants, jackets, shoes");
        System.out.println("   - Electronic accessories: chargers, headphones, cameras");

        System.out.println("\n[EXCLUDED ITEMS]:");
        System.out.println("   - Food and beverages (all types)");
        System.out.println("   - Low-cost disposable products:");
        System.out.println("     * Tissues, napkins, toilet paper");
        System.out.println("     * Plastic bottles, disposable cups/containers");
        System.out.println("     * Wrappers, packaging materials");
        System.out.println("   - Digital/virtual items (software, apps, online content)");

        System.out.println("\n[LOCATION REQUIREMENTS]:");
        System.out.println("   Items must be found within institution premises or specified areas:");
        System.out.println("   - Academic buildings: library, lab, classroom, lecture hall");
        System.out.println("   - Common areas: cafeteria, lounge, lobby, study room");
        System.out.println("   - Campus facilities: gym, sports field, parking lot");
        System.out.println("   - Administrative areas: office, corridor, restroom");

        System.out.println("\n[VALUE AND IMPORTANCE CRITERIA]:");
        System.out.printf("   - Minimum value threshold: %.2f PHP\n", MIN_VALUE);
        System.out.println("   - OR must contain valuable keywords (phones, laptops, IDs, etc.)");
        System.out.println("   - Perishable items require higher value threshold");
        System.out.printf("   - Perishable minimum value: %.2f PHP\n", PERISHABLE_MIN_VALUE);

        System.out.println("\n[TANGIBLE AND STORABLE REQUIREMENTS]:");
        System.out.println("   - Item must be physical and capable of being stored");
        System.out.println("   - Must be documentable and returnable to owner");
        System.out.println("   - Cannot be digital, virtual, or intangible");

        System.out.println("\n[CONTACT REQUIREMENTS]:");
        System.out.println("   - Reporter must provide valid contact information");
        System.out.println("   - Must contain phone number (digits) or email address (@)");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("   All criteria must be met for an item to be accepted into the system.");
        System.out.println("=".repeat(80) + "\n");
    }

    // Lost item search (steps 2,5,6,7)
    private static void submitLostItemAndSearch(Scanner sc) {
        System.out.println("\nSubmit Lost Item (this runs a search):");
        System.out.print("Name (optional, e.g., Black Wallet): ");
        String name = sc.nextLine().trim();

        System.out.print("Description (what you lost): ");
        String description = sc.nextLine().trim();

        System.out.print("Location lost (optional): ");
        String location = sc.nextLine().trim();

        // Build query text (name + description + location)
        String queryText = String.join(" ", Arrays.asList(name, description, location)).trim();
        if (queryText.isEmpty()) {
            System.out.println("You must provide at least some descriptive text to search.");
            return;
        }

        // Preprocess query tokens
        Set<String> queryTokens = preprocessToSet(queryText);

        // Score against all active found items
        List<Match> matches = new ArrayList<>();
        for (Item it : foundItems) {
            if (!it.status.equalsIgnoreCase("ACTIVE"))
                continue;
            Set<String> itemTokens = it.getTokenSet();
            double score = jaccard(queryTokens, itemTokens);
            if (score >= SIMILARITY_THRESHOLD) {
                matches.add(new Match(it, score));
            }
        }

        // Rank matches by score (desc)
        matches.sort((a, b) -> Double.compare(b.score, a.score));

        // Present results
        System.out.println("\nSearch results (threshold: " + SIMILARITY_THRESHOLD + "):");
        if (matches.isEmpty()) {
            System.out.println("No matches found.");
            log("SEARCH", "Query: " + truncate(queryText, 200) + " -> 0 matches");
        } else {
            System.out.printf("Found %d match(es):\n", matches.size());
            for (Match m : matches) {
                Item it = m.item;
                System.out.println("--------------------------------------------");
                System.out.printf(
                        "ID: %s\nName: %s\nDescription: %s\nDate Found: %s\nLocation: %s\nContact: %s\nEstimated Value: %.2f\nPerishable: %s\nSimilarity: %.2f%%\nStatus: %s\n",
                        it.id, it.name, it.description, it.dateFound.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        it.location, it.contact, it.estimatedValue, it.perishable ? "Yes" : "No", m.score * 100.0,
                        it.status);
            }
            System.out.println("--------------------------------------------");
            log("SEARCH", "Query: " + truncate(queryText, 200) + " -> " + matches.size() + " matches (top score: "
                    + String.format("%.4f", matches.get(0).score) + ")");
        }
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max)
            return s;
        return s.substring(0, max - 3) + "...";
    }

    // Maintenance (step 9: simple removal & logging)
    private static void maintenanceRemoveItem(Scanner sc) {
        System.out.println("\nMaintenance - Remove (mark inactive) an item");
        System.out.print("Enter item ID to remove: ");
        String id = sc.nextLine().trim();
        Optional<Item> opt = foundItems.stream().filter(i -> i.id.equals(id)).findFirst();
        if (!opt.isPresent()) {
            System.out.println("Item not found.");
            return;
        }
        Item it = opt.get();
        it.status = "REMOVED";
        saveItemsToFile();
        log("REMOVE", it.summaryForLog());
        System.out.println("Item marked REMOVED: " + id);
    }

    // Storage: load & save (step 4)
    private static void loadItemsFromFile() {
        Path p = Paths.get(STORAGE_FILE);
        if (!Files.exists(p)) {
            System.out.println("No storage file found. Starting with empty dataset.");
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(p)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                try {
                    Item it = Item.fromLine(line);
                    foundItems.add(it);
                } catch (Exception e) {
                    System.err.println("Skipping malformed line: " + line);
                }
            }
            System.out.println("Loaded " + foundItems.size() + " items from storage.");
        } catch (IOException e) {
            System.err.println("Error reading storage file: " + e.getMessage());
        }
    }

    private static void saveItemsToFile() {
        // Create backup before saving
        Path p = Paths.get(STORAGE_FILE);
        Path backup = Paths.get("items_store.bak");

        // Backup existing file if it exists
        if (Files.exists(p)) {
            try {
                Files.copy(p, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Warning: Could not create backup: " + e.getMessage());
            }
        }

        try (BufferedWriter w = Files.newBufferedWriter(p, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Item it : foundItems) {
                w.write(it.toLine());
                w.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving to storage: " + e.getMessage());
        }
    }

    // Logging (simple audit log)
    private static void log(String action, String message) {
        String entry = String.format("%s %s | %s", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                action, message);
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(AUDIT_FILE), StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            w.write(entry);
            w.newLine();
        } catch (IOException e) {
            // fallback to stdout
            System.out.println("LOG-ERR: " + entry);
        }
    }

    // Text normalization (step 2)
    private static Set<String> preprocessToSet(String text) {
        if (text == null)
            return Collections.emptySet();
        String normalized = text.toLowerCase();
        // remove punctuation except digits and letters -> replace with space
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        String[] words = normalized.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String w : words) {
            w = w.trim();
            if (w.isEmpty())
                continue;
            if (STOPWORDS.contains(w))
                continue;
            tokens.add(w);
        }
        return tokens;
    }

    // Similarity (Jaccard) (step 5)
    private static double jaccard(Set<String> a, Set<String> b) {
        if ((a == null || a.isEmpty()) && (b == null || b.isEmpty()))
            return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty())
            return 0.0;
        return (double) intersection.size() / (double) union.size();
    }

    // List found items (helper)
    private static void listFoundItems() {
        System.out.println("\nFound items (all):");
        if (foundItems.isEmpty()) {
            System.out.println("(no items)");
            return;
        }
        for (Item it : foundItems) {
            System.out.printf("ID: %s | Name: %s | Date: %s | Location: %s | Status: %s\n",
                    it.id, truncate(it.name, 30), it.dateFound.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    truncate(it.location, 25), it.status);
        }
    }

    // Utility classes

    private static class Match {
        Item item;
        double score;

        Match(Item i, double s) {
            item = i;
            score = s;
        }
    }

    private static class Item {
        String id;
        String name;
        String description;
        String category; // NEW: Item category
        LocalDate dateFound;
        String location;
        String contact;
        double estimatedValue;
        boolean perishable;
        String status; // ACTIVE or REMOVED
        LocalDateTime createdAt;

        // cached token set for efficient similarity
        private transient Set<String> tokenSet = null;

        Item(String id, String name, String description, String category, LocalDate dateFound, String location,
                String contact, double estimatedValue, boolean perishable, String status, LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category != null ? category : "Others";
            this.dateFound = dateFound;
            this.location = location;
            this.contact = contact;
            this.estimatedValue = estimatedValue;
            this.perishable = perishable;
            this.status = status;
            this.createdAt = createdAt;
            this.tokenSet = preprocessToSet(name + " " + description);
        }

        Set<String> getTokenSet() {
            if (tokenSet == null)
                tokenSet = preprocessToSet(name + " " + description);
            return tokenSet;
        }

        String toLine() {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(DELIM)
                    .append(clean(name)).append(DELIM)
                    .append(clean(description)).append(DELIM)
                    .append(clean(category)).append(DELIM) // NEW: category field
                    .append(dateFound.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(DELIM)
                    .append(clean(location)).append(DELIM)
                    .append(clean(contact)).append(DELIM)
                    .append(estimatedValue).append(DELIM)
                    .append(perishable).append(DELIM)
                    .append(status).append(DELIM)
                    .append(createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return sb.toString();
        }

        private static String clean(String s) {
            if (s == null)
                return "";
            return s.replace(DELIM, " ").replaceAll("\\r?\\n", " ");
        }

        static Item fromLine(String line) {
            String[] parts = line.split(Pattern.quote(DELIM), -1);

            // Support old format (10 parts) and new format (11 parts with category)
            if (parts.length < 10)
                throw new IllegalArgumentException("Invalid line");

            String id = parts[0];
            String name = parts[1];
            String description = parts[2];

            String category;
            LocalDate dateFound;
            String location;
            String contact;
            double estimatedValue;
            boolean perishable;
            String status;
            LocalDateTime createdAt;

            if (parts.length >= 11) {
                // New format with category
                category = parts[3];
                dateFound = LocalDate.parse(parts[4], DateTimeFormatter.ISO_LOCAL_DATE);
                location = parts[5];
                contact = parts[6];
                estimatedValue = Double.parseDouble(parts[7]);
                perishable = Boolean.parseBoolean(parts[8]);
                status = parts[9];
                createdAt = LocalDateTime.parse(parts[10], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                // Old format without category - migrate to "Others"
                category = "Others";
                dateFound = LocalDate.parse(parts[3], DateTimeFormatter.ISO_LOCAL_DATE);
                location = parts[4];
                contact = parts[5];
                estimatedValue = Double.parseDouble(parts[6]);
                perishable = Boolean.parseBoolean(parts[7]);
                status = parts[8];
                createdAt = LocalDateTime.parse(parts[9], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                System.out.println("[MIGRATION] Item " + id + " migrated with category: Others");
            }

            return new Item(id, name, description, category, dateFound, location, contact, estimatedValue, perishable,
                    status, createdAt);
        }

        String summaryForLog() {
            return String.format("ID=%s Name=%s Cat=%s Loc=%s Val=%.2f Per=%s Status=%s", id, truncate(name, 50),
                    category, truncate(location, 30), estimatedValue, perishable, status);
        }
    }

    // Add to LostAndFoundSystem
    public String guiSubmitFoundItem(String name, String desc, String category, String loc, LocalDate dateFound,
            double value, boolean perish,
            String contact) {
        Item item = new Item(
                java.util.UUID.randomUUID().toString(),
                name, desc, category, dateFound,
                loc, contact, value, perish, "ACTIVE", java.time.LocalDateTime.now());

        // Store original System.out to capture qualification output
        java.io.ByteArrayOutputStream qualificationOutput = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        java.io.PrintStream captureStream = new java.io.PrintStream(qualificationOutput);

        boolean qualified = false;
        try {
            System.setOut(captureStream);
            qualified = qualifyItem(item);
        } finally {
            System.setOut(originalOut);
        }

        String qualificationDetails = qualificationOutput.toString();

        if (!qualified) {
            return "Item not accepted (qualification failed).\\n\\nQualification Details:\\n" + qualificationDetails;
        }

        foundItems.add(item);
        saveItemsToFile();
        log("ADD-FOUND", item.summaryForLog());
        return "Item accepted and saved: " + item.id + "\\n\\nQualification Details:\\n" + qualificationDetails;
    }

    // Enhanced search with matching keywords
    public String guiSearchLostItem(String name, String desc, String loc) {
        java.util.Set<String> queryTokens = preprocessToSet(name + " " + desc + " " + loc);
        StringBuilder sb = new StringBuilder();
        java.util.List<MatchWithKeywords> matches = new java.util.ArrayList<>();

        for (Item it : foundItems) {
            if (!it.status.equalsIgnoreCase("ACTIVE"))
                continue;
            java.util.Set<String> itemTokens = it.getTokenSet();

            // Calculate Jaccard similarity
            double score = jaccard(queryTokens, itemTokens);

            if (score >= SIMILARITY_THRESHOLD) {
                // Find matching keywords (intersection)
                java.util.Set<String> matchingKeywords = new java.util.HashSet<>(queryTokens);
                matchingKeywords.retainAll(itemTokens);

                matches.add(new MatchWithKeywords(it, score, matchingKeywords));
            }
        }

        matches.sort((a, b) -> Double.compare(b.score, a.score));
        if (matches.isEmpty())
            return "No matches found.";

        for (MatchWithKeywords m : matches) {
            Item it = m.item;
            sb.append(String.format(
                    "ID: %s | Name: %s | Cat: %s | Loc: %s | Value: %.2f | Similarity: %.1f%%\\n",
                    it.id.substring(0, Math.min(8, it.id.length())), it.name, it.category, it.location,
                    it.estimatedValue, m.score * 100));

            // Show matching keywords
            if (!m.matchingKeywords.isEmpty()) {
                sb.append("  Matched words: ").append(String.join(", ", m.matchingKeywords)).append("\\n");
            }
            sb.append("\\n");
        }
        return sb.toString();
    }

    public String guiListItems(boolean showRemoved) {
        StringBuilder sb = new StringBuilder("Current Found Items:\\n\\n");
        int count = 0;
        for (Item it : foundItems) {
            if (!showRemoved && it.status.equals("REMOVED"))
                continue;
            sb.append(String.format("ID: %s | Name: %s | Cat: %s | Loc: %s | Date: %s | Status: %s\\n",
                    it.id.substring(0, Math.min(8, it.id.length())), it.name, it.category, it.location,
                    it.dateFound.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE), it.status));
            count++;
        }
        if (count == 0)
            sb.append("(none)\\n");
        return sb.toString();
    }

    public String guiRemoveItem(String id) {
        java.util.Optional<Item> opt = foundItems.stream().filter(i -> i.id.equals(id) || i.id.startsWith(id))
                .findFirst();
        if (opt.isEmpty())
            return "Item not found.";
        Item it = opt.get();
        it.status = "REMOVED";
        saveItemsToFile();
        log("REMOVE", it.summaryForLog());
        return "Item marked REMOVED: " + it.id;
    }

    // Statistics methods
    public int getTodayItemsCount() {
        LocalDate today = LocalDate.now();
        return (int) foundItems.stream()
                .filter(item -> item.dateFound.equals(today))
                .filter(item -> item.status.equals("ACTIVE"))
                .count();
    }

    public int getActiveItemsCount() {
        return (int) foundItems.stream()
                .filter(item -> item.status.equals("ACTIVE"))
                .count();
    }

    public int getPendingItemsCount() {
        // Pending: items that are ACTIVE (not yet claimed/resolved)
        return getActiveItemsCount();
    }

    // Helper class for matches with keywords
    private static class MatchWithKeywords {
        Item item;
        double score;
        java.util.Set<String> matchingKeywords;

        MatchWithKeywords(Item i, double s, java.util.Set<String> keywords) {
            item = i;
            score = s;
            matchingKeywords = keywords;
        }
    }

    // Public method for GUI to load data
    public void loadData() {
        loadItemsFromFile();
    }

    // Helper method to get active items for removal dialog
    public java.util.List<String[]> getActiveItemsForDialog() {
        java.util.List<String[]> result = new java.util.ArrayList<>();
        for (Item it : foundItems) {
            if (it.status.equals("ACTIVE")) {
                // Return array: [id, formatted display]
                String display = String.format("ID: %s | Name: %s | Cat: %s | Loc: %s",
                        it.id.substring(0, Math.min(8, it.id.length())),
                        it.name,
                        it.category,
                        it.location);
                result.add(new String[] { it.id, display });
            }
        }
        return result;
    }

}
