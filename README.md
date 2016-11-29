SingleThreadPoolWithIntervalTime
================================

java实现的线程池，可以在指定时间间隔内有只有一个任务执行，同时也可以设定在线程执行结束后释放的时间。

This thread pool will keep only one runnable is running at given time interval.
And if no task is to be executed and has been waiting for given aliveTimeMs time,
execute worker will be released.
