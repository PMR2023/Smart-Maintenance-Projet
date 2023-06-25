/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.smartmaintenance.java.common.helpers;

import static android.view.MotionEvent.ACTION_UP;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Helper to detect taps using Android GestureDetector, and pass the taps between UI thread and
 * render thread.
 */
public final class TapHelper implements OnTouchListener {
  private final GestureDetector gestureDetector;
  private final BlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);

    public void simulateTouch() {
        MotionEvent a = MotionEvent.obtain(21454436, 21454461, ACTION_UP, 371.25F, 1234.9766F, 0);
        Log.i("BUCETA", a.toString());
        queuedSingleTaps.offer(a);
    }

  /**
   * Creates the tap helper.
   *
   * @param context the application's context.
   */
  public TapHelper(Context context) {

    gestureDetector =
        new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onSingleTapUp(MotionEvent e) {
                // Queue tap if there is space. Tap is lost if queue is full.
                 /* action=ACTION_UP, actionButton=0, id[0]=0, x[0]=371.25, y[0]=1234.9766, toolType[0]=TOOL_TYPE_FINGER, buttonState=0,
                          classification=NONE, metaState=0, flags=0x0, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=21454461,
                          downTime=21454436, deviceId=2, source=0x1002, displayId=0, eventId=62927073*/
                    MotionEvent a = MotionEvent.obtain(21454436, 21454461, ACTION_UP, 371.25F, 1234.9766F, 0);
                  Log.i("BUCETA", e.toString());
                queuedSingleTaps.offer(a);
                return true;
              }


              @Override
              public boolean onDown(MotionEvent e) {
                return true;
              }
            });
  }

  /**
   * Polls for a tap.
   *
   * @return if a tap was queued, a MotionEvent for the tap. Otherwise null if no taps are queued.
   */
  public MotionEvent poll() {
    return queuedSingleTaps.poll();
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    return gestureDetector.onTouchEvent(motionEvent);
  }
}
