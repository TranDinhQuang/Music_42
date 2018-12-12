package com.framgia.quangtran.music_42.ui.genre;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.framgia.quangtran.music_42.R;
import com.framgia.quangtran.music_42.data.model.Genre;
import com.framgia.quangtran.music_42.data.model.Track;
import com.framgia.quangtran.music_42.data.repository.TrackRepository;
import com.framgia.quangtran.music_42.data.source.local.TrackLocalDataSource;
import com.framgia.quangtran.music_42.data.source.remote.TrackRemoteDataSource;
import com.framgia.quangtran.music_42.mediaplayer.ITracksPlayerManager;
import com.framgia.quangtran.music_42.service.TracksService;
import com.framgia.quangtran.music_42.service.TracksServiceManager;
import com.framgia.quangtran.music_42.ui.LoadMoreAbstract;
import com.framgia.quangtran.music_42.ui.UIPlayerListener;
import com.framgia.quangtran.music_42.ui.play.PlayActivity;
import com.framgia.quangtran.music_42.ui.search.SearchActivity;
import com.framgia.quangtran.music_42.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class GenreActivity extends LoadMoreAbstract implements GenreContract.View,
        SwipeRefreshLayout.OnRefreshListener, GenreAdapter.OnClickItemListener,
        ServiceConnection, View.OnClickListener, UIPlayerListener.ControlListener {
    private static final String EXIST_TRACK = "Exist track in favorite";
    private static final String BUNDLE_GENRE = "com.framgia.quangtran.music_42.ui.mGenre.BUNDLE_GENRE";
    public static final int LIMIT = 10;
    private int mOffset;
    private ConstraintLayout mMiniPlayer;
    private TracksServiceManager mTracksServiceManager;
    private TracksService mService;
    private GenrePresenter mGenrePresenter;
    private ContentResolver mContentResolverCursor;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Toolbar mToolbar;
    private GenreAdapter mAdapter;
    private String mApi;
    private Genre mGenre;
    private Track mTrack;
    private ImageView mImageGenre;
    private ImageView mImageSearch;
    private ImageView mImageTrack;
    private TextView mTrackName;
    private TextView mSingerName;
    private Button mButtonGenre;
    private Button mButtonNextSong;
    private Button mButtonPreviousSong;
    private Button mButtonPlaySong;

    public static Intent getGenreIntent(Context context, Genre genre) {
        Intent intent = new Intent(context, GenreActivity.class);
        intent.putExtra(BUNDLE_GENRE, genre);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre);
        initUI();
        connectService();
        initViewLoadMore();
        loadMusic();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mService.getStateMedia() != ITracksPlayerManager.StatePlayerType.PLAYING) {
            mButtonPlaySong.setBackgroundResource(R.drawable.ic_play_button);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mService = ((TracksService.LocalBinder) iBinder).getService();
        mService.addControlListener(this);
        if (mService.getStateMiniPlayer() == true) {
            mMiniPlayer.setVisibility(View.VISIBLE);
            setItemMiniPlayer(mService.getTrack());
        }
        if (mService.getStateMedia() != ITracksPlayerManager.StatePlayerType.PLAYING) {
            mButtonPlaySong.setBackgroundResource(R.drawable.ic_play_button);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    public void onSuccess(List<Track> tracks) {
        if (mOffset == 0) {
            mAdapter.updateTracks(tracks);
            mSwipeRefreshLayout.setRefreshing(false);
        } else {
            mAdapter.addTracks(tracks);
        }
    }

    @Override
    public void addFavoriteTrackSuccess() {
        Toast.makeText(this, R.string.text_add_success, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void deleteFavoriteTrackSuccess() {
        Toast.makeText(this, R.string.text_delete_favorite, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFailure(String message) {
        if (message.equals(EXIST_TRACK)) {
            mGenrePresenter.deleteFavoriteTrack(mTrack);
        }
    }

    @Override
    public void loadMoreData() {
        mIsScrolling = false;
        String api = StringUtil.genreApi(mGenre.getKey(), LIMIT, ++mOffset);
        mGenrePresenter.getGenres(api);
    }

    @Override
    public void initViewLoadMore() {
        mRecyclerView = findViewById(R.id.recycler_genre);
        List<Track> mTracks = new ArrayList<>();
        mAdapter = new GenreAdapter(mTracks, this);
        mRecyclerView.setAdapter(mAdapter);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        setLoadMore();
    }


    @Override
    public void onRefresh() {
        if (mGenre.getKey() != null && !mGenre.getKey().isEmpty()) {
            mOffset = 0;
            String api = StringUtil.genreApi(mGenre.getKey(), LIMIT, mOffset);
            mGenrePresenter.getGenres(api);
        }
    }

    @Override
    public void clickViewTrack(int i, List<Track> tracks) {
        mService.setTracks(tracks);
        mService.setPositionTrack(i);
        mService.play(i);
        if (mMiniPlayer.getVisibility() == View.GONE) {
            mMiniPlayer.setVisibility(View.VISIBLE);
            startActivity(PlayActivity.getPlayIntent(this));
        }
    }

    @Override
    public void clickFavorite(Track track, ImageView imageFavorite) {
        mTrack = track;
        mGenrePresenter.addFavoriteTrack(track);
    }

    @Override
    public void clickDownload(Track track, ImageView imageDownload) {
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.image_avatar:
                startActivity(PlayActivity.getPlayIntent(this));
                break;
            case R.id.button_back:
                mService.previous();
                mButtonPlaySong.setBackgroundResource(R.drawable.ic_pause);
                break;
            case R.id.button_play:
                setTrackPlayPause();
                break;
            case R.id.button_next:
                mService.next();
                mButtonPlaySong.setBackgroundResource(R.drawable.ic_pause);
                break;
            case R.id.image_search:
                startActivity(SearchActivity.getSearchIntent(this));
                break;
            default:
                break;
        }
    }

    @Override
    public void notifyShuffleChanged(int shuffleType) {
    }

    @Override
    public void notifyLoopChanged(int loopType) {
    }

    @Override
    public void notifyStateChanged(Track track, int stateType) {
    }

    private void connectService() {
        ServiceConnection connection = this;
        Intent intent = new Intent(this, TracksService.class);
        mTracksServiceManager = new TracksServiceManager(this, intent, connection,
                Context.BIND_AUTO_CREATE);
        mTracksServiceManager.bindService();
    }

    private void setItemMiniPlayer(Track track) {
        Glide.with(getApplicationContext())
                .load(track.getArtWorkUrl())
                .apply(new RequestOptions()
                        .placeholder(R.drawable.soundcloud)
                        .error(R.drawable.soundcloud))
                .into(mImageTrack);
        mTrackName.setText(track.getTitle());
        mSingerName.setText(track.getUserName());
        if (mService.getStateMedia() != ITracksPlayerManager.StatePlayerType.PLAYING) {
            mButtonPlaySong.setBackgroundResource(R.drawable.ic_play_button);
        }
    }

    private void loadMusic() {
        mGenre = getIntent().getParcelableExtra(BUNDLE_GENRE);
        mButtonGenre.setText(mGenre.getName());
        mImageGenre.setImageResource(mGenre.getImageUrl());
        mApi = StringUtil.genreApi(mGenre.getKey(), LIMIT, mOffset);
        mGenrePresenter.getGenres(mApi);
    }

    private void initUI() {
        mTrack = new Track();
        mMiniPlayer = findViewById(R.id.mini_player);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh);
        mToolbar = findViewById(R.id.tool_bar);
        mImageSearch = findViewById(R.id.image_search);
        mImageGenre = findViewById(R.id.image_genre);
        mImageTrack = findViewById(R.id.image_avatar);
        mButtonGenre = findViewById(R.id.button_genre);
        mTrackName = findViewById(R.id.text_song_name);
        mSingerName = findViewById(R.id.text_singer_name);
        mButtonNextSong = findViewById(R.id.button_next);
        mButtonPlaySong = findViewById(R.id.button_play);
        mButtonPreviousSong = findViewById(R.id.button_back);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        initPresenter();
        setOnclickListener();
    }

    private void setOnclickListener() {
        setOnClickToolBar();
        mImageTrack.setOnClickListener(this);
        mButtonNextSong.setOnClickListener(this);
        mButtonPreviousSong.setOnClickListener(this);
        mButtonPlaySong.setOnClickListener(this);
        mImageSearch.setOnClickListener(this);
    }

    private void initPresenter() {
        mContentResolverCursor = getApplicationContext().getContentResolver();
        TrackRepository repository = TrackRepository.getInstance(TrackRemoteDataSource
                .getInstance(), TrackLocalDataSource.getInstance(getApplicationContext(),
                mContentResolverCursor));
        mGenrePresenter = new GenrePresenter(repository);
        mGenrePresenter.setView(this);
    }

    private void setTrackPlayPause() {
        if (mService.getStateMedia() == ITracksPlayerManager.StatePlayerType.PLAYING) {
            mService.pause();
            mButtonPlaySong.setBackgroundResource(R.drawable.ic_play_button);
        } else {
            mService.start();
            mButtonPlaySong.setBackgroundResource(R.drawable.ic_pause);
        }
    }

    private void setOnClickToolBar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
}
