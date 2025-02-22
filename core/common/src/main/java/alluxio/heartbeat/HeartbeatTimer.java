/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.heartbeat;

/**
 * An interface for heartbeat timers. The {@link HeartbeatThread} calls the {@link #tick()} method.
 */
public interface HeartbeatTimer {

  /**
   * Sets the heartbeat interval.
   *
   * @param intervalMs the heartbeat interval in ms
   */
  default void setIntervalMs(long intervalMs) {
    throw new UnsupportedOperationException("Setting interval is not supported");
  }

  /**
   * Get the interval of HeartbeatTimer.
   *
   * @return the interval of this HeartbeatTimer
   */
  default long getIntervalMs() {
    throw new UnsupportedOperationException("Getting interval is not supported");
  }

  /**
   * Waits until next heartbeat should be executed.
   *
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  void tick() throws InterruptedException;
}
