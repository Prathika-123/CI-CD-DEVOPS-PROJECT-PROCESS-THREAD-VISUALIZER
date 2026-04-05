package processThreadVisualizer.PTV.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ThreadController {

    @GetMapping("/api/threads")
    public List<Map<String,String>> getThread(){
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false,false);

        List<Map<String,String>> threads = new ArrayList<>();

        for(ThreadInfo threadInfo : threadInfos){
            Map<String, String> t = new HashMap<>();
            t.put("id", String.valueOf(threadInfo.getThreadId()));
            t.put("name", threadInfo.getThreadName());
            t.put("state", threadInfo.getThreadState().toString());
            threads.add(t);
        }

        return threads;
    }
}
