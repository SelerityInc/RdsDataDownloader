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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.inject.Injector;
import com.seleritycorp.common.base.coreservices.CallErrorException;
import com.seleritycorp.common.base.inject.InjectorFactory;
import com.seleritycorp.common.base.state.AppState;
import com.seleritycorp.common.base.state.AppStatePushFacet;
import com.seleritycorp.common.base.state.StateManager;
import com.seleritycorp.common.base.test.InjectingTestCase;
import com.seleritycorp.common.base.test.SettableConfig;
import com.seleritycorp.common.base.test.TimeUtilsSettableClock;

public class RdsDataLifecycleTest extends InjectingTestCase {
  TimeUtilsSettableClock timeUtils;
  RdsDataFetcher fetcher;
  RdsDataPersister persister;
  StateManager sm;
  SettableConfig config;
  AppStatePushFacet facet;

  @Before
  public void setup() {
    Injector injector = InjectorFactory.getInjector();
    timeUtils = injector.getInstance(TimeUtilsSettableClock.class);

    fetcher = createMock(RdsDataFetcher.class);
    persister = createMock(RdsDataPersister.class);
    facet = createMock(AppStatePushFacet.class);
    sm = createMock(StateManager.class);
    expect(sm.createRegisteredAppStatePushFacet("RdsDataDownloader")).andReturn(facet);
    config = new SettableConfig();
    config.set("RdsDataDownloader.lifecycle.interval", "200");
    config.set("RdsDataDownloader.lifecycle.intervalUnit", "MILLISECONDS");
  }

  @Test
  public void testInitialRun() throws IOException, CallErrorException {
    JsonObject rdsData = new JsonObject();

    expect(fetcher.fetch()).andReturn(rdsData);
    persister.persist(rdsData);
    expectLastCall().once();
    facet.setAppState(AppState.READY);
    expectLastCall().once();

    replayAll();

    config.set("RdsDataDownloader.lifecycle.interval", "100000");
    RdsDataLifecycle lifecycle = createRdsDataLifecycle();

    lifecycle.start();

    timeUtils.wallClockSleepForMillis(300);

    lifecycle.stop();

    verifyAll();
  }

  @Test
  public void testPlainScheduling() throws IOException, CallErrorException {
    JsonObject rdsData = new JsonObject();

    expect(fetcher.fetch()).andReturn(rdsData).times(4, 7);
    persister.persist(rdsData);
    expectLastCall().times(4, 7);
    facet.setAppState(AppState.READY);
    expectLastCall().times(4, 7);

    replayAll();

    RdsDataLifecycle lifecycle = createRdsDataLifecycle();

    lifecycle.start();

    timeUtils.wallClockSleepForMillis(1000);

    lifecycle.stop();

    verifyAll();
  }

  @Test
  public void testFailingFetchThenRecovery() throws IOException, CallErrorException {
    JsonObject rdsData = new JsonObject();

    expect(fetcher.fetch()).andReturn(null).times(2);
    expect(fetcher.fetch()).andReturn(rdsData).times(2, 5);
    facet.setAppState(eq(AppState.FAULTY), anyString());
    expectLastCall().times(2);
    facet.setAppState(AppState.READY);
    expectLastCall().times(2, 5);
    persister.persist(rdsData);
    expectLastCall().times(2, 5);

    replayAll();

    RdsDataLifecycle lifecycle = createRdsDataLifecycle();

    lifecycle.start();

    timeUtils.wallClockSleepForMillis(1000);

    lifecycle.stop();

    verifyAll();
  }

  @Test
  public void testFetchRetry() throws IOException, CallErrorException {
    CallErrorException thrownE = new CallErrorException("catch me");

    JsonObject rdsData = new JsonObject();

    expect(fetcher.fetch()).andThrow(thrownE);
    expect(fetcher.fetch()).andReturn(rdsData);
    persister.persist(rdsData);
    expectLastCall().once();
    facet.setAppState(eq(AppState.WARNING), anyString());
    facet.setAppState(AppState.READY);
    expectLastCall().once();

    replayAll();

    config.set("RdsDataDownloader.lifecycle.interval", "100000");
    RdsDataLifecycle lifecycle = createRdsDataLifecycle();

    lifecycle.start();

    timeUtils.wallClockSleepForMillis(300);
    // By now the first fetch should have been failed.
    // So we trigger the re-fetch by advancing the clock.
    timeUtils.advanceClockSettled(200000);

    lifecycle.stop();

    verifyAll();
  }

  private RdsDataLifecycle createRdsDataLifecycle() {
    return new RdsDataLifecycle(sm, config, fetcher, persister, timeUtils);
  }
}
