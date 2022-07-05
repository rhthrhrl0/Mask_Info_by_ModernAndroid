package com.example.mask_info;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.mask_info.model.Store;
import com.example.mask_info.model.StoreInfo;
import com.example.mask_info.repository.MaskService;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
    // 4를 예측하고 실제 값이 같게 나오면 테스트성공이라고 알려주는 테스트 함수.
    // 2+3으로 바꾸고 이 함수를 실행 하면 틀렸다고 나옴.

    //테스트 코드를 작성.
    @Test
    public void retrofitTest() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MaskService.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())  //레트로핏에 무슨 컨버터 사용할지 지정.
                .build();

        MaskService service = retrofit.create(MaskService.class);

        // 받아오는 준비를 하는거지. 실제로 받아오는게 아님.
        Call<StoreInfo> storeInfoCall = service.fetchStoreInfo(10.3,127.3);

        // 실제로 받아옴. 근데 execute는 동기적으로 그냥 메인스레드로 받음.
        StoreInfo storeInfo=storeInfoCall.execute().body(); //execute는 예외를 던지는 함수임.

        // 제대로 받아온게 맞는지 확인하는 거임.
        assertEquals(222,storeInfo.getCount());
        assertEquals(222,storeInfo.getStores().size());
    }
}