package processThreadVisualizer.PTV.jdi;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import org.springframework.stereotype.Service;
import processThreadVisualizer.PTV.model.ThreadInfo;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class JdiService {

    private VirtualMachine vm = null;
    private boolean connected = false;


    public boolean connect(String host, int port) {
        try {
            VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

            AttachingConnector connector = vmm.attachingConnectors()
                    .stream()
                    .filter(c -> c.name().contains("SocketAttach"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No SocketAttach connector found"));

            // Print all available argument names so we can debug
            System.out.println("Connector: " + connector.name());
            connector.defaultArguments().forEach((k, v) ->
                    System.out.println("  arg: " + k + " = " + v.value()));

            Map<String, Connector.Argument> args = connector.defaultArguments();

            // Try both possible key names
            if (args.containsKey("host")) {
                args.get("host").setValue(host);
            } else if (args.containsKey("hostname")) {
                args.get("hostname").setValue(host);
            }

            if (args.containsKey("port")) {
                args.get("port").setValue(String.valueOf(port));
            }

            this.vm = connector.attach(args);
            this.connected = true;
            System.out.println(" Connected to JVM: " + vm.name());
            return true;

        } catch (Exception e) {
            System.err.println(" Failed to connect: " + e.getMessage());
            e.printStackTrace();
            this.connected = false;
            return false;
        }
    }


    public List<ThreadInfo> getThreads() {
        if (!connected || vm == null) {
            return Collections.emptyList();
        }

        try {
            return vm.allThreads().stream()
                    .map(this::buildThreadInfo)
                    .collect(Collectors.toList());

        } catch (VMDisconnectedException e) {
            System.err.println("Target JVM disconnected.");
            connected = false;
            vm = null;
            return Collections.emptyList();
        }
    }


    private ThreadInfo buildThreadInfo(ThreadReference t) {
        String state = resolveState(t);
        String method = "unknown";
        String sourceFile = "-";
        int lineNumber = -1;
        String groupName = "-";
        List<Map<String, String>> variables = new ArrayList<>();


        // Fix group name
        try {
            if (t.threadGroup() != null) {
                groupName = t.threadGroup().name();
            }
        } catch (Exception e) {
            groupName = "unknown";
        }

        // Read stack frames + variables
        try {
            t.suspend();
            try {
                List<StackFrame> frames = t.frames();

                for (StackFrame frame : frames) {
                    Location loc = frame.location();
                    String className = loc.declaringType().name();

                    // Skip JDK internal classes — look for YOUR code
                    if (className.startsWith("java.") ||
                            className.startsWith("javax.") ||
                            className.startsWith("sun.") ||
                            className.startsWith("jdk.") ||
                            className.startsWith("com.sun.")) {
                        // Still set method if nothing found yet
                        if (method.equals("unknown")) {
                            method = className + "." + loc.method().name() + "()";
                        }
                        continue; // skip to next frame
                    }

                    // This is YOUR code — use this frame
                    int line = loc.lineNumber();
                    String file = "-";
                    try { file = loc.sourceName(); }
                    catch (AbsentInformationException e) {
                        file = className + ".java";
                    }

                    method = className + "." + loc.method().name() + "()";
                    sourceFile = file;
                    lineNumber = line;

                    // Read local variables from YOUR frame
                    try {
                        List<LocalVariable> localVars = frame.visibleVariables();
                        for (LocalVariable lv : localVars) {
                            try {
                                Value val = frame.getValue(lv);
                                Map<String, String> varMap = new HashMap<>();
                                varMap.put("name", lv.name());
                                varMap.put("type", lv.typeName());
                                varMap.put("value", val != null ? val.toString() : "null");
                                variables.add(varMap);
                            } catch (Exception ignored) {}
                        }
                    } catch (AbsentInformationException e) {
                        // No debug info for this frame
                    }
                    break; // Found your code — stop
                }
            } finally {
                t.resume();
            }
        } catch (IncompatibleThreadStateException e) {
            method = "(running)";
        } catch (Exception e) {
            method = "(error reading)";
        }

        return new ThreadInfo(
                t.uniqueID(),
                t.name(),
                state,
                method,
                sourceFile,
                lineNumber,
                groupName,
                variables   // ← pass variables
        );
    }
    private String resolveState(ThreadReference t) {
        return switch (t.status()) {
            case ThreadReference.THREAD_STATUS_RUNNING  -> "RUNNABLE";
            case ThreadReference.THREAD_STATUS_SLEEPING -> "TIMED_WAITING";
            case ThreadReference.THREAD_STATUS_WAIT     -> "WAITING";
            case ThreadReference.THREAD_STATUS_MONITOR  -> "BLOCKED";
            case ThreadReference.THREAD_STATUS_ZOMBIE   -> "TERMINATED";
            case ThreadReference.THREAD_STATUS_NOT_STARTED -> "NEW";
            default -> "UNKNOWN";
        };
    }

    public boolean isConnected() {
        return connected && vm != null;
    }


    public void disconnect() {
        if (vm != null) {
            try {
                vm.dispose();
                System.out.println("Disconnected from target JVM.");
            } catch (Exception ignored) {}
            vm = null;
            connected = false;
        }
    }


    public void suspendVM() {
        if (vm != null) vm.suspend();
    }


    public void resumeVM() {
        if (vm != null) vm.resume();
    }


    public List<ThreadInfo> getThreadsSnapshot() {
        if (!connected || vm == null) return Collections.emptyList();
        try {
            vm.suspend();          // freeze everything
            List<ThreadInfo> result = vm.allThreads()
                    .stream()
                    .map(this::buildThreadInfo)
                    .collect(Collectors.toList());
            vm.resume();           // unfreeze
            return result;
        } catch (VMDisconnectedException e) {
            connected = false;
            vm = null;
            return Collections.emptyList();
        }
    }
}