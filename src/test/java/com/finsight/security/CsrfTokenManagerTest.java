package com.finsight.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CsrfTokenManager Security Tests")
public class CsrfTokenManagerTest {

    private static class MockSession implements HttpSession {
        private final Map<String, Object> attributes = new HashMap<>();

        @Override public Object getAttribute(String name) { return attributes.get(name); }
        @Override public void setAttribute(String name, Object value) { attributes.put(name, value); }
        @Override public void removeAttribute(String name) { attributes.remove(name); }
        @Override public long getCreationTime() { return 0; }
        @Override public String getId() { return "test-session"; }
        @Override public long getLastAccessedTime() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) {}
        @Override public int getMaxInactiveInterval() { return 0; }
        @Override public void invalidate() { attributes.clear(); }
        @Override public boolean isNew() { return false; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }
    }

    private static class TestRequest extends HttpServletRequestWrapper {
        private final HttpSession session;
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();

        public TestRequest(HttpSession session) {
            super(new DummyBaseRequest());
            this.session = session;
        }

        public void setParameter(String name, String value) {
            parameters.put(name, value);
        }

        public void setHeader(String name, String value) {
            headers.put(name, value);
        }

        @Override
        public HttpSession getSession(boolean create) {
            return session;
        }

        @Override
        public String getParameter(String name) {
            return parameters.get(name);
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }
    }

    private static class DummyBaseRequest implements HttpServletRequest {
        @Override public String getAuthType() { return null; }
        @Override public jakarta.servlet.http.Cookie[] getCookies() { return null; }
        @Override public long getDateHeader(String name) { return 0; }
        @Override public String getHeader(String name) { return null; }
        @Override public java.util.Enumeration<String> getHeaders(String name) { return null; }
        @Override public java.util.Enumeration<String> getHeaderNames() { return null; }
        @Override public int getIntHeader(String name) { return 0; }
        @Override public String getMethod() { return "POST"; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return ""; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getRequestURI() { return "/statement-analysis"; }
        @Override public StringBuffer getRequestURL() { return new StringBuffer(); }
        @Override public String getServletPath() { return "/statement-analysis"; }
        @Override public HttpSession getSession(boolean create) { return null; }
        @Override public HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) {}
        @Override public void logout() {}
        @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return null; }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return null; }
        @Override public String getCharacterEncoding() { return null; }
        @Override public void setCharacterEncoding(String env) {}
        @Override public int getContentLength() { return 0; }
        @Override public long getContentLengthLong() { return 0; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
        @Override public String getParameter(String name) { return null; }
        @Override public java.util.Enumeration<String> getParameterNames() { return null; }
        @Override public String[] getParameterValues(String name) { return null; }
        @Override public Map<String, String[]> getParameterMap() { return null; }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public String getScheme() { return "http"; }
        @Override public String getServerName() { return "localhost"; }
        @Override public int getServerPort() { return 8080; }
        @Override public java.io.BufferedReader getReader() { return null; }
        @Override public String getRemoteAddr() { return "127.0.0.1"; }
        @Override public String getRemoteHost() { return "localhost"; }
        @Override public void setAttribute(String name, Object o) {}
        @Override public void removeAttribute(String name) {}
        @Override public java.util.Locale getLocale() { return null; }
        @Override public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
        @Override public boolean isSecure() { return false; }
        @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public int getRemotePort() { return 0; }
        @Override public String getLocalName() { return null; }
        @Override public String getLocalAddr() { return null; }
        @Override public int getLocalPort() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
        @Override public String getRequestId() { return "req-1"; }
        @Override public String getProtocolRequestId() { return "proto-1"; }
        @Override public jakarta.servlet.ServletConnection getServletConnection() { return null; }
    }

    @Test
    @DisplayName("1. Valid token in request form parameter succeeds")
    void testValidTokenInFormParam() {
        MockSession session = new MockSession();
        String token = CsrfTokenManager.getOrCreateToken(session);
        assertNotNull(token);

        TestRequest req = new TestRequest(session);
        req.setParameter(CsrfTokenManager.CSRF_FORM_PARAM, token);

        assertTrue(CsrfTokenManager.isValidToken(req));
    }

    @Test
    @DisplayName("2. Valid token in request header succeeds")
    void testValidTokenInHeader() {
        MockSession session = new MockSession();
        String token = CsrfTokenManager.getOrCreateToken(session);

        TestRequest req = new TestRequest(session);
        req.setHeader(CsrfTokenManager.CSRF_HEADER_NAME, token);

        assertTrue(CsrfTokenManager.isValidToken(req));
    }

    @Test
    @DisplayName("3. Missing token in request is rejected")
    void testMissingTokenRejected() {
        MockSession session = new MockSession();
        CsrfTokenManager.getOrCreateToken(session);

        TestRequest req = new TestRequest(session);

        assertFalse(CsrfTokenManager.isValidToken(req));
    }

    @Test
    @DisplayName("4. Invalid or tampered token is rejected")
    void testInvalidTokenRejected() {
        MockSession session = new MockSession();
        CsrfTokenManager.getOrCreateToken(session);

        TestRequest req = new TestRequest(session);
        req.setParameter(CsrfTokenManager.CSRF_FORM_PARAM, "tampered-token-value-12345");

        assertFalse(CsrfTokenManager.isValidToken(req));
    }

    @Test
    @DisplayName("5. Token from another session is rejected")
    void testCrossSessionTokenRejected() {
        MockSession session1 = new MockSession();
        MockSession session2 = new MockSession();
        CsrfTokenManager.getOrCreateToken(session1);
        String token2 = CsrfTokenManager.getOrCreateToken(session2);

        TestRequest req = new TestRequest(session1);
        req.setParameter(CsrfTokenManager.CSRF_FORM_PARAM, token2); // Sending token2 to session1

        assertFalse(CsrfTokenManager.isValidToken(req));
    }

    @Test
    @DisplayName("6. Request with null session is rejected")
    void testNullSessionRejected() {
        TestRequest req = new TestRequest(null);
        assertFalse(CsrfTokenManager.isValidToken(req));
    }
}
