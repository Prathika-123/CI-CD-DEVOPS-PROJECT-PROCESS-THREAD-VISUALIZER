package processThreadVisualizer.PTV;

public class TestThreads {
    public static void workerThread(int id) {
        for (int j = 0; j < 5; j++) {   // runs only 5 times
            stepOne(id);
            stepTwo(id);
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
        System.out.println("Worker-" + id + " finished");
    }

    private static void stepOne(int id) {
        computeSomething(id);
    }

    private static void stepTwo(int id) {
        computeSomethingElse(id);
    }

    private static void computeSomething(int id) {
        int sum = 0;
        for (int i = 0; i < 5; i++) {
            sum += i * id;
        }
    }

    private static void computeSomethingElse(int id) {
        int prod = 1;
        for (int i = 1; i < 5; i++) {
            prod *= i + id;
        }
    }

    public static void mainTask() {
        helperMethod();
    }

    private static void helperMethod() {
        int x = 0;
        for(int i=0;i<10;i++) x += i;
    }
}
