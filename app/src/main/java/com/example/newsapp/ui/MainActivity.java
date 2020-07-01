package com.example.newsapp.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.newsapp.R;
import com.example.newsapp.adapters.NewsAdapter;
import com.example.newsapp.api.ApiClient;
import com.example.newsapp.api.ApiInterface;
import com.example.newsapp.helper.NotificationService;
import com.example.newsapp.helper.Utils;
import com.example.newsapp.models.Article;
import com.example.newsapp.models.News;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = "MainActivity";
    public static final String API_KEY = "f20381f7e7394deea008a15bddb819fa";

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private List<Article> articles = new ArrayList<>();
    private NewsAdapter newsAdapter;
    private SwipeRefreshLayout swipe_refresh_layout;
    private TextView top_headlines;
    private ConstraintLayout error_layout;
    private ImageView error_image;
    private TextView error_title, error_message;
    private Button btn_retry;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipe_refresh_layout = findViewById(R.id.swipe_refresh_layout);
        swipe_refresh_layout.setOnRefreshListener(this);
        swipe_refresh_layout.setColorSchemeResources(R.color.colorAccent);

        top_headlines = findViewById(R.id.top_headlines);
        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);

        onLoadingSwipeRefresh("");

        error_layout = findViewById(R.id.error_layout);
        error_image = findViewById(R.id.error_image);
        error_title = findViewById(R.id.error_title);
        error_message = findViewById(R.id.error_message);
        btn_retry = findViewById(R.id.btn_retry);

    }

    public void loadJSON(final String keyword) {

        error_layout.setVisibility(View.GONE);

        swipe_refresh_layout.setRefreshing(true);

        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);

        String country = Utils.getCountry();
        String language = Utils.getLanguage();

        Call<News> call;

        if (keyword.length() > 0) {
            call = apiInterface.getNewsSearch(keyword, language, "publishedAt", API_KEY);
        } else {
            call = apiInterface.getNews(country, API_KEY);
        }

        call.enqueue(new Callback<News>() {
            @Override
            public void onResponse(Call<News> call, Response<News> response) {
                Log.i(TAG, "onResponse: " + call.request());
                if (response.isSuccessful() && (response.body().getArticle() != null)) {

                    if (!articles.isEmpty()) {
                        articles.clear();
                    }

                    articles = response.body().getArticle();
                    newsAdapter = new NewsAdapter(articles, MainActivity.this);
                    recyclerView.setAdapter(newsAdapter);
                    newsAdapter.notifyDataSetChanged();

                    initListener();

                    top_headlines.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                    swipe_refresh_layout.setRefreshing(false);
                } else {
                    top_headlines.setVisibility(View.INVISIBLE);
                    recyclerView.setVisibility(View.INVISIBLE);
                    swipe_refresh_layout.setRefreshing(false);

                    String error_code;
                    switch (response.code()) {
                        case 404:
                            error_code = "404 Not found";
                            break;
                        case 500:
                            error_code = "500 Server error";
                            break;
                        default:
                            error_code = "Unknown error";
                            break;
                    }

                    showErrorMessage(R.drawable.no_result,
                            "No result",
                            "Please try again! \n" +
                                    error_code);
                }
            }

            @Override
            public void onFailure(Call<News> call, Throwable t) {
                top_headlines.setVisibility(View.INVISIBLE);
                recyclerView.setVisibility(View.INVISIBLE);
                swipe_refresh_layout.setRefreshing(false);
                showErrorMessage(R.drawable.no_result,
                        "Oops...",
                        "Network failure\nPlease try again and check your internet");
            }
        });
    }

    private void initListener() {
        newsAdapter.setOnItemClickListener(new NewsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ImageView imageView = view.findViewById(R.id.img);
                Intent intent = new Intent(MainActivity.this, ReadNewsActivity.class);

                Article article = articles.get(position);
                intent.putExtra("url", article.getUrl());
                intent.putExtra("title", article.getTitle());
                intent.putExtra("img", article.getUrlToImage());
                intent.putExtra("date", article.getPublishedAt());
                intent.putExtra("source", article.getSource().getName());
                intent.putExtra("author", article.getAuthor());

                Pair<View, String> pair = Pair.create((View) imageView, ViewCompat.getTransitionName(imageView));
                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, pair);

                startActivity(intent, optionsCompat.toBundle());

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint("Search for Latest news...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() > 2) {
                    onLoadingSwipeRefresh(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchMenuItem.getIcon().setVisible(false, false);

        return true;
    }

    @Override
    public void onRefresh() {
        loadJSON("");
    }

    private void onLoadingSwipeRefresh(final String keyword) {
        swipe_refresh_layout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        loadJSON(keyword);
                    }
                }
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                //Do something
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void showErrorMessage(int imageView, String title, String message) {

        if (error_layout.getVisibility() == View.GONE) {
            error_layout.setVisibility(View.VISIBLE);
        }
        error_image.setImageResource(imageView);
        error_title.setText(title);
        error_message.setText(message);

        btn_retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoadingSwipeRefresh("");
            }
        });
    }

    //Notifications
    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                startService();
                handler.postDelayed(this, 60000);
            }
        };
        handler.post(runnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopService();
    }

    public void startService() {
        Intent serviceIntent = new Intent(getApplicationContext(), NotificationService.class);
        loadJSON("");
        startService(serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, NotificationService.class);
        stopService(serviceIntent);
    }

}

