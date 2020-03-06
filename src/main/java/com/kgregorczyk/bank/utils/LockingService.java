package com.kgregorczyk.bank.utils;

import static java.util.Arrays.stream;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LockingService {
  private static final Map<String, Semaphore> SEMAPHORES = new ConcurrentHashMap<>();

  public static void lock(UUID... ids) {
    try {
      SEMAPHORES
          .computeIfAbsent(idsToKey(ids), key -> new Semaphore(1))
          .tryAcquire(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Failed to lock on ids=[]", ids);
    }
  }

  public static void unlock(UUID... ids) {
    SEMAPHORES.computeIfPresent(
        idsToKey(ids),
        (key, semaphore) -> {
          semaphore.release();
          return semaphore;
        });
  }

  private static String idsToKey(UUID... ids) {
    return stream(ids).sorted().map(UUID::toString).collect(Collectors.joining(":"));
  }
}
