package io.github.onyxyu.endurance.api.concurrent;

/**
 * 显式选择工作负载，禁止回退到公共 ForkJoinPool。
 */
public enum TaskPool {
    IO,
    CPU
}
