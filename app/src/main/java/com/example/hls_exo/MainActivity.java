package com.example.hls_exo;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Context context;
    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    
    // Список URL из плейлиста Playlists.m3u8
    private final List<String> streamUrls = new ArrayList<>();
    private int currentStreamIndex = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        context = this;
        playerView = findViewById(R.id.play);
        
        // Копируем плейлист из assets и загружаем его
        copyPlaylistFileFromAssets();
        loadM3U8Playlist();
        
        // Инициализируем плеер с первым потоком из плейлиста
        if (streamUrls.size() > 0) {
            initializePlayer(streamUrls.get(currentStreamIndex));
        } else {
            Toast.makeText(this, "Не удалось загрузить потоки из плейлиста", Toast.LENGTH_LONG).show();
        }
    }
    
    private void copyPlaylistFileFromAssets() {
        AssetManager assetManager = getAssets();
        String filename = "Playlists.m3u8";
        File externalDir = getExternalFilesDir(null);
        File outFile = new File(externalDir, filename);
        
        try {
            InputStream in = assetManager.open(filename);
            OutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            in.close();
            out.flush();
            out.close();
            
            Log.d(TAG, "Файл плейлиста скопирован в: " + outFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при копировании файла плейлиста: " + e.getMessage());
        }
    }
    
    private void loadM3U8Playlist() {
        streamUrls.clear(); // Очищаем список перед загрузкой
        
        // По умолчанию, если не удастся загрузить плейлист
        streamUrls.add("http://stream02.vnet.am/Channel_131/mono.m3u8");
        streamUrls.add("http://dmi3y-tv.ru/hls/CH_FASTSPORTSHD.m3u8");
        streamUrls.add("http://181.78.105.146:2000/play/a017/index.m3u8");
        
        try {
            // Попытка загрузить плейлист из файла в каталоге приложения
            String playlistPath = getExternalFilesDir(null) + "/Playlists.m3u8";
            File playlistFile = new File(playlistPath);
            
            if (playlistFile.exists()) {
                streamUrls.clear(); // Очищаем предварительные URL
                
                InputStream inputStream = new FileInputStream(playlistFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                
                while ((line = reader.readLine()) != null) {
                    // Пропускаем комментарии и директивы
                    if (!line.startsWith("#") && !line.trim().isEmpty()) {
                        streamUrls.add(line.trim());
                        Log.d(TAG, "Загружен URL из плейлиста: " + line.trim());
                    }
                }
                
                reader.close();
                inputStream.close();
            } else {
                Log.e(TAG, "Файл плейлиста не найден по пути: " + playlistPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке плейлиста: " + e.getMessage());
        }
        
        Log.d(TAG, "Загружено " + streamUrls.size() + " потоков из плейлиста");
    }
    
    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(String streamUrl) {
        if (player != null) {
            releasePlayer();
        }
        
        try {
            Log.d(TAG, "Начинаем инициализацию плеера для URL: " + streamUrl);
            
            // Настройки трек-селектора для выбора лучшего качества
            trackSelector = new DefaultTrackSelector(this);
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setMaxVideoSizeSd() // Начать с SD качества для быстрой буферизации
                    .setPreferredAudioLanguage("rus") // Предпочитать русский язык
                    .setForceHighestSupportedBitrate(false) // Не принуждать к высокому битрейту
            );
            
            // Создаем фабрику рендереров со стандартными настройками
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true); // Разрешаем запасной декодер
            
            // Создаем плеер с нашей фабрикой рендереров и трек-селектором
            player = new ExoPlayer.Builder(this)
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector)
                .build();
            
            // Добавляем слушатель ошибок
            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "Ошибка воспроизведения: " + error.getMessage());
                    Toast.makeText(MainActivity.this, 
                        "Ошибка воспроизведения: " + error.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    
                    // Пробуем следующий поток из плейлиста
                    tryNextStream();
                }
                
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        Log.d(TAG, "Плеер готов");
                        // После успешной подготовки, переключаем на высокое качество
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .clearVideoSizeConstraints()
                                .setForceHighestSupportedBitrate(true)
                        );
                    } else if (state == Player.STATE_BUFFERING) {
                        Log.d(TAG, "Буферизация...");
                    } else if (state == Player.STATE_ENDED) {
                        Log.d(TAG, "Воспроизведение завершено");
                    } else if (state == Player.STATE_IDLE) {
                        Log.d(TAG, "Плеер в режиме ожидания");
                    }
                }
                
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    Log.d(TAG, "Состояние воспроизведения изменилось: " + (isPlaying ? "играет" : "не играет"));
                }
            });
            
            playerView.setPlayer(player);
            
            // Добавляем установку режима заполнения для видео
            playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL); // Заполняем весь экран
            playerView.setKeepContentOnPlayerReset(true); // Сохраняем контент при сбросе плеера
            playerView.setControllerAutoShow(true); // Автопоказ управления
            playerView.setUseController(true); // Включаем контроллеры
            
            // Настраиваем источник данных и медиа-источник
            Uri videoUri = Uri.parse(streamUrl);
            
            // Создаем метаданные для более точного определения типа медиа
            MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                .setTitle("Live Stream")
                .setGenre("Sport")
                .build();
            
            // Создаем медиа айтем с метаданными
            MediaItem mediaItem = new MediaItem.Builder()
                .setUri(videoUri)
                .setMediaMetadata(mediaMetadata)
                .setMimeType(MimeTypes.APPLICATION_M3U8) // Явно указываем MIME тип для HLS
                .build();
            
            // Настройка DataSource с явной поддержкой HTTP перенаправлений
            DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setUserAgent("ExoPlayerDemo/1.0");
                
            // Используем DefaultDataSource для поддержки всех протоколов
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                context,
                httpDataSourceFactory
            );
            
            // Создаем медиа источник с поддержкой HLS
            HlsMediaSource.Factory hlsFactory = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true);
                
            HlsMediaSource hlsMediaSource = hlsFactory.createMediaSource(mediaItem);
            
            // Устанавливаем источник медиа
            player.setMediaSource(hlsMediaSource);
            player.prepare();
            player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
            player.setPlayWhenReady(true);
            
            // Громкость на максимум
            player.setVolume(1.0f);
            
            // Логирование для отладки
            Log.d(TAG, "Инициализация плеера с URL: " + streamUrl);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации плеера: " + e.getMessage());
            e.printStackTrace(); // Добавляем стек-трейс для лучшей диагностики
            Toast.makeText(this, "Ошибка инициализации плеера: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // Пробуем следующий поток из плейлиста
            tryNextStream();
        }
    }
    
    // Пробуем следующий поток из плейлиста
    private void tryNextStream() {
        if (streamUrls.size() <= 1) return;
        
        currentStreamIndex = (currentStreamIndex + 1) % streamUrls.size();
        Log.d(TAG, "Переключение на следующий поток #" + currentStreamIndex + ": " + streamUrls.get(currentStreamIndex));
        initializePlayer(streamUrls.get(currentStreamIndex));
    }
    
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (trackSelector != null) {
            trackSelector = null;
        }
    }
    
    @Override
    public void onBackPressed() {
        releasePlayer();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}