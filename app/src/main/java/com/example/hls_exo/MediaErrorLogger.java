package com.example.hls_exo;

import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;

/**
 * Хелпер-класс для логирования ошибок воспроизведения медиа
 */
@OptIn(markerClass = UnstableApi.class)
public class MediaErrorLogger implements Player.Listener {
    
    private static final String TAG = "MediaErrorLogger";
    private final android.content.Context context;
    
    public MediaErrorLogger(android.content.Context context) {
        this.context = context;
    }
    
    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        // Логируем ошибку в консоль для дебаггинга
        Log.e(TAG, "Ошибка воспроизведения: " + error.getMessage(), error);

        // Получаем более детальное описание ошибки для отображения
        String errorMessage = getDetailedErrorMessage(error);
        
        // Показываем Toast с сообщением об ошибке
        Toast.makeText(context, "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Получает детальное описание ошибки на основе кода ошибки
     */
    private String getDetailedErrorMessage(PlaybackException error) {
        // Анализируем код ошибки для определения причины
        int errorCode = error.errorCode;
        
        switch (errorCode) {
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
                return "Проблема с сетевым подключением";
                
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT:
                return "Таймаут сетевого подключения";
                
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS:
                return "Неправильный HTTP статус";
            
            case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND:
                return "Файл не найден";
                
            case PlaybackException.ERROR_CODE_TIMEOUT:
                return "Таймаут операции";
            
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
                return "Ошибка инициализации декодера";
            
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED:
                return "Ошибка инициализации аудио-дорожки";
            
            case PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR:
                return "Ошибка с DRM-защищенным контентом";
            
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED:
                return "Некорректный формат контейнера";
            
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED:
                return "Некорректный формат манифеста";
                
            default:
                return "Неизвестная ошибка: " + error.getMessage();
        }
    }
    
    /**
     * Обработчик изменения состояния плеера для логирования
     */
    @Override
    public void onPlaybackStateChanged(int state) {
        switch (state) {
            case Player.STATE_IDLE:
                Log.d(TAG, "Состояние плеера: IDLE");
                break;
            case Player.STATE_BUFFERING:
                Log.d(TAG, "Состояние плеера: BUFFERING");
                break;
            case Player.STATE_READY:
                Log.d(TAG, "Состояние плеера: READY");
                break;
            case Player.STATE_ENDED:
                Log.d(TAG, "Состояние плеера: ENDED");
                break;
        }
    }
} 