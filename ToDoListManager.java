import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.*;

/**
 * ============================================================
 *  TaskFlow — To-Do List Manager  (Terminal Edition)
 *  Features: Add, View, Edit, Mark Done, Delete, Search,
 *            Filter, Sort, Statistics, Persistent File Storage
 * ============================================================
 */
public class ToDoListManager {

    // ──────────────────────────────────────────
    //  CONSTANTS
    // ──────────────────────────────────────────
    private static final String TASKS_FILE      = "tasks.txt";
    private static final String DATE_PATTERN    = "yyyy-MM-dd";
    private static final DateTimeFormatter FMT  = DateTimeFormatter.ofPattern(DATE_PATTERN);

    // ──────────────────────────────────────────
    //  STORAGE
    // ──────────────────────────────────────────
    private static final List<Task> tasks = new ArrayList<>();
    private static int idCounter = 1;

    // ============================================================
    //  TASK CLASS
    // ============================================================
    private static class Task {
        private final int    id;
        private       String title;
        private       String description;
        private       String priority;   // High | Medium | Low
        private       String category;
        private final String dateAdded;
        private       String dueDate;    // yyyy-MM-dd or ""
        private       boolean done;

        Task(int id, String title, String description, String priority,
             String category, String dateAdded, String dueDate, boolean done) {
            this.id          = id;
            this.title       = title;
            this.description = description;
            this.priority    = priority;
            this.category    = category;
            this.dateAdded   = dateAdded;
            this.dueDate     = dueDate;
            this.done        = done;
        }

        // ── Getters / Setters ──────────────────
        int     getId()          { return id; }
        String  getTitle()       { return title; }
        String  getDesc()        { return description; }
        String  getPriority()    { return priority; }
        String  getCategory()    { return category; }
        String  getDateAdded()   { return dateAdded; }
        String  getDueDate()     { return dueDate; }
        boolean isDone()         { return done; }

        void setTitle(String v)       { title       = v; }
        void setDesc(String v)        { description = v; }
        void setPriority(String v)    { priority    = v; }
        void setCategory(String v)    { category    = v; }
        void setDueDate(String v)     { dueDate     = v; }
        void setDone(boolean v)       { done        = v; }

        /** True if past due and still pending */
        boolean isOverdue() {
            if (done || dueDate == null || dueDate.isEmpty()) return false;
            try {
                return LocalDate.parse(dueDate, FMT).isBefore(LocalDate.now());
            } catch (DateTimeParseException e) { return false; }
        }

        String statusLabel() {
            if (done)        return "DONE    ";
            if (isOverdue()) return "OVERDUE ";
            return "PENDING ";
        }

        // ── File serialization ─────────────────
        // Format: id|title|desc|priority|category|dateAdded|dueDate|done
        String toFileLine() {
            return id + "|" + escape(title) + "|" + escape(description) + "|"
                 + priority + "|" + escape(category) + "|"
                 + dateAdded + "|" + dueDate + "|" + done;
        }

        static Task fromFileLine(String line) throws Exception {
            String[] p = line.split("\\|", -1);
            if (p.length != 8) throw new Exception("Expected 8 fields, got " + p.length);
            return new Task(
                Integer.parseInt(p[0].trim()),
                unescape(p[1]), unescape(p[2]),
                p[3].trim(), unescape(p[4]),
                p[5].trim(), p[6].trim(),
                Boolean.parseBoolean(p[7].trim())
            );
        }

        // Escape pipe characters in text fields
        private static String escape(String s) {
            return s == null ? "" : s.replace("|", "\\pipe;");
        }
        private static String unescape(String s) {
            return s == null ? "" : s.replace("\\pipe;", "|");
        }

        @Override
        public String toString() {
            String cat = category.isEmpty() ? "-" : category;
            String due = dueDate.isEmpty()  ? "-" : dueDate;
            return String.format("%-4d | %-25s | %-6s | %-12s | %-10s | %-10s | %s",
                    id, trunc(title,25), priority, trunc(cat,12), due, dateAdded, statusLabel());
        }
    }

    // ============================================================
    //  MAIN ENTRY POINT
    // ============================================================
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        printBanner();
        loadTasks();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt(sc);
            switch (choice) {
                case 1:  addTask(sc);       break;
                case 2:  viewAllTasks();    break;
                case 3:  editTask(sc);      break;
                case 4:  markTask(sc);      break;
                case 5:  deleteTask(sc);    break;
                case 6:  searchTasks(sc);   break;
                case 7:  filterMenu(sc);    break;
                case 8:  sortMenu(sc);      break;
                case 9:  showStats();       break;
                case 10:
                    System.out.println("\nGoodbye! Tasks saved to \"" + TASKS_FILE + "\".");
                    running = false;
                    break;
                default:
                    System.out.println("  [!] Invalid option. Choose 1–10.");
            }
        }
        sc.close();
    }

    // ============================================================
    //  MENU
    // ============================================================
    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║       TaskFlow — To-Do List Manager          ║");
        System.out.println("║              Terminal Edition                ║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    private static void printMenu() {
        long pending   = count(false);
        long completed = count(true);
        long overdue   = tasks.stream().filter(Task::isOverdue).count();

        System.out.println("\n┌──────────────────────────────────────────────┐");
        System.out.printf( "│  📋 Tasks: %d total  |  %d pending  |  %d done  |  %d overdue%n",
                tasks.size(), pending, completed, overdue);
        System.out.println("├──────────────────────────────────────────────┤");
        System.out.println("│  1. Add Task          6. Search Tasks        │");
        System.out.println("│  2. View All Tasks    7. Filter Tasks        │");
        System.out.println("│  3. Edit Task         8. Sort Tasks          │");
        System.out.println("│  4. Mark Done/Pending 9. View Statistics     │");
        System.out.println("│  5. Delete Task      10. Exit                │");
        System.out.println("└──────────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }

    // ============================================================
    //  FEATURE 1 — ADD TASK
    // ============================================================
    private static void addTask(Scanner sc) {
        System.out.println("\n──── ADD TASK ────");

        // Title (required, unique)
        System.out.print("Title: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) { System.out.println("  [!] Title cannot be empty."); return; }
        if (findByTitle(title) != null) {
            System.out.println("  [!] A task with that title already exists (ID: "
                    + findByTitle(title).getId() + ").");
            return;
        }

        // Description
        System.out.print("Description (optional, press Enter to skip): ");
        String desc = sc.nextLine().trim();

        // Priority
        String priority = readPriority(sc);

        // Category
        System.out.print("Category (optional, e.g. Work / Study / Personal): ");
        String category = sc.nextLine().trim();

        // Due Date
        String dueDate = readDate(sc, "Due Date (yyyy-MM-dd, or Enter to skip): ");

        Task t = new Task(idCounter++, title, desc, priority, category,
                          todayStr(), dueDate, false);
        tasks.add(t);
        saveTasks();

        System.out.println("\n  ✓ Task added successfully!");
        printHeader();
        System.out.println(t);
    }

    // ============================================================
    //  FEATURE 2 — VIEW ALL TASKS
    // ============================================================
    private static void viewAllTasks() {
        displayList(tasks, "ALL TASKS");
    }

    // ============================================================
    //  FEATURE 3 — EDIT TASK
    // ============================================================
    private static void editTask(Scanner sc) {
        System.out.println("\n──── EDIT TASK ────");
        if (tasks.isEmpty()) { System.out.println("  No tasks to edit."); return; }

        viewAllTasks();

        System.out.print("\n  Enter Task ID to edit: ");
        int id = readInt(sc);
        Task t = findById(id);
        if (t == null) { System.out.println("  [!] ID " + id + " not found."); return; }

        System.out.println("\n  Editing: \"" + t.getTitle() + "\"");
        System.out.println("  (Press Enter to keep the current value)");

        // Title
        System.out.print("  New title [" + t.getTitle() + "]: ");
        String v = sc.nextLine().trim();
        if (!v.isEmpty()) t.setTitle(v);

        // Description
        System.out.print("  New description [" + trunc(t.getDesc(),30) + "]: ");
        v = sc.nextLine().trim();
        if (!v.isEmpty()) t.setDesc(v);

        // Priority
        System.out.println("  New priority (1=High, 2=Medium, 3=Low, Enter=keep [" + t.getPriority() + "]): ");
        System.out.print("  Choice: ");
        String p = sc.nextLine().trim();
        if (!p.isEmpty()) {
            switch (p) {
                case "1": t.setPriority("High");   break;
                case "2": t.setPriority("Medium"); break;
                case "3": t.setPriority("Low");    break;
                default:  System.out.println("  Invalid priority choice, kept unchanged.");
            }
        }

        // Category
        System.out.print("  New category [" + t.getCategory() + "]: ");
        v = sc.nextLine().trim();
        if (!v.isEmpty()) t.setCategory(v);

        // Due Date
        System.out.print("  New due date yyyy-MM-dd [" + t.getDueDate() + "] (Enter=keep, 'clear' to remove): ");
        v = sc.nextLine().trim();
        if (v.equalsIgnoreCase("clear")) {
            t.setDueDate("");
        } else if (!v.isEmpty()) {
            try {
                LocalDate.parse(v, FMT);
                t.setDueDate(v);
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid date format — kept unchanged.");
            }
        }

        // Status
        System.out.print("  New status (1=Pending, 2=Done, Enter=keep [" + t.statusLabel().trim() + "]): ");
        v = sc.nextLine().trim();
        if (v.equals("1")) t.setDone(false);
        else if (v.equals("2")) t.setDone(true);

        saveTasks();
        System.out.println("\n  ✓ Task updated!");
        printHeader();
        System.out.println(t);
    }

    // ============================================================
    //  FEATURE 4 — MARK DONE / PENDING
    // ============================================================
    private static void markTask(Scanner sc) {
        System.out.println("\n──── MARK TASK ────");
        if (tasks.isEmpty()) { System.out.println("  No tasks available."); return; }

        viewAllTasks();
        System.out.print("\n  Enter Task ID: ");
        int id = readInt(sc);
        Task t = findById(id);
        if (t == null) { System.out.println("  [!] ID " + id + " not found."); return; }

        System.out.println("  \"" + t.getTitle() + "\" is currently [" + t.statusLabel().trim() + "]");
        System.out.println("  1. Mark as Done");
        System.out.println("  2. Mark as Pending");
        System.out.print("  Choice (1/2): ");
        int ch = readInt(sc);

        if (ch == 1) {
            if (t.isDone()) System.out.println("  Already marked as Done.");
            else { t.setDone(true); saveTasks(); System.out.println("  ✓ Marked as DONE: \"" + t.getTitle() + "\""); }
        } else if (ch == 2) {
            if (!t.isDone()) System.out.println("  Already marked as Pending.");
            else { t.setDone(false); saveTasks(); System.out.println("  ✓ Marked as PENDING: \"" + t.getTitle() + "\""); }
        } else {
            System.out.println("  Invalid option. No changes made.");
        }
    }

    // ============================================================
    //  FEATURE 5 — DELETE TASK
    // ============================================================
    private static void deleteTask(Scanner sc) {
        System.out.println("\n──── DELETE TASK ────");
        if (tasks.isEmpty()) { System.out.println("  No tasks to delete."); return; }

        viewAllTasks();
        System.out.print("\n  Enter Task ID to delete: ");
        int id = readInt(sc);
        Task t = findById(id);
        if (t == null) { System.out.println("  [!] ID " + id + " not found."); return; }

        System.out.println("  Task: \"" + t.getTitle() + "\" [" + t.getPriority() + "]");
        System.out.print("  Confirm delete? (Y/N): ");
        String c = sc.nextLine().trim();
        if (c.equalsIgnoreCase("Y")) {
            tasks.remove(t);
            saveTasks();
            System.out.println("  ✓ Task deleted.");
        } else {
            System.out.println("  Delete cancelled.");
        }
    }

    // ============================================================
    //  FEATURE 6 — SEARCH TASKS
    // ============================================================
    private static void searchTasks(Scanner sc) {
        System.out.println("\n──── SEARCH TASKS ────");
        System.out.print("  Keyword (searches title, description, category): ");
        String kw = sc.nextLine().trim().toLowerCase();
        if (kw.isEmpty()) { System.out.println("  No keyword entered."); return; }

        List<Task> results = new ArrayList<>();
        for (Task t : tasks) {
            String hay = (t.getTitle() + " " + t.getDesc() + " " + t.getCategory()).toLowerCase();
            if (hay.contains(kw)) results.add(t);
        }
        displayList(results, "SEARCH RESULTS for \"" + kw + "\"");
    }

    // ============================================================
    //  FEATURE 7 — FILTER TASKS
    // ============================================================
    private static void filterMenu(Scanner sc) {
        System.out.println("\n──── FILTER TASKS ────");
        System.out.println("  1. All Tasks");
        System.out.println("  2. Pending");
        System.out.println("  3. Completed");
        System.out.println("  4. High Priority");
        System.out.println("  5. Due Today");
        System.out.println("  6. Overdue");
        System.out.println("  7. By Category");
        System.out.print("  Choose (1-7): ");
        int ch = readInt(sc);

        String td = todayStr();
        List<Task> result = new ArrayList<>();
        String label;

        switch (ch) {
            case 1:
                result.addAll(tasks); label = "ALL TASKS";
                break;
            case 2:
                for (Task t : tasks) if (!t.isDone()) result.add(t);
                label = "PENDING TASKS";
                break;
            case 3:
                for (Task t : tasks) if ( t.isDone()) result.add(t);
                label = "COMPLETED TASKS";
                break;
            case 4:
                for (Task t : tasks) if ("High".equals(t.getPriority())) result.add(t);
                label = "HIGH PRIORITY TASKS";
                break;
            case 5:
                for (Task t : tasks) if (td.equals(t.getDueDate())) result.add(t);
                label = "DUE TODAY (" + td + ")";
                break;
            case 6:
                for (Task t : tasks) if (t.isOverdue()) result.add(t);
                label = "OVERDUE TASKS";
                break;
            case 7:
                System.out.print("  Enter category name: ");
                String cat = sc.nextLine().trim();
                for (Task t : tasks) if (cat.equalsIgnoreCase(t.getCategory())) result.add(t);
                label = "CATEGORY: " + cat;
                break;
            default:
                System.out.println("  Invalid option."); return;
        }
        displayList(result, label);
    }

    // ============================================================
    //  FEATURE 8 — SORT TASKS
    // ============================================================
    private static void sortMenu(Scanner sc) {
        System.out.println("\n──── SORT TASKS ────");
        System.out.println("  1. Priority (High → Low)");
        System.out.println("  2. Due Date (soonest first)");
        System.out.println("  3. Date Added (newest first)");
        System.out.println("  4. Title (A → Z)");
        System.out.print("  Choose (1-4): ");
        int ch = readInt(sc);

        List<Task> sorted = new ArrayList<>(tasks);
        Map<String, Integer> pw = new HashMap<>();
        pw.put("High", 0); pw.put("Medium", 1); pw.put("Low", 2);

        String label;
        switch (ch) {
            case 1:
                sorted.sort(Comparator.comparingInt(t -> pw.getOrDefault(t.getPriority(), 1)));
                label = "SORTED BY PRIORITY";
                break;
            case 2:
                sorted.sort((a, b) -> {
                    boolean aEmpty = a.getDueDate().isEmpty(), bEmpty = b.getDueDate().isEmpty();
                    if (aEmpty && bEmpty) return 0;
                    if (aEmpty) return 1;
                    if (bEmpty) return -1;
                    return a.getDueDate().compareTo(b.getDueDate());
                });
                label = "SORTED BY DUE DATE";
                break;
            case 3:
                sorted.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
                label = "SORTED BY DATE ADDED (newest first)";
                break;
            case 4:
                sorted.sort(Comparator.comparing(t -> t.getTitle().toLowerCase()));
                label = "SORTED BY TITLE";
                break;
            default:
                System.out.println("  Invalid option."); return;
        }
        displayList(sorted, label);
    }

    // ============================================================
    //  FEATURE 9 — STATISTICS
    // ============================================================
    private static void showStats() {
        long total     = tasks.size();
        long pending   = count(false);
        long completed = count(true);
        long overdue   = tasks.stream().filter(Task::isOverdue).count();

        // Count by priority
        long high   = tasks.stream().filter(t -> "High".equals(t.getPriority())).count();
        long medium = tasks.stream().filter(t -> "Medium".equals(t.getPriority())).count();
        long low    = tasks.stream().filter(t -> "Low".equals(t.getPriority())).count();

        // Completion rate
        double rate = total > 0 ? (completed * 100.0 / total) : 0;

        System.out.println("\n──── TASK STATISTICS ────");
        System.out.println("  Total Tasks    : " + total);
        System.out.println("  Pending        : " + pending);
        System.out.println("  Completed      : " + completed);
        System.out.println("  Overdue        : " + overdue);
        System.out.println("  ────────────────────────");
        System.out.println("  High Priority  : " + high);
        System.out.println("  Medium Priority: " + medium);
        System.out.println("  Low Priority   : " + low);
        System.out.println("  ────────────────────────");
        System.out.printf( "  Completion Rate: %.1f%%%n", rate);

        // Simple bar for completion rate
        int filled = (int)(rate / 5);
        System.out.print("  [");
        for (int i = 0; i < 20; i++) System.out.print(i < filled ? "█" : "░");
        System.out.printf("] %.1f%%%n", rate);
    }

    // ============================================================
    //  FILE I/O
    // ============================================================

    /**
     * Loads tasks from TASKS_FILE on startup.
     * Gracefully handles missing file or malformed lines.
     */
    private static void loadTasks() {
        File file = new File(TASKS_FILE);
        if (!file.exists()) {
            System.out.println("  No task file found — starting fresh.");
            return;
        }
        int loaded = 0, skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    Task t = Task.fromFileLine(line);
                    tasks.add(t);
                    if (t.getId() >= idCounter) idCounter = t.getId() + 1;
                    loaded++;
                } catch (Exception e) {
                    System.out.println("  Skipping bad entry: " + line);
                    skipped++;
                }
            }
            System.out.printf("  Loaded %d task(s) from %s%s.%n",
                    loaded, TASKS_FILE,
                    skipped > 0 ? " (" + skipped + " skipped)" : "");
        } catch (IOException e) {
            System.out.println("  Error reading file: " + e.getMessage());
        }
    }

    /**
     * Overwrites TASKS_FILE with the current task list.
     * Called immediately after every state change.
     */
    private static void saveTasks() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(TASKS_FILE))) {
            pw.println("# TaskFlow — To-Do List Manager");
            pw.println("# Format: id|title|desc|priority|category|dateAdded|dueDate|done");
            pw.println("# DO NOT EDIT MANUALLY");
            for (Task t : tasks) pw.println(t.toFileLine());
        } catch (IOException e) {
            System.out.println("  [!] Error saving tasks: " + e.getMessage());
        }
    }

    // ============================================================
    //  DISPLAY HELPERS
    // ============================================================
    private static void printHeader() {
        String sep = "─".repeat(85);
        System.out.println(sep);
        System.out.printf("%-4s | %-25s | %-6s | %-12s | %-10s | %-10s | %s%n",
                "ID", "Title", "Prior.", "Category", "Due", "Added", "Status");
        System.out.println(sep);
    }

    private static void displayList(List<Task> list, String label) {
        System.out.println("\n──── " + label + " (" + list.size() + " task(s)) ────");
        if (list.isEmpty()) {
            System.out.println("  No tasks to display.");
            return;
        }
        printHeader();
        for (Task t : list) System.out.println(t);
        System.out.println("─".repeat(85));
        System.out.println("  Showing " + list.size() + " of " + tasks.size() + " task(s).");
    }

    // ============================================================
    //  UTILITY HELPERS
    // ============================================================
    private static Task findById(int id) {
        for (Task t : tasks) if (t.getId() == id) return t;
        return null;
    }

    private static Task findByTitle(String title) {
        for (Task t : tasks)
            if (t.getTitle().equalsIgnoreCase(title)) return t;
        return null;
    }

    private static long count(boolean done) {
        int c = 0;
        for (Task t : tasks) if (t.isDone() == done) c++;
        return c;
    }

    private static String todayStr() {
        return LocalDate.now().format(FMT);
    }

    /** Truncates string to max length, appending "…" if needed. */
    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * Reads priority from user: 1=High, 2=Medium, 3=Low.
     * Defaults to Medium on invalid input.
     */
    private static String readPriority(Scanner sc) {
        System.out.println("  Priority:  1. High   2. Medium   3. Low");
        System.out.print("  Choose (1-3): ");
        int p = readInt(sc);
        switch (p) {
            case 1:  return "High";
            case 3:  return "Low";
            default:
                if (p != 2) System.out.println("  Invalid choice, defaulting to Medium.");
                return "Medium";
        }
    }

    /**
     * Reads an optional date string in yyyy-MM-dd format.
     * Returns empty string if user presses Enter.
     * Re-prompts on invalid format.
     */
    private static String readDate(Scanner sc, String prompt) {
        while (true) {
            System.out.print("  " + prompt);
            String v = sc.nextLine().trim();
            if (v.isEmpty()) return "";
            try {
                LocalDate.parse(v, FMT);
                return v;
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Use yyyy-MM-dd (e.g. 2025-12-31).");
            }
        }
    }

    /**
     * Reads a valid integer from the Scanner.
     * Re-prompts on non-numeric input.
     */
    private static int readInt(Scanner sc) {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("  Invalid input. Enter a number: ");
            }
        }
    }
}
