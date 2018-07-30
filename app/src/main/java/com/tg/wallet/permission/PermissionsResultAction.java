package com.tg.wallet.permission;



public interface PermissionsResultAction {
    void onGranted();//允许
    void onDenied(String permission);//拒绝
}
