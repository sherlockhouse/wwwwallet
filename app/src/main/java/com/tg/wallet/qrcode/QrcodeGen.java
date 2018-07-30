package com.tg.wallet.qrcode;

import android.graphics.Bitmap;

import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder;



public class QrcodeGen {
    public static Bitmap genQrcodeBitmap(int dpWidth, String content){
        Bitmap bitmap= QRCodeEncoder.syncEncodeQRCode(content, dpWidth);
//        String base64Bmp= BitmapUtil.encode2Base64ByBitmap(bitmap);
        return bitmap;
    }
}
