package org.nearbyshops.enduserappnew.adminModule.ShopsList.Fragment;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.google.gson.Gson;

import org.nearbyshops.enduserappnew.API.ShopService;
import org.nearbyshops.enduserappnew.EditDataScreens.EditShop.EditShop;
import org.nearbyshops.enduserappnew.Model.ModelEndPoints.ShopEndPoint;
import org.nearbyshops.enduserappnew.Model.Shop;
import org.nearbyshops.enduserappnew.DaggerComponentBuilder;
import org.nearbyshops.enduserappnew.EditDataScreens.EditShop.EditShopFragment;
import org.nearbyshops.enduserappnew.Interfaces.GetLocation;
import org.nearbyshops.enduserappnew.Interfaces.NotifyLocation;
import org.nearbyshops.enduserappnew.Interfaces.NotifySearch;
import org.nearbyshops.enduserappnew.Interfaces.NotifySort;
import org.nearbyshops.enduserappnew.Interfaces.NotifyTitleChanged;
import org.nearbyshops.enduserappnew.R;
import org.nearbyshops.enduserappnew.Utility.UtilityFunctions;
import org.nearbyshops.enduserappnew.ViewHolders.ViewHolderShopType2;
import org.nearbyshops.enduserappnew.adminModule.ShopsList.SlidingLayerSort.PrefSortShops;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FragmentShopList extends Fragment implements SwipeRefreshLayout.OnRefreshListener , NotifySearch, NotifySort, NotifyLocation ,
        ViewHolderShopType2.ListItemClick {

    private static final String ARG_SECTION_NUMBER = "section_number";

    Location location;

    @Inject
    ShopService shopService;

    RecyclerView recyclerView;
    Adapter adapter;

    public List<Object> dataset = new ArrayList<>();

    GridLayoutManager layoutManager;
    SwipeRefreshLayout swipeContainer;


    final private int limit = 5;
    int offset = 0;
    int item_count = 0;

    boolean isDestroyed;

    public static final int MODE_ENABLED = 1;
    public static final int MODE_DISABLED = 2;
    public static final int MODE_WAITLISTED = 3;
    public static final int MODE_NEW = 4;



    public FragmentShopList() {

        DaggerComponentBuilder.getInstance()
                .getNetComponent()
                .Inject(this);
    }

    public static FragmentShopList newInstance(int sectionNumber) {
        FragmentShopList fragment = new FragmentShopList();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setRetainInstance(true);
        View rootView = inflater.inflate(R.layout.fragment_shop_approvals, container, false);


        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        swipeContainer = (SwipeRefreshLayout)rootView.findViewById(R.id.swipeContainer);


        if(savedInstanceState==null)
        {
            makeRefreshNetworkCall();
        }


        setupRecyclerView();
        setupSwipeContainer();

        return rootView;
    }





    private void setupSwipeContainer()
    {
        if(swipeContainer!=null) {

            swipeContainer.setOnRefreshListener(this);
            swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
        }

    }




    private void setupRecyclerView()
    {

        adapter = new Adapter(dataset,getActivity(),this);

        recyclerView.setAdapter(adapter);

        layoutManager = new GridLayoutManager(getActivity(),1);
        recyclerView.setLayoutManager(layoutManager);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

//        layoutManager.setSpanCount(metrics.widthPixels/400);


//        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));


        int spanCount = (int) (metrics.widthPixels/(230 * metrics.density));

        if(spanCount==0){
            spanCount = 1;
        }

        layoutManager.setSpanCount(1);


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);



                if(offset + limit > layoutManager.findLastVisibleItemPosition()+1-1)
                {
                    return;
                }

                if(layoutManager.findLastVisibleItemPosition()==dataset.size()-1+1)
                {
                    // trigger fetch next page

                    if((offset+limit)<=item_count)
                    {
                        offset = offset + limit;
                        makeNetworkCall(false,false);
                    }
                }

            }
        });
    }



    @Override
    public void onRefresh() {
        makeNetworkCall(true,true);
    }




    private void makeRefreshNetworkCall()
    {

        swipeContainer.post(new Runnable() {
            @Override
            public void run() {
                swipeContainer.setRefreshing(true);

                onRefresh();
            }
        });

    }







    private void makeNetworkCall(final boolean clearDataset, final boolean resetOffset)
    {
        if(resetOffset)
        {
            offset = 0;
        }


        Call<ShopEndPoint> call = null;

        String current_sort = "";
        current_sort = PrefSortShops.getSort(getContext()) + " " + PrefSortShops.getAscending(getContext());


        Double latitude = null;
        Double longitude = null;


        if(getActivity() instanceof GetLocation)
        {
            this.location = ((GetLocation)getActivity()).getLocation();
        }



        if(location!=null)
        {
            showToastMessage("Location" + String.valueOf(this.location.getLongitude()) + " : " + String.valueOf(this.location.getLatitude()));
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }


//        latitude = UtilityLocationServices.getLatitude(getActivity());
//        longitude = UtilityLocationServices.getLongitude(getActivity());

//        showToastMessage("Latitude : " + UtilityLocationServices.getLatitude(getActivity()) + " : Longitude " + UtilityLocationServices.getLongitude(getActivity()));


        if(getArguments().getInt(ARG_SECTION_NUMBER)==MODE_NEW)
        {
            call = shopService.getShopListSimple(
                    true,
                    null,null,
                    null,
                    latitude,longitude,
                    null,null, null,
                    searchQuery,current_sort,limit,offset);
        }
        else if(getArguments().getInt(ARG_SECTION_NUMBER)==MODE_ENABLED)
        {

            call = shopService.getShopListSimple(
                    null,
                    true,null,
                    null,
                    latitude,longitude,
                    null,null, null,
                    searchQuery,current_sort,limit,offset);

        }
        else if(getArguments().getInt(ARG_SECTION_NUMBER) == MODE_DISABLED)
        {

            call = shopService.getShopListSimple(
                    null,
                    false,false,
                    null,
                    latitude,longitude,
                    null,null,null,
                    searchQuery,current_sort,limit,offset);
        }
        else if (getArguments().getInt(ARG_SECTION_NUMBER) == MODE_WAITLISTED)
        {

            call = shopService.getShopListSimple(
                    null,
                    false,true,
                    null,
                    latitude,longitude,
                    null,null,null,
                    searchQuery,current_sort,limit,offset);
        }




        if(call == null)
        {
            showToastMessage("Error fetching shop list !");
            return;
        }


        call.enqueue(new Callback<ShopEndPoint>() {
            @Override
            public void onResponse(Call<ShopEndPoint> call, Response<ShopEndPoint> response) {


                if(isDestroyed)
                {
                    return;
                }

//                if(response.body()!= null)
//                {


                    if(response.code()==200)
                    {
                        item_count = response.body().getItemCount();

                        if(clearDataset)
                        {
                            dataset.clear();
                        }

                        if(response.body().getResults()!=null)
                        {
                            dataset.addAll(response.body().getResults());
                            adapter.notifyDataSetChanged();
                            notifyTitleChanged();
                        }


                    }
                    else
                    {
                        showToastMessage("Failed code : " + String.valueOf(response.code()));
                    }

//                showToastMessage("Status Code : " + String.valueOf(response.code())
//                + "\nDataset Size : " + dataset.size() + " Item Count : " + response.body().getItemCount());

                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<ShopEndPoint> call, Throwable t) {

                if(isDestroyed)
                {
                    return;
                }

                showToastMessage("Network Request failed !");
                swipeContainer.setRefreshing(false);

            }
        });

    }




    @Override
    public void onResume() {
        super.onResume();
        notifyTitleChanged();
    }





    private void showToastMessage(String message)
    {
        if(getActivity()!=null)
        {
            Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed=true;
    }




    private void notifyTitleChanged()
    {

        if(getActivity() instanceof NotifyTitleChanged)
        {

            if(getArguments().getInt(ARG_SECTION_NUMBER)==MODE_NEW)
            {
                ((NotifyTitleChanged)getActivity())
                        .NotifyTitleChanged(
                                "New (" + String.valueOf(dataset.size())
                                        + "/" + String.valueOf(item_count) + ")",0);
            }
            else if(getArguments().getInt(ARG_SECTION_NUMBER)==MODE_ENABLED)
            {
                ((NotifyTitleChanged)getActivity())
                        .NotifyTitleChanged(
                                "Enabled (" + String.valueOf(dataset.size())
                                        + "/" + String.valueOf(item_count) + ")",1);

            }
            else if(getArguments().getInt(ARG_SECTION_NUMBER)==MODE_DISABLED)
            {
                ((NotifyTitleChanged)getActivity())
                        .NotifyTitleChanged(
                                "Disabled (" + String.valueOf(dataset.size())
                                        + "/" + String.valueOf(item_count) + ")",2);
            }
            else if(getArguments().getInt(ARG_SECTION_NUMBER)==MODE_WAITLISTED)
            {
                ((NotifyTitleChanged)getActivity())
                        .NotifyTitleChanged(
                                "Waitlisted (" + String.valueOf(dataset.size())
                                        + "/" + String.valueOf(item_count) + ")",3);

            }

        }
    }


    // Refresh the Confirmed PlaceHolderFragment

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }








    private String searchQuery = null;

    @Override
    public void search(String searchString) {
        searchQuery = searchString;
        makeRefreshNetworkCall();
    }

    @Override
    public void endSearchMode() {
        searchQuery = null;
        makeRefreshNetworkCall();
    }


    @Override
    public void notifySortChanged() {
        makeRefreshNetworkCall();
    }



    @Override
    public void fetchedLocation(Location location) {
//        this.location = location;
        makeRefreshNetworkCall();
    }







    @Override
    public void listItemClick(Shop shop, int position) {


//        PrefShopForAdmin.saveShop(shop,getActivity());
//        Intent intent = new Intent(getActivity(), EditShopForAdmin.class);
//        intent.putExtra(EditShopFragment.EDIT_MODE_INTENT_KEY, EditShopForAdminFragment.MODE_UPDATE);
//        startActivity(intent);



        Gson gson = UtilityFunctions.provideGson();
        String jsonString = gson.toJson(shop);

        Intent intent = new Intent(getActivity(), EditShop.class);
        intent.putExtra("shop_profile",jsonString);
        intent.putExtra(EditShopFragment.EDIT_MODE_INTENT_KEY, EditShopFragment.MODE_UPDATE_BY_ADMIN);
        startActivity(intent);

    }



}