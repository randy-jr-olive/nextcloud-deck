package it.niedermann.nextcloud.deck.persistence.sync.adapters.db.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

public class LiveDataHelper {

    public interface DataChangeProcessor<T>{
        void onDataChanged(T data);
    }

    public interface DataTransformator<I, O>{
        O transform(I data);
    }

    public interface LiveDataWrapper<T>{
        T getData();

        default void postResult(WrappedLiveData<T> liveData){
            liveData.setError(null);
            T data = null;
            try {
                data = getData();
            } catch (RuntimeException e) {
                liveData.setError(e);
            }
            liveData.postValue(data);
        }
    }

    public static <T> MediatorLiveData<T> interceptLiveData(LiveData<T> data, DataChangeProcessor<T> onDataChange) {
        MediatorLiveData<T> ret = new MediatorLiveData<>();

        ret.addSource(data, changedData ->
            doAsync(() -> {
                onDataChange.onDataChanged(changedData);
                ret.postValue(changedData);
            })
        );
        return onlyIfChanged(ret);
    }


    public static <I, O> MediatorLiveData<O> postCustomValue(LiveData<I> data, DataTransformator<I, O> transformator) {
        MediatorLiveData<O> ret = new MediatorLiveData<>();

        ret.addSource(data, changedData -> doAsync(() ->ret.postValue(transformator.transform(changedData))));
        return onlyIfChanged(ret);
    }

    public static <I> MediatorLiveData<I> of(I oneShot) {
        MediatorLiveData<I> ret = new MediatorLiveData<I>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer observer) {
                super.observe(owner, observer);
                doAsync(() -> postValue(oneShot));
            }
        };
        return ret;
    }

    public static <I, O> MediatorLiveData<O> postSingleValue(LiveData<I> data, DataTransformator<I, O> transformator) {
        MediatorLiveData<O> ret = new MediatorLiveData<>();

        ret.addSource(data, changedData -> doAsync(() ->ret.postValue(transformator.transform(changedData))));
        return onlyIfChanged(ret);
    }

    public static <T> MediatorLiveData<T> onlyIfChanged(LiveData<T> data) {
        MediatorLiveData<T> ret = new MediatorLiveData<>();

        ret.addSource(data, new Observer<T>() {
            T lastObject = null;
            @Override
            public void onChanged(@Nullable T newData) {
                boolean hasValueChanged =
                        (lastObject != null && newData == null) ||
                        (lastObject == null && newData != null) ||
                        (lastObject != null && !lastObject.equals(newData)) ||
                        (newData != null && !newData.equals(lastObject))                        ;


                lastObject = newData;
                if (hasValueChanged){
                    ret.postValue(newData);
                }

            }
        });
        return ret;
    }

    public static <T> void observeOnce(LiveData<T> liveData, LifecycleOwner owner, Observer<T> observer){
        Observer<T> tempObserver = new Observer<T>() {
            @Override
            public void onChanged(T result) {
                liveData.removeObserver(this);
                observer.onChanged(result);
            }
        };
        liveData.observe(owner, tempObserver);
    }

    public static <T> WrappedLiveData<T> wrapInLiveData(final LiveDataWrapper<T> liveDataWrapper) {
        final WrappedLiveData<T> liveData = new WrappedLiveData<>();

        doAsync(() -> liveDataWrapper.postResult(liveData));

        return liveData;
    }

    private static void doAsync(Runnable r) {
        new Thread(r).start();
    }
}