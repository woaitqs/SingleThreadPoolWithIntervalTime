import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This thread pool will keep only one runnable is running at given time interval.
 * And if no task is to be executed and has been waiting for given aliveTimeMs time,
 * execute worker will be released.
 */
public class SingleThreadPoolWithIntervalTime {

  private final long minIntervalTimeMs;

  private final long aliveTimeMs;

  private final BlockingQueue<Runnable> waitingTasks;

  private final Set<Worker> workers = new HashSet<Worker>();

  private final int maxCurrentThread = 1;

  /**
   * Constructor
   *
   * @param intervalTime
   * @param aliveTimeMs
   * @param queue
   */
  public SingleThreadPoolWithIntervalTime(
      long intervalTime, long aliveTimeMs, BlockingQueue<Runnable> queue) {
    this.minIntervalTimeMs = intervalTime;
    this.aliveTimeMs = aliveTimeMs;
    this.waitingTasks = queue;
  }

  public SingleThreadPoolWithIntervalTime(
      long intervalTimeMs, BlockingQueue<Runnable> queue) {
    this(intervalTimeMs, 0L, queue);
  }

  public SingleThreadPoolWithIntervalTime(long intervalTime) {
    this(intervalTime, 0L, new LinkedBlockingQueue<Runnable>());
  }

  public SingleThreadPoolWithIntervalTime() {
    this(0L, 0L, new LinkedBlockingQueue<Runnable>());
  }

  /**
   * add task to waiting queue and waiting to be execute.
   *
   * @param task
   */
  public void excute(Runnable task) {
    if (task == null) {
      throw new IllegalArgumentException("runnable can't be null");
    }
    synchronized (waitingTasks) {
      waitingTasks.offer(task);
    }
    synchronized (workers) {
      if (workers.size() < maxCurrentThread) {
        Worker worker = new Worker();
        workers.add(worker);
        worker.start();
      }
    }
  }

  /**
   * Submit callable task and return future task.
   *
   * @param callable
   * @param <T>
   * @return future task.
   */
  public <T> Future<T> submit(Callable<T> callable) {
    if (callable == null) {
      throw new IllegalArgumentException("callable can't be null");
    }
    RunnableFuture<T> task = new FutureTask<T>(callable);
    excute(task);
    return task;
  }

  private class Worker extends Thread {
    private Runnable runnable;

    @Override
    public void run() {
      long lastExecuteTime = 0L;
      try {
        while (true) {
          if (aliveTimeMs > 0) {
            runnable = waitingTasks.poll(aliveTimeMs, TimeUnit.MILLISECONDS);
          } else {
            runnable = waitingTasks.poll();
          }
          // calc interval time between two tasks.
          long now = System.currentTimeMillis();
          if (now - lastExecuteTime < minIntervalTimeMs) {
            sleep(now - lastExecuteTime);
          }
          lastExecuteTime = System.currentTimeMillis();
          if (runnable != null) {
            runnable.run();
          } else {
            break;
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      // no task to execute, so remove from worker list.
      synchronized (workers) {
        workers.remove(this);
        if (workers.isEmpty()) {
          workers.notifyAll();
        }
      }
    }
  }

}
