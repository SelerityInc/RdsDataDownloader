/*
 * Copyright (C) 2016-2018 Selerity, Inc. (support@seleritycorp.com)
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
import java.io.Writer;

import org.easymock.IAnswer;
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
  Writer writer;

  @Before
  public void setup() throws IOException {
    Injector injector = InjectorFactory.getInjector();
    timeUtils = injector.getInstance(TimeUtilsSettableClock.class);
    fetcher = createMock(RdsDataFetcher.class);
    persister = createMock(RdsDataPersister.class);
    writer = createMock(Writer.class);
    facet = createMock(AppStatePushFacet.class);
    sm = createMock(StateManager.class);
    expect(persister.getCleanWriter()).andReturn(writer).anyTimes();
    expect(sm.createRegisteredAppStatePushFacet("RdsDataDownloader")).andReturn(facet);
    config = new SettableConfig();
    config.set("RdsDataDownloader.lifecycle.interval", "200");
    config.set("RdsDataDownloader.lifecycle.intervalUnit", "MILLISECONDS");
  }

  @Test
  public void testInitialRun() throws Exception {
    fetcher.fetch(writer);
    expectLastCall().once();
    persister.persist();
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
  public void testPlainScheduling() throws Exception {
    fetcher.fetch(writer);
    expectLastCall().times(4, 7);
    persister.persist();
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
  public void testCatchOutOfMemoryError() throws Exception {
    Throwable t = new OutOfMemoryError("catch me");

    fetcher.fetch(writer);
    expectLastCall().andAnswer(new IAnswer<JsonObject>() {
      int count=0;
      @Override
      public JsonObject answer() throws Throwable {
        if (count++ == 0) {
          return null;
        }
        throw t;
      }}).times(2,5);
    facet.setAppState(AppState.READY);
    facet.setAppState(eq(AppState.FAULTY), anyString());

    persister.persist();

    replayAll();

    RdsDataLifecycle lifecycle = createRdsDataLifecycle();

    lifecycle.start();

    timeUtils.wallClockSleepForMillis(1000);

    lifecycle.stop();

    verifyAll();
  }

  @Test
  public void testCatchThrowable() throws Exception {
    Throwable t = new Throwable("catch me");

    fetcher.fetch(writer);
    expectLastCall().andAnswer(new IAnswer<JsonObject>() {
      int count=0;
      @Override
      public JsonObject answer() throws Throwable {
        if (count++ == 0) {
          return null;
        }
        throw t;
      }}).times(2,5);
    facet.setAppState(AppState.READY);
    facet.setAppState(eq(AppState.FAULTY), anyString());

    persister.persist();

    replayAll();

    RdsDataLifecycle lifecycle = createRdsDataLifecycle();

    lifecycle.start();

    timeUtils.wallClockSleepForMillis(1000);

    lifecycle.stop();

    verifyAll();
  }

  @Test
  public void testFetchRetry() throws Exception {
    CallErrorException thrownE = new CallErrorException("catch me");

    fetcher.fetch(writer);
    expectLastCall().andThrow(thrownE);
    fetcher.fetch(writer);
    expectLastCall().once();
    persister.persist();
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
