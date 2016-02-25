package com.burlap.filetransfer;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;
import android.net.Uri;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.*;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Arrays;


public class FileTransferModule extends ReactContextBaseJavaModule {

  private final OkHttpClient client = new OkHttpClient();

  private static String siteUrl = "http://joinbevy.com";
  private static String apiUrl = "http://api.joinbevy.com";
  private static Integer port = 80;

  private String TAG = "ImageUploadAndroid";

  public FileTransferModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    // match up with the IOS name
    return "FileTransfer";
  }

    private ReadableMap getMapParam(ReadableMap map, String key, ReadableMap defaultValue) {
        if ( map.hasKey(key)) {
            return map.getMap(key);
        } else {
            return defaultValue;
        }
    }

  @ReactMethod
  public void upload(ReadableMap options, Callback complete) {

    final Callback completeCallback = complete;

    try {

      String url = options.getString("uploadUrl");
      ReadableMap headers = options.getMap("headers");
      ReadableMap data = options.getMap("data");
      MultipartBuilder multipartBuilder = new MultipartBuilder();
      ReadableArray files = options.getArray("files");

      multipartBuilder.type(MultipartBuilder.FORM);


      ReadableMapKeySetIterator keys = data.keySetIterator();

      while (keys.hasNextKey()) {
          String key = keys.nextKey();
          ReadableType type = data.getType(key);
          String value = null;

          switch (type) {
              case String:
                  value = data.getString(key);
                  break;
              case Number:
                  value = Integer.toString(data.getInt(key));
                  break;
              case Boolean:
                  value = Boolean.toString(data.getBoolean(key));
                  break;
              default:
                  completeCallback.invoke(type.toString() + " type not supported.", null);
                  break;
         }

            multipartBuilder.addFormDataPart(key, value);
      }

      for (int i = 0; i < files.size(); i++) {

             ReadableMap fileDescription = files.getMap(i);
             String mimeType = fileDescription.getString("mimeType");
             MediaType mediaType = MediaType.parse(mimeType);
             String fileName = fileDescription.getString("fileName");
             String fieldName = fileDescription.getString("fieldName");
             String uri = fileDescription.getString("uri");
             Uri fileUri = Uri.parse(uri);
             File file = new File(fileUri.getPath());

             if(file == null) {
               Log.d(TAG, "FILE NOT FOUND");
               completeCallback.invoke("FILE NOT FOUND", null);
                 return;
             }

             multipartBuilder.addFormDataPart(fieldName, fileName, RequestBody.create(mediaType, file));
      }

      multipartBuilder.build();

      RequestBody requestBody = multipartBuilder.build();
      Request request = new Request.Builder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .url(url)
                .post(requestBody)
                .build();


        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            Log.d(TAG, "Unexpected code" + response);
            completeCallback.invoke(response, null);
            return;
        }

        completeCallback.invoke(null, response.body().string());
    } catch(Exception e) {
      Log.d(TAG, e.toString());
    }
  }
}
