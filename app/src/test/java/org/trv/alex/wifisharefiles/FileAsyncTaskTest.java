package org.trv.alex.wifisharefiles;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppPreferences.class)
public class FileAsyncTaskTest {

    @Test
    public void testSetNewFileName() {
        PowerMockito.mockStatic(AppPreferences.class);
        Context mockContext = mock(Context.class);
        FileAsyncTask testClass = new ReceiverFileAsyncTask(mockContext) {
            @Override
            protected void init() {
            }
        };
        testClass.setFileName("test.txt");
        assertEquals("test.txt", testClass.getFileName());
        for (int i = 1; i < 20; ++i) {
            testClass.setNewFileName();
            assertEquals(String.format("test(%d).txt", i), testClass.getFileName());
        }

        testClass.setFileName(".txt");
        assertEquals(".txt", testClass.getFileName());
        for (int i = 1; i < 20; ++i) {
            testClass.setNewFileName();
            assertEquals(String.format("(%d).txt", i), testClass.getFileName());
        }

        testClass.setFileName("test(1a)(25).txt");
        for (int i = 26; i < 50; ++i) {
            testClass.setNewFileName();
            assertEquals(String.format("test(1a)(%d).txt", i), testClass.getFileName());
        }

        testClass.setFileName("(25)test(25)(25).txt");
        for (int i = 26; i < 50; ++i) {
            testClass.setNewFileName();
            assertEquals(String.format("(25)test(25)(%d).txt", i), testClass.getFileName());
        }

        testClass.setFileName("test(01)");
        for (int i = 2; i < 20; ++i) {
            testClass.setNewFileName();
            assertEquals(String.format("test(%d)", i), testClass.getFileName());
        }

    }
}