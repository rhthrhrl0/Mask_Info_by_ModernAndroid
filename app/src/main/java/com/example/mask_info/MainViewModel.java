package com.example.mask_info;

import android.location.Location;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mask_info.model.Store;
import com.example.mask_info.model.StoreInfo;
import com.example.mask_info.repository.MaskService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class MainViewModel extends ViewModel {
    private static final String TAG = MainViewModel.class.getSimpleName();

    // 변경이 가능한 라이브데이터로 감싸기. 지금 이상태는 List<Store>가 널임.
    public MutableLiveData<List<Store>> itemLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> loadingLiveData=new MutableLiveData<>();

    public Location location;

    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(MaskService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())  //레트로핏에 무슨 컨버터 사용할지 지정.
            .build();

    private MaskService service = retrofit.create(MaskService.class);

    // 받아오는 준비를 하는거지. 실제로 받아오는게 아님.
    //private Call<StoreInfo> storeInfoCall = service.fetchStoreInfo();

    public MainViewModel() {
        // 기본 생성자. 이게 기본적으로 호출됨. 초기화 할 작업은 여기에 넣으면 된다.
        if (location == null) {
            location = new Location("a");
        }
        fetchStoreInfo();
    }

    // 외부에서 이걸 호출하면 네트워크 통신으로 데이터를 받아옴.
    public void fetchStoreInfo() {
        //로딩시작
        loadingLiveData.postValue(true);

        //레트로핏의 콜 객체는 한번만 사용될 수 있다? 그러므로 클론을 사용하면 콜로 여러번 호출 가능하다고 스택오버플로우에 나옴.
        service.fetchStoreInfo(location.getLatitude(), location.getLongitude())
                .clone()
                .enqueue(new Callback<StoreInfo>() {
                    @Override
                    public void onResponse(Call<StoreInfo> call, Response<StoreInfo> response) {
                        Log.d(TAG, "onResponse: refresh");

                        // 실제로 네트워크 통신으로 받아온 데이터
                        List<Store> items = response.body().getStores().stream()
                                .filter(item -> item.getRemainStat() != null)//람다식임.
                                .filter(item -> !item.getRemainStat().equals("empty"))//람다식임.
                                .collect(Collectors.toList());

                        //for-each
                        for (Store store : items) {
                            double distance = LocationDistance.distance(store.getLat(), store.getLng(), location.getLatitude(), location.getLongitude(), "k");
                            store.setDistance(distance);
                        }
                        //정렬. 정렬기준은 Store에 설정함.
                        Collections.sort(items);
                        // setValue는 비동기에 적합하지 않음. 비동기인 백그라운드 상황에서는 postValue를 사용해라.
                        itemLiveData.postValue(items);

                        //로딩 끝
                        loadingLiveData.postValue(false);
                    }

                    @Override
                    public void onFailure(Call<StoreInfo> call, Throwable t) {
                        //실패시 실행할 로직
                        Log.e(TAG, "onFailure: ", t);
                        //네트워크 통시 실패하면 에러시. 빈 리스트를 주자.
                        itemLiveData.postValue(Collections.emptyList());

                        //로딩 끝
                        loadingLiveData.postValue(false);
                    }
                });
    }

}
