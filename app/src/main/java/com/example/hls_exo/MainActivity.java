package com.example.hls_exo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.metadata.MetadataOutput;
import androidx.media3.exoplayer.text.TextOutput;
import androidx.media3.exoplayer.video.VideoRendererEventListener;

import java.util.ArrayList;

@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private Context context;
    private PlayerView playerView;
    private ExoPlayer player;
    private int currentStreamIndex = 0;
    private TextView tvCurrentStream;
    private String[] streamUrls = {
            "http://stream02.vnet.am/Channel_131/mono.m3u8",
            "http://dmi3y-tv.ru/hls/CH_FASTSPORTSHD.m3u8",
            "http://181.78.105.146:2000/play/a017/index.m3u8",
            "http://hls.shansontv.cdnvideo.ru/shansontv/shansontv.sdp/playlist.m3u8"
    };
    
    private String[] streamNames = {
            "Fast Sports 1",
            "Fast Sports 2",
            "History",
            "Шансон ТВ"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        context = this;
        playerView = findViewById(R.id.play);
        tvCurrentStream = findViewById(R.id.tvCurrentStream);
        
        // Обновляем информацию о текущем потоке
        updateStreamInfo();
        
        // Инициализируем кнопку переключения потоков
        findViewById(R.id.btnSwitchStream).setOnClickListener(v -> switchStream());
        
        // Проверяем разрешения перед инициализацией плеера
        checkPermissionsAndInitialize();
    }
    
    private void checkPermissionsAndInitialize() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // Если разрешения нет, запрашиваем его
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}, 
                    PERMISSION_REQUEST_CODE);
        } else {
            // Если разрешение уже есть, инициализируем плеер
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(this::initializePlayer, 1000);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, инициализируем плеер
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(this::initializePlayer, 1000);
            } else {
                // Разрешение не получено, показываем сообщение
                Toast.makeText(this, "Для работы приложения необходим доступ к Интернету", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
        // Создаем фабрику рендереров с поддержкой FFmpeg
        RenderersFactory renderersFactory = new RenderersFactory() {
            @Override
            public Renderer[] createRenderers(
                    Handler eventHandler,
                    VideoRendererEventListener videoRendererEventListener,
                    AudioRendererEventListener audioRendererEventListener,
                    TextOutput textRendererOutput,
                    MetadataOutput metadataRendererOutput) {
                
                ArrayList<Renderer> renderersList = new ArrayList<>();
                
                // Добавляем стандартные рендереры от DefaultRenderersFactory
                Renderer[] renderers = new DefaultRenderersFactory(context).createRenderers(
                        eventHandler,
                        videoRendererEventListener,
                        audioRendererEventListener,
                        textRendererOutput,
                        metadataRendererOutput);
                
                for (Renderer renderer : renderers) {
                    renderersList.add(renderer);
                }
                
                // Добавляем FFmpeg аудио рендерер с приоритетом выше стандартного
                AudioSink audioSink = new DefaultAudioSink.Builder()
                        .setEnableFloatOutput(true)
                        .setEnableAudioTrackPlaybackParams(true)
                        .build();
                
                renderersList.add(new FfmpegAudioRenderer(
                        eventHandler,
                        audioRendererEventListener,
                        audioSink));
                
                return renderersList.toArray(new Renderer[0]);
            }
        };
        
        // Создаем плеер с кастомной фабрикой рендереров
        player = new ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(new HlsMediaSource.Factory(new DefaultHttpDataSource.Factory()))
                .build();
        
        // Добавляем логгер ошибок
        MediaErrorLogger errorLogger = new MediaErrorLogger(this);
        player.addListener(errorLogger);
                
        playerView.setPlayer(player);
        
        // Загружаем текущий поток
        playStream(currentStreamIndex);
    }
    
    private void playStream(int streamIndex) {
        if (player == null) return;
        
        Uri videoUri = Uri.parse(streamUrls[streamIndex]);
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000);
                
        MediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(videoUri));
                
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
    }
    
    // Метод для переключения между потоками
    public void switchStream() {
        currentStreamIndex = (currentStreamIndex + 1) % streamUrls.length;
        playStream(currentStreamIndex);
        updateStreamInfo();
    }
    
    // Обновление информации о текущем потоке
    private void updateStreamInfo() {
        tvCurrentStream.setText("Текущий поток: " + streamNames[currentStreamIndex]);
    }

    @Override
    public void onBackPressed() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
        } else {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(this::initializePlayer, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}







