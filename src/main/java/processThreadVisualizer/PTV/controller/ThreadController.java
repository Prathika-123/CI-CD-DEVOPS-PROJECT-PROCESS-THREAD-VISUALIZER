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
        System.out.println("Attempting to connect to target JVM at " + jdiHost + ":" + jdiPort + "...");
        boolean ok = jdiService.connect(jdiHost, jdiPort);
        if (!ok) {
            System.out.println("⚠️  Could not connect at startup.");
            System.out.println("   Use POST /api/connect to retry after starting the target program.");
        }
    }


    @Scheduled(fixedRate = 1000)
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

    /** POST /api/connect — manually reconnect to target JVM */
    @PostMapping("/connect")
    public Map<String, Object> connect(
            @RequestParam(defaultValue = "localhost") String host,
            @RequestParam(defaultValue = "5005") int port) {

        boolean ok = jdiService.connect(host, port);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", ok);
        resp.put("message", ok ? "Connected to JVM at " + host + ":" + port
                : "Failed to connect. Is the target program running with -agentlib:jdwp?");
        return resp;
    }

    /** Called when Spring Boot shuts down — cleanly disconnect from target JVM */
    @PreDestroy
    public void cleanup() {
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

}
