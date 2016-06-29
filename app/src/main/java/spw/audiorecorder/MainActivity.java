package spw.audiorecorder;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import spw.audiorecordlib.AudioRecorderEx;
import spw.audiorecordlib.util.FilePathUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button startRecord,stopRecord,mergeRecord;
    AudioRecorderEx audioRecorder;

    //MediaPlayer mMediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        audioRecorder =AudioRecorderEx.getInstance(MainActivity.this);

        startRecord = (Button)findViewById(R.id.start_record);
        stopRecord = (Button)findViewById(R.id.stop_record);
        mergeRecord = (Button)findViewById(R.id.merge_record);

        stopRecord.setEnabled(false);
        mergeRecord.setEnabled(false);

        startRecord.setOnClickListener(this);
        stopRecord.setOnClickListener(this);



//        findViewById(R.id.play_audio).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_record:
                //AudioRecorderEx.getInstance(MainActivity.this);
                audioRecorder.setOutputFile(FilePathUtil.makeFilePath(MainActivity.this,AudioRecorderEx.AUDIO_DIR_PATH, System.currentTimeMillis() + AudioRecorderEx.AUDIO_SUFFIX_WAV));
                audioRecorder.prepare();
                audioRecorder.start();

                startRecord.setText("正在录音");
                startRecord.setEnabled(false);
                stopRecord.setEnabled(true);
                mergeRecord.setEnabled(false);
                break;
            case R.id.stop_record:
                audioRecorder.stop();
                stopRecord.setEnabled(false);
                startRecord.setEnabled(true);
                mergeRecord.setEnabled(true);
                break;
            case R.id.merge_record:
                File file = audioRecorder.mergeAudioFile();
                if(file!=null && file.exists()){
                    Toast.makeText(MainActivity.this,"合并完成",Toast.LENGTH_SHORT).show();
                }
                break;


        }
    }

//    private void initMediaPlayer(){
//        mMediaPlayer = new MediaPlayer();
//        mMediaPlayer.set
//    }
}
