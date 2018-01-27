package com.example.user.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    Timer mTimer;
    double mTimerSec = 0.0;
    int imageMove = 0;

    Handler mHandler = new Handler();
    ImageView imageVIew;

    Button mReturnButton;
    Button mAutoButton;
    Button mProceedButton;

    ArrayList<Uri> imageUris = new ArrayList<Uri>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo();
        }
        Log.d("auto-slide", "get image uris");

        //Permissionがある場合、かつ保存された画像がある場合のみ通す
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && imageUris.size() > 0) {

            imageVIew = (ImageView) findViewById(R.id.imageView);

            //画像を表示するスレッドを立てる
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    imageVIew.setImageURI(imageUris.get(imageMove));
                    Log.d("auto-slide", "display " + imageMove + "th image");
                }
            });

            //ここからボタンのリスナー設定
            mReturnButton = (Button) findViewById(R.id.return_button);
            mAutoButton = (Button) findViewById(R.id.auto_button);
            mProceedButton = (Button) findViewById(R.id.proceed_button);

            mAutoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTimer == null) {
                        mAutoButton.setText("停止");
                        mTimer = new Timer();
                        mTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                mTimerSec += 0.1;
                                if (mTimerSec >= 2.0) {
                                    //最後の画像のときの処理
                                    if (imageMove >= imageUris.size() - 1) {
                                        imageMove = 0;
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                imageVIew.setImageURI(imageUris.get(imageMove));
                                                Log.d("auto-slide", "display " + imageMove + "th image");
                                            }
                                        });
                                    }
                                    else {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                imageVIew.setImageURI(imageUris.get(++imageMove));
                                                Log.d("auto-slide", "display " + imageMove + "th image");
                                            }
                                        });
                                    }
                                    mTimerSec = 0.0;
                                }

                            }
                        }, 100, 100);
                        Log.d("auto-slide", "start slideshow");
                    } else {
                        mAutoButton.setText("再生");
                        mTimerSec = 0.0;
                        mTimer.cancel();
                        mTimer = null;
                        Log.d("auto-slide", "stop slideshow");
                    }
                }
            });

            mReturnButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //タイマーが動いているときは操作不可
                    if (mTimer == null) {
                        //最初の画像のときの処理
                        if (imageMove < 1) {
                            imageMove = imageUris.size() - 1;
                            imageVIew.setImageURI(imageUris.get(imageMove));
                            Log.d("auto-slide", "display " + imageMove + "th image");
                        }
                        else {
                            imageVIew.setImageURI(imageUris.get(--imageMove));
                            Log.d("auto-slide", "display " + imageMove + "th image");
                        }
                    }
                }
            });

            mProceedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //タイマーが動いているときは操作不可
                    if (mTimer == null) {
                        //最後の画像のときの処理
                        if (imageMove >= imageUris.size() - 1){
                            imageMove = 0;
                            imageVIew.setImageURI(imageUris.get(imageMove));
                            Log.d("auto-slide", "display " + imageMove + "th image");
                        }
                        else {
                            imageVIew.setImageURI(imageUris.get(++imageMove));
                            Log.d("auto-slide", "display " + imageMove + "th image");
                        }
                    }
                }
            });
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo();
                }
                break;
            default:
                break;
        }
    }

    private void getContentsInfo() {
        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );
        Log.d("auto-slide", "create cursor");

        if (cursor.moveToFirst()) {
            int count = 0;
            do {
                // indexからIDを取得し、そのIDから画像のURIを取得する
                int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                Long id = cursor.getLong(fieldIndex);
                imageUris.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));

                Log.d("auto-slide", "URI : " + imageUris.get(count).toString());
                count++;
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}


