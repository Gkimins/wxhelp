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
import java.util.ArrayList;
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
                Context context = MainActivity.this;
                String path = context.getExternalFilesDir(null).getPath();
                FileUtils.deleteQuietly(new File(path + "/111111/rec/"));
                hideSoftInput(view);
                Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
            }
        });
        mSend = findViewById(R.id.send);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                FileProvider.getUriForFile(MainActivity.thi)
//                shareFile(MainActivity.this,);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Context context = MainActivity.this;
                String path = context.getExternalFilesDir(null).getPath();
                path = path + "/111111/rec.zip";

//                Uri apkUri =FileProvider.getUriForFile(context,getPackageName() + ".fileprovider",new File(path));///-----ide文件提供者名

//                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context,getPackageName() + ".fileprovider",new File(path)));
                intent.setType("application/octet-stream");
                startActivity(intent);
            }
        });
    }
    public static void shareFile(Context context, Uri uri) {
        // File file = new File("\sdcard\android123.cwj"); //附件文件地址

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra("subject", ""); //
        intent.putExtra("body", ""); // 正文
        intent.putExtra(Intent.EXTRA_STREAM, uri); // 添加附件，附件为file对象
        if (uri.toString().endsWith(".gz")) {
            intent.setType("application/x-gzip"); // 如果是gz使用gzip的mime
        } else if (uri.toString().endsWith(".txt")) {
            intent.setType("text/plain"); // 纯文本则用text/plain的mime
        } else {
            intent.setType("application/octet-stream"); // 其他的均使用流当做二进制数据来发送
        }
        context.startActivity(intent); // 调用系统的mail客户端进行发送
    }
    public static void shareMultipleFiles(Context context, ArrayList<Uri> uris) {

        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(
                multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
                        : android.content.Intent.ACTION_SEND);

        if (multiple) {
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            Uri value = uris.get(0);
            String ext = MimeTypeMap.getFileExtensionFromUrl(value.toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if(mimeType==null){
                mimeType = "*/*";
            }
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, value);
        }
        context.startActivity(Intent.createChooser(intent, "Share"));
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
                mTip.setText("文件变动，新增加" + (newList.length - fileList.length) + "个文件");
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
        String rootFilePaths;
        Context context = MainActivity.this;
        String path = context.getExternalFilesDir(null).getPath();

        rootFilePaths = path + "/111111/rec/" + name;
        File file = new File(rootFilePaths);
        file.mkdirs();
        for (int i = 0; i < diffList.size(); i++) {
            File file1 = diffList.get(i);
            File dest = new File(rootFilePaths + "/" + file1.getName());
//            FileUtils.moveFile(file1, dest);
            com.blankj.utilcode.util.FileUtils.move(file1, dest);
//            Files.move(file1.toPath(),dest.toPath(), REPLACE_EXISTING);
        }
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        fileList = getFileList();
    }

    private void packToZip() {
        Context context = MainActivity.this;
        String path = context.getExternalFilesDir(null).getPath();
        path = path + "/111111/rec/";
        String format = "zip";
        try {
            System.out.println(zipFile(new File(path), format));
            Toast.makeText(this, "打包成功！", Toast.LENGTH_SHORT).show();
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
        InputMethodManager imm =
                (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    //    /**
//     * 获取当前淘宝时间，用来验证时间 程序有效期一天 防止跑路
//     */
    private void getTime() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.m.taobao.com/rest/api3.do?api=mtop.common.getTimestamp")
                .build();
        Call call = okHttpClient.newCall(request);
        Toast.makeText(this, "试用版本", Toast.LENGTH_SHORT).show();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message message = handlers.obtainMessage();
                message.arg1 = 1;
                message.sendToTarget();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String s = response.body().string();
                String substring = s.substring(81, 94);
                if (Long.valueOf(substring) > 1621706455944L && Long.valueOf(substring) < 1621879255000L) {

                } else {
                    Message message = handlers.obtainMessage();
                    message.arg1 = 1;
                    message.sendToTarget();
                }
            }
        });
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


    private Handler handlers = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(MainActivity.this, "软件过期", Toast.LENGTH_SHORT).show();
            finish();
        }
    };
}
