package io.github.onyxyu.endurance.api.concurrent;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 模块共享的任务提交契约，调用方不依赖线程池、Spring 或追踪实现。
 */
public interface TaskDispatcher {
    <T> CompletableFuture<T> submit(TaskPool pool, Callable<T> work);

    <T> CompletableFuture<T> submit(TaskPool pool, Callable<T> work, Duration timeout);

    CompletableFuture<Void> execute(TaskPool pool, Runnable work);
}
