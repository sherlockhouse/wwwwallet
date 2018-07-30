package com.tg.wallet;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.CallBack;
import com.zhouyou.http.exception.ApiException;

import org.json.JSONException;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import de.greenrobot.event.EventBus;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.tg.wallet.permission.PermisionsConstant;
import com.tg.wallet.permission.PermissionsManager;
import com.tg.wallet.permission.PermissionsResultAction;
import com.tg.wallet.qrcode.QrCodeEvent;
import com.tg.wallet.qrcode.QrcodeActivity;
import com.tg.wallet.qrcode.QrcodeGen;
import com.tg.wallet.rx.RxLogicHandler;
import com.tg.wallet.rx.SimpleCallBack;

import static com.tg.wallet.Constant.WALLET_FILE_NAME;
import static com.tg.wallet.MyWalletUtil.KEYSTORE;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Credentials credentials;
    private String fromAddress;
    private String toAddress;
    private ImageView qrcodeIv;
    private TextView infoTv, errorTv, tv_address, tv_privateKey, tv_publicKey;
    private EditText tv_keystore;
    private Dialog dialog;
    private TextInputEditText et_password, et_keystore, et_privateKey;

    private ObjectReader objectReader;
    private ObjectMapper objectMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        qrcodeIv = findViewById(R.id.qrcode_iv);
        infoTv = findViewById(R.id.wallet_info_tv);
        errorTv = findViewById(R.id.error_info_tv);
        tv_address = findViewById(R.id.tv_address);
        tv_privateKey = findViewById(R.id.tv_privateKey);
        tv_publicKey = findViewById(R.id.tv_publicKey);
        tv_keystore = findViewById(R.id.tv_keystore);
        et_password = findViewById(R.id.et_password);
        et_keystore = findViewById(R.id.et_keystore);
        et_privateKey = findViewById(R.id.et_privateKey);
        init();
//        getJson();
//        setFromAddress("0xF7b344DdB477A3B9296e907A6e5D8AF42e661782");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void init() {
//        一进来先加载已存在的钱包
        loadWalletFile();
        if (!TextUtils.isEmpty(this.getFromAddress())) {
            qrcodeIv.setImageBitmap(QrcodeGen.genQrcodeBitmap(150, this.getFromAddress()));
        }
    }

//    private Credentials getFullWallet( String password, String source) throws IOException, JSONException, CipherException {
//
//        return WalletUtils.loadCredentials(password, source);
//    }

    /**
     * 导入钱包  Keystore
     *
     * @param view
     */
    public void importWallet(View view) {
        String password = et_password.getText().toString().trim();
        String keystore = et_keystore.getText().toString().trim();
        if (!password.equals("") && !keystore.equals("")) {
            try {
//                File destination = new File(AppFilePath.Wallet_DIR, Constant.WALLET_FILE_NAME);
//                objectMapper = ObjectMapperFactory.getObjectMapper();
//                objectMapper.writeValue(destination, keystore);
                String source = AppFilePath.Wallet_DIR + WALLET_FILE_NAME;//路径
                Log.e(TAG, "导入钱包路径: " + source);
                saveAsFileWriter(source, keystore);

                Credentials credentials = WalletUtils.loadCredentials(password, source);
                this.setFromAddress(credentials.getAddress());
                this.setCredentials(credentials);
                showInfoSafe2(credentials.getAddress(), "0x" + credentials.getEcKeyPair().getPublicKey().toString(16), "0x" + credentials.getEcKeyPair().getPrivateKey().toString(16));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CipherException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "请输入密码和keystore", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 导入钱包  私钥
     *
     * @param view
     */
    public void importWallet2(View view) {
        String password = et_password.getText().toString().trim();
        String privateKey = et_privateKey.getText().toString().trim();
        if (!password.equals("") && !privateKey.equals("")) {

            ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.toBigInt(privateKey));
            try {
                WalletFile walletFile = Wallet.create(password, ecKeyPair, 16, 1);
                String address = walletFile.getAddress();
                objectMapper = ObjectMapperFactory.getObjectMapper();
                String keystore = objectMapper.writeValueAsString(walletFile);

                showInfoSafe3(address, keystore);

            } catch (CipherException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(this, "请输入密码和privateKey", Toast.LENGTH_SHORT).show();
        }
    }


    //保存字符串到文件中
    private void saveAsFileWriter(String fileName, String content) {
        Log.e(TAG, "Keystore: " + content);
        System.out.println(content);
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(fileName);
            fwriter.write(content);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fwriter != null) {
                    fwriter.flush();
                    fwriter.close();
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * 扫码转账
     *
     * @param view
     */
    public void send(View view) {
        Intent intent = new Intent(this, QrcodeActivity.class);
        startActivity(intent);
    }

    /**
     * 加载钱包  点击事件
     *
     * @param view
     */
    public void loadWallet(View view) {
        RxLogicHandler.<String>doWork(new GetBalanceExcutor(), new GetBalanceCallBack());
    }

    /**
     * 查询账户余额
     */
    class GetBalanceExcutor implements RxLogicHandler.Excutor<String> {

        @Override
        public String excute() throws Exception {
            Web3j web3j = Web3jFactory.build(new HttpService(Constant.WEB_URL));  // defaults to http://localhost:8545/
            EthGetBalance ethGetBalance = web3j.ethGetBalance(getFromAddress(), new DefaultBlockParameter() {
                @Override
                public String getValue() {
                    return "latest";
                }
            }).sendAsync().get();
            BigInteger bigInteger = ethGetBalance.getBalance();
            return bigInteger + "";
        }
    }

    /**
     * 查询账户余额回调处理
     */
    class GetBalanceCallBack extends SimpleCallBack<String> {

        @Override
        protected void onSuccess(String s) {
            showInfoSafe(s);
        }

        @Override
        protected void onFailed(Throwable e) {
            showErrorInfoSafe("查询余额失败:" + e.getMessage() + "   " + e.getCause());
        }
    }

    /**
     * 创建钱包
     *
     * @param view
     */
    public void createWallet(View view) {
        if (!et_password.getText().toString().trim().equals("")) {
            Credentials credentials = MyWalletUtil.createWallet(et_password.getText().toString().trim());
            SharedPreferences mSharedPreferences = getSharedPreferences("wallet", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("password", et_password.getText().toString().trim());
            editor.apply();
            if (credentials == null) {
                showSafeToast("创建钱包失败!");
                return;
            }
            ECKeyPair ecKeyPair = credentials.getEcKeyPair();
            this.setCredentials(credentials);
            this.setFromAddress(credentials.getAddress());

            if (!TextUtils.isEmpty(this.getFromAddress())) {
                qrcodeIv.setImageBitmap(QrcodeGen.genQrcodeBitmap(150, this.getFromAddress()));
                tv_address.setText(this.getFromAddress());
                tv_privateKey.setText("私钥 " + ecKeyPair.getPrivateKey().toString(16));
                tv_publicKey.setText("公钥 " + ecKeyPair.getPublicKey().toString(16));
                tv_keystore.setText("KeyStore  " + KEYSTORE);
            }
        } else {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * 转账
     */
    private void sendTransaction() {

        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                Web3j web3j = Web3jFactory.build(new HttpService(Constant.WEB_URL));  // defaults to http://localhost:8545/
                TransactionReceipt transactionReceipt = Transfer.sendFunds(web3j, credentials, getToAddress(), BigDecimal.valueOf(1.0), Convert.Unit.ETHER).send();

                String hash = transactionReceipt.getTransactionHash();
                EthGetTransactionReceipt ethGetTransactionReceipt =
                        web3j.ethGetTransactionReceipt(hash).sendAsync().get();
                String info = "hash=" + hash + " \nvalue=" + BigDecimal.valueOf(1.0) + "\ntoAddress=" + getToAddress() + "\nformAddress=" + getFromAddress();
                e.onNext(info);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        showDialog("正在交易...");
                    }

                    @Override
                    public void onNext(String s) {
                        showErrorInfoSafe(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        showErrorInfoSafe("交易失败:" + e.getMessage() + "  cause=" + e.getCause());
                    }

                    @Override
                    public void onComplete() {
                        dismissDialog();
                    }
                });

    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    private void showSafeToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showErrorInfoSafe(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errorTv.setText(msg);
            }
        });
    }

    /**
     * 收到对端钱包地址 开始转账   使用EventBus传值
     *
     * @param event
     */
    public void onEventMainThread(QrCodeEvent event) {
        String addres = event.getResult();
        this.setToAddress(addres);
        sendTransaction();

    }


    /**
     * 加载钱包
     * 已存在的
     */
    private void loadWalletFile() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, new String[]{PermisionsConstant.WRITE_EXTERNAL_STORAGE, PermisionsConstant.READ_EXTERNAL_STORAGE, PermisionsConstant.CAMERA}, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                try {

                    SharedPreferences mSharedPreferences = getSharedPreferences("wallet", Context.MODE_PRIVATE);
                    String password = mSharedPreferences.getString("password", "");
                    String source = AppFilePath.Wallet_DIR + WALLET_FILE_NAME;//路径

                    Log.e(TAG, "onGranted: " + source);///storage/emulated/0/juethwallet/ju_wallet.json
                    if (!password.equals("")) {
                        Credentials credentials = WalletUtils.loadCredentials(password, source);
                        MainActivity.this.setFromAddress(credentials.getAddress());
                        MainActivity.this.setCredentials(credentials);
                    } else {
                        Toast.makeText(MainActivity.this, "密码为空", Toast.LENGTH_SHORT).show();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "加载钱包失败", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onDenied(String permission) {
                finish();
            }
        });
    }

    private void showInfoSafe(final String value) {
        String info = "钱包地址：" + getFromAddress() + "\n" + "钱包余额：" + value;

        infoTv.setText(info);
    }

    private void showInfoSafe2(final String address, String publicKey, String privatekey) {
        String info = "钱包地址：" + address + "\n" + "公钥：" + publicKey + "\n" + "私钥：" + privatekey;

        infoTv.setText(info);
    }

    private void showInfoSafe3(final String address, String keystore) {
        String info = "钱包地址：" + "0x"+address + "\n" + "Keystore:：" + keystore;

        infoTv.setText(info);
    }

    private void showDialog(String msg) {
        if (dialog == null) {
            dialog = new ProgressDialog(this);
        }
        dialog.show();
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * 测试查询交易记录
     *
     * @param view
     */
    public void testHis(View view) {
        EasyHttp.getInstance().setCertificates();
        EasyHttp.get("/address/0x2de128a62cadc32097b5719807c16db2024a08da")
                .baseUrl("https://etherscan.io")
                .execute(new CallBack<String>() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(ApiException e) {

                    }

                    @Override
                    public void onSuccess(String o) {

                    }
                });
    }

}
