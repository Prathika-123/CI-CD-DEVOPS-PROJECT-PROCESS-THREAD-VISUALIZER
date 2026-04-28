package processThreadVisualizer.PTV.model;

import java.util.List;
import java.util.Map;

public class ThreadInfo {

    private long id;
    private String name;
    private String state;
    private String currentMethod;
    private String sourceFile;
    private int lineNumber;
    private String threadGroup;
    private List<Map<String, String>> variables; // ← NEW: list of {name, value} maps

    public ThreadInfo() {}

    public ThreadInfo(long id, String name, String state,
                      String currentMethod, String sourceFile,
                      int lineNumber, String threadGroup,
                      List<Map<String, String>> variables) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.currentMethod = currentMethod;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.threadGroup = threadGroup;
        this.variables = variables;
    }

    // All existing getters/setters unchanged
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCurrentMethod() { return currentMethod; }
    public void setCurrentMethod(String m) { this.currentMethod = m; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String f) { this.sourceFile = f; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int l) { this.lineNumber = l; }
    public String getThreadGroup() { return threadGroup; }
    public void setThreadGroup(String g) { this.threadGroup = g; }

    // NEW getter/setter
    public List<Map<String, String>> getVariables() { return variables; }
    public void setVariables(List<Map<String, String>> v) { this.variables = v; }
}