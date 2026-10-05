package io.sala.krob_krong.common.filters;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class BoundedBodyCaptureResponseWrapper extends HttpServletResponseWrapper {

    private final int captureLimit;
    private ByteArrayOutputStream capture;
    private boolean overflowed;
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    BoundedBodyCaptureResponseWrapper(HttpServletResponse response, int captureLimit) {
        super(response);
        this.captureLimit = Math.max(0, captureLimit);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        if (outputStream == null) {
            outputStream = new TeeServletOutputStream(getResponse().getOutputStream());
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called");
        }
        if (writer == null) {
            ServletOutputStream tee = new TeeServletOutputStream(getResponse().getOutputStream());
            writer = new PrintWriter(new OutputStreamWriter(tee, charset()), false);
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else if (outputStream != null) {
            outputStream.flush();
        }
        super.flushBuffer();
    }

    /** Captured body prefix as a string, or {@code ""} when nothing was captured. */
    String capturedBody() {
        if (writer != null) {
            writer.flush();
        }
        if (capture == null || capture.size() == 0) {
            return "";
        }
        String body = capture.toString(charset());
        return overflowed ? body + "...[truncated]" : body;
    }

    private Charset charset() {
        String encoding = getCharacterEncoding();
        if (encoding != null) {
            try {
                return Charset.forName(encoding);
            } catch (Exception _) {
                // fall through to UTF-8
            }
        }
        return StandardCharsets.UTF_8;
    }

    private final class TeeServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        TeeServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            captureByte(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            captureBytes(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            delegate.setWriteListener(listener);
        }

        private void captureByte(int b) {
            if (captureLimit == 0) return;
            ByteArrayOutputStream buf = captureBuffer();
            if (buf.size() < captureLimit) {
                buf.write(b);
            } else {
                overflowed = true;
            }
        }

        private void captureBytes(byte[] b, int off, int len) {
            if (captureLimit == 0) return;
            ByteArrayOutputStream buf = captureBuffer();
            int room = captureLimit - buf.size();
            if (room <= 0) {
                overflowed = true;
                return;
            }
            int toCopy = Math.min(room, len);
            buf.write(b, off, toCopy);
            if (toCopy < len) {
                overflowed = true;
            }
        }

        private ByteArrayOutputStream captureBuffer() {
            if (capture == null) {
                capture = new ByteArrayOutputStream(Math.min(captureLimit, 1024));
            }
            return capture;
        }
    }
}
