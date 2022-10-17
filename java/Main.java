import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Main {
    final static int maxContainersCount = 40;
    final static int berthsCount = 3;

    final static ReentrantLock accountingLock = new ReentrantLock();
    static int containersCount = 0;
    public static void main(String[] args) {
        List<Ship> ships = new ArrayList<>();
        ships.add(new Ship(0, 5));
        ships.add(new Ship(0, 2));
        ships.add(new Ship(10, 0));
        ships.add(new Ship(4, 0));
        ships.add(new Ship(2, 10));
        ships.add(new Ship(2, 0));

        ExecutorService executor = Executors.newFixedThreadPool(berthsCount);
        List<Callable<Void>> tasks = ships.stream().map(Main::handleShip).collect(Collectors.toList());
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
        System.out.printf("All ships served. Port has %d containers", containersCount);
    }

    private static Callable<Void> handleShip(Ship ship) {
        return () -> {
            int comeDelay = ThreadLocalRandom.current().nextInt(500, 1500);
            TimeUnit.MILLISECONDS.sleep(comeDelay);

            for(;;) {
                accountingLock.lock();
                try {
                    if (containersCount + ship.toUnload <= maxContainersCount) {
                        containersCount += ship.toUnload;
                        break;
                    } else {
                        System.out.printf("Waiting to unload %d containers, current: %d\n", ship.toUnload, containersCount);
                    }
                } finally {
                    accountingLock.unlock();
                }
                TimeUnit.SECONDS.sleep(1);
            }

            for(;;) {
                accountingLock.lock();
                try {
                    if (containersCount - ship.toUpload >= 0) {
                        containersCount -= ship.toUpload;
                        break;
                    } else {
                        System.out.printf("Waiting to upload %d containers, current: %d\n", ship.toUpload, containersCount);
                    }
                } finally {
                    accountingLock.unlock();
                }
                TimeUnit.SECONDS.sleep(1);
            }

            int leaveDelay = ThreadLocalRandom.current().nextInt(1500, 2000);
            TimeUnit.MILLISECONDS.sleep(leaveDelay);
            System.out.printf("Ship served: %d, %d\n", ship.toUnload, ship.toUpload);
            return null;
        };
    }
}
