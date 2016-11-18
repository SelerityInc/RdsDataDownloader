/*
 * Copyright (C) 2016 Selerity, Inc. (support@seleritycorp.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seleritycorp.rds.downloader;

import com.google.inject.Injector;

import com.seleritycorp.common.base.inject.InjectorFactory;
import com.seleritycorp.common.base.logging.Log;
import com.seleritycorp.common.base.logging.LogFactory;
import com.seleritycorp.common.base.state.AppState;
import com.seleritycorp.common.base.state.StateManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Starts a Rds data downloading process and runs it periodically.
 */
public class RdsDataDownloaderMain {
  private static final Log log = LogFactory.getLog(RdsDataDownloaderMain.class);

  /**
   * Sets up and starts fetching and persisting of RDS data.
   * 
   * @param args Unused. Only here to meet Java's expectations of the main method.
   */
  @SuppressFBWarnings(value = {"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP"},
      justification = "Keeping the main thread alive (to avoid early exit) until user "
          + "interruptions occurs")
  public static void main(final String[] args) {
    Injector injector = InjectorFactory.getInjector();
    StateManager sm = injector.getInstance(StateManager.class);

    RdsDataLifecycle lifecycle = injector.getInstance(RdsDataLifecycle.class);
    lifecycle.start();

    sm.setMainAppState(AppState.READY);

    Object lock = new Object();
    synchronized (lock) {
      try {
        lock.wait();
      } catch (InterruptedException e) {
        log.info("Main method interrupted", e);
      }
    }
  }
}
