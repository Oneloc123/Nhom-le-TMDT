package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ChatbotService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Session-scoped chatbot API. Requests are URL-encoded form data; responses are JSON.
 * This first phase uses the centralized FAQ service and does not persist conversations.
 */
@WebServlet(name = "chatbotMessageServlet", value = "/api/chatbot/message")
public class ChatbotMessageServlet extends HttpServlet {
    private static final String CONVERSATION_ID = "pk.chatbot.conversationId";
    private static final String RESPONSE_CACHE = "pk.chatbot.responseCache";
    private static final String REQUEST_TIMES = "pk.chatbot.requestTimes";
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final int MAX_CACHED_RESPONSES = 30;
    private static final long RATE_WINDOW_MS = 60_000L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Allow", "POST");
        sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "method_not_allowed", "Chatbot chỉ nhận yêu cầu POST.", false, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        prepareResponse(response);

        String action = trim(request.getParameter("action"));
        if (action.isEmpty()) {
            action = "message";
        }

        HttpSession session = request.getSession(true);
        synchronized (session) {
            if ("new".equals(action)) {
                startNewConversation(session, request, response);
                return;
            }
            if (!"message".equals(action)) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "invalid_action", "Thao tác chatbot không hợp lệ.", false,
                        currentConversationId(session), null);
                return;
            }

            String requestId = trim(request.getParameter("requestId"));
            if (!requestId.matches("[A-Za-z0-9_-]{8,80}")) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "invalid_request_id", "Mã yêu cầu không hợp lệ. Vui lòng gửi lại câu hỏi.",
                        false, currentConversationId(session), null);
                return;
            }

            Map<String, Map<String, Object>> cache = responseCache(session);
            Map<String, Object> cachedResponse = cache.get(requestId);
            if (cachedResponse != null) {
                sendJson(response, HttpServletResponse.SC_OK, cachedResponse);
                return;
            }

            String question = trim(request.getParameter("question"));
            if (question.isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "empty_question", "Bạn hãy nhập câu hỏi trước khi gửi.", false,
                        currentConversationId(session), requestId);
                return;
            }
            if (question.length() > MAX_QUESTION_LENGTH) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "question_too_long", "Câu hỏi tối đa " + MAX_QUESTION_LENGTH + " ký tự.",
                        false, currentConversationId(session), requestId);
                return;
            }

            if (!allowRequest(session)) {
                sendError(response, 429,
                        "rate_limited", "Bạn gửi câu hỏi hơi nhanh. Vui lòng chờ một chút rồi thử lại.",
                        true, currentConversationId(session), requestId);
                return;
            }

            String conversationId = currentConversationId(session);
            if (conversationId == null) {
                conversationId = UUID.randomUUID().toString();
                session.setAttribute(CONVERSATION_ID, conversationId);
            }

            try {
                ChatbotService.Answer answer = ChatbotService.answer(question);
                Map<String, Object> payload = successPayload(conversationId, requestId, answer);
                cache.put(requestId, payload);
                trimCache(cache);
                sendJson(response, HttpServletResponse.SC_OK, payload);
            } catch (RuntimeException error) {
                System.getLogger(ChatbotMessageServlet.class.getName()).log(
                        System.Logger.Level.ERROR, "Chatbot request failed", error);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "chatbot_unavailable",
                        "Hiện chưa thể xử lý câu hỏi. Vui lòng thử lại sau.",
                        true, conversationId, requestId);
            }
        }
    }

    private static void startNewConversation(HttpSession session, HttpServletRequest request,
                                            HttpServletResponse response) throws IOException {
        String conversationId = UUID.randomUUID().toString();
        session.setAttribute(CONVERSATION_ID, conversationId);
        session.setAttribute(RESPONSE_CACHE, new LinkedHashMap<String, Map<String, Object>>());

        Map<String, Object> payload = successPayload(
                conversationId,
                trim(request.getParameter("requestId")),
                ChatbotService.answer("xin chao"));
        payload.put("event", "conversation_started");
        sendJson(response, HttpServletResponse.SC_OK, payload);
    }

    private static Map<String, Object> successPayload(String conversationId, String requestId,
                                                       ChatbotService.Answer answer) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId);
        message.put("role", "assistant");
        message.put("intent", answer.intent());
        message.put("text", answer.text());
        message.put("fallback", answer.fallback());

        List<Map<String, Object>> actions = new ArrayList<>();
        for (ChatbotService.Action action : answer.actions()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", action.label());
            item.put("path", action.path());
            actions.add(item);
        }
        message.put("actions", actions);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", "faq");
        meta.put("generatedAt", Instant.now().toString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("conversationId", conversationId);
        payload.put("requestId", requestId == null || requestId.isBlank() ? null : requestId);
        payload.put("message", message);
        payload.put("meta", meta);
        return payload;
    }

    private static boolean allowRequest(HttpSession session) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = requestTimes(session);
        timestamps.removeIf(timestamp -> timestamp == null || now - timestamp >= RATE_WINDOW_MS);
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        timestamps.add(now);
        return true;
    }

    private static Map<String, Map<String, Object>> responseCache(HttpSession session) {
        Object value = session.getAttribute(RESPONSE_CACHE);
        if (value instanceof Map<?, ?> existing) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> cache = (Map<String, Map<String, Object>>) existing;
            return cache;
        }
        Map<String, Map<String, Object>> cache = new LinkedHashMap<>();
        session.setAttribute(RESPONSE_CACHE, cache);
        return cache;
    }

    private static List<Long> requestTimes(HttpSession session) {
        Object value = session.getAttribute(REQUEST_TIMES);
        if (value instanceof List<?> existing) {
            @SuppressWarnings("unchecked")
            List<Long> timestamps = (List<Long>) existing;
            return timestamps;
        }
        List<Long> timestamps = new ArrayList<>();
        session.setAttribute(REQUEST_TIMES, timestamps);
        return timestamps;
    }

    private static void trimCache(Map<String, Map<String, Object>> cache) {
        while (cache.size() > MAX_CACHED_RESPONSES) {
            Iterator<String> iterator = cache.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static String currentConversationId(HttpSession session) {
        Object value = session.getAttribute(CONVERSATION_ID);
        return value instanceof String id && !id.isBlank() ? id : null;
    }

    private static void sendError(HttpServletResponse response, int status, String code,
                                  String message, boolean retryable, String conversationId,
                                  String requestId) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("retryable", retryable);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        if (conversationId != null) {
            payload.put("conversationId", conversationId);
        }
        if (requestId != null && !requestId.isBlank()) {
            payload.put("requestId", requestId);
        }
        payload.put("error", error);
        sendJson(response, status, payload);
    }

    private static void prepareResponse(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private static void sendJson(HttpServletResponse response, int status,
                                 Map<String, Object> payload) throws IOException {
        prepareResponse(response);
        response.setStatus(status);
        response.getWriter().write(Json.stringify(payload));
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
