# Kyky
A library to simplify the communication between Android Wear devices an Android devices on Android projects

#Getting started
In your main `gradle.build` file:
```Gradle
allprojects {
    repositories {
        jcenter()
        mavenCentral()

        maven {
            url 'https://dl.bintray.com/betomaluje/maven'
        }
    }
}
```

Afterwards, in the `gradle.build` file on the module just add this line:
```Gradle
compile 'com.betomaluje.android:kyky:1.0.1'
```

Now the interesting part. On your `Activity`, `Fragment` or `Service` you have to create a Kyky variable (Example for a Fragment)

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
addString(String key, int value)
addLong(String key, int value)
addBoolean(String key, int value)
addByteArray(String key, int value)
addByte(String key, int value)
addAsset(String key, int value)
```

##Sending Messages
```Java
 DataMap config = Kyky.DataMapBuilder.create()
                .addString("color", "#FFFFFF")
                .build();

  kyky.syncMessage(config);
```

Very simple!

##Sending DataItems
```Java
 DataMap config = Kyky.DataMapBuilder.create()
                .addString("color", "#FFFFFF")
                .addBoolean("boolean", false)
                .addInt("position", position)
                .build();

  kyky.syncData(config);
```

Alternatively you can use `kyky.syncData(config, true);` to tell Kyky that's an urgent message.

##Listen to messages or DataItems
Every project has different aproaches so you just need to set the respective listener for your purpose after you create your Kyky instance:

###DataApi
```Java
kyky = new Kyky(context, "/MY_PATH");

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
###MessageApi

```Java
kyky = new Kyky(context, "/MY_PATH");

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
    byte[] rawData = messageEvent.getData();
    DataMap dataMap = DataMap.fromByteArray(rawData);

    //we make sure that kyky's path is the same
    if (messageEvent.getPath().equals(kyky.getPath())) {
      String myKey = "color";
    
      if (dataMap.containsKey(myKey)) {
        mainColor = Color.parseColor(dataMap.getString(keyMainColor));
        mIndicatorPaint.setColor(mainColor);
      }
    }
  }
};
```
