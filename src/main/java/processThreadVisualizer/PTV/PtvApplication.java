package processThreadVisualizer.PTV;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PtvApplication {

	public static void main(String[] args) {
		SpringApplication.run(PtvApplication.class, args);
	}

	@PostConstruct
	public void startDemoThreads() {
		for (int i = 1; i <= 5; i++) {
			int t = i;
			new Thread(() -> TestThreads.workerThread(t), "Worker-" + t).start();
		}
	}
}
