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

import com.google.gson.JsonArray;

import com.seleritycorp.common.base.config.ApplicationConfig;
import com.seleritycorp.common.base.config.Config;
import com.seleritycorp.common.base.config.ConfigUtils;
import com.seleritycorp.common.base.coreservices.CallErrorException;
import com.seleritycorp.common.base.logging.Log;
import com.seleritycorp.common.base.logging.LogFactory;
import com.seleritycorp.common.base.state.AppState;
import com.seleritycorp.common.base.state.AppStatePushFacet;
import com.seleritycorp.common.base.state.StateManager;
import com.seleritycorp.common.base.time.TimeUtils;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

/**
 * Lifecycle manager for RDS data
 * 
 * <p>This class handles fetching and persisting of RDS data, and scheduling of thereof.
 */
public class RdsDataLifecycle {
  private static final Log log = LogFactory.getLog(RdsDataLifecycle.class);

  private final AppStatePushFacet facet;

  private final int interval;
  private final TimeUnit intervalUnit;
  private final RdsDataFetcher fetcher;
  private final RdsDataPersister persister;
  private final TimeUtils timeUtils;
  private final long retryPauseMillis;
  private ScheduledExecutorService executorService;

  /**
   * Creates a lifecycle handler for RDS data
   * 
   * <p>The handler is initially stopped. You have to call {@link #start()} to acutally start the
   * handler's scheduling.
   * 
   * @param sm The application's state manager to report to 
   * @param appConfig The application config to use.
   * @param fetcher handler of single, plain fetches 
   * @param persister persister of RDS data
   * @param timeUtils timing for fetch re-tries.
   */
  @Inject
  public RdsDataLifecycle(StateManager sm, @ApplicationConfig Config appConfig,
      RdsDataFetcher fetcher, RdsDataPersister persister, TimeUtils timeUtils) {
    this.facet = sm.createRegisteredAppStatePushFacet("RdsDataDownloader");
    this.fetcher = fetcher;
    this.persister = persister;
    this.timeUtils = timeUtils;
    this.executorService = null;
    this.retryPauseMillis = 3 * 60 * 1000;

    Config config = ConfigUtils.subconfig(appConfig, "RdsDataDownloader.lifecycle");
    this.interval = config.getInt("interval", 3600);
    this.intervalUnit = TimeUnit.valueOf(config.get("intervalUnit", "SECONDS"));
  }

  /**
   * Fetches RDS data and retry once if there are errors
   * 
   * @return The fetched RDS data. null, if RDS could not get fetched.
   */
  private JsonArray fetch() {
    JsonArray rdsData = null;
    try {
      rdsData = fetcher.fetch();
    } catch (IOException | CallErrorException e2) {
      String msg2 =
          "Fetching RDS data failed. Will rertry in " + retryPauseMillis / 1000 + " seconds";
      log.warn(msg2, e2);
      facet.setAppState(AppState.WARNING, msg2);

      // Pausing before retry.
      timeUtils.sleepForMillis(retryPauseMillis);

      try {
        rdsData = fetcher.fetch();
      } catch (IOException | CallErrorException e3) {
        String msg3 = "Fetching RDS data failed two times in a row";
        log.error(msg3, e3);
        facet.setAppState(AppState.FAULTY, msg3);
      }
    }
    return rdsData;
  }

  private void singleRun() {
    log.info("Starting data fetch run");
    try {
      JsonArray rdsData = fetch();

      if (rdsData == null) {
        facet.setAppState(AppState.FAULTY, "Failed to receive good RDS data");
      } else {
        persister.persist(rdsData);
        facet.setAppState(AppState.READY);
      }
    } catch (Exception e) {
      String msg = "Downloading/Persisting data failed";
      log.error(msg, e);
      facet.setAppState(AppState.FAULTY, msg + ": " + e.toString());
    }
  }

  /**
   * Starts scheduling jobs to fetch and persist RDS data.
   */
  public synchronized void start() {
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        singleRun();
      }
    };

    executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      private AtomicInteger count = new AtomicInteger();

      @Override
      public Thread newThread(Runnable runnable) {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("RdsDataDownloader-" + count.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      }
    });
    executorService.scheduleAtFixedRate(runnable, 0, interval, intervalUnit);
    log.info("Scheduled RDS data downloads every " + interval + " " + intervalUnit);
  }

  /**
   * Stops scheduling further jobs to fetch and persist of RDS data.
   */
  public synchronized void stop() {
    executorService.shutdown();
    executorService = null;
  }
}
