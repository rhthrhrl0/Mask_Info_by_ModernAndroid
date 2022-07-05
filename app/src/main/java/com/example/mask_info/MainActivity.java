package com.example.mask_info;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mask_info.model.Store;
import com.example.mask_info.model.StoreInfo;
import com.example.mask_info.repository.MaskService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class MainActivity extends AppCompatActivity {

    //보통은 태그를 이렇게 사용함.
    private static final String TAG = MainActivity.class.getSimpleName();

    private MainViewModel model;

    //위치정보를 얻기 위한 통합위치정보제공자
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 위치 정보 얻을 준비.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //위치권한 있는지 체크
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                performAction();
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        //권한 체크
        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                .check();

        //model.fetchStoreInfo(); // 네트워크 통신 호출 => 이걸 뷰모델의 생성자에 넣으면 뷰모델 생성시 한번만 됨.
    }

    // 퍼미션 체크를 안해도 에러처리를 숨겨줌.
    @SuppressLint("MissingPermission")
    private void performAction() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        //통합위치 관리자로부터 getLastLocation()을 수행해서 성공시 마지막 위치의 위도 경도 얻기.
                        Log.d(TAG,"performAction: "+ location.getLatitude());
                        Log.d(TAG,"performAction: "+ location.getLongitude());
                        //위치 정보 받고 네트워크 통신
                        location.setLatitude(37.188);
                        location.setLongitude(127.043);
                        model.location=location;
                        model.fetchStoreInfo();
                    }
                    //알면 좋은 사실: 익명클래스객체 안에서 this를 쓰면 그 객체를 가리키지만, 람다식에서 this는 액티비티에 접근 가능.
                });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        // 리사이클러뷰에는 레이아웃 매니저를 의존성주입으로 넣어줘야 함. 우리는 그 중에서 리니어 레이아웃 매니저이고(컨텍스트,방향,역순서 여부)
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // 어댑터 설정.
        final StoreAdapter adapter=new StoreAdapter();
        recyclerView.setAdapter(adapter);

        // 뷰모델로 연결.
        model = new ViewModelProvider(this).get(MainViewModel.class);
        model.itemLiveData.observe(this, itemLiveData -> {
            // 관찰하다가 바뀌면 그때 이 부분을 실행. 여기에는 ui변경 로직만.
            adapter.updateItems(itemLiveData);
            getSupportActionBar().setTitle("마스크 재고 있는 곳: "+itemLiveData.size());
        });

        model.loadingLiveData.observe(this, isLoading->{
            if (isLoading){
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            }
            else{
                findViewById(R.id.progressBar).setVisibility(View.GONE);
            }
        });
    }

    // 메뉴를 내가 커스텀한 걸로 설정.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // 리프레쉬 요청
                model.fetchStoreInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {
    private List<Store> mItems = new ArrayList<>(); //초반에 그냥 초기화 해놓는게 널에러 안나고 좋음.

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //안드로이드의 모든 뷰 및 뷰그룹에서 자체적으로 컨텍스트를 얻을 수 있다. 이 리사이클러뷰가 붙을 뷰그룹의 컨텍스트를 얻음.
        // 이건 그냥 외우면 됨. 지정한 레이아웃 대로 인플레이트 시킬건데, parent 에게 올릴 뷰임. 근데 인플레이트 시킬때 붙여서 나오지는 않게
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store, parent, false);

        // 인플레이트 된 view를 지닌 뷰홀더를 생성해서 리턴함.
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = mItems.get(position); //뷰홀더와 연결시킬 Store를 얻음.
        holder.nameTextView.setText(store.getName()); //뷰홀더와 연결된 자식뷰에 값을 설정.
        holder.addressTextView.setText(store.getAddr());
        holder.distanceTextView.setText(String.format("%.2fkm",store.getDistance())); //가공 데이터 필요

        String remainStat = "충분";

        String count = "100개 이상"; //디폴트 값.
        int color= Color.GREEN;

        switch (store.getRemainStat()) {
            case "plenty":
                count = "100개 이상";
                remainStat = "충분";
                color= Color.GREEN;
                break;
            case "some":
                count = "30개 이상";
                remainStat = "여유";
                color= Color.YELLOW;
                break;
            case "few":
                count = "2개 이상";
                remainStat = "매진 임박";
                color= Color.RED;
                break;
            case "empty":
                count = "1개 이하";
                remainStat = "재고 없음";
                color= Color.GRAY;
                break;
            default:
        }
        holder.remainTextView.setText(remainStat);
        holder.countTextView.setText(count);

        holder.remainTextView.setTextColor(color);
        holder.countTextView.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    //외부에서 새로운 데이터를 주입해서 교체시킴.
    public void updateItems(List<Store> mItems) {
        this.mItems = mItems;
        notifyDataSetChanged(); //UI 갱신
    }

    // 뷰홀더는 static클래스로 만들어야 함.아이템 뷰 정보를 가지고 있는 클래스임.
    static class StoreViewHolder extends RecyclerView.ViewHolder {
        // 뷰홀더의 멤버변수들.
        TextView nameTextView;
        TextView addressTextView;
        TextView distanceTextView;
        TextView remainTextView;
        TextView countTextView;

        //생성자.
        public StoreViewHolder(@NonNull View itemView) {
            super(itemView); //final로 선언된 뷰에다가 의존성주입으로 설정시킴. 이제 이 뷰홀더는 이거임.

            //생성자 안에서 멤버변수와 연결해줌. 이제 뷰홀더 쪽의 멤버변수로 뷰홀더와 연결된 뷰의 자식뷰들에 접근 가능.
            nameTextView = itemView.findViewById(R.id.name_text_view);
            addressTextView = itemView.findViewById(R.id.addr_text_view);
            distanceTextView = itemView.findViewById(R.id.distance_text_view);
            remainTextView = itemView.findViewById(R.id.remain_text_view);
            countTextView = itemView.findViewById(R.id.count_text_view);
        }
    }

}

