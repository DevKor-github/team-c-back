package devkor.com.teamcback;


public class TestTimer {

    public static void run(String label, Runnable testLogic) {
        long start = System.currentTimeMillis();
        testLogic.run();
        long duration = System.currentTimeMillis() - start;
        System.out.printf("⏱️ [%s] 실행 시간: %dms%n", label, duration);
    }

}
