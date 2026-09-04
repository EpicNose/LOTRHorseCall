package com.epicnose.lotrcallablehorse.lotr.common;

import cpw.mods.fml.common.FMLLog;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class CallableHorseServerTasks {
    private static final int MAX_PENDING_TASKS = 1024;
    private static final int MAX_TASKS_PER_TICK = 64;
    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<Runnable>();
    private static final AtomicInteger PENDING_TASKS = new AtomicInteger();
    private static final Object STATE_LOCK = new Object();
    private static volatile boolean accepting = true;

    private CallableHorseServerTasks() {
    }

    public static void enqueue(Runnable task) {
        if (task == null) {
            return;
        }
        synchronized (STATE_LOCK) {
            if (!accepting) {
                return;
            }
            int pending = PENDING_TASKS.get();
            if (pending >= MAX_PENDING_TASKS) {
                FMLLog.warning("[召之马来]服务端任务队列已满，已丢弃异常高频请求");
                return;
            }
            PENDING_TASKS.incrementAndGet();
            TASKS.offer(task);
        }
    }

    public static void beginServerSession() {
        synchronized (STATE_LOCK) {
            TASKS.clear();
            PENDING_TASKS.set(0);
            accepting = true;
        }
    }

    public static void endServerSession() {
        synchronized (STATE_LOCK) {
            accepting = false;
            TASKS.clear();
            PENDING_TASKS.set(0);
        }
    }

    public static int pendingTasks() {
        return Math.max(0, PENDING_TASKS.get());
    }

    public static void clear() {
        synchronized (STATE_LOCK) {
            TASKS.clear();
            PENDING_TASKS.set(0);
        }
    }

    public static void runPendingTasks() {
        for (int i = 0; i < MAX_TASKS_PER_TICK; i++) {
            Runnable task;
            synchronized (STATE_LOCK) {
                if (!accepting) {
                    return;
                }
                task = TASKS.poll();
                if (task == null) {
                    return;
                }
                PENDING_TASKS.decrementAndGet();
            }
            try {
                task.run();
            } catch (Throwable throwable) {
                FMLLog.severe("[召之马来]执行服务端载具任务失败");
                throwable.printStackTrace();
            }
        }
    }
}
