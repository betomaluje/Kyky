package com.betomaluje.android.kyky;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by betomaluje on 4/5/16.
 */
public class Kyky implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener,
        MessageApi.MessageListener {

    private final String TAG = Kyky.class.getSimpleName();

    private String path;
    private GoogleApiClient googleApiClient;

    private DataApi.DataListener externalDataListener;
    private MessageApi.MessageListener externalMessageListener;

    public Kyky(Context context, String path) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApiIfAvailable(Wearable.API)
                .build();

        this.path = path;
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

    public String getPath() {
        return path;
    }

    public void setExternalDataListener(DataApi.DataListener externalDataListener) {
        this.externalDataListener = externalDataListener;
    }

    public void setExternalMessageListener(MessageApi.MessageListener externalMessageListener) {
        this.externalMessageListener = externalMessageListener;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(TAG, "onConnected");
        Wearable.MessageApi.addListener(googleApiClient, this);
        Wearable.DataApi.addListener(googleApiClient, this);
    }

    public void syncData(DataMap dataMap) {
        syncData(dataMap, false);
    }

    public void syncData(DataMap dataMap, boolean isUrgent) {
        new KykyDataItemAsyncTask().execute(dataMap, isUrgent);
    }

    public void syncMessage(DataMap dataMap) {
        new KykyMessageAsyncTask().execute(dataMap);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed (" + connectionResult.getErrorCode() + ") : " + connectionResult.getErrorMessage());
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

    public class KykyDataItemAsyncTask extends AsyncTask<Object, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {

            if (!googleApiClient.isConnected()) {
                return false;
            } else {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
                DataMap dataMap = putDataMapReq.getDataMap();
                dataMap.putAll((DataMap) params[0]);

                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                if ((Boolean) params[1])
                    putDataReq.setUrgent();

                //finally we send the data to the different nodes
                for (Node node : nodes.getNodes()) {

                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, putDataReq);

                    DataApi.DataItemResult result = pendingResult.await();

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

    public class KykyMessageAsyncTask extends AsyncTask<DataMap, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(DataMap... params) {

            if (!googleApiClient.isConnected()) {
                return false;
            } else {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, params[0].toByteArray()).await();
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

        private DataMap map = new DataMap();

        public static DataMapBuilder create() {
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