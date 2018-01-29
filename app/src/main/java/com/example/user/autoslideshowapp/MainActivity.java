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

    Cursor cursor;

    Handler mHandler = new Handler();
    ImageView imageVIew;

    Button mReturnButton;
    Button mAutoButton;
    Button mProceedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //終了時の定義
        Thread hook=new Thread(){
            public void run() {
                cursor.close();
            }
        };
        Runtime.getRuntime().addShutdownHook(hook);

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                createCursor();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            createCursor();
        }
        Log.d("auto-slide", "get image uris");

        //Permissionがある場合、かつ保存された画像がある場合のみ通す
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && cursor.moveToFirst()) {

            imageVIew = (ImageView) findViewById(R.id.imageView);

            //初回画像表示
            int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            Long id = cursor.getLong(fieldIndex);
            imageVIew.setImageURI(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));


            //ここからボタンのリスナー設定
            mReturnButton = (Button) findViewById(R.id.return_button);
            mAutoButton = (Button) findViewById(R.id.auto_button);
            mProceedButton = (Button) findViewById(R.id.proceed_button);

            mAutoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTimer == null) {
                        mAutoButton.setText("停止");
                        mProceedButton.setEnabled(false);
                        mReturnButton.setEnabled(false);
                        mTimer = new Timer();
                        mTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            imageVIew.setImageURI(getUriProceed());
                                        }
                                    });
                            }
                        }, 0, 2000);
                    } else {
                        mAutoButton.setText("再生");
                        mProceedButton.setEnabled(true);
                        mReturnButton.setEnabled(true);
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
                            imageVIew.setImageURI(getUriReturn());
                    }
                }
            });

            mProceedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //タイマーが動いているときは操作不可
                    //set enableの方が良い
                    if (mTimer == null) {
                            imageVIew.setImageURI(getUriProceed());
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
                    createCursor();
                }
                break;
            default:
                break;
        }
    }

    private void createCursor() {
        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );

        Log.d("auto-slide", "create cursor");
    }


    private Uri getUriProceed() {

        if(!cursor.moveToNext()){
            cursor.moveToFirst();
        }

        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

    }

    private Uri getUriReturn() {

        if(!cursor.moveToPrevious()){
            cursor.moveToLast();
        }

        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

    }

}


