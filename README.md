# Kyky
A library to simplify the communication between Android Wear devices an Android devices on Android projects

#Getting started
In your main `gradle.build` file:
```Gradle
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}
```

Afterwards, in the `gradle.build` file on the module just add this line:
```Gradle
compile 'com.betomaluje.android:kyky:1.0.2'
```

Now the interesting part. 

You can use the out-of-the-box classes to speed up your development such as:

* `KykyActivity` (extends from `Activity`)
* `KykyFragment` (extends from `Fragment`)
* `KykyService` (extends from `CanvasWatchFaceService`)
* `KykyWearableActivity` (extends from `WearableActivity`)

and use `Kyky` easily as:

```Java
public class MainActivity extends KykyWearableActivity {

    ...

    @Override
    public String getPath() {
        return "/MY_PATH"; //SUPER IMPORTANT!! SET THE PATH TO LISTEN TO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        kyky.setExternalDataListener(dataListener);
        kyky.setExternalMessageListener(messageListener);

        kyky.setOnKykyStatus(new Kyky.KykyStatus() {
            @Override
            public void onConnected() {
                sendConnectedStatus(true);
            }

            @Override
            public void onDisconnected() {
                sendConnectedStatus(false);
            }
        });
    }
```

Alternatively, on your `AppCompatActivity`, `Fragment` or `Service` you have to create a Kyky variable (Example for a Fragment)

```Java
...
  private Kyky kyky;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    kyky = new Kyky(context, "/MY_PATH");
  }
  
  @Override
  public void onPause() {
    super.onPause();
    kyky.disconnect();
  }

  @Override
  public void onResume() {
    super.onResume();
    kyky.connect();
  }
...
```

Now you have your Kyky ready! 

To send messages or data, you can use the `DataMapBuilder` inside Kyky. You can:

```Java
addInt(String key, int value)
addString(String key, String value)
addLong(String key, Long value)
addBoolean(String key, Boolean value)
addByteArray(String key, byte[] value)
addByte(String key, byte value)
addAsset(String key, Asset value)
```

## Sending Messages
```Java
 DataMap config = Kyky.DataMapBuilder.create()
                .addString("color", "#FFFFFF")
                .build();

  kyky.syncMessage(config);
```

Very simple!

## Sending DataItems
```Java
 DataMap config = Kyky.DataMapBuilder.create()
                .addString("color", "#FFFFFF")
                .addBoolean("boolean", false)
                .addInt("position", position)
                .build();

  kyky.syncData(config);
```

Alternatively you can use `kyky.syncData(config, true);` to tell Kyky that's an urgent message.

## Listen to messages or DataItems
Every project has different aproaches so you just need to set the respective listener for your purpose after you create your Kyky instance:

### DataApi
```Java
//now we set our listener
kyky.setExternalDataListener(dataListener);

...

private DataApi.DataListener dataListener = new DataApi.DataListener() {
  @Override
  public void onDataChanged(DataEventBuffer dataEvents) {
    for (DataEvent event : FreezableUtils.freezeIterable(dataEvents)) {
      if (event.getType() == DataEvent.TYPE_DELETED) {
        Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
      } else if (event.getType() == DataEvent.TYPE_CHANGED) {
        Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
        DataItem item = event.getDataItem();
                        
        //here we can retrieve the given path to Kyky
        if (item.getUri().getPath().equals(kyky.getPath())) {
          DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
          String myKey = "color";

          if (dataMap.containsKey(myKey)) {
            //do what we want with this key
            String color = dataMap.getString(myKey);
          }
      }
    }
  }
```
### MessageApi

```Java
//now we set our listener
kyky.setExternalMessageListener(messageListener);

...

private MessageApi.MessageListener messageListener = new MessageApi.MessageListener() {
  @Override
  public void onMessageReceived(MessageEvent messageEvent) {
    if (messageEvent == null)
      return;

    Log.e(TAG, "You have a message from " + messageEvent.getPath());

    // convert a byte array to DataMap
    DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());

    //we make sure that kyky's path is the same
    if (messageEvent.getPath().equals(kyky.getPath())) {
      String myKey = "color";
    
      if (dataMap.containsKey(myKey)) {
        //do something with the color String
        String color = dataMap.getString(myKey);
      }
    }
  }
};
```

Kyky comes with a in-built way to convert an `Asset` to a `Bitmap` called `loadBitmapFromAsset(Asset asset, Kyky.KykyBitmapListener kykyBitmapListener)` just like this:

```Java
    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
    Asset asset = dataMapItem.getDataMap().getAsset("photo");

    kyky.loadBitmapFromAsset(asset, new Kyky.KykyBitmapListener() {
        @Override
        public void onBitmapReady(final Bitmap bitmap) {
            if (bitmap != null) {
                imageBackground.post(new Runnable() {
                    @Override
                    public void run() {
                        imageBackground.setImageBitmap(bitmap);
                        imageBackground.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                });
             }
         }
         @Override
         public void onBitmapError(String s) {
            Log.e("MainActivity", "bitmap error : " + s);
        }
    });
```
