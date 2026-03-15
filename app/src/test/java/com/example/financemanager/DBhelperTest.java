package com.example.financemanager;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

public class DBhelperTest extends TestCase {
    private DBhelper dbHelper;
    @Before  // Выполняется перед каждым тестом
    public void setUp() {
        dbHelper = new DBhelper(RuntimeEnvironment.application);
    }
    @Test
    public void testRegisterUser_Success() {
        boolean result = dbHelper.registerUser("test@example.com", "password");
        assertTrue("Регистрация должна быть успешной", result);
    }
}