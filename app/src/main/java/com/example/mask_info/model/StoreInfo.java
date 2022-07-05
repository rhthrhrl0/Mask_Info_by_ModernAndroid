
package com.example.mask_info.model;

import java.util.List;

public class StoreInfo {

    @com.squareup.moshi.Json(name = "count")
    private int count;
    @com.squareup.moshi.Json(name = "stores")
    private List<Store> stores = null;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Store> getStores() {
        return stores;
    }

    public void setStores(List<Store> stores) {
        this.stores = stores;
    }

}
