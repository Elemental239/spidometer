package com.application.elemental.spidometer;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger implements ILogger {
    private PrintWriter m_writer = null;
    private Context m_context = null;

    FileLogger(Context context) {
        m_context = context;
    }

    public void StartNewFile() throws IOException {
        if (!isExternalStorageWritable())
        {
            throw new IOException("External storage not available");
        }

        Date now = new Date();
        String logName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(now) + ".txt";
        File logsFile = new File(m_context.getExternalFilesDir(null), logName);

        if (!logsFile.exists())
        {
            logsFile.createNewFile();
        }

        m_writer = new PrintWriter(new BufferedWriter(new FileWriter(logsFile, true)));

        i("===Log start===");
    }

    public void ClearLogsDirectory()
    {
        File logsDir = m_context.getExternalFilesDir(null);

        for(File file: logsDir.listFiles())
            if (!file.isDirectory())
                file.delete();
    }

//    public void StartNewFile()
//    {
//        Close();
//
//        Date now = new Date();
//        String logName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(now) + ".txt";
//        File logsFile = new File(m_context.getExternalFilesDir(null), logName);
//
//        try {
//            if (!logsFile.exists()) {
//                logsFile.createNewFile();
//            }
//
//            m_writer = new PrintWriter(new BufferedWriter(new FileWriter(logsFile, true)));
//            i("===Log start===");
//        } catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//    }

    public void Close()
    {
        i("===Log finish===");
        m_writer.close();
        m_writer = null;
    }

    @Override
    public void i(String log) {
        if (m_writer == null)
        {
            return;
        }

        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSZ", Locale.US).format(new Date());

        m_writer.println(time + " " + log);
        m_writer.flush();
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
