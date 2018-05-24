package net.openhft.chronicle.queue.impl.single;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;

public class TestTailAfterRoll {


    public static final String EXPECTED = "hello world  3";
    @Rule
    public final TestName testName = new TestName();

    @NotNull
    protected File getTmpDir() {

        final String methodName = testName.getMethodName();
        return net.openhft.chronicle.queue.DirectoryUtils.tempDir(methodName != null ?
                methodName.replaceAll("[\\[\\]\\s]+", "_") : "NULL-" + UUID.randomUUID());
    }

    @Test
    public void test()  {
        File tmpDir = getTmpDir();
        File[] files;
        try (SingleChronicleQueue writeQ = SingleChronicleQueueBuilder.binary(tmpDir).build()) {
            ExcerptAppender appender = writeQ.acquireAppender();
            long wp;
            Wire wire;
            try (DocumentContext dc = appender.writingDocument()) {
                wire = dc.wire();
                wire.write().text("hello world");
                Bytes<?> bytes = wire.bytes();
                wp = bytes.writePosition();
            }


            File dir = new File(appender.queue().fileAbsolutePath());
            files = dir.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".cq4"));

            wire.bytes().writeInt(wp, Wires.END_OF_DATA);
            appender.writeText("hello world  2");
        }

        Assert.assertEquals(files.length, 1);
        File file = files[0];
        file.delete();

        try (SingleChronicleQueue q = SingleChronicleQueueBuilder.binary(tmpDir).build()) {
            ExcerptTailer excerptTailer = q.createTailer().toEnd();
            q.acquireAppender().writeText(EXPECTED);
            Assert.assertEquals(EXPECTED, excerptTailer.readText());
        }

    }


}
