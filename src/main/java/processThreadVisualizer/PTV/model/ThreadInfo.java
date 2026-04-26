package processThreadVisualizer.PTV.model;

public class ThreadInfo {

    private long id;
    private String name;
    private String state;          // RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED
    private String currentMethod;  // The method currently executing (top of stack)
    private String sourceFile;     // Source file name (e.g. "Main.java")
    private int lineNumber;        // Current line number being executed
    private String threadGroup;    // Thread group name

    // Default constructor (required for Jackson JSON serialization)
    public ThreadInfo() {}

    public ThreadInfo(long id, String name, String state,
                      String currentMethod, String sourceFile,
                      int lineNumber, String threadGroup) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.currentMethod = currentMethod;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.threadGroup = threadGroup;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCurrentMethod() { return currentMethod; }
    public void setCurrentMethod(String currentMethod) { this.currentMethod = currentMethod; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public String getThreadGroup() { return threadGroup; }
    public void setThreadGroup(String threadGroup) { this.threadGroup = threadGroup; }
}
