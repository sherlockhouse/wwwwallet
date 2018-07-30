package com.tg.wallet;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.ObjectMapperFactory;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * 钱包Utils
 */
public class MyWalletUtil {
    private static final String TAG = "MyWalletUtil";
    public static String KEYSTORE = "MyWalletUtil";
    static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
    public static String ETH_JAXX_TYPE = "m/44'/60'/0'/0/0";
    /**
     * 随机
     */
    private static final SecureRandom secureRandom = SecureRandomUtils.secureRandom();



    public static Credentials createWallet(String password) {
        ECKeyPair ecKeyPair = null;
        WalletFile walletFile = null;
        try {
            ecKeyPair = Keys.createEcKeyPair();
//            walletFile = Wallet.create(Constant.PASSWORD, ecKeyPair, 16, 1); // WalletUtils. .generateNewWalletFile();
            walletFile = Wallet.create(password, ecKeyPair, 16, 1); // WalletUtils. .generateNewWalletFile();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


        File destination = new File(AppFilePath.Wallet_DIR, Constant.WALLET_FILE_NAME);

        //目录不存在则创建目录，创建不了则报错
        if (!createParentDir(destination)) {
            return null;
        }
        try {
            /**
             * 保存钱包到本地
             */
            objectMapper.writeValue(destination, walletFile);
            KEYSTORE = objectMapper.writeValueAsString(walletFile);
            Log.e(TAG, "createWallet: " + KEYSTORE);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return Credentials.create(ecKeyPair);
    }

    private static boolean createParentDir(File file) {
        //判断目标文件所在的目录是否存在
        if (!file.getParentFile().exists()) {
            //如果目标文件所在的目录不存在，则创建父目录
            System.out.println("目标文件所在目录不存在，准备创建");
            if (!file.getParentFile().mkdirs()) {
                System.out.println("创建目标文件所在目录失败！");
                return false;
            }
        }
        return true;
    }
}
