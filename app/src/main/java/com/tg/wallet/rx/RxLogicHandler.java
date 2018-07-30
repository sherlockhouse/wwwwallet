package com.tg.wallet.rx;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;


public class RxLogicHandler {
    public static <T> void doWork(final Excutor excutor,BaseCallBack<T> callBack){
        Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(ObservableEmitter<T> e) throws Exception {
                T t=(T)excutor.excute();
                e.onNext(t);
                e.onComplete();
            }
        }).subscribe(new BaseOberver<T>(callBack));
    }

    public interface Excutor<Result>{
        Result excute()throws Exception;
    }
}
