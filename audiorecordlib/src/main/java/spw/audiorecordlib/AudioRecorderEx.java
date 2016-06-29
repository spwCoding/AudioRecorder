package spw.audiorecordlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import spw.audiorecordlib.util.FilePathUtil;
import spw.audiorecordlib.util.Logger;

@SuppressLint("NewApi")
public class AudioRecorderEx  {

    public static final String AUDIO_SUFFIX_WAV = ".wav";
    public static final String AUDIO_SUFFIX_MP3 = ".mp3";

    public static final String AUDIO_DIR_PATH = "audioRecorderEx";

    private List<String> mRecordFiles = new ArrayList<String>();
    private String savePath;

    private static Context mContext;

    private final static int[] sampleRates = {44100, 22050, 11025, 8000};

    public static AudioRecorderEx getInstance(Context context) {
        mContext = context;
        AudioRecorderEx result  = new AudioRecorderEx(AudioSource.MIC,
                sampleRates[3], AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        return result;
    }


    /**
     * INITIALIZING : recorder is initializing; READY : recorder has been
     * initialized, recorder not yet started RECORDING : recording ERROR :
     * reconstruction needed STOPPED: reset needed
     */
    public enum State {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED
    };



    //public static final boolean RECORDING_UNCOMPRESSED = true;

   // public static final boolean RECORDING_COMPRESSED = false;

    // The interval in which the recorded samples are output to the file
    // Used only in uncompressed mode
    private static final int TIMER_INTERVAL = 120;

    // Toggles uncompressed recording on/off; RECORDING_UNCOMPRESSED /
    // RECORDING_COMPRESSED
    //private boolean rUncompressed;

    // Recorder used for uncompressed recording
    private AudioRecord audioRecorder = null;


    // Stores current amplitude (only in uncompressed mode)
    private int cAmplitude = 0;

    // Output file path
    //private String filePath = null;

    // Recorder state; see State
    public State state;

    // File writer (only in uncompressed mode)
    private  RandomAccessFile randomAccessWriter;

    //private FileOutputStream rawFile;

    // Number of channels, sample rate, sample size(size in bits), buffer size,
    // audio source, sample size(see AudioFormat)
    private short nChannels;

    private int sRate;

    private short bSamples;

    private int bufferSize;

    private int aSource;

    private int aFormat;

    // Number of frames written to file on each output(only in uncompressed
    // mode)
    private int framePeriod;

    // Buffer for output(only in uncompressed mode)
    private byte[] buffer;

    // Number of bytes written to file after header(only in uncompressed mode)
    // after stop() is called, this size is written to the header/data chunk in
    // the wave file
    private int payloadSize;

    /**
     * Returns the state of the recorder in a RehearsalAudioRecord.State typed
     * object. Useful, as no exceptions are thrown.
     *
     * @return recorder state
     */
    public State getState() {
        return state;
    }


    private int lastCamplitude = 0;
    /*
     * Method used for recording.
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {

        public void onPeriodicNotification(AudioRecord recorder) {
            //Log.i("buffer", "start write time:" + System.currentTimeMillis());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    writeData();
                }
            }).start();

        }

        public void onMarkerReached(AudioRecord recorder) {
            // NOT USED
        }
    };

    private synchronized void writeData(){
        if(state== State.STOPPED){
            return;
            //stopRecording();
        }

        audioRecorder.read(buffer, 0, buffer.length); // Fill buffer
        //Toast.makeText(mContext, "writing", Toast.LENGTH_SHORT).show();
        try {
            //rawFile.write(buffer);
            randomAccessWriter.write(buffer); // Write buffer to file
            payloadSize += buffer.length;
            Logger.i("buffer size:" + buffer.length + " payloadSize:" + payloadSize);
            if (bSamples == 16) {
                //long len = 0;
                for (int i = 0; i < buffer.length / 2; i++) { // 16bit
                    // sample
                    // size
                    short curSample = getShort(buffer[i * 2],
                            buffer[i * 2 + 1]);
                    if (curSample > cAmplitude) { // Check amplitude
                        //cAmplitude = Math.abs(curSample);
                        cAmplitude = curSample;
                    }
                    //len += buffer[i] * buffer[i];
                }
                //cAmplitude = (int)(len / (double) r);
            } else { // 8bit sample size
                for (int i = 0; i < buffer.length; i++) {
                    if (buffer[i] > cAmplitude) { // Check amplitude
                        cAmplitude = buffer[i];
                    }
                }
            }
        } catch (Exception e) {
            Logger.e( "Error occured in updateListener, recording is aborted");
            //stop();
        }
    }

    /**
     * Default constructor
     * <p/>
     * Instantiates a new recorder, in case of compressed recording the
     * parameters can be left as 0. In case of errors, no exception is thrown,
     * but the state is set to ERROR
     */
    public AudioRecorderEx(int audioSource,
                           int sampleRate, int channelConfig, int audioFormat) {
        try {
            if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                bSamples = 16;
            } else {
                bSamples = 8;
            }

            if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
                nChannels = 1;
            } else {
                nChannels = 2;
            }

            aSource = audioSource;
            sRate = sampleRate;
            aFormat = audioFormat;

            framePeriod = sampleRate * TIMER_INTERVAL / 1000;
            bufferSize = framePeriod * 2 * bSamples * nChannels / 8;
            if (bufferSize < AudioRecord.getMinBufferSize(sampleRate,
                    channelConfig, audioFormat)) { // Check to make sure
                // buffer size is not
                // smaller than the
                // smallest allowed one
                bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                        channelConfig, audioFormat);
                // Set frame period and timer interval accordingly
                framePeriod = bufferSize / (2 * bSamples * nChannels / 8);
                Logger.i("Increasing buffer size to "
                        + Integer.toString(bufferSize));

            }

            audioRecorder = new AudioRecord(audioSource, sampleRate,
                    channelConfig, audioFormat, bufferSize);

            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                throw new Exception("AudioRecord initialization failed");
            audioRecorder.setRecordPositionUpdateListener(updateListener);
            audioRecorder.setPositionNotificationPeriod(framePeriod);


            cAmplitude = 0;
            savePath = null;
            state = State.INITIALIZING;
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Logger.e(e.getMessage());
            } else {
                Logger.e( "Unknown error occured while initializing recording");

            }
            state = State.ERROR;
        }
    }

    /**
     * Sets output file path, call directly after construction/reset.
     */
    public void setOutputFile(String argPath) {

        try {
            if (state == State.INITIALIZING) {
                this.savePath = argPath;
//                if (!rUncompressed) {
//                    mediaRecorder.setOutputFile(argPath);
//                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Logger.e(e.getMessage());
            } else {
                Logger.e("Unknown error occured while setting output path");
            }
            state = State.ERROR;
        }
    }

    /**
     * Returns the largest amplitude sampled since the last call to this method.
     *
     * @return returns the largest amplitude since the last call, or 0 when not
     * in recording state.
     */
    public int getMaxAmplitude() {
        if (state == State.RECORDING) {
            int result = cAmplitude;

            if (result == 0) {
                result = lastCamplitude;
            }
            //double volume = 10 * Math.log10(result);
            int volume = 0;
            if (result > 28000) {
                volume = result / 400;
            } else if (result <= 28000 && result > 2000) {
                volume = result / 327;
            } else if (result > 327 && result <= 2000) {
                volume = result / 150;
            } else {
                volume = result / 50;
            }
//				Log.e("amplitude","amplitude:" + result + " time:" + System.currentTimeMillis() + " show volume:" + volume);
            //Log.e("amplitude","amplitude:" + result + " time:" + System.currentTimeMillis() + " show volume:" + volume);
            lastCamplitude = cAmplitude;
            cAmplitude = 0;
            return volume;
        } else {
            return 0;
        }
    }

    /**
     * Prepares the recorder for recording, in case the recorder is not in the
     * INITIALIZING state and the file path was not set the recorder is set to
     * the ERROR state, which makes a reconstruction necessary. In case
     * uncompressed recording is toggled, the header of the wave file is
     * written. In case of an exception, the state is changed to ERROR
     */
    public void prepare() {
        try {
            if (state == State.INITIALIZING) {

                if ((audioRecorder.getState() == AudioRecord.STATE_INITIALIZED)
                        & (savePath != null)) {

                    randomAccessWriter = new RandomAccessFile(savePath,
                            "rw");

                    randomAccessWriter.setLength(0); // Set file length to
                    // 0, to prevent
                    // unexpected
                    // behavior
                    // in case the file
                    // already existed
                    randomAccessWriter.writeBytes("RIFF");
                    randomAccessWriter.writeInt(0); // Final file size not
                    // known yet, write 0
                    randomAccessWriter.writeBytes("WAVE");
                    randomAccessWriter.writeBytes("fmt ");
                    randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk
                    // size,
                    // 16
                    // for
                    // PCM
                    randomAccessWriter.writeShort(Short
                            .reverseBytes((short) 1)); // AudioFormat, 1 for
                    // PCM
                    randomAccessWriter.writeShort(Short
                            .reverseBytes(nChannels));// Number of channels,
                    // 1 for mono, 2 for
                    // stereo
                    randomAccessWriter
                            .writeInt(Integer.reverseBytes(sRate)); // Sample
                    // rate
                    randomAccessWriter.writeInt(Integer.reverseBytes(sRate
                            * bSamples * nChannels / 8)); // Byte rate,
                    // SampleRate*NumberOfChannels*BitsPerSample/8
                    randomAccessWriter
                            .writeShort(Short
                                    .reverseBytes((short) (nChannels
                                            * bSamples / 8))); // Block
                    // align,
                    // NumberOfChannels*BitsPerSample/8
                    randomAccessWriter.writeShort(Short
                            .reverseBytes(bSamples)); // Bits per sample
                    randomAccessWriter.writeBytes("data");
                    randomAccessWriter.writeInt(0); // Data chunk size not
                    // known yet, write 0


                    Logger.i("fileheader length:" + randomAccessWriter.length());
                    buffer = new byte[framePeriod * bSamples / 8
                            * nChannels];
                    state = State.READY;
                } else {
                    Logger.e("prepare() method called on uninitialized recorder");
                    state = State.ERROR;
                }
            } else {
                Logger.e(  "prepare() method called on illegal state");
                release();
                state = State.ERROR;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Logger.e(e.getMessage());
            } else {
                Logger.e("Unknown error occured in prepare()");
            }
            state = State.ERROR;
        }
    }

    /**
     * Releases the resources associated with this class, and removes the
     * unnecessary files, when necessary
     */
    public void release() {
        if (state == State.RECORDING) {
            stop();
        } else {
            if (state == State.READY) {
                try {
                    randomAccessWriter.close(); // Remove prepared file
                } catch (IOException e) {
                    Logger.e("I/O exception occured while closing output file");
                }
                (new File(savePath)).delete();
            }
        }

        if (audioRecorder != null) {
            audioRecorder.release();
        }
    }

    /**
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped. In
     * case of exceptions the class is set to the ERROR state.
     */
    public void reset() {
        try {
            if (state != State.ERROR) {
                //release();
                savePath = null; // Reset file path
                cAmplitude = 0; // Reset amplitude
                state = State.INITIALIZING;
            }
        } catch (Exception e) {
            Logger.e(e.getMessage());
            state = State.ERROR;
        }
    }

    /**
     * Starts the recording, and sets the state to RECORDING. Call after
     * prepare().
     */
    public void start() {
        if (state == State.READY) {
            payloadSize = 0;
            audioRecorder.startRecording();
            audioRecorder.read(buffer, 0, buffer.length);
            state = State.RECORDING;
            mRecordFiles.add(savePath);
        } else {
            Logger.e( "start() called on illegal state");

            state = State.ERROR;
        }
    }

    /**
     * Stops the recording, and sets the state to STOPPED. In case of further
     * usage, a reset is needed. Also finalizes the wave file in case of
     * uncompressed recording.
     */
    public synchronized void stop() {
        if (state == State.RECORDING) {
            audioRecorder.stop();
            Logger.i("payloadSize:" + payloadSize);
            try {
                randomAccessWriter.seek(4); // Write size to RIFF header
                randomAccessWriter.writeInt(Integer
                        .reverseBytes(36 + payloadSize));

                randomAccessWriter.seek(40); // Write size to Subchunk2Size
                // field
                randomAccessWriter.writeInt(Integer
                        .reverseBytes(payloadSize));

                randomAccessWriter.close();
            } catch (IOException e) {
                Logger.e(e);
                Logger.e("I/O exception occured while closing output file");

                state = State.ERROR;
            }
            state = State.STOPPED;
        } else {
            Logger.e( "stop() called on illegal state");
            state = State.ERROR;
        }
    }



    public void finishRecord() {
        if (state != State.RECORDING) {
            return;
        }
        stop();
    }

    /*
     *
     * Converts a byte[2] to a short, in LITTLE_ENDIAN format
     */
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }


    public File mergeAudioFile() {
        String path = FilePathUtil.makeFilePath(mContext, AUDIO_DIR_PATH, "merge" + System.currentTimeMillis()+AUDIO_SUFFIX_WAV);
        File mixFile = new File(path);
        if (mRecordFiles.size() == 0) {
            return mixFile;
        }
        if (mixFile.exists()) {
            RandomAccessFile dscAudioFile = null;
            try {
                dscAudioFile = new RandomAccessFile(path, "rw");
                for (String filePath : mRecordFiles) {
                    dscAudioFile.seek(mixFile.length());
                    RandomAccessFile itemFile = new RandomAccessFile(filePath, "rw");
                    itemFile.seek(44);
                    byte[] b = new byte[1024 * 5];
                    int len;
                    while ((len = itemFile.read(b)) != -1) {
                        dscAudioFile.write(b, 0, len);
                    }
                    dscAudioFile.seek(4);
                    dscAudioFile.writeInt(Integer.reverseBytes(36 /*+ ((int) itemFile.length()) - 44)*/ +
                            ((int) dscAudioFile.length()) - 44));
                    dscAudioFile.seek(40);
                    dscAudioFile.writeInt(Integer.reverseBytes(/*((int) itemFile.length()) - 44) +*/
                            ((int) dscAudioFile.length()) - 44));
                    itemFile.close();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    dscAudioFile.close();
                } catch (Exception e) {

                }
            }
        } else {
            try {
                //如果合成文件还不存在，则把第一个需要合并的文件重命名为要合并生成的文件
                File file = new File(mRecordFiles.get(0));
                if (!file.exists()) {
                    return mixFile;
                }
                file.renameTo(mixFile);

                //从第二个文件开始合并文件
                RandomAccessFile dscAudioFile = new RandomAccessFile(path, "rw");
                for (int i = 1; i < mRecordFiles.size(); i++) {
                    dscAudioFile.seek(dscAudioFile.length());
                    RandomAccessFile itemFile = new RandomAccessFile(mRecordFiles.get(i), "rw");
                    itemFile.seek(44);
                    byte[] b = new byte[1024 * 5];
                    int len;
                    while ((len = itemFile.read(b)) != -1) {
                        dscAudioFile.write(b, 0, len);
                    }
                    dscAudioFile.seek(4);
                    dscAudioFile.writeInt(Integer.reverseBytes(36/* + ((int) itemFile.length()) - 44)*/ +
                            ((int) dscAudioFile.length()) - 44));
                    dscAudioFile.seek(40);
                    dscAudioFile.writeInt(Integer.reverseBytes(/*((int) itemFile.length()) - 44) +*/
                            ((int) dscAudioFile.length()) - 44));
                    itemFile.close();
                }
                dscAudioFile.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        deleteListRecord();
        mRecordFiles.clear();
        if(mixFile.exists()){
            Logger.i("file size:" + mixFile.length());
        }
        return mixFile;
    }


    public String getMixAudioFilePath(String mixFileName) {
        return FilePathUtil.makeFilePath(mContext, AUDIO_DIR_PATH, mixFileName);
    }


    /**
     * 上传完成后删除本地录音文件
     *
     * @param path
     */
    public void deleteMixRecorderFile(String path) {
        File audioDir = new File(path);
        if (audioDir.exists() && audioDir.isDirectory()) {
            File[] files = audioDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (!files[i].getName().endsWith(AudioRecorderEx.AUDIO_SUFFIX_MP3)) {
                    files[i].delete();
                }
            }
        }
    }


    public void deleteRecorderFileUnLessSpec(String path,String specFilePath){
        File audioDir = new File(path);
        if (audioDir.exists() && audioDir.isDirectory()) {
            File[] files = audioDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (!files[i].getPath().equals(specFilePath)) {
                    files[i].delete();
                }
            }
        }
    }


    public void deleteListRecord() {
        for (String fileName : mRecordFiles) {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }
        mRecordFiles.clear();
    }

}
