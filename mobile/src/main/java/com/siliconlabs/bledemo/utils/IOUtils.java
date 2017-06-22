package com.siliconlabs.bledemo.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import timber.log.Timber;

public final class IOUtils {
    static final String TAG = "IOUtils";

    private static final ThreadLocal<ByteArrayOutputStream> sByteArrayStreamBuffer = new ThreadLocal<ByteArrayOutputStream>() {
        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(8192) {
                private final int MAX_LENGTH_BUFFER = 1024 * 1024;

                @Override
                public final synchronized void reset() {
                    shrinkBufferIfNecessary();
                    super.reset();
                }

                @Override
                public final void close() throws IOException {
                    shrinkBufferIfNecessary();
                    super.close();
                }

                private void shrinkBufferIfNecessary() {
                    if (buf.length >= MAX_LENGTH_BUFFER) {
                        buf = new byte[8192];
                        count = 0;
                    }
                }
            };
        }
    };

    private static final ThreadLocal<byte[]> sByteArrayBuffer = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[8192];
        }
    };

    private static final ThreadLocal<char[]> sCharArrayBuffer = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[8192];
        }
    };

    /**
     * If the file doesn't exist, it creates the specified file.
     * If the file does exists, it updates its last-modified time.
     * @param filePath
     */
    public static void touchFile(String filePath) {
        // File.setLastModified() may not work
        long time = System.currentTimeMillis();
        File file = new File(filePath);
        file.setLastModified(time);
        file = new File(filePath);
        if (file.lastModified() != time) {
            // https://code.google.com/p/android/issues/detail?id=18624#c5
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(filePath, "rw");
                long length = raf.length();
                raf.setLength(length + 1);
                raf.setLength(length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            closeStream(raf);
        }
    }

    /**
     * Copy the content of the input stream into the output stream.
     *
     * @param in  The input stream to copy from.
     * @param out The output stream to copy to.
     * @throws IOException If any error occurs during the copy.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = sByteArrayBuffer.get();
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    /**
     * Returns a String representation of the contents of an InputStream
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static String toString(InputStream is) throws IOException {
        final char[] buffer = sCharArrayBuffer.get();
        final Writer writer = new StringWriter();

        try {
            final Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }

            return writer.toString();
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns a String representation of the contents of an InputStream
     *
     * @param is
     * @param close
     * @return
     * @throws IOException
     */
    public static String toString(InputStream is, boolean close) throws IOException {
        try {
            return toString(is);
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        } finally {
            if (close) {
                closeStream(is);
            }
        }
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    public static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Timber.e(TAG, "Could not close stream", e);
                Log.e(TAG, "Could not close stream: " + e);
            }
        }
    }

    /**
     * Makes sure that media files (images/music/videos/etc) in the given parent-dir are not picked up
     * by the Media Scanner.
     *
     * @param parentDir The directory whose (grand)children should not be scanned by the media scanner.
     */
    public static void createNoMediaFile(File parentDir) {
        File noMedia = new File(parentDir, ".nomedia");
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Returns a byte-array representation of the given InputStream.
     * <p/>
     * The returned byte-array should never be cached, since it is local to the calling thread
     * and may be re-used for subsequent calls to this method or to {@link #toByteArray(InputStream, long)}.
     *
     * @param input
     * @return
     * @throws IOException
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = sByteArrayStreamBuffer.get();

        output.reset();
        copy(input, output);

        return output.toByteArray();
    }

    /**
     * Returns a *new* byte-array that represents the given InputStream.
     *
     * @param input       The InputStream
     * @param inputLength Must be the length of the given InputStream (in bytes).
     * @return
     * @throws IOException
     */
    public static byte[] toByteArray(InputStream input, long inputLength) throws IOException {
        final byte[] output = new byte[(int) inputLength];
        final byte[] buffer = sByteArrayBuffer.get();
        int read;
        int totalRead = 0;
        while ((read = input.read(buffer)) != -1) {
            System.arraycopy(buffer, 0, output, totalRead, read);
            totalRead += read;
        }
        return output;
    }
}
