package processThreadVisualizer.PTV.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import processThreadVisualizer.PTV.jdi.JdiService;
import processThreadVisualizer.PTV.model.ThreadInfo;

import java.util.*;


@RestController
@RequestMapping("/api")
public class ThreadController {

    @Autowired
    private JdiService jdiService;

    // SimpMessagingTemplate sends messages to WebSocket subscribers
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Read from application.properties — defaults to localhost:5005
    @Value("${jdi.host:localhost}")
    private String jdiHost;

    @Value("${jdi.port:5005}")
    private int jdiPort;


    @PostConstruct
    public void init() {
       boolean ok = false;
 try {
        ok = jdiService.connect(jdiHost, jdiPort);
    } catch (Exception e) {
        System.out.println("⚠️ Skipping JDI connection (not available in container)");
    }
        if (ok) {
            // Start the event-driven loop — no more 1-second polling needed
            jdiService.startEventLoop(messagingTemplate);
        }
        else if (!ok) {
            System.out.println("⚠️  Could not connect at startup.");
            System.out.println("   Use POST /api/connect to retry after starting the target program.");
        }
    }


    @Scheduled(fixedRate = 2000)
    public void pushThreadData() {
        if (!jdiService.isConnected()) return;
        // Uses suspend→read→resume so we get EXACT line numbers
        List<ThreadInfo> threads = jdiService.getThreadsSnapshot();
        System.out.println("Pushing " + threads.size() + " threads");
        messagingTemplate.convertAndSend("/topic/threads", threads);
    }

    /** GET /api/status — is Spring Boot currently connected to target JVM? */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("connected", jdiService.isConnected());
        resp.put("host", jdiHost);
        resp.put("port", jdiPort);
        return resp;
    }

    /** GET /api/threads — one-time snapshot of all threads (not live) */
    @GetMapping("/threads")
    public List<ThreadInfo> getThreadsOnce() {
        return jdiService.getThreads();
    }

    @PostMapping("/connect")
    public Map<String, Object> connect(
            @RequestParam(defaultValue = "localhost") String host,
            @RequestParam(defaultValue = "5005") int port) {

        jdiService.stopEventLoop(); // stop old loop if any
        boolean ok = jdiService.connect(host, port);
        if (ok) jdiService.startEventLoop(messagingTemplate);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", ok);
        resp.put("message", ok ? "Connected" : "Failed to connect");
        return resp;
    }

    @PreDestroy
    public void cleanup() {
        jdiService.stopEventLoop();
        jdiService.disconnect();
    }

    /** POST /api/suspend — freeze the target program */
    @PostMapping("/suspend")
    public Map<String, String> suspend() {
        jdiService.suspendVM();
        Map<String, String> r = new HashMap<>();
        r.put("message", "JVM suspended");
        return r;
    }

    /** POST /api/resume — unfreeze the target program */
    @PostMapping("/resume")
    public Map<String, String> resume() {
        jdiService.resumeVM();
        Map<String, String> r = new HashMap<>();
        r.put("message", "JVM resumed");
        return r;
    }

    /** POST /api/step — resume JVM, capture one snapshot, suspend again */
    @PostMapping("/step")
    public Map<String, Object> step() throws InterruptedException {
        // Resume briefly
        jdiService.resumeVM();
        // Wait 100ms for one instruction to execute
        Thread.sleep(100);
        // Suspend again
        jdiService.suspendVM();
        // Capture current state
        List<ThreadInfo> threads = jdiService.getThreadsSnapshot();
        // Push to WebSocket immediately
        messagingTemplate.convertAndSend("/topic/threads", threads);

        Map<String, Object> r = new HashMap<>();
        r.put("message", "Stepped");
        r.put("threads", threads.size());
        return r;
    }


    @PostMapping("/speed")
    public Map<String, Object> setSpeed(@RequestParam long delayMs) {
        jdiService.setStepDelay(delayMs);
        Map<String, Object> r = new HashMap<>();
        r.put("delayMs", delayMs);
        r.put("message", "Speed updated");
        return r;
    }

    @GetMapping("/speed")
    public Map<String, Object> getSpeed() {
        Map<String, Object> r = new HashMap<>();
        r.put("delayMs", jdiService.getStepDelay());
        return r;
    }
}
