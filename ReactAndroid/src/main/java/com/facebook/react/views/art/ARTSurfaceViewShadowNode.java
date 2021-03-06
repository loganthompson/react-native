/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.views.art;

import javax.annotation.Nullable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.Surface;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import com.facebook.common.logging.FLog;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.UIViewOperationQueue;
import com.facebook.react.uimanager.ReactShadowNode;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;

/**
 * Shadow node for ART virtual tree root - ARTSurfaceView
 */
public class ARTSurfaceViewShadowNode extends LayoutShadowNode
  implements TextureView.SurfaceTextureListener {

  private @Nullable Surface mSurface;
  private @Nullable boolean mHasPendingUpdates;

  private @Nullable Integer mBackgroundColor;

  @ReactProp(name = ViewProps.BACKGROUND_COLOR, customType = "Color")
  public void setBackgroundColor(Integer color) {
    mBackgroundColor = color;
    markUpdated();
  }

  @Override
  public boolean isVirtual() {
    return false;
  }

  @Override
  public boolean isVirtualAnchor() {
    return true;
  }

  @Override
  public void onCollectExtraUpdates(UIViewOperationQueue uiUpdater) {
    super.onCollectExtraUpdates(uiUpdater);
    drawOutput();
    uiUpdater.enqueueUpdateExtraData(getReactTag(), this);
  }

  private void drawOutput() {
    if (mSurface == null || !mSurface.isValid()) {
      mHasPendingUpdates = true;
      return;
    }

    try {
      Canvas canvas = mSurface.lockCanvas(null);
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
      if (mBackgroundColor != null) {
        canvas.drawColor(mBackgroundColor);
      }

      Paint paint = new Paint();
      for (int i = 0; i < getChildCount(); i++) {
        ARTVirtualNode child = (ARTVirtualNode) getChildAt(i);
        child.draw(canvas, paint, 1f);
      }

      if (mSurface == null) {
        return;
      }

      mSurface.unlockCanvasAndPost(canvas);
      mHasPendingUpdates = false;
    } catch (IllegalArgumentException | IllegalStateException e) {
      FLog.e(ReactConstants.TAG, e.getClass().getSimpleName() + " in SurfaceView.drawOutput");
    } catch (RuntimeException e) {
      FLog.e(ReactConstants.TAG, e.getClass().getSimpleName() + " in SurfaceView.drawOutput");
    }
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    mSurface = new Surface(surface);
    if (mHasPendingUpdates) {
      drawOutput();
    }
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    surface.release();
    mSurface = null;
    return true;
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    drawOutput();
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
}
