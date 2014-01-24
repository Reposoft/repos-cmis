/**
 * Copyright (C) Repos Mjukvara AB
 */
package se.repos.cmis;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;

/**
 * InputStream that can limit and/or skip over data in the underlying input stream.
 * Used to implement length and offset in the getContentStream operation in
 * {@link ReposCmisRepository}.
 */
public class ContentRangeInputStream extends FilterInputStream {

    private static final int BUFFER_SIZE = 4096;

    private long offset;
    private long length;
    private long remaining;

    public ContentRangeInputStream(InputStream stream, BigInteger offset,
            BigInteger length) {
        super(stream);

        this.offset = offset != null ? offset.longValue() : 0;
        this.length = length != null ? length.longValue() : Long.MAX_VALUE;

        this.remaining = this.length;

        if (this.offset > 0) {
            this.skipBytes();
        }
    }

    private void skipBytes() {
        long remainingSkipBytes = this.offset;

        try {
            while (remainingSkipBytes > 0) {
                long skipped = super.skip(remainingSkipBytes);
                remainingSkipBytes -= skipped;

                if (skipped == 0) {
                    // stream might not support skipping
                    this.skipBytesByReading(remainingSkipBytes);
                    break;
                }
            }
        } catch (IOException e) {
            throw new CmisRuntimeException("Skipping the stream failed!", e);
        }
    }

    private void skipBytesByReading(long remainingSkipBytes) {
        long remainingBytes = remainingSkipBytes;
        try {
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (remainingBytes > 0) {
                long skipped = super.read(buffer, 0,
                        (int) Math.min(buffer.length, remainingBytes));
                if (skipped == -1) {
                    break;
                }

                remainingBytes -= skipped;
            }
        } catch (IOException e) {
            throw new CmisRuntimeException("Reading the stream failed!", e);
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long skip(long n) throws IOException {
        if (this.remaining <= 0) {
            return 0;
        }

        long skipped = super.skip(n);
        this.remaining -= skipped;

        return skipped;
    }

    @Override
    public int available() throws IOException {
        if (this.remaining <= 0) {
            return 0;
        }

        int avail = super.available();

        if (this.remaining < avail) {
            return (int) this.remaining;
        }

        return avail;
    }

    @Override
    public int read() throws IOException {
        if (this.remaining <= 0) {
            return -1;
        }

        this.remaining--;

        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.remaining <= 0) {
            return -1;
        }

        int readBytes = super.read(b, off, (int) Math.min(len, this.remaining));
        if (readBytes == -1) {
            return -1;
        }

        this.remaining -= readBytes;

        return readBytes;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }
}
