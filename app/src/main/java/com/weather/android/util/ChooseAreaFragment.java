package com.weather.android.util;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.weather.android.R;
import com.weather.android.db.City;
import com.weather.android.db.County;
import com.weather.android.db.Province;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    // vars
    final int LEVEL_PROVINCE = 0;
    final int LEVEL_CITY = 1;
    final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;

    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;
    // methodso
@Override
public View onCreateView(LayoutInflater inflater , ViewGroup container, Bundle savedInstanceState){
    View view = inflater.inflate(R.layout.choose_area,container,false);
    titleText = (TextView) view.findViewById(R.id.title_text);
    backButton = (Button) view.findViewById(R.id.back_button);
    listView = (ListView) view.findViewById(R.id.list_view);
    adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
    listView.setAdapter(adapter);
    return view;
}
@Override
    public void onActivityCreated(Bundle savedInstanceState) {

    super.onActivityCreated(savedInstanceState);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (currentLevel == LEVEL_PROVINCE){
                selectedProvince = provinceList.get(position);
                queryCities();
            }else if (currentLevel == LEVEL_CITY){
                selectedCity = cityList.get(position);
                queryCounties();
            }
        }
    });
    backButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentLevel ==LEVEL_COUNTY){
                queryCities();
            }else if (currentLevel == LEVEL_CITY){
                queryProvinces();
            }
        }
    });
    queryProvinces();
}
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
}
private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china"+provinceCode;
            queryFromServer(address, "city");
        }
}
private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList= DataSupport.where("cityid = ?",
            String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china"+provinceCode+"/"+cityCode;
            queryFromServer(address, "county");
        }
}

private void queryFromServer(String address,final String type){
showProgressDialog();
    HttpUtil.sendOKHttpRequest(address, new Callback() {
        @Override
        public void onResponse(Call call, Response response)throws IOException {
            String responseText = response.body().string();
            boolean result = false;
            if ("province".equals(type)){
                result = Utility.handleProvinceResponse(responseText);
            }else if("city".equals(type)){
                result = Utility.handleCityResponse(responseText,selectedProvince.getId());
            }else if ("county".equals(type)){
                result = Utility.handleCountyResponse(responseText,selectedCity.getId());
            }
        if (result){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    closePragressDialog();
                    if ("province".equals(type)){
                        queryProvinces();
                    }else if ("city".equals(type)){
                        queryCities();
                    }else if ("county".equals(type)){
                        queryCounties();
                    }
                }
            });
        }
        }

        @Override
        public void onFailure(Call call, IOException e){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    closePragressDialog();
                    Toast.makeText(getContext(),"加载失败...",Toast.LENGTH_SHORT).show();
                }
            });
        }
    });
}
private void showProgressDialog(){
    if (progressDialog == null){
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("加载中...");
        progressDialog.setCanceledOnTouchOutside(false);
    }
    progressDialog.show();
}
private void closePragressDialog(){
    if (progressDialog == null){
        progressDialog.dismiss();
    }
}
}