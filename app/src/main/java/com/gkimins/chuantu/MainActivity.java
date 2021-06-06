package com.gkimins.chuantu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pub.devrel.easypermissions.EasyPermissions;

import static com.gkimins.chuantu.ZipTool.zipFile;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private Button mStart;
    private TextView mTip;
    private EditText mFilename;
    private Button mSave;
    private TextView mStatue;
    private File[] fileList;
    private File[] newList;
    private List<File> diffList;
    private Button mDel;
    private Button mSend;
    private String zipName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ask_permission();
        initView();
    }

    private void ask_permission() {
        String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
                , Manifest.permission.INTERNET
        };
        //若是没有权限,请求
        if (!EasyPermissions.hasPermissions(this, permission)) {
            EasyPermissions.requestPermissions(this, "需要给予权限才能使用", 981, permission);
        }
    }

    private void initView() {
        mStart = findViewById(R.id.start);
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                packToZip();
            }
        });
        mTip = findViewById(R.id.tip);
        mFilename = findViewById(R.id.filename);
        mSave = findViewById(R.id.save);
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    out(mFilename.getEditableText().toString());
                    hideSoftInput(view);
                    Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mStatue = findViewById(R.id.statue);
        mDel = findViewById(R.id.del);
        mDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.deleteQuietly(new File(Environment.getExternalStorageDirectory().getPath() + "/111111/rec/"));
                MainActivity.this.hideSoftInput(view);
                Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
            }
        });
        mSend = findViewById(R.id.send);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!zipName.equals("")){
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Context context = MainActivity.this;
                    String path = zipName;
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context,getPackageName() + ".fileprovider",new File(path)));
                    intent.setType("application/octet-stream");
                    startActivity(intent);
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        judgeFileChange();
    }

    private File[] getFileList() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        String absolutePath = externalStorageDirectory.getAbsolutePath();
        File[] files1 = externalStorageDirectory.listFiles();
        if (files1 != null) {
            for (File f : files1) {
                if (f.getPath().equals(absolutePath + "/Pictures")) {
                    File[] pict = f.listFiles();
                    for (File weix : pict) {
                        if (weix.getPath().equals(absolutePath + "/Pictures/WeiXin")) {
                            return weix.listFiles();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void judgeFileChange() {
        newList = getFileList();
        if (fileList == null) {
            fileList = newList;
            mStatue.setText("文件夹内有" + fileList.length + "个文件");
        } else {
            if (newList != null && fileList.length < newList.length) {
                String format = new SimpleDateFormat("hh:mm:ss").format(new Date());
                mTip.setText(format+"文件变动，新增加" + (newList.length - fileList.length) + "个文件");
                analyse();
            }
        }
    }

    private void analyse() {
        diffList = new ArrayList<>();
        for (File f2 : newList) {
            boolean flag = false;
            for (File f : fileList) {
                if (f.getName().equals(f2.getName())) {
                    flag = true;
                }
            }
            if (!flag) {
                diffList.add(f2);
            }
        }
        fileList = newList;
    }

    private void out(String name) throws IOException {
        String str2 = Environment.getExternalStorageDirectory().getPath() + "/111111/rec/" + name;
        new File(str2).mkdirs();
        for (int i = 0; i < diffList.size(); i++) {
            File file = diffList.get(i);
            FileUtils.moveFile(file, new File(str2 + "/" + file.getName()));
        }
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        this.fileList = getFileList();
    }

    private void packToZip() {
        try {
            zipName = zipFile(new File(Environment.getExternalStorageDirectory().getPath() + "/111111/rec/"), "zip");
            Toast.makeText(this, "打包成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * 输入法隐藏类，用来隐藏输入法
     *
     * @param view 当前view
     */

    public void hideSoftInput(final View view) {
        InputMethodManager imm =
                (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == InputMethodManager.RESULT_UNCHANGED_SHOWN
                        || resultCode == InputMethodManager.RESULT_SHOWN) {
                    toggleSoftInput();
                }
            }
        });
    }

    /**
     * 软键盘切换
     */
    public void toggleSoftInput() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(2, 0);
    }

    /**
     * 默认请求回调
     *
     * @param requestCode  请求码
     * @param permissions  请求的权限
     * @param grantResults 结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        转交给easyPermission处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 全局授权成功
     *
     * @param requestCode 请求码
     * @param perms       权限列表
     */
    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, "授权成功，欢迎使用", Toast.LENGTH_SHORT).show();
    }

    /**
     * easyPermission回调，当权限拒绝时
     *
     * @param requestCode 请求码
     * @param perms       权限列表
     */
    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        String[] permission = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                , Manifest.permission.WRITE_EXTERNAL_STORAGE};
//        再次检查有无权限
        if (!EasyPermissions.hasPermissions(this, permission)) {
            Toast.makeText(this, "没有权限。正在退出", Toast.LENGTH_SHORT).show();
//            结束application
            finish();
        }
    }
}
