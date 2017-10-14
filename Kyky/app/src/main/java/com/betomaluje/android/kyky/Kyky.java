package com.betomaluje.android.kyky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by betomaluje on 4/5/16.
 */
public class Kyky implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener,
        MessageApi.MessageListener {

    public interface KykyStatus {
        void onConnected();

        void onDisconnected();
    }

    public interface KykyBitmapListener {
        void onBitmapReady(Bitmap bitmap);

        void onBitmapError(String error);
    }

    private final String TAG = Kyky.class.getSimpleName();

    private ArrayList<String> paths = new ArrayList<>();

    private GoogleApiClient googleApiClient;

    private static final long BITMAP_DECODE_TIMEOUT_MS = 2000;

    private DataApi.DataListener externalDataListener;
    private MessageApi.MessageListener externalMessageListener;

    private KykyStatus onKykyStatus;

    public Kyky(Context context, String path) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApiIfAvailable(Wearable.API)
                .build();

        if (path == null || path.isEmpty()) {
            throw new RuntimeException("You need to set a path to listen to. Try overriding getPath() on your service");
        } else {

            if (!paths.contains(path)) {
                paths.add(path);
            }
        }
    }

    public void setOnKykyStatus(KykyStatus onKykyStatus) {
        this.onKykyStatus = onKykyStatus;
    }

    public void connect() {
        if (googleApiClient != null && !googleApiClient.isConnected())
            googleApiClient.connect();
    }

    public void disconnect() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
            Wearable.MessageApi.removeListener(googleApiClient, this);
            Wearable.DataApi.removeListener(googleApiClient, this);
        }
    }

    public Kyky addPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("You need to set a path to listen to. Try overriding getPath() on your service");
        } else {
            if (!paths.contains(path)) {
                paths.add(path);
            }
        }

        return this;
    }

    public ArrayList<String> getPaths() {
        return paths;
    }

    public String getPath() {
        return getPath(0);
    }

    public String getPath(int index) {
        return paths != null && !paths.isEmpty() ? paths.get(index) : "";
    }

    public void setExternalDataListener(DataApi.DataListener externalDataListener) {
        this.externalDataListener = externalDataListener;
    }

    public void setExternalMessageListener(MessageApi.MessageListener externalMessageListener) {
        this.externalMessageListener = externalMessageListener;
    }

    public void loadBitmapFromAsset(Asset asset, KykyBitmapListener kykyBitmapListener) {
        loadBitmapFromAsset(asset, kykyBitmapListener, BITMAP_DECODE_TIMEOUT_MS);
    }

    public void loadBitmapFromAsset(final Asset asset, @NonNull final KykyBitmapListener kykyBitmapListener, final long bitmapDecodeTimeout) {
        if (asset != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long time = bitmapDecodeTimeout;

                    if (bitmapDecodeTimeout < 0) {
                        time = BITMAP_DECODE_TIMEOUT_MS;
                    }

                    ConnectionResult result = googleApiClient.blockingConnect(time, TimeUnit.MILLISECONDS);

                    if (!result.isSuccess()) {
                        kykyBitmapListener.onBitmapError("BlockingConnect failed");
                    } else {
                        // convert asset into a file descriptor and block until it's ready
                        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                                googleApiClient, asset).await().getInputStream();

                        if (assetInputStream == null) {
                            Log.e(TAG, "Requested an unknown Asset.");
                            kykyBitmapListener.onBitmapError("Requested an unknown Asset");
                        } else {
                            // decode the stream into a bitmap
                            BitmapFactory.Options options = new BitmapFactory.Options();

                            //biggest screen size until today 13-10-2017 is 320x240
                            options.inSampleSize = calculateInSampleSize(options, 320, 320);

                            kykyBitmapListener.onBitmapReady(BitmapFactory.decodeStream(assetInputStream, null, options));
                        }
                    }
                }
            }).start();

        } else {
            kykyBitmapListener.onBitmapError("Asset must not be null");
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "onConnected");
        Wearable.MessageApi.addListener(googleApiClient, this);
        Wearable.DataApi.addListener(googleApiClient, this);

        if (onKykyStatus != null)
            onKykyStatus.onConnected();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended");

        if (onKykyStatus != null)
            onKykyStatus.onDisconnected();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed (" + connectionResult.getErrorCode() + ") : " + connectionResult.getErrorMessage());

        if (onKykyStatus != null)
            onKykyStatus.onDisconnected();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        if (externalDataListener != null)
            externalDataListener.onDataChanged(dataEventBuffer);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (externalMessageListener != null)
            externalMessageListener.onMessageReceived(messageEvent);
    }

    public void syncAllData(DataMap dataMap) {
        for (String path : paths) {
            syncData(path, dataMap, false);
        }
    }

    public void syncAllData(DataMap dataMap, boolean isUrgent) {
        for (String path : paths) {
            syncData(path, dataMap, isUrgent);
        }
    }

    public void syncData(DataMap dataMap) {
        syncData(getPath(), dataMap);
    }

    public void syncData(String path, DataMap dataMap) {
        syncData(path, dataMap, false);
    }

    public void syncData(String path, DataMap dataMap, boolean isUrgent) {
        new KykyDataItemAsyncTask().execute(path, dataMap, isUrgent);
    }

    public void syncAllMessages(DataMap dataMap) {
        for (String path : paths) {
            syncMessage(path, dataMap);
        }
    }

    public void syncMessage(DataMap dataMap) {
        syncMessage(getPath(), dataMap);
    }

    public void syncMessage(String path, DataMap dataMap) {
        new KykyMessageAsyncTask().execute(path, dataMap);
    }

    public class KykyDataItemAsyncTask extends AsyncTask<Object, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {

            if (!googleApiClient.isConnected()) {
                return false;
            } else {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(String.valueOf(params[0]));
                DataMap dataMap = putDataMapReq.getDataMap();
                dataMap.putAll((DataMap) params[1]);

                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                if ((Boolean) params[2])
                    putDataReq.setUrgent();

                //finally we send the data to the different nodes
                for (Node node : nodes.getNodes()) {

                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, putDataReq);

                    DataApi.DataItemResult result = pendingResult.await();

                    if (result.getStatus().isSuccess()) {
                        Log.e(TAG, "Data sent to: " + node.getDisplayName());
                    } else {
                        Log.e(TAG, "ERROR: failed to send Data");
                    }
                }

                return true;
            }
        }
    }

    public class KykyMessageAsyncTask extends AsyncTask<Object, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {

            if (!googleApiClient.isConnected()) {
                return false;
            } else {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(),
                            String.valueOf(params[0]),
                            ((DataMap) params[1]).toByteArray()).await();

                    if (result.getStatus().isSuccess()) {
                        Log.e(TAG, "Message sent to: " + node.getDisplayName());
                    } else {
                        Log.e(TAG, "ERROR: failed to send Message");
                    }
                }

                return true;
            }
        }
    }

    public static class DataMapBuilder {

        private static DataMap map;

        public static DataMapBuilder create() {
            map = new DataMap();
            return new DataMapBuilder();
        }

        public DataMapBuilder addInt(String key, int value) {
            map.putInt(key, value);
            return this;
        }

        public DataMapBuilder addString(String key, String value) {
            map.putString(key, value);
            return this;
        }

        public DataMapBuilder addLong(String key, long value) {
            map.putLong(key, value);
            return this;
        }

        public DataMapBuilder addBoolean(String key, boolean value) {
            map.putBoolean(key, value);
            return this;
        }

        public DataMapBuilder addByteArray(String key, byte[] value) {
            map.putByteArray(key, value);
            return this;
        }

        public DataMapBuilder addByte(String key, byte value) {
            map.putByte(key, value);
            return this;
        }

        public DataMapBuilder addAsset(String key, Asset value) {
            map.putAsset(key, value);
            return this;
        }

        public DataMap build() {
            return map;
        }
    }
}