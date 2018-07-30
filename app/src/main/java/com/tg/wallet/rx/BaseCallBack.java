package com.tg.wallet.rx;


/**
 * 回调基类
 * @param <T>
 */
public abstract class BaseCallBack<T> {
    protected abstract void onStart();
    protected abstract void onSuccess(T t);
    protected abstract void onFailed(Throwable e);
    protected abstract void onCompleted();
}
