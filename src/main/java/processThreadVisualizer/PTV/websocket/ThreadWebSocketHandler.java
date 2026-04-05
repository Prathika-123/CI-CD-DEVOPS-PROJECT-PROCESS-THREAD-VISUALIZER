package processThreadVisualizer.PTV.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.management.*;
import java.util.*;

public class ThreadWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    // 🔥 Store history
    private static final Map<Long, List<Map<String, String>>> threadHistory = new HashMap<>();

    // 🔥 Store last known thread info (IMPORTANT FIX)
    private static final Map<Long, Map<String, Object>> threadMeta = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        new Thread(() -> {
            try {
                while (session.isOpen()) {

                    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                    ThreadInfo[] infos = bean.dumpAllThreads(true, true);

                    List<Map<String, Object>> threads = new ArrayList<>();
                    Set<Long> currentIds = new HashSet<>();

                    for (ThreadInfo info : infos) {

                        String threadName = info.getThreadName();

                        // ✅ Only your threads
                        if (!threadName.startsWith("Worker")) continue;

                        long id = info.getThreadId();
                        currentIds.add(id);

                        Map<String, Object> t = new HashMap<>();
                        t.put("id", id);
                        t.put("name", threadName);
                        t.put("state", info.getThreadState().toString());

                        // 🔍 Class info
                        boolean found = false;
                        for (StackTraceElement elem : info.getStackTrace()) {
                            if (elem.getClassName().startsWith("processThreadVisualizer.PTV")) {
                                t.put("class", elem.getClassName());
                                t.put("method", elem.getMethodName());
                                t.put("line", elem.getLineNumber());
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            t.put("class", "-");
                            t.put("method", "-");
                            t.put("line", "-");
                        }

                        // 🔥 SAVE META (important)
                        threadMeta.put(id, t);

                        // 🔥 HISTORY
                        threadHistory.putIfAbsent(id, new ArrayList<>());
                        List<Map<String,String>> hist = threadHistory.get(id);

                        String currentState = info.getThreadState().toString();
                        String lastState = hist.isEmpty() ? "" : hist.get(hist.size()-1).get("state");

                        if (hist.isEmpty() || !lastState.equals(currentState)) {
                            Map<String,String> snap = new HashMap<>();
                            snap.put("state", currentState);
                            snap.put("time", String.valueOf(System.currentTimeMillis()));
                            hist.add(snap);
                        }

                        t.put("history", hist);

                        threads.add(t);
                    }

                    // 🔥 ADD TERMINATED THREADS (FIXED PROPERLY)
                    for (Long id : threadHistory.keySet()) {

                        if (!currentIds.contains(id)) {

                            Map<String, Object> old = threadMeta.get(id);
                            if (old == null) continue;

                            Map<String, Object> t = new HashMap<>(old);
                            t.put("state", "TERMINATED");
                            t.put("history", threadHistory.get(id));

                            threads.add(t);
                        }
                    }

                    session.sendMessage(
                            new TextMessage(mapper.writeValueAsString(threads))
                    );

                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}